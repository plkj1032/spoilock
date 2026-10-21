package com.spoilock.backend.chat.entity.id;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChatRoomMemberId implements Serializable {

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "user_id")
    private Long userId;
}
