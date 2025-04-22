package com.amcamp.domain.task.dao;

import static com.amcamp.domain.member.domain.QMember.member;
import static com.amcamp.domain.project.domain.QProjectParticipant.projectParticipant;
import static com.amcamp.domain.task.domain.QTask.task;
import static com.amcamp.domain.team.domain.QTeamParticipant.teamParticipant;

import com.amcamp.domain.member.domain.QMember;
import com.amcamp.domain.project.domain.ProjectParticipant;
import com.amcamp.domain.task.domain.AssignedStatus;
import com.amcamp.domain.task.domain.QTask;
import com.amcamp.domain.task.dto.response.TaskBasicInfoResponse;
import com.amcamp.domain.task.dto.response.TaskInfoResponse;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TaskRepositoryImpl implements TaskRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Slice<TaskInfoResponse> findBySprint(Long sprintId, Long lastTaskId, int size) {
        List<TaskInfoResponse> results =
                jpaQueryFactory
                        .select(
                                Projections.constructor(
                                        TaskInfoResponse.class,
                                        task.id,
                                        task.description,
                                        task.taskDifficulty,
                                        task.dueDt,
                                        task.taskStatus,
                                        task.assignedStatus,
                                        task.sosStatus,
                                        getAssigneeId(task),
                                        getAssigneeNickname(member),
                                        getAssigneeProfileImageUrl(member)))
                        .from(task)
                        .leftJoin(task.assignee, projectParticipant)
                        .leftJoin(projectParticipant.teamParticipant, teamParticipant)
                        .leftJoin(teamParticipant.member, member)
                        .where(task.sprint.id.eq(sprintId), lastTaskId(lastTaskId))
                        .orderBy(task.createdDt.asc())
                        .limit(size + 1)
                        .fetch();

        return checkLastPage(size, results);
    }

    @Override
    public Slice<TaskBasicInfoResponse> findBySprintAndAssignee(
            Long sprintId, ProjectParticipant assignee, Long lastTaskId, int size) {
        List<TaskBasicInfoResponse> results =
                jpaQueryFactory
                        .select(
                                Projections.constructor(
                                        TaskBasicInfoResponse.class,
                                        task.id,
                                        task.description,
                                        task.taskStatus,
                                        task.sosStatus))
                        .from(task)
                        .where(
                                task.sprint.id.eq(sprintId),
                                lastTaskId(lastTaskId),
                                task.assignee.eq(assignee))
                        .orderBy(task.createdDt.asc())
                        .limit(size + 1)
                        .fetch();

        return checkLastPage(size, results);
    }

    public Expression<Long> getAssigneeId(QTask task) {
        return new CaseBuilder()
                .when(task.assignedStatus.eq(AssignedStatus.ASSIGNED))
                .then(task.assignee.id)
                .otherwise(Expressions.nullExpression());
    }

    public Expression<String> getAssigneeNickname(QMember member) {
        return new CaseBuilder()
                .when(member.isNotNull())
                .then(member.nickname)
                .otherwise(Expressions.nullExpression());
    }

    private Expression<String> getAssigneeProfileImageUrl(QMember member) {
        return new CaseBuilder()
                .when(member.isNotNull())
                .then(member.profileImageUrl)
                .otherwise(Expressions.nullExpression());
    }

    private BooleanExpression lastTaskId(Long taskId) {
        if (taskId == null) {
            return null;
        }
        return task.id.gt(taskId);
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
