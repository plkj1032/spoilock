package com.spoilock.backend.report.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.user.enums.AccountStatus;
import com.spoilock.backend.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.spoilock.backend.user.entity.QUser.user;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatReportRecipientRepository {
    private final JPAQueryFactory queryFactory;

    public List<Long> findActiveAdminIds(){
        return queryFactory
                .select(user.id)
                .from(user)
                .where(
                        user.role.eq(UserRole.ADMIN),
                        user.accountStatus.eq(AccountStatus.ACTIVE)
                )
                .orderBy(user.id.asc())
                .fetch();
    }
}
