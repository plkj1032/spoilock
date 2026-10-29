
package com.spoilock.backend.drama.service;

import com.spoilock.backend.chat.domain.entity.ChatRoom;
import com.spoilock.backend.chat.repository.ChatRoomRepository;
import com.spoilock.backend.drama.domain.dto.AdminDramaDetailResponse;
import com.spoilock.backend.drama.domain.dto.AdminDramaResponse;
import com.spoilock.backend.drama.domain.dto.CreateDramaRequest;
import com.spoilock.backend.drama.domain.entity.Drama;
import com.spoilock.backend.drama.domain.entity.Episode;
import com.spoilock.backend.drama.domain.enums.DramaStatus;
import com.spoilock.backend.drama.repository.DramaRepository;
import com.spoilock.backend.drama.repository.EpisodeRepository;

import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.enums.UserRole;
import com.spoilock.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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


    @Transactional
    public AdminDramaResponse update(
            Long dramaId,
            CreateDramaRequest request
    ) {
        // 1. 수정할 드라마 조회
        Drama drama = findDrama(dramaId);

        String title = request.title().trim();

        // 3. 자신을 제외한 작품 중 제목 중복 확인
        if (dramaRepository.existsByTitleIgnoreCaseAndIdNot(
                title,
                dramaId
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 등록된 작품입니다."
            );
        }

        // 4. 전체 회차 검증
        validateEpisodes(
                request.totalEpisodeCount(),
                request.episodes().stream()
                        .map(
                                CreateDramaRequest.EpisodeItem
                                        ::episodeNumber
                        )
                        .toList()
        );

        if(episodeVideoRepository.existsByEpisode_Drama_Id(dramaId))
        {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "MP4 영상이 등록된 작품입니다. "
                    + "연결된 회차를 전체 교체할 수 없습니다."
            );
        }

        // 5. QueryDSL로 기존 초안 회차 삭제
        episodeRepository.deleteDraftEpisodes(dramaId);

        // 6. Bulk Delete 후 clear()가 실행되므로 재조회
        drama = findDrama(dramaId);

        // 7. 드라마 정보 수정
        drama.updateDraft(
                title,
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

        // Lambda에서 사용할 영속 상태의 Drama
        Drama managedDrama = drama;

        // 8. 새 회차 생성
        List<Episode> newEpisodes = request.episodes()
                .stream()
                .map(item -> Episode.create(
                        managedDrama,
                        item.episodeNumber(),
                        item.title().trim()
                ))
                .toList();

        // 9. 회차 저장
        episodeRepository.saveAll(newEpisodes);

        // 10. 응답 DTO 반환
        return AdminDramaResponse.from(managedDrama);
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
