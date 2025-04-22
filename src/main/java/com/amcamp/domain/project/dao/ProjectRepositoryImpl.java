package com.amcamp.domain.project.dao;

import static com.amcamp.domain.project.domain.QProject.project;
import static com.amcamp.domain.project.domain.QProjectParticipant.projectParticipant;
import static com.amcamp.domain.project.domain.QProjectRegistration.projectRegistration;

import com.amcamp.domain.project.domain.ProjectParticipantRole;
import com.amcamp.domain.project.domain.ProjectParticipantStatus;
import com.amcamp.domain.project.dto.response.ProjectInfoResponse;
import com.amcamp.domain.project.dto.response.ProjectListInfoResponse;
import com.amcamp.domain.project.dto.response.ProjectParticipantInfoResponse;
import com.amcamp.domain.team.domain.TeamParticipant;
import com.amcamp.global.exception.CommonException;
import com.amcamp.global.exception.errorcode.ProjectErrorCode;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

@RequiredArgsConstructor
public class ProjectRepositoryImpl implements ProjectRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Slice<ProjectListInfoResponse> findAllByTeamIdWithPagination(
            Long teamId, Long lastProjectId, int pageSize, TeamParticipant teamParticipant) {

        NumberExpression<Integer> activeParticipation =
                new CaseBuilder()
                        .when(projectParticipant.status.eq(ProjectParticipantStatus.ACTIVE))
                        .then(1)
                        .otherwise(0)
                        .coalesce(0);

        NumberExpression<Integer> adminCase =
                new CaseBuilder()
                        .when(projectParticipant.projectRole.eq(ProjectParticipantRole.ADMIN))
                        .then(1)
                        .otherwise(0);

        StringExpression joinStatusExpr =
                projectRegistration.requestStatus.stringValue().min().coalesce("NONE");

        List<ProjectListInfoResponse> responses =
                jpaQueryFactory
                        .select(
                                Projections.constructor(
                                        ProjectListInfoResponse.class,
                                        Projections.constructor(
                                                ProjectInfoResponse.class,
                                                project.id,
                                                project.title,
                                                project.team.name,
                                                project.description,
                                                project.startDt,
                                                project.dueDt),
                                        activeParticipation.sum().gt(0),
                                        adminCase.sum().gt(0),
                                        joinStatusExpr))
                        .from(project)
                        .leftJoin(projectParticipant)
                        .on(
                                projectParticipant
                                        .project
                                        .eq(project)
                                        .and(
                                                projectParticipant.teamParticipant.eq(
                                                        teamParticipant)))
                        .leftJoin(projectRegistration)
                        .on(
                                projectRegistration
                                        .project
                                        .eq(project)
                                        .and(projectRegistration.requester.eq(teamParticipant)))
                        .where(project.team.id.eq(teamId), lastProjectCondition(lastProjectId))
                        .groupBy(project.id)
                        .orderBy(project.id.desc())
                        .limit(pageSize + 1)
                        .fetch();

        return checkLastPage(pageSize, responses);
    }

    @Override
    public Slice<ProjectParticipantInfoResponse> findAllProjectParticipantByProject(
            Long projectId, Long lastProjectParticipantId, int pageSize) {
        List<ProjectParticipantInfoResponse> results =
                jpaQueryFactory
                        .select(
                                Projections.constructor(
                                        ProjectParticipantInfoResponse.class,
                                        projectParticipant.id,
                                        projectParticipant.teamParticipant.member.nickname,
                                        projectParticipant.teamParticipant.member.profileImageUrl,
                                        projectParticipant.projectRole,
                                        projectParticipant.status))
                        .from(projectParticipant)
                        .where(
                                projectParticipant.project.id.eq(projectId),
                                lastProjectParticipantId(lastProjectParticipantId))
                        .limit(pageSize + 1)
                        .fetch();

        if (results.isEmpty()) {
            throw new CommonException(ProjectErrorCode.PROJECT_PARTICIPANT_NOT_EXISTS);
        }

        return checkLastPage(pageSize, results);
    }

    private BooleanExpression lastProjectCondition(Long projectId) {
        return (projectId == null) ? null : project.id.lt(projectId);
    }

    private BooleanExpression lastProjectParticipantId(Long projectParticipantId) {
        return (projectParticipantId == null)
                ? null
                : projectParticipant.id.gt(projectParticipantId);
    }

    private <T> Slice<T> checkLastPage(int pageSize, List<T> results) {
        boolean hasNext = false;

        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }

        return new SliceImpl<>(results, PageRequest.of(0, pageSize), hasNext);
    }
}
