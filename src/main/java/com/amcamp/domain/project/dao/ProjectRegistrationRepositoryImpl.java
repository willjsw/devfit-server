package com.amcamp.domain.project.dao;

import static com.amcamp.domain.member.domain.QMember.member;
import static com.amcamp.domain.project.domain.QProjectRegistration.projectRegistration;

import com.amcamp.domain.project.dto.response.ProjectRegisterDetailResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

@RequiredArgsConstructor
public class ProjectRegistrationRepositoryImpl implements ProjectRegistrationRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Slice<ProjectRegisterDetailResponse> findAllByProjectIdWithPagination(
            Long projectId, Long lastRegistrationId, int pageSize) {

        List<ProjectRegisterDetailResponse> responses =
                jpaQueryFactory
                        .select(
                                Projections.constructor(
                                        ProjectRegisterDetailResponse.class,
                                        projectRegistration.project.id,
                                        projectRegistration.id,
                                        projectRegistration.requester.id,
                                        member.nickname,
                                        member.profileImageUrl,
                                        projectRegistration.requestStatus))
                        .from(projectRegistration)
                        .leftJoin(member)
                        .on(projectRegistration.requester.member.eq(member))
                        .where(
                                projectRegistration.project.id.eq(projectId),
                                lastProjectRegistrationCondition(lastRegistrationId))
                        .orderBy(projectRegistration.id.desc())
                        .limit(pageSize + 1)
                        .fetch();

        return checkLastPage(pageSize, responses);
    }

    private BooleanExpression lastProjectRegistrationCondition(Long projectRegistrationId) {
        return (projectRegistrationId == null)
                ? null
                : projectRegistration.id.lt(projectRegistrationId);
    }

    private Slice<ProjectRegisterDetailResponse> checkLastPage(
            int pageSize, List<ProjectRegisterDetailResponse> results) {
        boolean hasNext = false;

        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }

        return new SliceImpl<>(results, PageRequest.of(0, pageSize), hasNext);
    }
}
