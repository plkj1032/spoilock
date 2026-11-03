package com.spoilock.backend.post.service;

import com.spoilock.backend.ai.service.MediaAnalysisRequested;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.drama.repository.UserProgressRepository;
import com.spoilock.backend.post.entity.Post;
import com.spoilock.backend.post.entity.PostMedia;
import com.spoilock.backend.post.repository.PostMediaRepository;
import com.spoilock.backend.post.repository.PostQueryRepository;
import com.spoilock.backend.post.repository.PostRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import com.spoilock.backend.drama.domain.enums.DramaStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * 게시글 이미지 등록·조회.
 *
 * 스포일러 판정(MediaAnalysisService)이 끝나기 전, 또는 '아직 안 본 회차'의 장면으로 판정된 이미지는
 * 다른 사용자에게 블러로 보여준다. 블러 화면은 아주 작게 줄인 미리보기라 원본 내용이 드러나지 않고,
 * 사용자가 '이미지 보기'를 눌렀을 때만 원본(reveal=true)을 내려준다.
 */
@Service
@RequiredArgsConstructor
public class PostMediaService {

    private static final long MAX_IMAGE_SIZE =
            5L * 1024 * 1024;

    private static final int MAX_IMAGES = 4;

    // 블러 미리보기 가로 픽셀 (화면에서 크게 늘려 흐리게 표시)
    private static final int PREVIEW_WIDTH = 24;

    public static final String BLUR_SPOILER = "SPOILER";
    public static final String BLUR_CHECKING = "CHECKING";

    private final PostRepository postRepository;
    private final PostMediaRepository mediaRepository;
    private final PostVisibilityService visibilityService;
    private final UserProgressRepository progressRepository;
    private final PostQueryRepository postQueryRepository;
    private final PostMediaStorage storage;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @param blurred         이 사용자에게 블러로 보여야 하는지
     * @param blurReason      SPOILER(아직 안 본 회차 장면) / CHECKING(판정 중·재시도 중), 블러가 아니면 null
     * @param requiredEpisode SPOILER 일 때 원본을 바로 볼 수 있는 시청 회차
     */
    public record MediaInfo(
            Long id,
            Long postId,
            String filename,
            String url,
            String previewUrl,
            boolean blurred,
            String blurReason,
            Integer requiredEpisode
    ) {}

    public record MediaFile(
            byte[] bytes,
            String contentType
    ) {}

    private record CheckedImage(
            byte[] bytes,
            String filename,
            String contentType,
            String extension
    ) {}

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "피드를 찾을 수 없습니다."
                        )
                );
    }

    private CheckedImage check(MultipartFile file) {
        if (file == null || file.isEmpty()
                || file.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이미지는 한 장당 5MB 이하여야 합니다."
            );
        }

        try {
            byte[] bytes = file.getBytes();

            String filename = Optional
                    .ofNullable(file.getOriginalFilename())
                    .orElse("image");

            filename = filename.replace('\\', '/');
            filename = filename.substring(
                    filename.lastIndexOf('/') + 1
            );

            if (filename.isBlank()
                    || filename.length() > 255
                    || filename.chars().anyMatch(
                    Character::isISOControl)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "잘못된 파일명입니다."
                );
            }

            try (ImageInputStream input =
                         ImageIO.createImageInputStream(
                                 new ByteArrayInputStream(bytes))) {

                if (input == null) {
                    throw new IOException("Invalid image stream");
                }

                Iterator<ImageReader> readers =
                        ImageIO.getImageReaders(input);

                if (!readers.hasNext()) {
                    throw new IOException("Unsupported image");
                }

                ImageReader reader = readers.next();

                try {
                    reader.setInput(input);

                    String format = reader.getFormatName()
                            .toLowerCase(Locale.ROOT);

                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);

                    if (width < 1 || height < 1
                            || width > 4096 || height > 4096) {
                        throw new IOException(
                                "Image dimensions exceeded"
                        );
                    }

                    if (format.equals("jpeg")
                            || format.equals("jpg")) {
                        return new CheckedImage(
                                bytes,
                                filename,
                                "image/jpeg",
                                ".jpg"
                        );
                    }

                    if (format.equals("png")) {
                        return new CheckedImage(
                                bytes,
                                filename,
                                "image/png",
                                ".png"
                        );
                    }

                    throw new IOException("Unsupported format");

                } finally {
                    reader.dispose();
                }
            }

        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "올바른 JPG 또는 PNG 이미지만 등록해 주세요.",
                    ex
            );
        }
    }

    private static String baseUrl(PostMedia media) {
        return "/api/feed/posts/" + media.getPost().getId()
                + "/media/" + media.getId();
    }

    private MediaInfo toInfo(PostMedia media, String blurReason) {
        boolean blurred = blurReason != null;
        return new MediaInfo(
                media.getId(),
                media.getPost().getId(),
                media.getOriginalFilename(),
                baseUrl(media),
                baseUrl(media) + "/preview",
                blurred,
                blurReason,
                BLUR_SPOILER.equals(blurReason) ? media.getRequiredEpisodeNumber() : null
        );
    }

    // =============================================
    // 이미지 등록: 최대 4장 → 커밋 후 스포일러 판정 시작
    // =============================================
    @Transactional
    public List<MediaInfo> upload(
            Long postId,
            Long userId,
            MultipartFile[] files
    ) {
        Post post = postQueryRepository.findForMediaUpload(postId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND
                        )
                );

        if (!post.getUser().getId().equals(userId)
                || post.getParentPost() != null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "본인의 피드에만 이미지를 등록할 수 있습니다."
            );
        }

        if (post.getModerationStatus() == ModerationStatus.HIDDEN
                || post.getModerationStatus() == ModerationStatus.REMOVED
                || post.getDrama().getStatus() != DramaStatus.PUBLISHED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이 피드에는 이미지를 추가할 수 없습니다."
            );
        }

        long currentCount =
                mediaRepository.countByPost_Id(postId);

        if (files == null || files.length < 1
                || files.length > MAX_IMAGES
                || currentCount + files.length > MAX_IMAGES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "피드당 이미지는 최대 4장입니다."
            );
        }

        // 저장 전에 모든 이미지를 검증
        List<CheckedImage> checked =
                Arrays.stream(files).map(this::check).toList();

        List<String> createdKeys = new ArrayList<>();

        // DB 트랜잭션이 롤백되면 저장한 파일 정리
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            createdKeys.forEach(storage::deleteQuietly);
                        }
                    }
                }
        );

        List<MediaInfo> result = new ArrayList<>();
        List<Long> savedIds = new ArrayList<>();

        int order = (int) currentCount;

        for (CheckedImage image : checked) {

            String key = storage.save(
                    image.bytes(),
                    image.contentType(),
                    image.extension()
            );
            createdKeys.add(key);

            PostMedia media = mediaRepository.saveAndFlush(
                    PostMedia.create(
                            post,
                            key,
                            image.filename(),
                            image.contentType(),
                            image.bytes().length,
                            order++
                    )
            );

            savedIds.add(media.getId());
            // 작성자 본인 화면: 블러 없음
            result.add(toInfo(media, null));
        }

        // 커밋 후 별도 스레드에서 스포일러 판정 (MediaAnalysisListener)
        eventPublisher.publishEvent(new MediaAnalysisRequested(savedIds));

        return result;
    }

    // =============================================
    // 이미지 공개 여부
    // =============================================

    /** 관리자 숨김·삭제가 아니고 글을 볼 수 있으면 (블러 여부와 무관하게) 접근 가능 */
    private boolean canAccess(PostMedia media, Long userId) {
        if (media.getModerationStatus() == ModerationStatus.REMOVED
                || media.getModerationStatus() == ModerationStatus.HIDDEN) {
            return false;
        }
        return visibilityService.canRead(media.getPost(), userId);
    }

    /** 이 사용자에게 블러로 보일 이유. null = 원본 그대로 */
    private String blurReason(PostMedia media, Long userId, Integer watched) {
        if (media.getPost().getUser().getId().equals(userId)) {
            return null;   // 작성자는 항상 원본
        }
        if (media.getAnalysisStatus() != AnalysisStatus.COMPLETED) {
            return BLUR_CHECKING;   // 판정 중·실패 후 재시도 중
        }
        Integer required = media.getRequiredEpisodeNumber();
        if (required == null) {
            return null;
        }
        return watched != null && watched >= required ? null : BLUR_SPOILER;
    }

    private int watchedEpisode(Long userId, Post post) {
        return progressRepository
                .findByUser_IdAndDrama_Id(userId, post.getDrama().getId())
                .map(p -> p.getWatchedEpisodeNumber())
                .orElse(0);
    }

    // 사용자별 이미지 목록 (블러 여부 포함)
    @Transactional(readOnly = true)
    public List<MediaInfo> list(Long postId, Long userId) {
        Post post = findPost(postId);

        if (!visibilityService.canRead(post, userId)) {
            return List.of();
        }

        int watched = watchedEpisode(userId, post);

        return mediaRepository
                .findByPost_IdOrderBySortOrderAsc(postId)
                .stream()
                .filter(media -> canAccess(media, userId))
                .map(media -> toInfo(media, blurReason(media, userId, watched)))
                .toList();
    }

    private PostMedia findAccessible(Long postId, Long mediaId, Long userId) {
        PostMedia media = mediaRepository.findById(mediaId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND)
                );

        if (!media.getPost().getId().equals(postId)
                || !canAccess(media, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return media;
    }

    /**
     * 원본 이미지. 블러 대상이면 reveal=true('이미지 보기'를 누른 요청)일 때만 내려준다.
     */
    @Transactional(readOnly = true)
    public MediaFile file(
            Long postId,
            Long mediaId,
            Long userId,
            boolean reveal
    ) {
        PostMedia media = findAccessible(postId, mediaId, userId);

        if (!reveal
                && blurReason(media, userId, watchedEpisode(userId, media.getPost())) != null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "스포일러가 포함될 수 있는 이미지입니다. '이미지 보기'를 눌러 확인해 주세요."
            );
        }

        return new MediaFile(storage.read(media.getStorageKey()), media.getMediaType());
    }

    /** 블러용 미리보기: 아주 작게 줄인 PNG (화면에서 늘려 흐리게 표시, 원본 내용은 알아볼 수 없음) */
    @Transactional(readOnly = true)
    public MediaFile preview(
            Long postId,
            Long mediaId,
            Long userId
    ) {
        PostMedia media = findAccessible(postId, mediaId, userId);
        return new MediaFile(shrink(storage.read(media.getStorageKey())), "image/png");
    }

    static byte[] shrink(byte[] original) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
            if (source == null) {
                throw new IOException("Unsupported image");
            }

            int width = Math.min(PREVIEW_WIDTH, source.getWidth());
            int height = Math.max(1, Math.round(
                    (float) source.getHeight() * width / source.getWidth()));

            BufferedImage small = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = small.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                g.drawImage(source, 0, 0, width, height, null);
            } finally {
                g.dispose();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(small, "png", out);
            return out.toByteArray();

        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "미리보기를 만들지 못했습니다.",
                    ex
            );
        }
    }

    // 관리자 검수 대기 이미지 목록
    @Transactional(readOnly = true)
    public List<MediaInfo> pendingForAdmin() {
        return mediaRepository
                .findTop100ByModerationStatusOrderByIdAsc(
                        ModerationStatus.PENDING
                )
                .stream()
                .map(media -> new MediaInfo(
                        media.getId(),
                        media.getPost().getId(),
                        media.getOriginalFilename(),
                        "/api/admin/feed/media/" + media.getId(),
                        null,
                        false,
                        null,
                        media.getRequiredEpisodeNumber()
                ))
                .toList();
    }

    // 관리자 원본 이미지 조회
    @Transactional(readOnly = true)
    public MediaFile adminFile(Long mediaId) {
        PostMedia media = mediaRepository.findById(mediaId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND)
                );

        return new MediaFile(storage.read(media.getStorageKey()), media.getMediaType());
    }

    // 관리자 이미지 검수 (자동 판정 결과를 덮어씀)
    @Transactional
    public void review(
            Long mediaId,
            boolean approved,
            Integer requiredEpisode
    ) {
        PostMedia media = mediaRepository.findById(mediaId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND)
                );

        if (approved && requiredEpisode == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "승인 시 필요한 회차 번호를 지정해야 합니다."
            );
        }

        if (requiredEpisode != null && requiredEpisode < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "회차 번호는 0 이상이어야 합니다."
            );
        }

        media.review(
                approved,
                approved && requiredEpisode > 0
                        ? requiredEpisode : null
        );
    }
}
