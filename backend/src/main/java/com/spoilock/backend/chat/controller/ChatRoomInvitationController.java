package com.spoilock.backend.chat.controller;

import com.spoilock.backend.chat.domain.dto.ChatInvitationRoomResponse;
import com.spoilock.backend.chat.repository.ChatInviteCandidateQueryRepository;
import com.spoilock.backend.chat.service.ChatRoomInvitationService;
import com.spoilock.backend.chat.service.ChatRoomInvitationService.InvitationCreatedResponse;
import com.spoilock.backend.chat.service.ChatRoomInvitationService.InvitationAcceptedResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatRoomInvitationController {

    private final ChatRoomInvitationService invitationService;

    public record SendInvitationRequest(
            @NotNull
            @Positive
            Long receiverUserId
    ) {
    }

    @PostMapping("/rooms/{roomId}/invitations")
    public ResponseEntity<InvitationCreatedResponse> sendInvitation(
            @PathVariable("roomId")
            @Positive
            Long roomId,

            @Valid @RequestBody SendInvitationRequest request,

            @AuthenticationPrincipal Jwt jwt
    ) {
        Long senderUserId = Long.valueOf(jwt.getSubject());

        InvitationCreatedResponse result =
                invitationService.sendInvitation(
                        roomId,
                        senderUserId,
                        request.receiverUserId()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(result);
    }
    public record AcceptInvitationCodeRequest(
            @NotBlank
            @Size(max = 100)
            String code
    ) {
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public ResponseEntity<InvitationAcceptedResponse> acceptInvitation(
            @PathVariable("invitationId")
            @Positive
            Long invitationId,

            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(invitationService.acceptInvitation(
                        invitationId,
                        userId
                ));
    }

    @PostMapping("/invitations/accept-by-code")
    public ResponseEntity<InvitationAcceptedResponse> acceptByCode(
            @Valid @RequestBody AcceptInvitationCodeRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(invitationService.acceptByCode(
                        request.code(),
                        userId
                ));
    }

    @PostMapping("/invitations/{invitationId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectInvitation(
            @PathVariable("invitationId")
            @Positive
            Long invitationId,

            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        invitationService.rejectInvitation(invitationId, userId);
    }

    @GetMapping("/invitations/rooms")
    public ResponseEntity<List<ChatInvitationRoomResponse>> getInvitableRooms(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(invitationService.getInvitableRooms(userId));
    }

    @GetMapping("/rooms/{roomId}/invitations/candidates")
    public ResponseEntity<ChatInviteCandidateQueryRepository.CandidatePage> getCandidates(
            @PathVariable("roomId")
            @Positive
            Long roomId,

            @RequestParam(name = "page", defaultValue = "0")
            @Min(0)
            int page,

            @AuthenticationPrincipal Jwt jwt
    ) {
        Long senderId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(invitationService.getCandidates(
                        roomId,
                        senderId,
                        page
                ));
    }
}