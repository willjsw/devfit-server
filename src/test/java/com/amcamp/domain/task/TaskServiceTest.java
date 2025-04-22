package com.amcamp.domain.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.amcamp.IntegrationTest;
import com.amcamp.domain.contribution.dao.ContributionRepository;
import com.amcamp.domain.contribution.domain.Contribution;
import com.amcamp.domain.member.dao.MemberRepository;
import com.amcamp.domain.member.domain.Member;
import com.amcamp.domain.member.domain.OauthInfo;
import com.amcamp.domain.project.dao.ProjectParticipantRepository;
import com.amcamp.domain.project.dao.ProjectRepository;
import com.amcamp.domain.project.domain.Project;
import com.amcamp.domain.project.domain.ProjectParticipant;
import com.amcamp.domain.project.domain.ProjectParticipantRole;
import com.amcamp.domain.sprint.dao.SprintRepository;
import com.amcamp.domain.sprint.domain.Sprint;
import com.amcamp.domain.task.application.TaskService;
import com.amcamp.domain.task.dao.TaskRepository;
import com.amcamp.domain.task.domain.*;
import com.amcamp.domain.task.dto.request.TaskBasicInfoUpdateRequest;
import com.amcamp.domain.task.dto.request.TaskCreateRequest;
import com.amcamp.domain.task.dto.response.TaskBasicInfoResponse;
import com.amcamp.domain.task.dto.response.TaskInfoResponse;
import com.amcamp.domain.team.application.TeamService;
import com.amcamp.domain.team.dao.TeamParticipantRepository;
import com.amcamp.domain.team.dao.TeamRepository;
import com.amcamp.domain.team.domain.Team;
import com.amcamp.domain.team.domain.TeamParticipant;
import com.amcamp.domain.team.domain.TeamParticipantRole;
import com.amcamp.domain.team.dto.request.TeamInviteCodeRequest;
import com.amcamp.domain.team.dto.response.TeamInviteCodeResponse;
import com.amcamp.global.exception.CommonException;
import com.amcamp.global.exception.errorcode.*;
import com.amcamp.global.security.PrincipalDetails;
import com.amcamp.global.util.MemberUtil;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class TaskServiceTest extends IntegrationTest {
    @Autowired private TeamRepository teamRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberUtil memberUtil;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TeamService teamService;
    @Autowired private TaskService taskService;
    @Autowired private TaskRepository taskRepository;
    @Autowired private SprintRepository sprintRepository;
    @Autowired private ContributionRepository contributionRepository;
    @Autowired private ProjectParticipantRepository projectParticipantRepository;
    @Autowired private TeamParticipantRepository teamParticipantRepository;

    private ProjectParticipant participant;
    private ProjectParticipant newParticipant;
    private Sprint sprint;
    private Sprint anotherSprint;
    private Project project;
    private Project anotherProject;
    private Member newMember;

    private void loginAs(Member member) {
        UserDetails userDetails = new PrincipalDetails(member.getId(), member.getRole());
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @BeforeEach
    void setUp() {
        Member member =
                memberRepository.save(
                        Member.createMember(
                                "testNickname",
                                "testProfileImageUrl",
                                OauthInfo.createOauthInfo("testOauthId", "testOauthProvider")));

        newMember =
                memberRepository.save(
                        Member.createMember("newNickname", "newProfileImageUrl", null));

        UserDetails userDetails = new PrincipalDetails(member.getId(), member.getRole());
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);

        Team team = teamRepository.save(Team.createTeam("testName", "testDescription"));
        TeamParticipant teamParticipantAdmin =
                teamParticipantRepository.save(
                        TeamParticipant.createParticipant(member, team, TeamParticipantRole.ADMIN));
        TeamParticipant teamParticipantUser =
                teamParticipantRepository.save(
                        TeamParticipant.createParticipant(
                                newMember, team, TeamParticipantRole.USER));

        project =
                projectRepository.save(
                        Project.createProject(
                                team, "testTitle", "testDescription", LocalDate.of(2026, 12, 1)));
        anotherProject =
                projectRepository.save(
                        Project.createProject(
                                team, "testTitle", "testDescription", LocalDate.of(2026, 12, 1)));

        participant =
                projectParticipantRepository.save(
                        ProjectParticipant.createProjectParticipant(
                                teamParticipantAdmin, project, ProjectParticipantRole.ADMIN));
        newParticipant =
                projectParticipantRepository.save(
                        ProjectParticipant.createProjectParticipant(
                                teamParticipantUser, project, ProjectParticipantRole.MEMBER));

        sprint =
                sprintRepository.save(
                        Sprint.createSprint(
                                project, "1차 스프린트", "아이디어 기획서 제출", LocalDate.of(2026, 3, 1)));

        anotherSprint =
                sprintRepository.save(
                        Sprint.createSprint(
                                project, "2차 스프린트", "기능 개발", LocalDate.of(2030, 12, 1)));
    }

    @Test
    void 태스크를_생성한다() {
        // given
        Member member = memberUtil.getCurrentMember();
        TaskCreateRequest taskRequest =
                new TaskCreateRequest(1L, "피그마 화면 설계 수정", TaskDifficulty.MID);

        // when
        taskService.createTask(taskRequest);

        // then
        Task task =
                taskRepository
                        .findById(1L)
                        .orElseThrow(() -> new CommonException(TaskErrorCode.TASK_NOT_FOUND));

        assertThat(task.getSprint().getId()).isEqualTo(taskRequest.sprintId());
        assertThat(task.getDescription()).isEqualTo(taskRequest.description());
        assertThat(task.getTaskDifficulty()).isEqualTo(taskRequest.taskDifficulty());
    }

    @Nested
    class 태스크_수정_시 {
        @Test
        void 태스크가_존재하지않으면_예외처리() {
            assertThatThrownBy(() -> taskService.updateTaskStatus(1L))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(TaskErrorCode.TASK_NOT_FOUND.getMessage());
        }

        @Test
        @Transactional
        void 수정_권한이_없으면_예외처리() {
            TaskCreateRequest taskRequest =
                    new TaskCreateRequest(1L, "피그마 화면 설계 수정", TaskDifficulty.MID);
            taskService.createTask(taskRequest);
            taskService.assignTask(1L);

            Task task =
                    taskRepository
                            .findById(1L)
                            .orElseThrow(() -> new CommonException(TaskErrorCode.TASK_NOT_FOUND));

            loginAs(newMember);

            assertThatThrownBy(
                            () ->
                                    taskService.updateTaskBasicInfo(
                                            1L,
                                            new TaskBasicInfoUpdateRequest(
                                                    "피그마 화면 설계 재수정", TaskDifficulty.LOW)))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(TaskErrorCode.TASK_MODIFY_FORBIDDEN.getMessage());
        }

        @Test
        @Transactional
        void 태스크가_완료상태면_예외처리() {
            // given
            TaskCreateRequest taskRequest =
                    new TaskCreateRequest(1L, "피그마 화면 설계 수정", TaskDifficulty.MID);
            taskService.createTask(taskRequest);

            Task task =
                    taskRepository
                            .findById(1L)
                            .orElseThrow(() -> new CommonException(TaskErrorCode.TASK_NOT_FOUND));

            // when
            task.assignTask(participant);
            task.updateTaskStatus();

            // then
            assertThatThrownBy(
                            () ->
                                    taskService.updateTaskBasicInfo(
                                            1L,
                                            new TaskBasicInfoUpdateRequest(
                                                    "피그마 화면 설계 재수정", TaskDifficulty.HIGH)))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(TaskErrorCode.TASK_MODIFY_FORBIDDEN.getMessage());

            assertThatThrownBy(() -> taskService.deleteTask(1L))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(TaskErrorCode.TASK_MODIFY_FORBIDDEN.getMessage());
        }

        @Test
        @Transactional
        void 태스크가_SOS상태이면서_본인을_할당하려는_경우_예외처리() {
            // given
            TaskCreateRequest taskRequest =
                    new TaskCreateRequest(1L, "피그마 화면 설계 수정", TaskDifficulty.MID);
            taskService.createTask(taskRequest);

            Task task =
                    taskRepository
                            .findById(1L)
                            .orElseThrow(() -> new CommonException(TaskErrorCode.TASK_NOT_FOUND));

            task.assignTask(participant);
            task.updateTaskSOS();

            // when & then (1) 자기자신을 할당하는 경우
            assertThatThrownBy(() -> taskService.assignTask(1L))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(TaskErrorCode.TASK_ASSIGN_FORBIDDEN.getMessage());

            // when & then (2) 그렇지 않은 경우
            loginAs(newMember);
            task.assignTask(newParticipant);
            assertThat(task.getSosStatus()).isEqualTo(SOSStatus.NOT_SOS);
            assertThat(task.getAssignee()).isEqualTo(newParticipant);
        }

        @Test
        @Transactional
        void 태스크가_SOS_상태면_예외처리() {
            // given
            TaskCreateRequest taskRequest =
                    new TaskCreateRequest(1L, "피그마 화면 설계 수정", TaskDifficulty.MID);
            taskService.createTask(taskRequest);

            Task task =
                    taskRepository
                            .findById(1L)
                            .orElseThrow(() -> new CommonException(TaskErrorCode.TASK_NOT_FOUND));

            task.assignTask(participant);
            task.updateTaskSOS();

            // when & then
            assertThatThrownBy(() -> taskService.updateTaskStatus(1L))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(TaskErrorCode.TASK_COMPLETE_FORBIDDEN.getMessage());
        }

        @Test
        @Transactional
        void 정상적으로_완료된_태스크를_미완료처리한다() {
            // given
            TaskCreateRequest taskRequest =
                    new TaskCreateRequest(1L, "피그마 화면 설계 수정", TaskDifficulty.MID);
            taskService.createTask(taskRequest);

            Task task =
                    taskRepository
                            .findById(1L)
                            .orElseThrow(() -> new CommonException(TaskErrorCode.TASK_NOT_FOUND));

            task.assignTask(participant);

            // when & then
            task.updateTaskStatus();
            assertThat(task.getDueDt()).isEqualTo(LocalDate.now());

            // when & then
            task.updateTaskStatus();
            assertThat(task.getDueDt()).isNull();
            assertThat(task.getTaskStatus()).isEqualTo(TaskStatus.ON_GOING);
        }

        @Test
        void 정상적으로_기본_기한정보를_수정_한다() {
            // given
            Member member = memberUtil.getCurrentMember();
            TaskCreateRequest taskRequest =
                    new TaskCreateRequest(1L, "피그마 화면 설계 수정", TaskDifficulty.MID);
            taskService.createTask(taskRequest);

            // when - update basic info & assigned
            TaskBasicInfoUpdateRequest taskBasicInfoUpdateRequest =
                    new TaskBasicInfoUpdateRequest("피그마 화면 설계 재수정", TaskDifficulty.HIGH);

            Task task =
                    taskRepository
                            .findById(1L)
                            .orElseThrow(() -> new CommonException(TaskErrorCode.TASK_NOT_FOUND));

            TaskInfoResponse response =
                    taskService.updateTaskBasicInfo(1L, taskBasicInfoUpdateRequest);
            response = taskService.assignTask(1L);

            // then
            assertThat(task.getSprint().getId()).isEqualTo(taskRequest.sprintId());
            assertThat(response.description()).isEqualTo(taskBasicInfoUpdateRequest.description());
            assertThat(response.taskDifficulty())
                    .isEqualTo(taskBasicInfoUpdateRequest.taskDifficulty());
            assertThat(response.taskStatus()).isEqualTo(TaskStatus.ON_GOING);
            assertThat(response.assignedStatus()).isEqualTo(AssignedStatus.ASSIGNED);
        }

        @Test
        @Transactional
        void 정상적으로_진행상태를_수정한다() {
            Sprint sprint =
                    sprintRepository
                            .findById(1L)
                            .orElseThrow(
                                    () -> new CommonException(SprintErrorCode.SPRINT_NOT_FOUND));

            TeamParticipant teamParticipant =
                    teamParticipantRepository
                            .findByMemberAndTeam(
                                    memberUtil.getCurrentMember(), sprint.getProject().getTeam())
                            .orElseThrow(
                                    () ->
                                            new CommonException(
                                                    TeamErrorCode.TEAM_PARTICIPANT_REQUIRED));

            ProjectParticipant participant =
                    projectParticipantRepository
                            .findByProjectAndTeamParticipant(sprint.getProject(), teamParticipant)
                            .orElseThrow(
                                    () ->
                                            new CommonException(
                                                    ProjectErrorCode
                                                            .PROJECT_PARTICIPATION_REQUIRED));

            // when & then # of completed Task is 0
            taskService.createTask(new TaskCreateRequest(1L, "피그마 화면 설계 수정", TaskDifficulty.MID));
            taskService.createTask(new TaskCreateRequest(1L, "피그마 화면 설계 수정", TaskDifficulty.MID));
            taskService.createTask(new TaskCreateRequest(1L, "피그마 화면 설계 수정", TaskDifficulty.MID));

            assertThat(sprint.getProgress()).isEqualTo(0);

            // when & then # of completed Task is 1
            taskService.assignTask(1L);
            taskService.updateTaskStatus(1L);

            Contribution contribution =
                    contributionRepository
                            .findBySprintAndParticipant(sprint, participant)
                            .orElseThrow(
                                    () ->
                                            new CommonException(
                                                    ContributionErrorCode.CONTRIBUTION_NOT_FOUND));

            assertThat(contribution.getScore()).isEqualTo(33.333333333333336);

            Task task =
                    taskRepository
                            .findById(1L)
                            .orElseThrow(() -> new CommonException(TaskErrorCode.TASK_NOT_FOUND));

            assertThat(task.getTaskStatus()).isEqualTo(TaskStatus.COMPLETED);
            assertThat(sprint.getProgress()).isEqualTo(33.333333333333336);

            // when & then # of completed Task is 3
            taskService.assignTask(2L);
            taskService.updateTaskStatus(2L);
            taskService.assignTask(3L);
            taskService.updateTaskStatus(3L);

            assertThat(sprint.getProgress()).isEqualTo(100.0);
            assertThat(contribution.getScore()).isEqualTo(100.0);
        }
    }

    @Nested
    class sos_실행_시 {
        @Test
        void 태스크가_할당되지않았을_경우_SOS_실행하지_않는다() {
            // given
            Member member = memberUtil.getCurrentMember();
            TaskCreateRequest taskRequest =
                    new TaskCreateRequest(1L, "피그마 화면 설계 수정", TaskDifficulty.MID);
            taskService.createTask(taskRequest);

            // when
            Task task =
                    taskRepository
                            .findById(1L)
                            .orElseThrow(() -> new CommonException(TaskErrorCode.TASK_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> taskService.updateTaskSOS(task.getId()))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(TaskErrorCode.TASK_NOT_ASSIGNED.getMessage());
        }

        @Test
        void 태스크가_할당되었을_경우_SOS_실행한다() {
            // given
            Member member = memberUtil.getCurrentMember();
            TaskCreateRequest taskRequest =
                    new TaskCreateRequest(1L, "피그마 화면 설계 수정", TaskDifficulty.MID);
            taskService.createTask(taskRequest);

            // when & then
            Task task =
                    taskRepository
                            .findById(1L)
                            .orElseThrow(() -> new CommonException(TaskErrorCode.TASK_NOT_FOUND));

            TaskInfoResponse response = taskService.assignTask(task.getId());
            assertThat(response.sosStatus()).isEqualTo(SOSStatus.NOT_SOS);

            response = taskService.updateTaskSOS(task.getId());
            assertThat(response.sosStatus()).isEqualTo(SOSStatus.SOS);
        }

        @Test
        @Transactional
        void sos인_상태에서_태스크_할당상태를_수정한다() {
            // given
            TaskCreateRequest taskRequest =
                    new TaskCreateRequest(1L, "피그마 화면 설계 수정", TaskDifficulty.MID);
            taskService.createTask(taskRequest);

            Task task =
                    taskRepository
                            .findById(1L)
                            .orElseThrow(() -> new CommonException(TaskErrorCode.TASK_NOT_FOUND));

            taskService.assignTask(task.getId());
            taskService.updateTaskSOS(task.getId());

            loginAs(newMember);
            taskService.assignTask(task.getId());

            assertThat(task.getAssignedStatus()).isEqualTo(AssignedStatus.ASSIGNED);
            assertThat(task.getSosStatus()).isEqualTo(SOSStatus.NOT_SOS);
            assertThat(task.getAssignee()).isEqualTo(newParticipant);
        }
    }

    @Nested
    class 태스크_목록_조회_시 {
        @Test
        void 스프린트가_유효하지않으면_에러를_반환한다() {
            // given
            TaskCreateRequest taskRequest1 =
                    new TaskCreateRequest(1L, "피그마 화면 설계 수정", TaskDifficulty.MID);
            taskService.createTask(taskRequest1);
            TaskCreateRequest taskRequest2 =
                    new TaskCreateRequest(1L, "mvp 완성", TaskDifficulty.HIGH);
            taskService.createTask(taskRequest2);

            Task task = taskRepository.findById(1L).get();

            taskService.assignTask(task.getId()); // 첫번쨰 태스크에만 담당자 배정

            assertThatThrownBy(() -> taskService.getTasksBySprint(999L, 0L, 3))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(SprintErrorCode.SPRINT_NOT_FOUND.getMessage());
        }

        @Test
        void 내가_담당하는_태스크_조회_시_프로젝트_참가자가_아니면_에러를_반환한다() {
            // given
            Member member = memberUtil.getCurrentMember();
            TeamInviteCodeResponse teamInviteCodeResponse = teamService.getInviteCode(1L);
            TaskCreateRequest taskRequest1 =
                    new TaskCreateRequest(1L, "피그마 화면 설계 수정", TaskDifficulty.MID);
            taskService.createTask(taskRequest1);
            TaskCreateRequest taskRequest2 =
                    new TaskCreateRequest(1L, "mvp 완성", TaskDifficulty.HIGH);
            taskService.createTask(taskRequest2);

            Member nonMember =
                    memberRepository.save(
                            Member.createMember("nonMember", "testProfileImageUrl", null));
            loginAs(nonMember);

            teamService.joinTeam(new TeamInviteCodeRequest(teamInviteCodeResponse.inviteCode()));

            // when & then
            assertThatThrownBy(() -> taskService.getTasksByMember(1l, 0L, 3))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(ProjectErrorCode.PROJECT_PARTICIPATION_REQUIRED.getMessage());
        }

        @Test
        void 프로젝트_태스크_조회_시_팀_참가자가_아니면_에러를_반환한다() {
            // given
            Member member = memberUtil.getCurrentMember();
            TeamInviteCodeResponse teamInviteCodeResponse = teamService.getInviteCode(1L);
            TaskCreateRequest taskRequest1 =
                    new TaskCreateRequest(1L, "피그마 화면 설계 수정", TaskDifficulty.MID);
            taskService.createTask(taskRequest1);
            TaskCreateRequest taskRequest2 =
                    new TaskCreateRequest(1L, "mvp 완성", TaskDifficulty.HIGH);
            taskService.createTask(taskRequest2);

            Member nonMember =
                    memberRepository.save(
                            Member.createMember("nonMember", "testProfileImageUrl", null));
            loginAs(nonMember);

            // when & then
            assertThatThrownBy(() -> taskService.getTasksBySprint(1l, 0L, 3))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(TeamErrorCode.TEAM_PARTICIPANT_REQUIRED.getMessage());
        }

        @Test
        @Transactional
        void 프로젝트별로_조회한다() {
            // given
            List<Task> taskList =
                    List.of(
                            Task.createTask(sprint, "피그마 화면 설계 수정", TaskDifficulty.MID),
                            Task.createTask(sprint, "mvp 완성", TaskDifficulty.HIGH));
            taskRepository.saveAll(taskList);

            for (Task task : taskList) {
                task.assignTask(participant);
            }

            for (Task task : taskList) {
                System.out.println(
                        task.getAssignee().getTeamParticipant().getMember().getNickname());
            }

            // when
            Slice<TaskInfoResponse> result = taskService.getTasksBySprint(1L, null, 3);

            // then
            assertThat(result.getContent()).hasSize(2);
            System.out.println(result.getContent());
        }

        @Test
        void 멤버별로_조회한다() {
            // given
            Member member = memberUtil.getCurrentMember();
            TaskCreateRequest taskRequest1 =
                    new TaskCreateRequest(1L, "피그마 화면 설계 수정", TaskDifficulty.MID);
            taskService.createTask(taskRequest1);
            TaskCreateRequest taskRequest2 =
                    new TaskCreateRequest(1L, "mvp 완성", TaskDifficulty.HIGH);
            taskService.createTask(taskRequest2);

            Task task =
                    taskRepository
                            .findById(1L)
                            .orElseThrow(() -> new CommonException(TaskErrorCode.TASK_NOT_FOUND));

            taskService.assignTask(task.getId()); // 첫번쨰 태스크에만 담당자 배정

            Task task1 =
                    taskRepository
                            .findById(2L)
                            .orElseThrow(() -> new CommonException(TaskErrorCode.TASK_NOT_FOUND));

            taskService.assignTask(task1.getId()); // 첫번쨰 태스크에만 담당자 배정

            // when
            Slice<TaskBasicInfoResponse> result = taskService.getTasksByMember(1L, 0L, 3);

            // then
            assertThat(result.getContent()).hasSize(2);
        }
    }
}
