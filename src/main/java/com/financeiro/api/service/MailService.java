package com.financeiro.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private final String fromAddress;
    private final String frontendUrl;
    private final String resendApiKey;
    private final String resendFrom;

    public MailService(JavaMailSender mailSender,
                        ObjectMapper objectMapper,
                        @Value("${spring.mail.username:}") String fromAddress,
                        @Value("${app.frontend-url}") String frontendUrl,
                        @Value("${app.resend.api-key:}") String resendApiKey,
                        @Value("${app.resend.from:}") String resendFrom) {
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl;
        this.resendApiKey = resendApiKey;
        this.resendFrom = resendFrom;
    }

    /**
     * Falha de envio (provedor mal configurado, fora do ar, porta bloqueada pelo host etc.) fica
     * só no log - o chamador (AuthService.forgotPassword) sempre responde com sucesso genérico
     * pro front, pra não revelar se um e-mail existe ou não na base.
     *
     * Dois caminhos: Resend (API HTTP, usado quando RESEND_API_KEY está configurada) e Gmail SMTP
     * (fallback, usado quando não está). Motivo de ter os dois: hosts free tier (Render incluso)
     * costumam bloquear conexão SMTP de saída (porta 587/465/25) - a requisição trava por minutos
     * até estourar o timeout ao invés de simplesmente falhar, porque o TCP fica esperando um SYN-ACK
     * que nunca chega. HTTP (porta 443) não tem esse problema. Local (sem nenhuma das duas chaves
     * configuradas) continua funcionando via SMTP normalmente, sem porta bloqueada.
     */
    public void sendPasswordReset(String toEmail, String name, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        String subject = "Redefinir senha - Financeiro";
        String body = "Oi " + name + ",\n\n"
                + "Recebemos um pedido para redefinir a senha da sua conta. Clique no link abaixo (válido por 1 hora):\n\n"
                + link + "\n\n"
                + "Se você não pediu isso, pode ignorar este e-mail - sua senha continua a mesma.";

        if (!resendApiKey.isBlank()) {
            sendViaResend(toEmail, subject, body);
        } else {
            sendViaSmtp(toEmail, subject, body);
        }
    }

    private void sendViaResend(String toEmail, String subject, String body) {
        try {
            Map<String, Object> payload = Map.of(
                    "from", resendFrom.isBlank() ? "onboarding@resend.dev" : resendFrom,
                    "to", List.of(toEmail),
                    "subject", subject,
                    "text", body
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.error("Resend recusou o e-mail de reset pra {}: {} - {}", toEmail, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de reset de senha via Resend para {}", toEmail, e);
        }
    }

    private void sendViaSmtp(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Falha ao enviar e-mail de reset de senha via SMTP para {}", toEmail, e);
        }
    }
}
