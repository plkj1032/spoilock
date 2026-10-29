package com.spoilock.backend.chat.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class ChatInvitationCode {

    // 헷갈리는 I, O, 0, 1 제외
    private static final String ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final int CODE_LENGTH = 20;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < CODE_LENGTH; i++) {
            if (i > 0 && i % 5 == 0) {
                code.append('-');
            }

            code.append(
                    ALPHABET.charAt(random.nextInt(ALPHABET.length()))
            );
        }

        return code.toString();
    }

    public String hash(String code) {
        String normalized = normalize(code);

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "초대코드 처리에 필요한 SHA-256을 사용할 수 없습니다.",
                    e
            );
        }
    }

    private String normalize(String code) {
        if (code == null) {
            throw new IllegalArgumentException(
                    "초대코드를 입력해 주세요."
            );
        }

        String normalized = code
                .trim()
                .replace("-", "")
                .replace(" ", "")
                .toUpperCase(Locale.ROOT);

        if (normalized.length() != CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "올바른 초대코드를 입력해 주세요."
            );
        }

        for (int i = 0; i < normalized.length(); i++) {
            if (ALPHABET.indexOf(normalized.charAt(i)) < 0) {
                throw new IllegalArgumentException(
                        "올바른 초대코드를 입력해 주세요."
                );
            }
        }

        return normalized;
    }
}