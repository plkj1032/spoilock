package com.spoilock.backend.user.service;

import com.spoilock.backend.user.dto.SignUpRequest;
import com.spoilock.backend.user.dto.SignUpResponse;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignUpResponse signUp(SignUpRequest request)
    {
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if(userRepository.existByEmail(email))
        {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 가입된 이메일입니다."
            );
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.createLocal(
                email,
                encodedPassword,
                request.nickname().trim()
        );

        User savedUser = userRepository.save(user);

        return new SignUpResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getNickname()
        );
    }

}
