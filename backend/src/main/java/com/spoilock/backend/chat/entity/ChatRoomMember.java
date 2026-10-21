package com.spoilock.backend.chat.entity;

import com.spoilock.backend.chat.entity.id.ChatRoomMemberId;
import com.spoilock.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember {

    @EmbeddedId
    private ChatRoomMemberId id = new ChatRoomMemberId();

    @MapsId("roomId")
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "room_id",
            nullable = false
    )
    private ChatRoom room;

    @MapsId("userId")
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @CreationTimestamp
    @Column(
            name = "joined_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime joinedAt;
}
