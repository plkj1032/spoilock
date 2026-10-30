
package com.spoilock.backend.drama.service;

import com.spoilock.backend.chat.domain.entity.ChatRoom;
import com.spoilock.backend.chat.repository.ChatRoomRepository;
import com.spoilock.backend.drama.domain.dto.AdminDramaDetailResponse;
import com.spoilock.backend.drama.domain.dto.AdminDramaResponse;
import com.spoilock.backend.drama.domain.dto.CreateDramaRequest;
import com.spoilock.backend.drama.domain.entity.Drama;
import com.spoilock.backend.drama.domain.entity.Episode;
import com.spoilock.backend.drama.domain.enums.DramaStatus;
import com.spoilock.backend.drama.repository.DramaQueryRepository;
import com.spoilock.backend.drama.repository.DramaRepository;
import com.spoilock.backend.drama.repository.EpisodeRepository;

import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.enums.UserRole;
import com.spoilock.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.IntStream;

import com.spoilock.backend.drama.repository.EpisodeVideoRepository;

@Service
@RequiredArgsConstructor
public class AdminDramaService {

    private final DramaRepository dramaRepository;
    private final EpisodeRepository episodeRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository  userRepository;
    private final EpisodeVideoRepository episodeVideoRepository;
    private final DramaQueryRepository dramaQueryRepository;

    @Transactional(readOnly = true)
    public List<AdminDramaResponse> getAll() {
        return dramaRepository
                .findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(AdminDramaResponse::from)
                .toList();
    }

    @Transactional
    public AdminDramaResponse create(
            CreateDramaRequest request,
            Long adminId
    ) {
        User administrator = userRepository.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "로그인 회원을 찾을 수 없습니다."
                ));

        if (administrator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "관리자만 드라마를 등록할 수 있습니다."
            );
        }

        validateEpisodes(
                request.totalEpisodeCount(),
                request.episodes().stream()
                        .map(CreateDramaRequest.EpisodeItem::episodeNumber)
                        .toList()
        );

        Drama drama = Drama.createDraft(
                request.title().trim(),
                request.synopsis(),
                blankToNull(request.posterUrl()),
                request.totalEpisodeCount()
        );

        drama.updateMetadata(
                request.genres(),
                request.premiereDate(),
                request.directors(),
                request.writers(),
                request.castNames()
        );
        drama.updateOfficialHomepageUrl(request.officialHomepageUrl());
        drama.updateProvisionType(request.provisionType());

        Drama savedDrama = dramaRepository.save(drama);

        List<Episode> episodes = request.episodes()
                .stream()
                .map(item -> Episode.create(
                        savedDrama,
                        item.episodeNumber(),
                        item.title().trim()
                ))
                .toList();

        episodeRepository.saveAll(episodes);

        ChatRoom defaultRoom = ChatRoom.createDefault(
                savedDrama,
                administrator,
                "기본 토론방"
        );

        chatRoomRepository.save(defaultRoom);

        return AdminDramaResponse.from(savedDrama);
    }

    @Transactional
    public AdminDramaResponse publish(Long dramaId) {

        Drama drama = dramaRepository.findById(dramaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "드라마를 찾을 수 없습니다."
                ));

        List<Integer> numbers = episodeRepository
                .findByDrama_IdOrderByEpisodeNumberAsc(dramaId)
                .stream()
                .map(Episode::getEpisodeNumber)
                .toList();

        validateEpisodes(drama.getTotalEpisodeCount(), numbers);

        drama.publish();

        return AdminDramaResponse.from(drama);
    }

    private void validateEpisodes(
            Integer total,
            List<Integer> numbers
    ) {
        if (total == null || total < 1 || numbers.size() != total) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "전체 회차 수와 등록 회차 수가 일치하지 않습니다."
            );
        }

        List<Integer> sorted = numbers.stream().sorted().toList();

        List<Integer> expected = IntStream
                .rangeClosed(1, total)
                .boxed()
                .toList();

        if (!sorted.equals(expected)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "회차 번호는 1화부터 빠짐없이 등록해야 합니다."
            );
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    @Transactional(readOnly = true)
    public AdminDramaDetailResponse getDetail(Long dramaId)
    {
        Drama drama = findDrama(dramaId);

        return AdminDramaDetailResponse.from(
                drama,
                episodeRepository
                        .findByDrama_IdOrderByEpisodeNumberAsc(dramaId)
        );
    }


    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AdminDramaResponse update(
            Long dramaId,
            CreateDramaRequest request
    ) {
        Drama drama = dramaQueryRepository.findLockedForEdit(dramaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "드라마를 찾을 수 없습니다."
                ));

        String title = request.title().trim();

        if (dramaQueryRepository.existsOtherTitle(title, dramaId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 등록된 작품입니다."
            );
        }

        // 요청한 회차가 1화부터 전체 회차 수까지 연속인지 확인
        validateEpisodes(
                request.totalEpisodeCount(),
                request.episodes().stream()
                        .map(CreateDramaRequest.EpisodeItem::episodeNumber)
                        .toList()
        );

        List<Episode> existingEpisodes =
                dramaQueryRepository.findEpisodesForEdit(dramaId);

        // 기존 회차 구성도 1화부터 연속인지 확인
        validateEpisodes(
                drama.getTotalEpisodeCount(),
                existingEpisodes.stream()
                        .map(Episode::getEpisodeNumber)
                        .toList()
        );

        int existingCount = existingEpisodes.size();
        int requestedCount = request.totalEpisodeCount();

        if (requestedCount < existingCount) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "기존 회차는 삭제할 수 없습니다. 새 회차를 추가해 주세요."
            );
        }

        // 기존 회차는 변경하지 않고 마지막 회차 뒤에 새 회차만 추가
        List<Episode> addedEpisodes = request.episodes()
                .stream()
                .filter(item -> item.episodeNumber() > existingCount)
                .map(item -> Episode.create(
                        drama,
                        item.episodeNumber(),
                        item.title().trim()
                ))
                .toList();

        episodeRepository.saveAll(addedEpisodes);

        // 작품 정보와 전체 회차 수 갱신
        drama.updateDraft(
                title,
                request.synopsis(),
                blankToNull(request.posterUrl()),
                requestedCount
        );

        drama.updateMetadata(
                request.genres(),
                request.premiereDate(),
                request.directors(),
                request.writers(),
                request.castNames()
        );

        drama.updateOfficialHomepageUrl(request.officialHomepageUrl());
        drama.updateProvisionType(request.provisionType());

        return AdminDramaResponse.from(drama);
    }


    @Transactional
    public AdminDramaResponse hide(Long dramaId) {
        Drama drama = findDrama(dramaId);

        if (drama.getStatus() != DramaStatus.PUBLISHED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "공개된 작품만 숨길 수 있습니다."
            );
        }

        drama.hide();

        return AdminDramaResponse.from(drama);
    }

    private Drama findDrama(Long dramaId) {
        return dramaRepository.findById(dramaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "드라마를 찾을 수 없습니다."
                ));
    }
}
