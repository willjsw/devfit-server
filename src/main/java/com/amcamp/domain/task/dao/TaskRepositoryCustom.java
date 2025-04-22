package com.amcamp.domain.task.dao;

import com.amcamp.domain.project.domain.ProjectParticipant;
import com.amcamp.domain.task.dto.response.TaskBasicInfoResponse;
import com.amcamp.domain.task.dto.response.TaskInfoResponse;
import org.springframework.data.domain.Slice;

public interface TaskRepositoryCustom {
    Slice<TaskInfoResponse> findBySprint(Long sprintId, Long lastTaskId, int pageSize);

    Slice<TaskBasicInfoResponse> findBySprintAndAssignee(
            Long sprintId, ProjectParticipant assignee, Long lastTaskId, int pageSize);
}
