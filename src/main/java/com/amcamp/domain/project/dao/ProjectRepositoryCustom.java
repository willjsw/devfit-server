package com.amcamp.domain.project.dao;

import com.amcamp.domain.project.dto.response.ProjectListInfoResponse;
import com.amcamp.domain.project.dto.response.ProjectParticipantInfoResponse;
import com.amcamp.domain.team.domain.TeamParticipant;
import org.springframework.data.domain.Slice;

public interface ProjectRepositoryCustom {

    Slice<ProjectListInfoResponse> findAllByTeamIdWithPagination(
            Long teamId, Long lastProjectId, int pageSize, TeamParticipant teamParticipant);

    Slice<ProjectParticipantInfoResponse> findAllProjectParticipantByProject(
            Long projectId, Long lastProjectParticipantId, int pageSize);
}
