package com.spoilock.backend.user.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

@Component
public class NicknamePolicy {

    // 초기 금칙어
    private static final Set<String> BLOCKED_TERMS = Set.of(
            "시발",
            "씨발",
            "씨빨",
            "병신",
            "개새끼",
            "지랄",
            "좆",
            "관리자",
            "운영자",
            "admin",
            "administrator",
            "spoilock"
    );

    public String normalize(String nickname)
    {
        if(nickname == null || nickname.isBlank())
        {
            throw badRequest("닉네임을 입력해 주세요.");
        }

        // 호환 문자와 전각 문자 등을 표준화
        String normalized = Normalizer.normalize(
                nickname.trim(),
                Normalizer.Form.NFKC
        );

        // 한글, 영문, 숫자, 밑줄 허용
        if(!normalized.matches("[가-힣a-zA-Z0-9_]{2,20}"))
        {
            throw badRequest(
                    "닉네임은 한글, 영문, 숫자, 밑줄을 사용해 2~20자로 입력해주세요."
            );
        }

        // 대소문자와 밑줄 변형을 제거하여 검사
        String key = normalized
                .toLowerCase(Locale.ROOT)
                .replace("_","");

        for(String blocked : BLOCKED_TERMS)
        {
            if(key.contains(blocked))
            {
                throw badRequest(
                        "사용할 수 없는 표현이 포함된 닉네임입니다."
                );
            }
        }

        return normalized;
    }

    private ResponseStatusException badRequest(String message)
    {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
