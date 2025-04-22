package com.amcamp.domain.feedback.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.amcamp.IntegrationTest;
import com.amcamp.domain.feedback.dao.FeedbackRepository;
import com.amcamp.domain.feedback.domain.Feedback;
import com.amcamp.domain.feedback.dto.request.FeedbackSendRequest;
import com.amcamp.domain.feedback.dto.response.FeedbackInfoResponse;
import com.amcamp.domain.member.dao.MemberRepository;
import com.amcamp.domain.member.domain.Member;
import com.amcamp.domain.member.domain.OauthInfo;
import com.amcamp.domain.project.dao.ProjectParticipantRepository;
import com.amcamp.domain.project.dao.ProjectRepository;
import com.amcamp.domain.project.domain.Project;
import com.amcamp.domain.project.domain.ProjectParticipant;
import com.amcamp.domain.project.domain.ProjectParticipantRole;
import com.amcamp.domain.project.domain.ProjectParticipantStatus;
import com.amcamp.domain.project.dto.response.ProjectParticipantFeedbackInfoResponse;
import com.amcamp.domain.sprint.dao.SprintRepository;
import com.amcamp.domain.sprint.domain.Sprint;
import com.amcamp.domain.team.dao.TeamParticipantRepository;
import com.amcamp.domain.team.dao.TeamRepository;
import com.amcamp.domain.team.domain.Team;
import com.amcamp.domain.team.domain.TeamParticipant;
import com.amcamp.domain.team.domain.TeamParticipantRole;
import com.amcamp.global.exception.CommonException;
import com.amcamp.global.exception.errorcode.FeedbackErrorCode;
import com.amcamp.global.exception.errorcode.ProjectErrorCode;
import com.amcamp.global.exception.errorcode.SprintErrorCode;
import com.amcamp.global.security.PrincipalDetails;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

public class FeedbackServiceTest extends IntegrationTest {

    private final String feedbackMessage = "이번 스프린트에서 아주 잘해주셨습니다.";

    @Autowired private FeedbackService feedbackService;
    @Autowired private FeedbackRepository feedbackRepository;
    @Autowired private SprintRepository sprintRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private TeamParticipantRepository teamParticipantRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectParticipantRepository projectParticipantRepository;

    private ProjectParticipant sender;
    private ProjectParticipant anotherSender;
    private ProjectParticipant receiver;
    private ProjectParticipant anotherReceiver;
    private ProjectParticipant notReceiver;
    private ProjectParticipant unknownReceiver;
    private Sprint sprint;
    private Sprint anotherSprint;
    private Project project;
    private Project anotherProject;

    @BeforeEach
    void setUp() {
        Member senderMember =
                memberRepository.save(
                        Member.createMember(
                                "testSenderNickname",
                                "testSenderProfileImageUrl",
                                OauthInfo.createOauthInfo("testOauthId", "testOauthProvider")));
        Member receiverMember =
                memberRepository.save(
                        Member.createMember(
                                "testReceiverNickname",
                                "testReceiverProfileImageUrl",
                                OauthInfo.createOauthInfo("testOauthId", "testOauthProvider")));

        Member notReceiverMember =
                memberRepository.save(
                        Member.createMember(
                                "testNotReceiverNickname",
                                "testNotReceiverProfileImageUrl",
                                OauthInfo.createOauthInfo("testOauthId", "testOauthProvider")));

        // 초기에 로그인한 사용자를 sender로 설정
        setAuthenticatedUser(senderMember);

        Team team = teamRepository.save(Team.createTeam("testName", "testDescription"));

        TeamParticipant teamParticipantAdmin =
                teamParticipantRepository.save(
                        TeamParticipant.createParticipant(
                                senderMember, team, TeamParticipantRole.ADMIN));
        TeamParticipant teamParticipantUser =
                teamParticipantRepository.save(
                        TeamParticipant.createParticipant(
                                receiverMember, team, TeamParticipantRole.USER));

        TeamParticipant teamParticipantUserNotReceiver =
                teamParticipantRepository.save(
                        TeamParticipant.createParticipant(
                                notReceiverMember, team, TeamParticipantRole.USER));

        project =
                projectRepository.save(
                        Project.createProject(
                                team, "testTitle", "testDescription", LocalDate.of(2030, 1, 1)));
        anotherProject =
                projectRepository.save(
                        Project.createProject(
                                team, "testTitle", "testDescription", LocalDate.of(2030, 1, 1)));

        sender =
                projectParticipantRepository.save(
                        ProjectParticipant.createProjectParticipant(
                                teamParticipantAdmin, project, ProjectParticipantRole.ADMIN));

        receiver =
                projectParticipantRepository.save(
                        ProjectParticipant.createProjectParticipant(
                                teamParticipantUser, project, ProjectParticipantRole.MEMBER));
        notReceiver =
                projectParticipantRepository.save(
                        ProjectParticipant.createProjectParticipant(
                                teamParticipantUserNotReceiver,
                                project,
                                ProjectParticipantRole.MEMBER));

        anotherReceiver =
                projectParticipantRepository.save(
                        ProjectParticipant.createProjectParticipant(
                                teamParticipantUser, anotherProject, ProjectParticipantRole.ADMIN));

        anotherSender =
                projectParticipantRepository.save(
                        ProjectParticipant.createProjectParticipant(
                                teamParticipantUser, anotherProject, ProjectParticipantRole.ADMIN));

        unknownReceiver =
                projectParticipantRepository.save(
                        ProjectParticipant.createProjectParticipant(
                                teamParticipantUser,
                                anotherProject,
                                ProjectParticipantRole.MEMBER));

        unknownReceiver.changeStatus(ProjectParticipantStatus.INACTIVE);

        sprint =
                sprintRepository.save(
                        Sprint.createSprint(project, "testSprint", "testGoal", LocalDate.now()));

        anotherSprint =
                sprintRepository.save(
                        Sprint.createSprint(
                                anotherProject,
                                "testAnotherSprint",
                                "testAnotherGoal",
                                LocalDate.now()));
    }

    @Nested
    class 피드백_메시지를_전송할_때 {

        @Test
        @Transactional
        void 입력_값이_정상이라면_피드백_메시지_전송에_성공한다() {
            // given
            FeedbackSendRequest request =
                    new FeedbackSendRequest(sprint.getId(), receiver.getId(), feedbackMessage);

            // when
            feedbackService.sendFeedback(request);

            // then
            Feedback feedback = feedbackRepository.findById(1L).get();
            assertThat(feedback.getSender()).isEqualTo(sender);
            assertThat(feedback.getReceiver()).isEqualTo(receiver);
            assertThat(feedback.getSprint()).isEqualTo(sprint);
            assertThat(feedback.getMessage()).isEqualTo(feedbackMessage);
        }

        @Test
        void 본인에게_피드백_메시지를_전송하면_예외가_발생한다() {
            // given
            FeedbackSendRequest request =
                    new FeedbackSendRequest(sprint.getId(), sender.getId(), feedbackMessage);

            // when & then
            assertThatThrownBy(() -> feedbackService.sendFeedback(request))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(FeedbackErrorCode.CANNOT_SEND_FEEDBACK_TO_SELF.getMessage());
        }

        @Test
        void 같은_스프린트에서_특정_대상에게_피드백_메시지를_두_번_전송하면_예외가_발생한다() {
            // given
            FeedbackSendRequest request =
                    new FeedbackSendRequest(sprint.getId(), receiver.getId(), feedbackMessage);
            feedbackService.sendFeedback(request);

            // when & then
            FeedbackSendRequest duplicateRequest =
                    new FeedbackSendRequest(sprint.getId(), receiver.getId(), feedbackMessage);
            assertThatThrownBy(() -> feedbackService.sendFeedback(duplicateRequest))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(FeedbackErrorCode.FEEDBACK_ALREADY_SENT.getMessage());
        }

        @Test
        void 스프린트가_존재하지_않는다면_예외가_발생한다() {
            // given
            FeedbackSendRequest request =
                    new FeedbackSendRequest(999L, receiver.getId(), feedbackMessage);

            // when & then
            assertThatThrownBy(() -> feedbackService.sendFeedback(request))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(SprintErrorCode.SPRINT_NOT_FOUND.getMessage());
        }

        @Test
        void 피드백을_받을_대상이_존재하지_않는다면_예외가_발생한다() {
            // given
            FeedbackSendRequest request =
                    new FeedbackSendRequest(sprint.getId(), 999L, feedbackMessage);

            // when & then
            assertThatThrownBy(() -> feedbackService.sendFeedback(request))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(FeedbackErrorCode.RECEIVER_NOT_FOUND.getMessage());
        }

        @Test
        void 같은_프로젝트에_속하지_않은_대상에게_피드백_메시지를_전송하면_예외가_발생한다() {
            // given
            FeedbackSendRequest request =
                    new FeedbackSendRequest(
                            sprint.getId(), anotherReceiver.getId(), feedbackMessage);

            // when & then
            assertThatThrownBy(() -> feedbackService.sendFeedback(request))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(FeedbackErrorCode.INVALID_PROJECT_PARTICIPANT.getMessage());
        }

        @Test
        @Transactional
        void 스프린트_마감_당일_날이_아니라면_예외가_발생한다() {
            // given
            sprint =
                    sprintRepository.save(
                            Sprint.createSprint(
                                    project, "testSprint", "testGoal", LocalDate.of(2030, 1, 1)));

            FeedbackSendRequest request =
                    new FeedbackSendRequest(sprint.getId(), receiver.getId(), feedbackMessage);

            // when & then
            assertThatThrownBy(() -> feedbackService.sendFeedback(request))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(FeedbackErrorCode.FEEDBACK_DUE_DATE_ONLY.getMessage());
        }

        @Test
        @Transactional
        void 프로젝트에서_나간_사용자에게_피드백_메시지를_전송하면_예외가_발생한다() {
            // given
            FeedbackSendRequest request =
                    new FeedbackSendRequest(
                            sprint.getId(), unknownReceiver.getId(), feedbackMessage);

            // when & then
            assertThatThrownBy(() -> feedbackService.sendFeedback(request))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(FeedbackErrorCode.PARTICIPANT_IS_UNKNOWN.getMessage());
        }
    }

    @Nested
    class 피드백_메시지를_조회할_때 {

        @BeforeEach
        void 피드백_메시지_조회를_위해_로그인한_사용자를_receiver로_변경한다() {
            setAuthenticatedUser(receiver.getTeamParticipant().getMember());
        }

        @Test
        void 받은_피드백_메시지가_있다면_성공한다() {
            // given
            feedbackRepository.save(
                    Feedback.createFeedback(sender, receiver, sprint, feedbackMessage));

            // when
            Slice<FeedbackInfoResponse> result =
                    feedbackService.findSprintFeedbacksByParticipant(
                            project.getId(), sprint.getId(), null, 1);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent().size()).isEqualTo(1);
            assertThat(result.getContent().get(0).message()).isEqualTo("이번 스프린트에서 아주 잘해주셨습니다.");
        }

        @Test
        void 요청한_프로젝트가_로그인된_사용자가_참여한_프로젝트가_아니라면_예외가_발생한다() {
            // given

            // sender는 project(1)에만 참여 중이고,
            // receiver는 project(1)와 anotherProject(2) 둘 다 참여 중이므로
            // 검증을 명확히 하기 위해 로그인한 사용자를 sender로 변경
            setAuthenticatedUser(sender.getTeamParticipant().getMember());

            // when & then
            assertThatThrownBy(
                            () ->
                                    feedbackService.findSprintFeedbacksByParticipant(
                                            anotherProject.getId(), sprint.getId(), null, 1))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(ProjectErrorCode.PROJECT_PARTICIPATION_REQUIRED.getMessage());
        }

        @Test
        void 참여_중인_프로젝트_스프린트가_아닌_스프린트_ID로_요청하는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(
                            () ->
                                    feedbackService.findSprintFeedbacksByParticipant(
                                            project.getId(), anotherSprint.getId(), null, 1))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(ProjectErrorCode.PROJECT_SPRINT_MISMATCH.getMessage());
        }

        @Test
        void 해당_스프린트에서_받은_피드백_메시지가_없는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(
                            () ->
                                    feedbackService.findSprintFeedbacksByParticipant(
                                            project.getId(), sprint.getId(), null, 1))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(FeedbackErrorCode.FEEDBACK_NOT_EXISTS.getMessage());
        }

        @Test
        void 프로젝트가_존재하지_않는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(
                            () ->
                                    feedbackService.findSprintFeedbacksByParticipant(
                                            999L, sprint.getId(), null, 1))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(ProjectErrorCode.PROJECT_NOT_FOUND.getMessage());
        }

        @Test
        void 스프린트가_존재하지_않는_경우_예외가_발생한다() {
            assertThatThrownBy(
                            () ->
                                    feedbackService.findSprintFeedbacksByParticipant(
                                            project.getId(), 999L, null, 1))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(SprintErrorCode.SPRINT_NOT_FOUND.getMessage());
        }
    }

    @Nested
    class 스프린트별_동료평가_여부를_확인할_때 {
        @Test
        void 스프린트가_존재하지않으면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(
                            () ->
                                    feedbackService.findFeedbackStatusBySprint(
                                            project.getId(), 999L, null, 1))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(SprintErrorCode.SPRINT_NOT_FOUND.getMessage());
        }

        @Test
        void 요청한_프로젝트가_로그인된_사용자가_참여한_프로젝트가_아니라면_예외가_발생한다() {
            // given
            // sender는 project(1)에만 참여 중이고,
            // receiver는 project(1)와 anotherProject(2) 둘 다 참여 중이므로
            // 검증을 명확히 하기 위해 로그인한 사용자를 sender로 변경
            setAuthenticatedUser(sender.getTeamParticipant().getMember());

            // when & then
            assertThatThrownBy(
                            () ->
                                    feedbackService.findFeedbackStatusBySprint(
                                            anotherProject.getId(), sprint.getId(), null, 1))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(ProjectErrorCode.PROJECT_PARTICIPATION_REQUIRED.getMessage());
        }

        @Test
        void 팀원정보와_동료평가여부를_반환한다() {
            feedbackRepository.save(Feedback.createFeedback(sender, receiver, sprint, "수고하셨습니다!"));
            // when: sender 기준, 해당 스프린트에서의 팀원 평가 상태 조회
            Slice<ProjectParticipantFeedbackInfoResponse> result =
                    feedbackService.findFeedbackStatusBySprint(
                            project.getId(), sprint.getId(), 0L, 10);

            // then
            assertThat(result.getContent().size())
                    .isEqualTo(
                            3); // receiver 1명만 ACTIVE 상태로 있는 프로젝트 참가자, 본인과 피드백을 받지 않은 사람은 PENDING

            // (1) 동료평가를 받은 사람은 COMPLETED
            ProjectParticipantFeedbackInfoResponse received_response =
                    result.getContent().stream()
                            .filter(r -> r.projectParticipantId().equals(receiver.getId()))
                            .findFirst()
                            .orElseThrow(() -> new AssertionError("receiver에 대한 응답이 존재하지 않음"));

            assertThat(received_response.nickname()).isEqualTo("testReceiverNickname");
            assertThat(received_response.feedbackStatus()).isEqualTo("COMPLETED");

            // (1) 동료평가를 받지 사람 & sender는 PENDING
            ProjectParticipantFeedbackInfoResponse sender_response =
                    result.getContent().stream()
                            .filter(r -> r.projectParticipantId().equals(sender.getId()))
                            .findFirst()
                            .orElseThrow(() -> new AssertionError("sender에 대한 응답이 존재하지 않음"));

            assertThat(sender_response.nickname()).isEqualTo("testSenderNickname");
            assertThat(sender_response.feedbackStatus()).isEqualTo("PENDING");
        }
    }

    private void setAuthenticatedUser(Member member) {
        UserDetails userDetails = new PrincipalDetails(member.getId(), member.getRole());
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);
    }
}
