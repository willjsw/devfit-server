package com.amcamp.domain.meeting.domain;

import com.amcamp.domain.common.model.BaseTimeEntity;
import com.amcamp.domain.sprint.domain.Sprint;
import com.amcamp.global.exception.CommonException;
import com.amcamp.global.exception.errorcode.MeetingErrorCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meeting_id")
    private Long id;

    @Column(name = "meeting_title")
    private String title;

    @Column(name = "meeting_start")
    private LocalDateTime meetingStart;

    @Column(name = "meeting_end")
    private LocalDateTime meetingEnd;

    @Column(name = "meeting_status")
    @Enumerated(EnumType.STRING)
    private MeetingStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @Builder(access = AccessLevel.PRIVATE)
    private Meeting(
            String title,
            LocalDateTime meetingStart,
            LocalDateTime meetingEnd,
            Sprint sprint,
            MeetingStatus status) {
        this.title = title;
        this.meetingStart = meetingStart;
        this.meetingEnd = meetingEnd;
        this.sprint = sprint;
        this.status = status;
    }

    public static Meeting createMeeting(
            String title, LocalDateTime meetingStart, LocalDateTime meetingEnd, Sprint sprint) {

        if (meetingStart.isAfter(meetingEnd) || meetingStart.isEqual(meetingEnd)) {
            throw new CommonException(MeetingErrorCode.INVALID_MEETING_TIME_RANGE);
        }
        return Meeting.builder()
                .title(title)
                .meetingStart(meetingStart)
                .meetingEnd(meetingEnd)
                .sprint(sprint)
                .status(MeetingStatus.OPEN)
                .build();
    }

    public void updateStatus(MeetingStatus status) {
        this.status = status;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateMeetingDt(LocalDateTime meetingStart, LocalDateTime meetingEnd) {
        if (meetingStart != null) {
            this.meetingStart = meetingStart;
        }
        if (meetingEnd != null) {
            this.meetingEnd = meetingEnd;
        }
    }
}
