package com.spoilock.backend.chat.domain.entity;

import com.spoilock.backend.chat.domain.type.ChatRoomType;
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

    @Enumerated(EnumType.STRING)
    @Column( name ="room_type", nullable = false, length = 20)
    private ChatRoomType roomType;

    @Column(nullable = false)
    private boolean active = true;

    public static ChatRoom create(
            Drama drama,
            User createdBy,
            String roomName,
            String description
    ) {
        ChatRoom room = new ChatRoom();

        room.drama = drama;
        room.createdBy = createdBy;
        room.roomName = roomName;
        room.description = description;
        room.roomType = ChatRoomType.CUSTOM;
        room.active = true;

        return room;
    }

    //드라마 등록시 자동 생성할 기본토론방
    public static ChatRoom createDefault(
            Drama drama,
            User administrator,
            String roomName
    ){
        ChatRoom room = create(
                drama,
                administrator,
                roomName,
                "작품에 대해 자유롭게 이야기하는 기본 토론방입니다"
        );
        room.roomType = ChatRoomType.DEFAULT;

        return room;
    }
}
