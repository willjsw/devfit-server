package com.amcamp.domain.meeting.dao;

import com.amcamp.domain.meeting.dto.response.MeetingInfoResponse;
import org.springframework.data.domain.Slice;

public interface MeetingRepositoryCustom {
    Slice<MeetingInfoResponse> findAllBySprintIdWithPagination(
            Long sprintId, Long lastMeetingId, int pageSize);
}
