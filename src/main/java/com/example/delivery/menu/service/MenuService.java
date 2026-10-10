package com.example.delivery.menu.service;

import com.example.delivery.menu.dto.request.MenuRequest;
import com.example.delivery.menu.dto.response.MenuResponse;
import com.example.delivery.menu.entity.Menu;
import com.example.delivery.menu.repository.MenuRepository;
import com.example.delivery.user.entity.User;
import com.example.delivery.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    private final UserRepository userRepository;

    @Transactional
    public MenuResponse create(MenuRequest menuRequest, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new
                        ResponseStatusException(HttpStatus.FORBIDDEN, "접근할 권한이 없습니다."));

        Menu saved = menuRepository.save(new Menu(user, menuRequest.name(), menuRequest.price(),
                menuRequest.description()));

        return new MenuResponse(saved.getId(), saved.getName(), saved.getPrice(), saved.getDescription(),
                saved.getOwner().getUsername(), saved.getCreatedAt(), saved.getUpdatedAt());
    }
}