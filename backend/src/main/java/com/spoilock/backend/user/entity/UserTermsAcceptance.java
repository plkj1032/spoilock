package com.spoilock.backend.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_terms_acceptances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTermsAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "terms_document_id", nullable = false)
    private TermsDocument termsDocument;

    @CreationTimestamp
    @Column(name = "accepted_at", nullable = false, updatable = false)
    private LocalDateTime acceptedAt;

    @Column(name = "acceptance_source", nullable = false, length = 20)
    private String acceptanceSource;

    public static UserTermsAcceptance create(
            User user,
            TermsDocument document,
            String source
    )
    {
        UserTermsAcceptance acceptance =
                new UserTermsAcceptance();

        acceptance.user = user;
        acceptance.termsDocument = document;
        acceptance.acceptanceSource = source;

        return acceptance;
    }

}
