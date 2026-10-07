package com.example.delivery.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.formLogin(form -> form.disable());
        http.httpBasic(basic -> basic.disable());

        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()

                        // 1. 회원가입 (POST /api/users) - 누구나
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        // 2. 로그인 (POST /api/auth/login) - 누구나
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        // 3. 메뉴 등록 (POST /api/menus) - OWNER

                        // 4. 메뉴 목록 조회 (GET /api/menus) - 누구나
                        .requestMatchers(HttpMethod.GET, "/api/menus").permitAll()
                        // 5. 메뉴 단건 조회 (GET /api/menus/{menuId}) - 누구나

                        // 6. 메뉴 수정 (PUT /api/menus/{menuId}) - OWNER (본인 메뉴)

                        // 7. 메뉴 삭제 (DELETE /api/menus/{menuId}) - OWNER (본인 메뉴)

                        // 8. 주문 생성 (POST /api/orders) - CUSTOMER

                        // 9. 주문 목록 조회 (GET /api/orders) - 로그인한 사용자 (역할별로 다른 목록)

                        // 10. 주문 취소 (PATCH /api/orders/{orderId}/cancel) - CUSTOMER (본인 주문)

                        // 11. 주문 상태 변경 (PATCH /api/orders/{orderId}/status) - OWNER (본인 메뉴 주문)

                        // 12. 결제 (POST /api/orders/{orderId}/payments - CUSTOMER (본인 주문)

        );


        return http.build();
    }
}