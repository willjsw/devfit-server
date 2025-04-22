package com.amcamp.domain.meeting.dao;

import static com.amcamp.domain.meeting.domain.QMeeting.meeting;
import static com.amcamp.domain.sprint.domain.QSprint.sprint;

import com.amcamp.domain.meeting.dto.response.MeetingInfoResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

@RequiredArgsConstructor
public class MeetingRepositoryImpl implements MeetingRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Slice<MeetingInfoResponse> findAllBySprintIdWithPagination(
            Long sprintId, Long lastMeetingId, int pageSize) {

        List<MeetingInfoResponse> responses =
                jpaQueryFactory
                        .select(
                                Projections.constructor(
                                        MeetingInfoResponse.class,
                                        meeting.id,
                                        meeting.title,
                                        meeting.meetingStart,
                                        meeting.meetingEnd))
                        .from(meeting)
                        .leftJoin(sprint)
                        .on(meeting.sprint.eq(sprint))
                        .where(meeting.sprint.id.eq(sprintId), lastMeetingCondition(lastMeetingId))
                        .orderBy(meeting.id.desc())
                        .limit(pageSize + 1)
                        .fetch();

        return checkLastPage(pageSize, responses);
    }

    private BooleanExpression lastMeetingCondition(Long meetingId) {
        return (meetingId == null) ? null : meeting.id.lt(meetingId);
    }

    private Slice<MeetingInfoResponse> checkLastPage(
            int pageSize, List<MeetingInfoResponse> results) {
        boolean hasNext = false;

        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }

        return new SliceImpl<>(results, PageRequest.of(0, pageSize), hasNext);
    }
}
