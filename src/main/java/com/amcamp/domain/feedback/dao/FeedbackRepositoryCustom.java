package com.amcamp.domain.feedback.dao;

import com.amcamp.domain.feedback.dto.response.FeedbackInfoResponse;
import com.amcamp.domain.project.domain.ProjectParticipant;
import com.amcamp.domain.project.dto.response.ProjectParticipantFeedbackInfoResponse;
import org.springframework.data.domain.Slice;

public interface FeedbackRepositoryCustom {
    Slice<FeedbackInfoResponse> findSprintFeedbacksByParticipant(
            Long projectParticipantId, Long sprintId, Long lastFeedbackId, int pageSize);

    Slice<ProjectParticipantFeedbackInfoResponse> findSprintFeedbackStatusByParticipant(
            ProjectParticipant projectParticipant,
            Long sprintId,
            Long lastProjectParticipantId,
            int pageSize);
}
