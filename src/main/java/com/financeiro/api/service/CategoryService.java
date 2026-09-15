package com.financeiro.api.service;

import com.financeiro.api.dto.category.CategoryRequest;
import com.financeiro.api.dto.category.CategoryResponse;
import com.financeiro.api.entity.Category;
import com.financeiro.api.entity.Tab;
import com.financeiro.api.entity.User;
import com.financeiro.api.exception.AppException;
import com.financeiro.api.repository.CategoryRepository;
import com.financeiro.api.repository.ExpenseRepository;
import com.financeiro.api.repository.TabRepository;
import com.financeiro.api.repository.UserRepository;
import com.financeiro.api.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final TabRepository tabRepository;

    public CategoryService(CategoryRepository categoryRepository, ExpenseRepository expenseRepository,
                            UserRepository userRepository, TabRepository tabRepository) {
        this.categoryRepository = categoryRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.tabRepository = tabRepository;
    }

    public List<CategoryResponse> list(UUID tabId) {
        Tab tab = findOwnedTab(tabId);
        return categoryRepository.findByTabIdOrderByNameAsc(tab.getId())
                .stream().map(this::toResponse).toList();
    }

    public CategoryResponse create(UUID tabId, CategoryRequest request) {
        Tab tab = findOwnedTab(tabId);
        categoryRepository.findByTabIdAndName(tab.getId(), request.name())
                .ifPresent(c -> { throw new AppException("Você já tem uma categoria com esse nome nessa aba.", HttpStatus.CONFLICT); });

        User user = userRepository.getReferenceById(CurrentUser.id());
        Category category = new Category();
        category.setUser(user);
        category.setTab(tab);
        category.setName(request.name());
        category.setColor(request.color());
        category.setIcon(request.icon());
        categoryRepository.save(category);

        return toResponse(category);
    }

    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = findOwned(id);
        category.setName(request.name());
        category.setColor(request.color());
        category.setIcon(request.icon());
        categoryRepository.save(category);
        return toResponse(category);
    }

    public void delete(UUID id) {
        Category category = findOwned(id);
        if (expenseRepository.countByCategoryId(category.getId()) > 0) {
            throw new AppException("Não é possível excluir uma categoria que já possui gastos lançados.", HttpStatus.CONFLICT);
        }
        categoryRepository.delete(category);
    }

    private Category findOwned(UUID id) {
        return categoryRepository.findByIdAndUserId(id, CurrentUser.id())
                .orElseThrow(() -> new AppException("Categoria não encontrada.", HttpStatus.NOT_FOUND));
    }

    private Tab findOwnedTab(UUID tabId) {
        return tabRepository.findByIdAndUserId(tabId, CurrentUser.id())
                .orElseThrow(() -> new AppException("Aba não encontrada.", HttpStatus.NOT_FOUND));
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getColor(), c.getIcon());
    }
}
