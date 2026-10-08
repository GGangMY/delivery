package com.example.delivery.user.service;

import com.example.delivery.global.security.JwtUtil;
import com.example.delivery.user.dto.request.LoginRequest;
import com.example.delivery.user.dto.request.SignupRequest;
import com.example.delivery.user.dto.response.TokenResponse;
import com.example.delivery.user.dto.response.UserResponse;
import com.example.delivery.user.entity.User;
import com.example.delivery.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String LOGIN_FAILED_MESSAGE = "로그인 정보가 일치하지 않습니다.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserResponse signup(SignupRequest signupRequest) {
        // 중복 확인
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }
        // 암호화
        String encoded = passwordEncoder.encode(signupRequest.getPassword());

        // DB 저장
        User user = new User(signupRequest.getUsername(), encoded, signupRequest.getRole());
        userRepository.save(user);

        // 요청 응답
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.getCreatedAt());
    }

    public TokenResponse login(LoginRequest loginRequest) {
        // 로그인 정보 조회
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, LOGIN_FAILED_MESSAGE));
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, LOGIN_FAILED_MESSAGE);
        }

        // 요청 응답
        return new TokenResponse(jwtUtil.createToken(user.getUsername(), user.getRole()), JwtUtil.TOKEN_TYPE);
    }
}
