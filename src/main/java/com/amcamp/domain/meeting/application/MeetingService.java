package com.amcamp.domain.meeting.application;

import com.amcamp.domain.meeting.dao.MeetingRepository;
import com.amcamp.domain.meeting.domain.Meeting;
import com.amcamp.domain.meeting.domain.MeetingStatus;
import com.amcamp.domain.meeting.dto.request.MeetingCreateRequest;
import com.amcamp.domain.meeting.dto.request.MeetingDtUpdateRequest;
import com.amcamp.domain.meeting.dto.request.MeetingTitleUpdateRequest;
import com.amcamp.domain.meeting.dto.response.MeetingInfoResponse;
import com.amcamp.domain.member.domain.Member;
import com.amcamp.domain.project.dao.ProjectParticipantRepository;
import com.amcamp.domain.project.domain.Project;
import com.amcamp.domain.project.domain.ProjectParticipant;
import com.amcamp.domain.sprint.dao.SprintRepository;
import com.amcamp.domain.sprint.domain.Sprint;
import com.amcamp.domain.team.dao.TeamParticipantRepository;
import com.amcamp.domain.team.domain.Team;
import com.amcamp.domain.team.domain.TeamParticipant;
import com.amcamp.global.exception.CommonException;
import com.amcamp.global.exception.errorcode.MeetingErrorCode;
import com.amcamp.global.exception.errorcode.ProjectErrorCode;
import com.amcamp.global.exception.errorcode.SprintErrorCode;
import com.amcamp.global.exception.errorcode.TeamErrorCode;
import com.amcamp.global.util.MemberUtil;
import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MeetingService {

    private final TeamParticipantRepository teamParticipantRepository;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final MeetingRepository meetingRepository;
    private final SprintRepository sprintRepository;
    private final MemberUtil memberUtil;

    // 미팅 생성
    public void createMeeting(MeetingCreateRequest request) {
        Member member = memberUtil.getCurrentMember();
        Sprint sprint = getValidSprint(member, request.sprintId());
        validateMeetingTime(null, sprint, request.meetingStart(), request.meetingEnd());
        meetingRepository.save(
                Meeting.createMeeting(
                        request.title(), request.meetingStart(), request.meetingEnd(), sprint));
    }

    @Transactional(readOnly = true)
    public MeetingInfoResponse getMeeting(Long meetingId) {
        Member member = memberUtil.getCurrentMember();
        Meeting meeting = getMeetingById(meetingId);
        validateProjectParticipant(member, meeting.getSprint().getProject());
        return MeetingInfoResponse.from(meeting);
    }

    @Transactional(readOnly = true)
    public Slice<MeetingInfoResponse> getMeetingList(
            Long sprintId, Long lastMeetingId, int pageSize) {
        Member member = memberUtil.getCurrentMember();
        Sprint sprint = getValidSprint(member, sprintId);
        validateProjectParticipant(member, sprint.getProject());
        return meetingRepository.findAllBySprintIdWithPagination(sprintId, lastMeetingId, pageSize);
    }

    // 업데이트
    public void updateMeetingTitle(Long meetingId, MeetingTitleUpdateRequest request) {
        Member member = memberUtil.getCurrentMember();
        Meeting meeting = getMeetingById(meetingId);
        validateProjectParticipant(member, meeting.getSprint().getProject());

        meeting.updateTitle(request.title());
    }

    public void updateMeetingDt(Long meetingId, MeetingDtUpdateRequest request) {
        Member member = memberUtil.getCurrentMember();
        Meeting meeting = getMeetingById(meetingId);
        validateProjectParticipant(member, meeting.getSprint().getProject());
        LocalDateTime meetingStart =
                request.meetingStart() != null ? request.meetingStart() : meeting.getMeetingStart();
        LocalDateTime meetingEnd =
                request.meetingEnd() != null ? request.meetingEnd() : meeting.getMeetingEnd();
        validateMeetingTime(meeting.getId(), meeting.getSprint(), meetingStart, meetingEnd);
        meeting.updateMeetingDt(meetingStart, meetingEnd);
    }

    // 삭제
    public void deleteMeeting(Long meetingId) {
        Member member = memberUtil.getCurrentMember();
        Meeting meeting = getMeetingById(meetingId);
        validateProjectParticipant(member, meeting.getSprint().getProject());
        meetingRepository.delete(meeting);
    }

    // util
    private void validateMeetingTime(
            @Nullable Long meetingId,
            Sprint sprint,
            LocalDateTime meetingStart,
            LocalDateTime meetingEnd) {

        if (!meetingStart.isBefore(meetingEnd) || meetingStart.isEqual(meetingEnd)) {
            throw new CommonException(MeetingErrorCode.INVALID_MEETING_TIME_RANGE);
        }
        // 8:00~00:00
        if (meetingStart.getHour() < 8
                || (meetingEnd.getHour() == 0 && meetingEnd.getMinute() > 0)) {
            throw new CommonException(MeetingErrorCode.INVALID_MEETING_TIME_RANGE);
        }
        // 스프린트 범위 확인
        if (meetingStart.toLocalDate().isBefore(sprint.getStartDt())
                || meetingEnd.toLocalDate().isAfter(sprint.getDueDt())) {
            throw new CommonException(MeetingErrorCode.MEETING_DATE_OUT_OF_SPRINT);
        } // 기존 일정과의 중복 확인
        if (meetingRepository
                .findOverlappingMeeting(meetingId, sprint, meetingStart, meetingEnd)
                .isPresent()) {
            throw new CommonException(MeetingErrorCode.MEETING_ALREADY_EXISTS);
        }
    }

    private TeamParticipant getValidTeamParticipant(Member member, Team team) {
        return teamParticipantRepository
                .findByMemberAndTeam(member, team)
                .orElseThrow(() -> new CommonException(TeamErrorCode.TEAM_PARTICIPANT_REQUIRED));
    }

    private ProjectParticipant validateProjectParticipant(Member member, Project project) {
        TeamParticipant teamParticipant = getValidTeamParticipant(member, project.getTeam());
        return projectParticipantRepository
                .findByProjectAndTeamParticipant(project, teamParticipant)
                .orElseThrow(
                        () -> new CommonException(ProjectErrorCode.PROJECT_PARTICIPATION_REQUIRED));
    }

    private Sprint getValidSprint(Member member, Long sprintId) {
        Sprint sprint =
                sprintRepository
                        .findById(sprintId)
                        .orElseThrow(() -> new CommonException(SprintErrorCode.SPRINT_NOT_FOUND));
        validateProjectParticipant(member, sprint.getProject());
        return sprint;
    }

    private Meeting getMeetingById(Long meetingId) {
        return meetingRepository
                .findById(meetingId)
                .orElseThrow(() -> new CommonException(MeetingErrorCode.MEETING_NOT_FOUND));
    }

    public void updateExpiredMeetings() {
        // 상태:OPEN, meetingEnd 현재 시간보다 이전인 미팅
        List<Meeting> expiredMeetings =
                meetingRepository.findByStatusAndMeetingEndBefore(
                        MeetingStatus.OPEN, LocalDateTime.now());

        // CLOSE로 변경
        expiredMeetings.forEach(meeting -> meeting.updateStatus(MeetingStatus.CLOSE));
    }
}
