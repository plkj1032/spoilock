package com.spoilock.backend.chat.domain.entity;

import com.spoilock.backend.chat.domain.type.ChatRoomBanStatus;
import com.spoilock.backend.common.entity.CreatedAtEntity;
import com.spoilock.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "chat_room_bans")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomBan extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 강퇴된 채팅방
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "room_id",
            nullable = false
    )
    private ChatRoom room;

    // 강퇴당한 사용자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    // 강퇴를 실행한 사용자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "kicked_by",
            nullable = false
    )
    private User kickedBy;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private ChatRoomBanStatus status = ChatRoomBanStatus.ACTIVE;

    // 강퇴 취소 시간
    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    // 강퇴 취소한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "released_by")
    private User releasedBy;


    public ChatRoomBan(
            ChatRoom room,
            User user,
            User kickedBy,
            String reason
    ) {
        this.room = room;
        this.user = user;
        this.kickedBy = kickedBy;
        this.reason = reason;
        this.status = ChatRoomBanStatus.ACTIVE;
    }


    // 강퇴 취소
    public void release(User releasedBy) {

        this.status = ChatRoomBanStatus.RELEASED;
        this.releasedAt = LocalDateTime.now();
        this.releasedBy = releasedBy;
    }
}