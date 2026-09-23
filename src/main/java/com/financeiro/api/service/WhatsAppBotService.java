package com.financeiro.api.service;

import com.financeiro.api.dto.expense.ExpenseRequest;
import com.financeiro.api.entity.Category;
import com.financeiro.api.entity.Tab;
import com.financeiro.api.entity.User;
import com.financeiro.api.entity.WhatsAppPendingExpense;
import com.financeiro.api.repository.CategoryRepository;
import com.financeiro.api.repository.TabRepository;
import com.financeiro.api.repository.UserRepository;
import com.financeiro.api.repository.WhatsAppPendingExpenseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lógica do bot de gastos por WhatsApp: reconhece o número pelo telefone cadastrado em
 * Configurações, tenta extrair valor + descrição da mensagem, casa com uma categoria existente (ou
 * pergunta qual, guardando o gasto "pendente" até a resposta) e cadastra via ExpenseService - sem
 * duplicar nenhuma regra de negócio de gastos.
 *
 * Reaproveita ExpenseService/CategoryRepository/TabRepository do jeito que eles já são usados pelo
 * resto do app (checagem de dono via CurrentUser.id()) - o único "truque" é popular manualmente o
 * SecurityContext com o usuário dono do número de telefone, já que essa requisição não passa pelo
 * JwtAuthenticationFilter (o webhook do Meta não manda JWT nenhum). Isso evita reescrever
 * ExpenseService pra aceitar um userId explícito só por causa do bot.
 */
@Slf4j
@Service
public class WhatsAppBotService {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d{1,2})?)");

    private final UserRepository userRepository;
    private final TabRepository tabRepository;
    private final CategoryRepository categoryRepository;
    private final WhatsAppPendingExpenseRepository pendingExpenseRepository;
    private final ExpenseService expenseService;
    private final WhatsAppClient whatsAppClient;

    public WhatsAppBotService(UserRepository userRepository, TabRepository tabRepository,
                               CategoryRepository categoryRepository,
                               WhatsAppPendingExpenseRepository pendingExpenseRepository,
                               ExpenseService expenseService, WhatsAppClient whatsAppClient) {
        this.userRepository = userRepository;
        this.tabRepository = tabRepository;
        this.categoryRepository = categoryRepository;
        this.pendingExpenseRepository = pendingExpenseRepository;
        this.expenseService = expenseService;
        this.whatsAppClient = whatsAppClient;
    }

    @Transactional
    public void handleIncomingMessage(String rawPhone, String text) {
        String phone = rawPhone.replaceAll("[^0-9]", "");
        Optional<User> userOpt = userRepository.findByWhatsappPhone(phone);
        if (userOpt.isEmpty()) {
            whatsAppClient.sendText(rawPhone, "Esse número ainda não tá vinculado a nenhuma conta. "
                    + "Entra no app, vá em Configurações e cadastre seu WhatsApp exatamente assim: " + phone);
            return;
        }
        User user = userOpt.get();
        // popula o contexto de segurança manualmente - ver javadoc da classe.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getId(), null, Collections.emptyList()));

        Optional<WhatsAppPendingExpense> pending = pendingExpenseRepository.findByPhone(phone);
        if (pending.isPresent()) {
            continuePendingExpense(pending.get(), phone, text.trim());
        } else {
            startNewExpense(user, phone, text.trim());
        }
    }

    private void startNewExpense(User user, String phone, String text) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (!matcher.find()) {
            whatsAppClient.sendText(phone, "Não entendi o valor. Manda assim: \"50 mercado\" ou \"35,90 uber\".");
            return;
        }
        BigDecimal amount = parseAmount(matcher.group(1));
        String description = (text.substring(0, matcher.start()) + text.substring(matcher.end()))
                .replaceAll("(?i)\\br\\$", "")
                .trim();
        if (description.isBlank()) {
            description = "Gasto via WhatsApp";
        }

        Tab tab = tabRepository.findFirstByUserIdOrderByCreatedAtAsc(user.getId())
                .orElseThrow(() -> new IllegalStateException("Usuário sem nenhuma aba - não deveria acontecer."));
        List<Category> categories = categoryRepository.findByTabIdOrderByNameAsc(tab.getId());

        List<Category> matches = matchCategories(categories, description);
        if (matches.size() == 1) {
            createExpense(tab.getId(), matches.get(0).getId(), amount, description, LocalDate.now());
            whatsAppClient.sendText(phone, confirmationMessage(amount, description, matches.get(0).getName()));
            return;
        }

        askForCategory(user, phone, tab, categories, amount, description, LocalDate.now());
    }

    private void continuePendingExpense(WhatsAppPendingExpense pending, String phone, String text) {
        List<Category> categories = categoryRepository.findByTabIdOrderByNameAsc(pending.getTab().getId());

        Category chosen = null;
        try {
            int index = Integer.parseInt(text.trim()) - 1;
            if (index >= 0 && index < categories.size()) {
                chosen = categories.get(index);
            }
        } catch (NumberFormatException ignored) {
            List<Category> matches = matchCategories(categories, text);
            if (matches.size() == 1) {
                chosen = matches.get(0);
            }
        }

        if (chosen == null) {
            whatsAppClient.sendText(phone, "Não entendi. Responde só com o número da categoria:\n" + categoryList(categories));
            return;
        }

        createExpense(pending.getTab().getId(), chosen.getId(), pending.getAmount(), pending.getDescription(), pending.getDate());
        pendingExpenseRepository.deleteByPhone(phone);
        whatsAppClient.sendText(phone, confirmationMessage(pending.getAmount(), pending.getDescription(), chosen.getName()));
    }

    private void askForCategory(User user, String phone, Tab tab, List<Category> categories,
                                 BigDecimal amount, String description, LocalDate date) {
        if (categories.isEmpty()) {
            whatsAppClient.sendText(phone, "Você ainda não tem nenhuma categoria cadastrada em \"" + tab.getName()
                    + "\". Cria uma no app antes de lançar gastos por aqui.");
            return;
        }

        WhatsAppPendingExpense pending = pendingExpenseRepository.findByPhone(phone).orElseGet(WhatsAppPendingExpense::new);
        pending.setPhone(phone);
        pending.setTab(tab);
        pending.setAmount(amount);
        pending.setDescription(description);
        pending.setDate(date);
        pendingExpenseRepository.save(pending);

        whatsAppClient.sendText(phone, "Beleza, R$" + formatAmount(amount) + (description.isBlank() ? "" : " (" + description + ")")
                + ". Qual categoria?\n" + categoryList(categories));
    }

    private void createExpense(UUID tabId, UUID categoryId, BigDecimal amount, String description, LocalDate date) {
        expenseService.create(tabId, new ExpenseRequest(categoryId, amount, description, date, null, null));
    }

    private String categoryList(List<Category> categories) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < categories.size(); i++) {
            sb.append(i + 1).append(". ").append(categories.get(i).getName()).append('\n');
        }
        return sb.toString().trim();
    }

    private String confirmationMessage(BigDecimal amount, String description, String categoryName) {
        return "✅ R$" + formatAmount(amount) + " em " + categoryName
                + (description.isBlank() ? "" : " (" + description + ")") + " cadastrado.";
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toString().replace('.', ',');
    }

    private BigDecimal parseAmount(String raw) {
        return new BigDecimal(raw.replace(",", "."));
    }

    /** Casa a descrição digitada com o nome de uma categoria - sem acento, sem caixa, substring nos
     * dois sentidos (pega "mercado" -> "Mercado" e "compras no mercado" -> "Mercado"). */
    private List<Category> matchCategories(List<Category> categories, String text) {
        String normalizedText = normalize(text);
        if (normalizedText.isBlank()) {
            return List.of();
        }
        return categories.stream()
                .filter(c -> {
                    String normalizedName = normalize(c.getName());
                    return normalizedText.contains(normalizedName) || normalizedName.contains(normalizedText);
                })
                .toList();
    }

    private String normalize(String s) {
        String noAccents = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.toLowerCase().trim();
    }
}
