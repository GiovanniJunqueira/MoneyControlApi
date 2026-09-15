package com.financeiro.api.service;

import com.financeiro.api.dto.tab.TabRequest;
import com.financeiro.api.dto.tab.TabResponse;
import com.financeiro.api.entity.Tab;
import com.financeiro.api.entity.User;
import com.financeiro.api.exception.AppException;
import com.financeiro.api.repository.TabRepository;
import com.financeiro.api.repository.UserRepository;
import com.financeiro.api.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TabService {

    private final TabRepository tabRepository;
    private final UserRepository userRepository;

    public TabService(TabRepository tabRepository, UserRepository userRepository) {
        this.tabRepository = tabRepository;
        this.userRepository = userRepository;
    }

    public List<TabResponse> list() {
        return tabRepository.findByUserIdOrderByNameAsc(CurrentUser.id())
                .stream().map(this::toResponse).toList();
    }

    public TabResponse create(TabRequest request) {
        tabRepository.findByUserIdAndName(CurrentUser.id(), request.name())
                .ifPresent(t -> { throw new AppException("Você já tem uma aba com esse nome.", HttpStatus.CONFLICT); });

        User user = userRepository.getReferenceById(CurrentUser.id());
        Tab tab = new Tab();
        tab.setUser(user);
        tab.setName(request.name());
        tab.setColor(request.color());
        tabRepository.save(tab);

        return toResponse(tab);
    }

    public TabResponse update(UUID id, TabRequest request) {
        Tab tab = findOwned(id);
        tab.setName(request.name());
        tab.setColor(request.color());
        tabRepository.save(tab);
        return toResponse(tab);
    }

    public void delete(UUID id) {
        Tab tab = findOwned(id);
        if (tabRepository.findByUserIdOrderByNameAsc(CurrentUser.id()).size() <= 1) {
            throw new AppException("Você precisa ter pelo menos uma aba.", HttpStatus.CONFLICT);
        }
        tabRepository.delete(tab);
    }

    private Tab findOwned(UUID id) {
        return tabRepository.findByIdAndUserId(id, CurrentUser.id())
                .orElseThrow(() -> new AppException("Aba não encontrada.", HttpStatus.NOT_FOUND));
    }

    private TabResponse toResponse(Tab t) {
        return new TabResponse(t.getId(), t.getName(), t.getColor());
    }
}
