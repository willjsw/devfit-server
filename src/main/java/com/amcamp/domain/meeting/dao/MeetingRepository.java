package com.amcamp.domain.meeting.dao;

import com.amcamp.domain.meeting.domain.Meeting;
import com.amcamp.domain.meeting.domain.MeetingStatus;
import com.amcamp.domain.sprint.domain.Sprint;
import feign.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MeetingRepository extends JpaRepository<Meeting, Long>, MeetingRepositoryCustom {
    @Query(
            "select m from Meeting m where m.sprint = :sprint "
                    + " and ((:meetingId is NULL) or m.id <> :meetingId)"
                    + "and ((:meetingStart between m.meetingStart and m.meetingEnd) "
                    + "or (:meetingEnd between m.meetingStart and m.meetingEnd) "
                    + "or (m.meetingStart between :meetingStart and :meetingEnd) "
                    + "or (m.meetingEnd between :meetingStart and :meetingEnd))")
    Optional<Meeting> findOverlappingMeeting(
            @Param("meetingId") Long meetingId,
            @Param("sprint") Sprint sprint,
            @Param("meetingStart") LocalDateTime meetingStart,
            @Param("meetingEnd") LocalDateTime meetingEnd);

    List<Meeting> findByStatusAndMeetingEndBefore(MeetingStatus status, LocalDateTime dateTime);
}
