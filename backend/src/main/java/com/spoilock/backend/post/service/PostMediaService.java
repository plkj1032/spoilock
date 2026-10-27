
package com.spoilock.backend.post.service;

import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.drama.repository.UserProgressRepository;
import com.spoilock.backend.post.entity.Post;
import com.spoilock.backend.post.entity.PostMedia;
import com.spoilock.backend.post.repository.PostMediaRepository;
import com.spoilock.backend.post.repository.PostQueryRepository;
import com.spoilock.backend.post.repository.PostRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PostMediaService {

    private static final long MAX_IMAGE_SIZE =
            5L * 1024 * 1024;

    private static final int MAX_IMAGES = 4;

    private final PostRepository postRepository;
    private final PostMediaRepository mediaRepository;
    private final PostVisibilityService visibilityService;
    private final UserProgressRepository progressRepository;
    private final PostQueryRepository postQueryRepository;

    @Value("${app.feed-image-storage-dir:./private/feed-images}")
    private String storageDir;

    public record MediaInfo(
            Long id,
            Long postId,
            String filename,
            String url
    ) {}

    public record MediaFile(
            Path path,
            String contentType
    ) {}

    private record CheckedImage(
            byte[] bytes,
            String filename,
            String contentType,
            String extension
    ) {}

    private Path directory() {
        return Path.of(storageDir)
                .toAbsolutePath()
                .normalize();
    }

    private Path safeFilePath(String storageKey) {
        Path root = directory();
        Path path = root.resolve(storageKey).normalize();

        if (!path.startsWith(root)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "잘못된 파일 경로입니다."
            );
        }

        return path;
    }

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

    private MediaInfo toInfo(PostMedia media) {
        return new MediaInfo(
                media.getId(),
                media.getPost().getId(),
                media.getOriginalFilename(),
                "/api/feed/posts/" + media.getPost().getId()
                        + "/media/" + media.getId()
        );
    }

    // =============================================
    // 이미지 등록: 최대 4장
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
                || post.getModerationStatus() == ModerationStatus.REMOVED) {
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

        // 파일 작성 전에 모든 이미지를 검증
        List<CheckedImage> checked =
                Arrays.stream(files).map(this::check).toList();

        List<Path> createdFiles = new ArrayList<>();

        // DB 트랜잭션이 롤백되면 저장 파일 정리
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            cleanup(createdFiles);
                        }
                    }
                }
        );

        List<MediaInfo> result = new ArrayList<>();

        try {
            Files.createDirectories(directory());

            int order = (int) currentCount;

            for (CheckedImage image : checked) {

                String key = UUID.randomUUID()
                        + image.extension();

                Path path = safeFilePath(key);

                Files.write(
                        path,
                        image.bytes(),
                        StandardOpenOption.CREATE_NEW
                );

                createdFiles.add(path);

                PostMedia media = PostMedia.create(
                        post,
                        key,
                        image.filename(),
                        image.contentType(),
                        image.bytes().length,
                        order++
                );

                result.add(
                        toInfo(mediaRepository.saveAndFlush(media))
                );
            }

            return result;

        } catch (IOException | RuntimeException ex) {
            cleanup(createdFiles);

            if (ex instanceof RuntimeException runtime) {
                throw runtime;
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "이미지를 저장하지 못했습니다.",
                    ex
            );
        }
    }

    private void cleanup(List<Path> paths) {
        for (Path path : paths) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // 운영에서는 삭제 실패 로그 및 재처리 필요
            }
        }
    }

    // =============================================
    // 이미지 공개 여부
    // =============================================
    private boolean canViewMedia(
            PostMedia media,
            Long userId
    ) {
        Post post = media.getPost();

        if (post.getModerationStatus() == ModerationStatus.HIDDEN
                || post.getModerationStatus() == ModerationStatus.REMOVED
                || media.getModerationStatus() == ModerationStatus.REMOVED
                || media.getModerationStatus() == ModerationStatus.HIDDEN) {
            return false;
        }

        // 작성자는 검수 전 자기 이미지 확인 가능
        if (post.getUser().getId().equals(userId)) {
            return true;
        }

        if (!visibilityService.canRead(post, userId)
                || media.getModerationStatus() != ModerationStatus.VISIBLE) {
            return false;
        }

        Integer required = media.getRequiredEpisodeNumber();

        if (required == null) return true;

        int watched = progressRepository
                .findByUser_IdAndDrama_Id(
                        userId,
                        post.getDrama().getId()
                )
                .map(p -> p.getWatchedEpisodeNumber())
                .orElse(0);

        return watched >= required;
    }

    // 사용자에게 허용된 이미지 목록
    @Transactional(readOnly = true)
    public List<MediaInfo> list(Long postId, Long userId) {
        Post post = findPost(postId);

        if (!visibilityService.canRead(post, userId)) {
            return List.of();
        }

        return mediaRepository
                .findByPost_IdOrderBySortOrderAsc(postId)
                .stream()
                .filter(media -> canViewMedia(media, userId))
                .map(this::toInfo)
                .toList();
    }

    // 이미지 파일 응답 전 권한 확인
    @Transactional(readOnly = true)
    public MediaFile file(
            Long postId,
            Long mediaId,
            Long userId
    ) {
        PostMedia media = mediaRepository.findById(mediaId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND)
                );

        if (!media.getPost().getId().equals(postId)
                || !canViewMedia(media, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return loadFile(media);
    }

    private MediaFile loadFile(PostMedia media) {
        Path path = safeFilePath(media.getStorageKey());

        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return new MediaFile(path, media.getMediaType());
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
                        "/api/admin/feed/media/" + media.getId()
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

        return loadFile(media);
    }

    // 관리자 이미지 검수
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
