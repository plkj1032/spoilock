package com.spoilock.backend.user.entity;

import com.spoilock.backend.common.entity.AuditedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "terms_documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermsDocument extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "terms_type", nullable = false, length = 30)
    private String termsType;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "effective_at")
    private LocalDateTime effectiveAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    public static TermsDocument draft(
            String type,
            String version,
            String title,
            String content,
            User admin
    )
    {
        TermsDocument document = new TermsDocument();

        document.termsType = type;
        document.version = version.trim();
        document.title = title.trim();
        document.content = content.trim();
        document.required = "TERMS".equals(type);
        document.createdBy = admin;

        return document;
    }

    public void updateDraft(String title, String content)
    {
        if(!"DRAFT".equals(status))
        {
            throw new IllegalStateException(
                    "초안 상태에서만 수정할 수 있습니다."
            );
        }

        this.title = title.trim();
        this.content = content.trim();
    }

    public void publish(LocalDateTime now)
    {
        if(!"DRAFT".equals(status))
        {
            throw new IllegalStateException(
                    "초안만 게시할 수 있습니다."
            );
        }

        this.status = "PUBLISHED";
        this.publishedAt = now;
        this.effectiveAt = now;
    }

    public void archive()
    {
        if("PUBLISHED".equals(status))
        {
            this.status = "ARCHIVED";
        }
    }


}
