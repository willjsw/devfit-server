package com.amcamp.domain.sprint.dao;

import static com.amcamp.domain.sprint.domain.QSprint.sprint;
import static com.amcamp.domain.task.domain.QTask.task;

import com.amcamp.domain.project.domain.ProjectParticipant;
import com.amcamp.domain.sprint.dto.response.SprintDetailResponse;
import com.amcamp.domain.sprint.dto.response.SprintIdResponse;
import com.amcamp.domain.task.dto.response.TaskBasicInfoResponse;
import com.amcamp.global.exception.CommonException;
import com.amcamp.global.exception.errorcode.SprintErrorCode;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SprintRepositoryImpl implements SprintRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Slice<SprintDetailResponse> findAllSprintByProjectId(
            Long projectId, Long baseSprintId, SprintPagingDirection direction) {
        List<SprintDetailResponse> sprintList = fetchSprintList(projectId, baseSprintId, direction);
        List<TaskBasicInfoResponse> taskList = fetchTaskList(sprintList.get(0).id());

        List<SprintDetailResponse> results = convertToSprintDetails(sprintList, taskList);

        return checkLastPage(results);
    }

    @Override
    public Slice<SprintDetailResponse> findAllSprintByProjectIdAndAssignee(
            Long projectId,
            Long baseSprintId,
            SprintPagingDirection direction,
            ProjectParticipant participant) {
        List<SprintDetailResponse> sprintList = fetchSprintList(projectId, baseSprintId, direction);
        List<TaskBasicInfoResponse> taskList =
                fetchTaskListByAssignee(sprintList.get(0).id(), participant);

        List<SprintDetailResponse> results = convertToSprintDetails(sprintList, taskList);

        return checkLastPage(results);
    }

    @Override
    public List<SprintIdResponse> findAllSprintIdByProjectId(Long projectId) {
        List<SprintIdResponse> results =
                jpaQueryFactory
                        .select(
                                Projections.constructor(
                                        SprintIdResponse.class, sprint.id, sprint.title))
                        .from(sprint)
                        .where(sprint.project.id.eq(projectId))
                        .orderBy(sprint.id.asc())
                        .fetch();

        return results;
    }

    private BooleanExpression buildPagingCondition(
            Long baseSprintId, SprintPagingDirection direction) {
        if (baseSprintId == null) {
            return null;
        }
        return direction == SprintPagingDirection.NEXT
                ? sprint.id.gt(baseSprintId)
                : sprint.id.lt(baseSprintId);
    }

    private OrderSpecifier<?> getSprintPagingOrder(SprintPagingDirection direction) {
        return (direction == null || direction == SprintPagingDirection.PREV)
                ? sprint.id.desc()
                : sprint.id.asc();
    }

    private Slice<SprintDetailResponse> checkLastPage(List<SprintDetailResponse> results) {
        boolean hasNext = false;

        if (results.size() > 1) {
            hasNext = true;
            results.remove(1);
        }

        return new SliceImpl<>(results, PageRequest.of(0, 1), hasNext);
    }

    private List<SprintDetailResponse> convertToSprintDetails(
            List<SprintDetailResponse> sprintList, List<TaskBasicInfoResponse> taskList) {
        return sprintList.stream()
                .map(
                        sprint ->
                                new SprintDetailResponse(
                                        sprint.id(),
                                        sprint.title(),
                                        sprint.goal(),
                                        sprint.startDt(),
                                        sprint.dueDt(),
                                        sprint.progress(),
                                        taskList))
                .collect(Collectors.toList());
    }

    private List<SprintDetailResponse> fetchSprintList(
            Long projectId, Long baseSprintId, SprintPagingDirection direction) {
        List<SprintDetailResponse> results =
                jpaQueryFactory
                        .select(
                                Projections.constructor(
                                        SprintDetailResponse.class,
                                        sprint.id,
                                        sprint.title,
                                        sprint.goal,
                                        sprint.startDt,
                                        sprint.dueDt,
                                        sprint.progress.intValue(),
                                        Expressions.constant(Collections.emptyList())))
                        .from(sprint)
                        .where(
                                buildPagingCondition(baseSprintId, direction),
                                sprint.project.id.eq(projectId))
                        .orderBy(getSprintPagingOrder(direction))
                        .limit(2)
                        .fetch();

        if (results.isEmpty()) {
            if (direction == null) {
                throw new CommonException(SprintErrorCode.SPRINT_NOT_EXISTS);
            }

            switch (direction) {
                case NEXT -> throw new CommonException(SprintErrorCode.NEXT_SPRINT_NOT_EXISTS);
                case PREV -> throw new CommonException(SprintErrorCode.PREV_SPRINT_NOT_EXISTS);
            }
        }

        return results;
    }

    private List<TaskBasicInfoResponse> fetchTaskList(Long sprintId) {
        return fetchTaskListWithCondition(sprintId, null);
    }

    private List<TaskBasicInfoResponse> fetchTaskListByAssignee(
            Long sprintId, ProjectParticipant participant) {
        return fetchTaskListWithCondition(sprintId, task.assignee.eq(participant));
    }

    private List<TaskBasicInfoResponse> fetchTaskListWithCondition(
            Long sprintId, BooleanExpression condition) {
        return jpaQueryFactory
                .select(
                        Projections.constructor(
                                TaskBasicInfoResponse.class,
                                task.id,
                                task.description,
                                task.taskStatus,
                                task.sosStatus))
                .from(task)
                .where(task.sprint.id.eq(sprintId), condition)
                .orderBy(task.id.asc())
                .fetch();
    }
}
