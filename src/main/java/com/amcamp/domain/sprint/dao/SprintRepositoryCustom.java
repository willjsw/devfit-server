package com.amcamp.domain.sprint.dao;

import com.amcamp.domain.project.domain.ProjectParticipant;
import com.amcamp.domain.sprint.dto.response.SprintDetailResponse;
import com.amcamp.domain.sprint.dto.response.SprintIdResponse;
import java.util.List;
import org.springframework.data.domain.Slice;

public interface SprintRepositoryCustom {
    Slice<SprintDetailResponse> findAllSprintByProjectId(
            Long projectId, Long baseSprintId, SprintPagingDirection direction);

    Slice<SprintDetailResponse> findAllSprintByProjectIdAndAssignee(
            Long projectId,
            Long baseSprintId,
            SprintPagingDirection direction,
            ProjectParticipant participant);

    List<SprintIdResponse> findAllSprintIdByProjectId(Long projectId);
}
