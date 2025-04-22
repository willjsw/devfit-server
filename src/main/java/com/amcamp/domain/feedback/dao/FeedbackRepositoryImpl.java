package com.amcamp.domain.feedback.dao;

import static com.amcamp.domain.feedback.domain.QFeedback.feedback;
import static com.amcamp.domain.member.domain.QMember.member;
import static com.amcamp.domain.project.domain.QProjectParticipant.projectParticipant;
import static com.amcamp.domain.team.domain.QTeamParticipant.teamParticipant;

import com.amcamp.domain.feedback.dto.response.FeedbackInfoResponse;
import com.amcamp.domain.project.domain.ProjectParticipant;
import com.amcamp.domain.project.dto.response.ProjectParticipantFeedbackInfoResponse;
import com.amcamp.global.exception.CommonException;
import com.amcamp.global.exception.errorcode.FeedbackErrorCode;
import com.amcamp.global.exception.errorcode.ProjectErrorCode;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedbackRepositoryImpl implements FeedbackRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Slice<FeedbackInfoResponse> findSprintFeedbacksByParticipant(
            Long projectParticipantId, Long sprintId, Long lastFeedbackId, int pageSize) {
        List<FeedbackInfoResponse> results =
                jpaQueryFactory
                        .select(
                                Projections.constructor(
                                        FeedbackInfoResponse.class,
                                        feedback.id,
                                        feedback.message,
                                        feedback.createdDt))
                        .from(feedback)
                        .where(
                                feedback.sprint.id.eq(sprintId),
                                feedback.receiver.id.eq(projectParticipantId),
                                lastFeedbackId(lastFeedbackId))
                        .orderBy(feedback.createdDt.desc())
                        .limit(pageSize + 1)
                        .fetch();

        if (results.isEmpty()) {
            throw new CommonException(FeedbackErrorCode.FEEDBACK_NOT_EXISTS);
        }

        return checkLastPage(pageSize, results);
    }

    @Override
    public Slice<ProjectParticipantFeedbackInfoResponse> findSprintFeedbackStatusByParticipant(
            ProjectParticipant sender, Long sprintId, Long lastProjectParticipantId, int pageSize) {

        // 1. sender가 이 sprint에서 피드백한 receiver ID 목록 먼저 뽑기
        List<Long> feedbackGivenReceiverIds =
                jpaQueryFactory
                        .select(feedback.receiver.id)
                        .from(feedback)
                        .where(feedback.sender.eq(sender), feedback.sprint.id.eq(sprintId))
                        .fetch();

        // 2. 프로젝트 참여자 목록 뽑고, 해당 ID가 feedbackGivenReceiverIds 안에 있는지 체크해서 status 판단
        List<ProjectParticipantFeedbackInfoResponse> results =
                jpaQueryFactory
                        .select(
                                Projections.constructor(
                                        ProjectParticipantFeedbackInfoResponse.class,
                                        projectParticipant.id,
                                        member.nickname,
                                        member.profileImageUrl,
                                        projectParticipant.projectRole,
                                        projectParticipant.status,
                                        new CaseBuilder()
                                                .when(
                                                        projectParticipant.id.in(
                                                                feedbackGivenReceiverIds))
                                                .then("COMPLETED")
                                                .otherwise("PENDING")))
                        .from(projectParticipant)
                        .leftJoin(projectParticipant.teamParticipant, teamParticipant)
                        .leftJoin(teamParticipant.member, member)
                        .where(
                                projectParticipant.project.eq(sender.getProject()),
                                lastProjectParticipantId(lastProjectParticipantId))
                        .orderBy(projectParticipant.id.asc())
                        .limit(pageSize + 1)
                        .fetch();

        if (results.isEmpty()) {
            throw new CommonException(ProjectErrorCode.PROJECT_PARTICIPANT_NOT_EXISTS);
        }

        return checkLastPage(pageSize, results);
    }

    private BooleanExpression lastFeedbackId(Long feedbackId) {
        if (feedbackId == null) {
            return null;
        }

        return feedback.id.lt(feedbackId);
    }

    private BooleanExpression lastProjectParticipantId(Long projectParticipantId) {
        if (projectParticipantId == null) {
            return null;
        }

        return projectParticipant.id.gt(projectParticipantId);
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
