package com.spoilock.backend.user.dto;

import com.spoilock.backend.chat.domain.type.ChatRoomType;

import java.time.LocalDateTime;
import java.util.List;

public record MyPageResponse(
        MeResponse profile,
        List<WatchingDrama> dramas,
        List<JoinedRoom> rooms
) {
    public record WatchingDrama(
            Long dramaId,
            String title,
            String posterUrl,
            Integer totalEpisodeCount,
            Integer watchedEpisodeNumber,
            LocalDateTime updatedAt
    ){
    }
    public record JoinedRoom(
            Long roomId,
            String roomName,
            ChatRoomType roomType,
            Long dramaId,
            String dramaTitle,
            LocalDateTime joinedAt
    ){}
}
