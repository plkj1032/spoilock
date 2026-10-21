package com.spoilock.backend.chat.entity;

import com.spoilock.backend.common.entity.CreatedAtEntity;
import com.spoilock.backend.drama.entity.Drama;
import com.spoilock.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "drama_id",
            nullable = false
    )
    private Drama drama;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "created_by",
            nullable = false
    )
    private User createdBy;

    @Column(
            name = "room_name",
            nullable = false,
            length = 100
    )
    private String roomName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
