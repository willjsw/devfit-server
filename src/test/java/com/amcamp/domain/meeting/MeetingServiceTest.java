package com.amcamp.domain.meeting;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.amcamp.IntegrationTest;
import com.amcamp.domain.meeting.application.MeetingService;
import com.amcamp.domain.meeting.dao.MeetingRepository;
import com.amcamp.domain.meeting.domain.Meeting;
import com.amcamp.domain.meeting.domain.MeetingStatus;
import com.amcamp.domain.meeting.dto.request.MeetingCreateRequest;
import com.amcamp.domain.meeting.dto.request.MeetingDtUpdateRequest;
import com.amcamp.domain.meeting.dto.request.MeetingTitleUpdateRequest;
import com.amcamp.domain.meeting.dto.response.MeetingInfoResponse;
import com.amcamp.domain.member.dao.MemberRepository;
import com.amcamp.domain.member.domain.Member;
import com.amcamp.domain.member.domain.OauthInfo;
import com.amcamp.domain.project.application.ProjectService;
import com.amcamp.domain.project.dao.ProjectParticipantRepository;
import com.amcamp.domain.project.dao.ProjectRegistrationRepository;
import com.amcamp.domain.project.dao.ProjectRepository;
import com.amcamp.domain.project.dto.request.ProjectCreateRequest;
import com.amcamp.domain.sprint.application.SprintService;
import com.amcamp.domain.sprint.dao.SprintRepository;
import com.amcamp.domain.sprint.dto.request.SprintCreateRequest;
import com.amcamp.domain.team.application.TeamService;
import com.amcamp.domain.team.dao.TeamParticipantRepository;
import com.amcamp.domain.team.dao.TeamRepository;
import com.amcamp.domain.team.dto.request.TeamCreateRequest;
import com.amcamp.domain.team.dto.request.TeamInviteCodeRequest;
import com.amcamp.global.exception.CommonException;
import com.amcamp.global.exception.errorcode.MeetingErrorCode;
import com.amcamp.global.security.PrincipalDetails;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class MeetingServiceTest extends IntegrationTest {
    @Autowired private SprintService sprintService;
    @Autowired private ProjectService projectService;
    @Autowired private TeamService teamService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private TeamParticipantRepository teamParticipantRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private ProjectRegistrationRepository projectRegistrationRepository;
    @Autowired private ProjectParticipantRepository projectParticipantRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private SprintRepository sprintRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private MeetingService meetingService;

    private Member memberAdmin;
    private final String projectTitle = "projectTitle";
    private final String description = "projectDescription";
    private final String meetingTitle = "meetingTitle";
    private final LocalDate projectDueDt = LocalDate.of(2026, 12, 1);
    private final LocalDate sprintDueDt = LocalDate.of(2026, 3, 31);
    private final LocalDateTime meetingStart = LocalDateTime.of(2026, 3, 15, 17, 0);
    private final LocalDateTime meetingEnd = LocalDateTime.of(2026, 3, 15, 18, 0);

    private void loginAs(Member member) {
        UserDetails userDetails = new PrincipalDetails(member.getId(), member.getRole());
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    private void logout() {
        SecurityContextHolder.clearContext();
    }

    private TeamInviteCodeRequest teamInviteCodeRequest;

    // 팀 가입 및 아이디 생성
    private Long getTeamId() {
        TeamCreateRequest teamCreateRequest = new TeamCreateRequest("팀 이름", "팀 설명");
        String inviteCode = teamService.createTeam(teamCreateRequest).inviteCode();
        teamInviteCodeRequest = new TeamInviteCodeRequest(inviteCode);
        return teamService.getTeamByCode(teamInviteCodeRequest).teamId();
    }

    // 프로젝트 생성
    void createTestProject() {
        Long teamId = getTeamId();
        ProjectCreateRequest request =
                new ProjectCreateRequest(teamId, projectTitle, projectDueDt, description);

        projectService.createProject(request);
    }

    void createTestProject(Long teamId) {
        ProjectCreateRequest request =
                new ProjectCreateRequest(teamId, projectTitle, projectDueDt, description);

        projectService.createProject(request);
    }

    void createTestProject(Long teamId, String title, LocalDate dueDt, String description) {
        ProjectCreateRequest request = new ProjectCreateRequest(teamId, title, dueDt, description);

        projectService.createProject(request);
    }

    // 스프린트 생성
    void createTestSprint(Long projectId) {
        SprintCreateRequest request =
                new SprintCreateRequest(projectId, "testSprintGoal", sprintDueDt);
        sprintService.createSprint(request);
    }

    // 미팅 생성
    void createTestMeeting(Long sprintId) {
        MeetingCreateRequest request =
                new MeetingCreateRequest(sprintId, meetingTitle, meetingStart, meetingEnd);
        meetingService.createMeeting(request);
    }

    void createTestMeeting(Long sprintId, LocalDateTime meetingStart, LocalDateTime meetingEnd) {
        MeetingCreateRequest request =
                new MeetingCreateRequest(sprintId, meetingTitle, meetingStart, meetingEnd);
        meetingService.createMeeting(request);
    }

    void createTestMeeting(
            Long sprintId,
            String meetingTitle,
            LocalDateTime meetingStart,
            LocalDateTime meetingEnd) {
        MeetingCreateRequest request =
                new MeetingCreateRequest(sprintId, meetingTitle, meetingStart, meetingEnd);
        meetingService.createMeeting(request);
    }

    @BeforeEach
    public void setUp() {
        memberAdmin =
                Member.createMember(
                        "memberAdmin",
                        "testProfileImageUrl",
                        OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"));
        memberRepository.save(memberAdmin);
        loginAs(memberAdmin);
        createTestProject();
        createTestSprint(1L);
    }

    @AfterEach
    public void afterEach() {
        logout();
        projectRegistrationRepository.deleteAll();
        projectParticipantRepository.deleteAll();
        projectRepository.deleteAll();
        teamParticipantRepository.deleteAll();
        teamRepository.deleteAll();
        memberRepository.deleteAll();
        meetingRepository.deleteAll();
        sprintRepository.deleteAll();
    }

    @Nested
    class 미팅_생성 {
        @Test
        void 미팅을_생성하면_정상적으로_저장된다() {
            // given
            createTestMeeting(1L);
            // then
            Meeting meeting = meetingRepository.findById(1L).get();
            assertThat(meeting.getId()).isEqualTo(1L);
            assertThat(meeting)
                    .extracting("id", "title", "meetingStart", "meetingEnd", "status")
                    .containsExactlyInAnyOrder(
                            1L, meetingTitle, meetingStart, meetingEnd, MeetingStatus.OPEN);
        }

        @Test
        void 스프린트_시작일을_벗어나면_오류가_발생한다() {
            // given
            LocalDateTime wrongDtBeforeStartDt = LocalDateTime.of(2020, 3, 15, 17, 0);

            // then
            assertThatThrownBy(() -> createTestMeeting(1L, wrongDtBeforeStartDt, meetingEnd))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.MEETING_DATE_OUT_OF_SPRINT.getMessage());
        }

        @Test
        void 스프린트_마감일을_벗어나면_오류가_발생한다() {
            // given
            LocalDateTime wrongDtAfterDueDt = LocalDateTime.of(2030, 3, 15, 17, 0);
            // then
            assertThatThrownBy(() -> createTestMeeting(1L, meetingStart, wrongDtAfterDueDt))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.MEETING_DATE_OUT_OF_SPRINT.getMessage());
        }

        @Test
        void 시작시간이_종료시간보다_느리거나_같으면_오류가_발생한다() {

            // then
            assertThatThrownBy(() -> createTestMeeting(1L, meetingEnd, meetingStart))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.INVALID_MEETING_TIME_RANGE.getMessage());
            assertThatThrownBy(() -> createTestMeeting(1L, meetingStart, meetingStart))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.INVALID_MEETING_TIME_RANGE.getMessage());
        }

        @Test
        void 시간이_8시부터_00시_범위를_벗어나면_오류가_발생한다() {
            // given
            LocalDateTime before8 = LocalDateTime.of(2026, 3, 15, 1, 0);
            LocalDateTime after0 = LocalDateTime.of(2026, 3, 16, 0, 1);
            // then
            assertThatThrownBy(() -> createTestMeeting(1L, before8, meetingEnd))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.INVALID_MEETING_TIME_RANGE.getMessage());
            assertThatThrownBy(() -> createTestMeeting(1L, meetingStart, after0))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.INVALID_MEETING_TIME_RANGE.getMessage());
        }

        @Test
        void 기존_미팅과_일시가_겹치면_오류가_발생한다() {
            // given
            createTestMeeting(1L); // 기존 meetingStart, meetingEnd로 생성
            // when
            LocalDateTime beforeStart = LocalDateTime.of(2026, 3, 15, 16, 0);
            LocalDateTime afterEnd = LocalDateTime.of(2026, 3, 15, 19, 0);
            LocalDateTime afterStart = LocalDateTime.of(2026, 3, 15, 17, 20);
            LocalDateTime beforeEnd = LocalDateTime.of(2026, 3, 15, 17, 40);
            // then
            assertThatThrownBy(() -> createTestMeeting(1L, meetingStart, meetingEnd))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.MEETING_ALREADY_EXISTS.getMessage());
            assertThatThrownBy(() -> createTestMeeting(1L, beforeStart, meetingEnd))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.MEETING_ALREADY_EXISTS.getMessage());
            assertThatThrownBy(() -> createTestMeeting(1L, meetingStart, afterEnd))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.MEETING_ALREADY_EXISTS.getMessage());
            assertThatThrownBy(() -> createTestMeeting(1L, afterStart, beforeEnd))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.MEETING_ALREADY_EXISTS.getMessage());
        }
    }

    @Nested
    class 미팅_업데이트 {
        @Test
        void 미팅_타이틀을_변경하면_정상적으로_변경된다() {
            // given
            createTestMeeting(1L);
            MeetingTitleUpdateRequest request = new MeetingTitleUpdateRequest("new title");
            // when
            meetingService.updateMeetingTitle(1L, request);
            // then
            Meeting meeting = meetingRepository.findById(1L).get();
            assertThat(meeting.getTitle()).isEqualTo("new title");
        }

        @Test
        void 미팅_일시를_변경하면_정상적으로_변경된다() {
            createTestMeeting(1L);
            LocalDateTime modifiedStart = LocalDateTime.of(2026, 3, 25, 17, 0);
            LocalDateTime modifiedEnd = LocalDateTime.of(2026, 3, 25, 18, 0);
            MeetingDtUpdateRequest request = new MeetingDtUpdateRequest(modifiedStart, modifiedEnd);
            // when
            meetingService.updateMeetingDt(1L, request);
            // then
            Meeting meeting = meetingRepository.findById(1L).get();
            assertThat(meeting.getMeetingStart()).isEqualTo(modifiedStart);
            assertThat(meeting.getMeetingEnd()).isEqualTo(modifiedEnd);
        }

        @Test
        void 미팅_일시를_하나만_변경해도_정상적으로_변경된다() {
            // given
            createTestMeeting(1L);
            LocalDateTime modifiedStart = LocalDateTime.of(2026, 3, 15, 14, 0);
            LocalDateTime modifiedEnd = LocalDateTime.of(2026, 3, 15, 20, 0);

            // when
            MeetingDtUpdateRequest request1 = new MeetingDtUpdateRequest(modifiedStart, null);
            MeetingDtUpdateRequest request2 = new MeetingDtUpdateRequest(null, modifiedEnd);

            // then
            meetingService.updateMeetingDt(1L, request1);
            assertThat(meetingRepository.findById(1L).get().getMeetingStart())
                    .isEqualTo(modifiedStart);
            assertThat(meetingRepository.findById(1L).get().getMeetingEnd()).isEqualTo(meetingEnd);
            meetingService.updateMeetingDt(1L, request2);
            assertThat(meetingRepository.findById(1L).get().getMeetingEnd()).isEqualTo(modifiedEnd);
        }

        @Test
        void 스프린트_시작일을_벗어나면_업데이트중_오류가_발생한다() {
            // given
            createTestMeeting(1L);
            LocalDateTime modifiedStart = LocalDateTime.of(2026, 3, 25, 17, 0);
            LocalDateTime modifiedEnd = LocalDateTime.of(2026, 3, 25, 18, 0);
            LocalDateTime wrongDtBeforeStartDt = LocalDateTime.of(2020, 3, 15, 17, 0);
            MeetingDtUpdateRequest request =
                    new MeetingDtUpdateRequest(wrongDtBeforeStartDt, modifiedEnd);
            // then
            assertThatThrownBy(() -> meetingService.updateMeetingDt(1L, request))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.MEETING_DATE_OUT_OF_SPRINT.getMessage());
        }

        @Test
        void 스프린트_마감일을_벗어나면_업데이트중_오류가_발생한다() {
            // given
            createTestMeeting(1L);
            LocalDateTime modifiedStart = LocalDateTime.of(2026, 3, 25, 17, 0);
            LocalDateTime wrongDtAfterDueDt = LocalDateTime.of(2030, 3, 15, 17, 0);
            MeetingDtUpdateRequest request =
                    new MeetingDtUpdateRequest(modifiedStart, wrongDtAfterDueDt);
            // then
            assertThatThrownBy(() -> meetingService.updateMeetingDt(1L, request))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.MEETING_DATE_OUT_OF_SPRINT.getMessage());
        }

        @Test
        void 시작시간이_종료시간보다_느리거나_같으면_업데이트중_오류가_발생한다() {
            createTestMeeting(1L);
            MeetingDtUpdateRequest request1 = new MeetingDtUpdateRequest(meetingEnd, meetingStart);
            MeetingDtUpdateRequest request2 =
                    new MeetingDtUpdateRequest(meetingStart, meetingStart);
            // then
            assertThatThrownBy(() -> meetingService.updateMeetingDt(1L, request1))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.INVALID_MEETING_TIME_RANGE.getMessage());
            assertThatThrownBy(() -> meetingService.updateMeetingDt(1L, request2))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.INVALID_MEETING_TIME_RANGE.getMessage());
        }

        @Test
        void 시간이_8시부터_00시_범위를_벗어나면_업데이트중_오류가_발생한다() {
            // given
            createTestMeeting(1L); // 기존 meetingStart, meetingEnd로 생성
            // when
            LocalDateTime before8 = LocalDateTime.of(2026, 3, 15, 1, 0);
            LocalDateTime after0 = LocalDateTime.of(2026, 3, 16, 0, 1);

            MeetingDtUpdateRequest request1 = new MeetingDtUpdateRequest(before8, meetingEnd);
            MeetingDtUpdateRequest request2 = new MeetingDtUpdateRequest(meetingStart, after0);

            // then
            assertThatThrownBy(() -> meetingService.updateMeetingDt(1L, request1))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.INVALID_MEETING_TIME_RANGE.getMessage());
            assertThatThrownBy(() -> meetingService.updateMeetingDt(1L, request2))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.INVALID_MEETING_TIME_RANGE.getMessage());
        }

        @Test
        void 기존_미팅과_일시가_겹치면_오류가_발생한다() {
            // given
            LocalDateTime anotherMeetingStart = LocalDateTime.of(2026, 3, 16, 17, 0);
            LocalDateTime anotherMeetingEnd = LocalDateTime.of(2026, 3, 16, 18, 0);

            createTestMeeting(1L); // 기존 meetingStart, meetingEnd로 생성
            createTestMeeting(1L, anotherMeetingStart, anotherMeetingEnd); //  새로운 미팅

            // when
            LocalDateTime beforeStart = LocalDateTime.of(2026, 3, 15, 16, 0);
            LocalDateTime afterEnd = LocalDateTime.of(2026, 3, 15, 19, 0);
            LocalDateTime afterStart = LocalDateTime.of(2026, 3, 15, 17, 20);
            LocalDateTime beforeEnd = LocalDateTime.of(2026, 3, 15, 17, 40);

            MeetingDtUpdateRequest request1 = new MeetingDtUpdateRequest(meetingStart, meetingEnd);
            MeetingDtUpdateRequest request2 = new MeetingDtUpdateRequest(beforeStart, meetingEnd);
            MeetingDtUpdateRequest request3 = new MeetingDtUpdateRequest(meetingStart, afterEnd);
            MeetingDtUpdateRequest request4 = new MeetingDtUpdateRequest(afterStart, beforeEnd);
            // then
            assertThatThrownBy(() -> meetingService.updateMeetingDt(2L, request1))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.MEETING_ALREADY_EXISTS.getMessage());
            assertThatThrownBy(() -> meetingService.updateMeetingDt(2L, request2))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.MEETING_ALREADY_EXISTS.getMessage());
            assertThatThrownBy(() -> meetingService.updateMeetingDt(2L, request3))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.MEETING_ALREADY_EXISTS.getMessage());
            assertThatThrownBy(() -> meetingService.updateMeetingDt(2L, request4))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.MEETING_ALREADY_EXISTS.getMessage());
        }
    }

    @Nested
    class 미팅_삭제 {

        @Test
        void 미팅을_삭제하면_정상적으로_삭제된다() {
            createTestMeeting(1L);
            // when
            meetingService.deleteMeeting(1L);
            // then
            assertThatThrownBy(() -> meetingService.getMeeting(1L))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.MEETING_NOT_FOUND.getMessage());
        }

        @Test
        void 미팅이_존재하지않으면_삭제중_오류가_발생한다() {

            assertThatThrownBy(() -> meetingService.deleteMeeting(1L))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.MEETING_NOT_FOUND.getMessage());
        }
    }

    @Nested
    class 미팅_조회 {
        @Test
        void 미팅아이디로_미팅을_조회하면_정상적으로_조회된다() {
            createTestMeeting(1L);
            // when
            MeetingInfoResponse response = meetingService.getMeeting(1L);
            // then
            assertThat(response)
                    .extracting("meetingTitle", "meetingStart", "meetingEnd")
                    .containsExactlyInAnyOrder(meetingTitle, meetingStart, meetingEnd);
        }

        @Test
        void 미팅ID가_유효하지_않으면_예외가_발생한다() {
            // given
            createTestMeeting(1L);
            Long invalidMeetingId = Long.MAX_VALUE;
            // then
            assertThatThrownBy(() -> meetingService.getMeeting(invalidMeetingId))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(MeetingErrorCode.MEETING_NOT_FOUND.getMessage());
        }

        @Test
        void 스프린트ID로_조회하면_전체미팅목록이_정상적으로_조회된다() {
            // given
            String title1 = "title1";
            LocalDateTime start1 = LocalDateTime.of(2026, 3, 15, 17, 0);
            LocalDateTime end1 = LocalDateTime.of(2026, 3, 15, 18, 0);
            String title2 = "title2";
            LocalDateTime start2 = LocalDateTime.of(2026, 3, 16, 17, 0);
            LocalDateTime end2 = LocalDateTime.of(2026, 3, 16, 18, 0);
            String title3 = "title3";
            LocalDateTime start3 = LocalDateTime.of(2026, 3, 17, 17, 0);
            LocalDateTime end3 = LocalDateTime.of(2026, 3, 17, 18, 0);

            createTestMeeting(1L, title1, start1, end1);
            createTestMeeting(1L, title2, start2, end2);
            createTestMeeting(1L, title3, start3, end3);
            // when, then
            Slice<MeetingInfoResponse> response = meetingService.getMeetingList(1L, null, 10);

            List<String> titles = response.stream().map(MeetingInfoResponse::meetingTitle).toList();

            assertThat(new HashSet<>(titles)).isEqualTo(Set.of(title1, title2, title3));
        }
    }
}
