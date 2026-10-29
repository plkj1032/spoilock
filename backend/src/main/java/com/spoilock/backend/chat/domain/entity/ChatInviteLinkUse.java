package com.spoilock.backend.chat.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(
        name = "chat_invite_link_uses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_link_user",
                columnNames = {"link_id", "user_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatInviteLinkUse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "link_id", nullable = false)
    private Long linkId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    public static ChatInviteLinkUse create(
            Long linkId,
            Long userId
    ) {
        Objects.requireNonNull(linkId);
        Objects.requireNonNull(userId);

        ChatInviteLinkUse use = new ChatInviteLinkUse();

        use.linkId = linkId;
        use.userId = userId;

        return use;
    }
}