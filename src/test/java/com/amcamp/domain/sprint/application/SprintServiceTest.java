package com.amcamp.domain.sprint.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

import com.amcamp.IntegrationTest;
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
import com.amcamp.domain.sprint.dto.request.SprintCreateRequest;
import com.amcamp.domain.sprint.dto.request.SprintUpdateRequest;
import com.amcamp.domain.sprint.dto.response.SprintDetailResponse;
import com.amcamp.domain.sprint.dto.response.SprintIdResponse;
import com.amcamp.domain.sprint.dto.response.SprintInfoResponse;
import com.amcamp.domain.task.dao.TaskRepository;
import com.amcamp.domain.task.domain.Task;
import com.amcamp.domain.task.domain.TaskDifficulty;
import com.amcamp.domain.task.domain.TaskStatus;
import com.amcamp.domain.team.dao.TeamParticipantRepository;
import com.amcamp.domain.team.dao.TeamRepository;
import com.amcamp.domain.team.domain.Team;
import com.amcamp.domain.team.domain.TeamParticipant;
import com.amcamp.domain.team.domain.TeamParticipantRole;
import com.amcamp.global.exception.CommonException;
import com.amcamp.global.exception.errorcode.ProjectErrorCode;
import com.amcamp.global.exception.errorcode.SprintErrorCode;
import com.amcamp.global.security.PrincipalDetails;
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
import org.springframework.transaction.annotation.Transactional;

public class SprintServiceTest extends IntegrationTest {

    private final LocalDate dueDt = LocalDate.of(2026, 3, 1);

    @Autowired private SprintService sprintService;
    @Autowired private SprintRepository sprintRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private TeamParticipantRepository teamParticipantRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectParticipantRepository projectParticipantRepository;
    @Autowired private TaskRepository taskRepository;

    private Project project;
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

        newMember = memberRepository.save(Member.createMember("member", null, null));

        UserDetails userDetails = new PrincipalDetails(member.getId(), member.getRole());
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);

        Team team = Team.createTeam("testName", "testDescription");
        teamRepository.save(team);
        TeamParticipant teamParticipant =
                TeamParticipant.createParticipant(member, team, TeamParticipantRole.ADMIN);
        teamParticipantRepository.save(teamParticipant);
        TeamParticipant teamParticipantUser =
                teamParticipantRepository.save(
                        TeamParticipant.createParticipant(
                                newMember, team, TeamParticipantRole.USER));

        project = Project.createProject(team, "testTitle", "testDescription", dueDt);
        projectRepository.save(project);
        ProjectParticipant projectParticipant =
                ProjectParticipant.createProjectParticipant(
                        teamParticipant, project, ProjectParticipantRole.ADMIN);
        projectParticipantRepository.save(projectParticipant);
    }

    @Nested
    class 스프린트_생성할_때 {
        @Test
        void 프로젝트_기간_내라면_스프린트를_생성한다() {
            // given
            SprintCreateRequest request =
                    new SprintCreateRequest(project.getId(), "testGoal", dueDt);

            // when
            SprintInfoResponse response = sprintService.createSprint(request);

            // then
            assertThat(response.goal()).isEqualTo("testGoal");
            assertThat(response.dueDt()).isEqualTo(LocalDate.of(2026, 3, 1));
        }

        @Test
        void 스프린트_마감_날짜가_프로젝트_마감_날짜_이후라면_예외가_발생한다() {
            // given
            SprintCreateRequest request =
                    new SprintCreateRequest(project.getId(), "testGoal", LocalDate.of(2026, 3, 2));

            // when & then
            assertThatThrownBy(() -> sprintService.createSprint(request))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(SprintErrorCode.SPRINT_DUE_DATE_EXCEEDS_PROJECT_END.getMessage());
        }

        @Test
        void 스프린트_마감_날짜가_현재_날짜_이전이라면_예외가_발생한다() {
            // given
            SprintCreateRequest request =
                    new SprintCreateRequest(project.getId(), "testGoal", LocalDate.of(2024, 1, 1));

            // when
            assertThatThrownBy(() -> sprintService.createSprint(request))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(SprintErrorCode.SPRINT_DUE_DATE_BEFORE_START.getMessage());
        }

        @Test
        void 기존_스프린트가_종료되지_않은_상태에서_새로운_스프린트를_생성하면_예외가_발생한다() {
            // given
            sprintRepository.save(
                    Sprint.createSprint(
                            project, "testTitle", "testGoal", LocalDate.of(2030, 1, 1)));

            SprintCreateRequest request =
                    new SprintCreateRequest(project.getId(), "testGoal", LocalDate.of(2031, 1, 1));

            // when & then
            assertThatThrownBy(() -> sprintService.createSprint(request))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(SprintErrorCode.PREVIOUS_SPRINT_NOT_ENDED.getMessage());
        }
    }

    @Nested
    class 스프린트_수정할_때 {
        @Test
        void 스프린트가_존재하지_않으면_예외가_발생한다() {
            // given
            SprintUpdateRequest request = new SprintUpdateRequest("testGoal", null);

            // when & then
            assertThatThrownBy(() -> sprintService.updateSprint(2L, request))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(SprintErrorCode.SPRINT_NOT_FOUND.getMessage());
        }

        @Test
        void 스프린트_마감_날짜가_프로젝트_마감_날짜_이내라면_성공한다() {
            // given
            sprintRepository.save(
                    Sprint.createSprint(project, "testTitle", "testDescription", dueDt));
            SprintUpdateRequest request = new SprintUpdateRequest(null, LocalDate.of(2026, 2, 28));

            // when
            SprintInfoResponse response = sprintService.updateSprint(1L, request);

            // then
            assertThat(response.dueDt()).isEqualTo(LocalDate.of(2026, 2, 28));
            assertThat(response.title()).isEqualTo("testTitle");
        }

        @Test
        void 스프린트_마감_날짜가_프로젝트_마감_날짜_이후라면_예외가_발생한다() {
            // given
            sprintRepository.save(
                    Sprint.createSprint(project, "testTitle", "testDescription", dueDt));
            SprintUpdateRequest request = new SprintUpdateRequest(null, LocalDate.of(2030, 1, 1));

            // when & then
            assertThatThrownBy(() -> sprintService.updateSprint(1L, request))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(SprintErrorCode.SPRINT_DUE_DATE_EXCEEDS_PROJECT_END.getMessage());
        }

        @Test
        void 스프린트_마감_날짜가_스프린트_시작_날짜_이전이라면_예외가_발생한다() {
            // given
            sprintRepository.save(
                    Sprint.createSprint(project, "testTitle", "testDescription", dueDt));
            SprintUpdateRequest request = new SprintUpdateRequest(null, LocalDate.of(2024, 1, 1));

            // when & then
            assertThatThrownBy(() -> sprintService.updateSprint(1L, request))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(SprintErrorCode.SPRINT_DUE_DATE_BEFORE_START.getMessage());
        }

        @Test
        void 다음_스프린트의_시작일_이전에_마감일을_설정하면_예외가_발생한다() {
            // given
            sprintRepository.save(
                    Sprint.createSprint(project, "sprint 1", "sprint 1", LocalDate.now()));
            sprintRepository.save(Sprint.createSprint(project, "sprint 2", "sprint 2", dueDt));

            SprintUpdateRequest request = new SprintUpdateRequest(null, LocalDate.of(2026, 1, 1));

            // when & then
            assertThatThrownBy(() -> sprintService.updateSprint(1L, request))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(SprintErrorCode.SPRINT_DUE_DATE_CONFLICT_WITH_NEXT.getMessage());
        }
    }

    @Nested
    class 스프린트_삭제할_때 {
        @Test
        void 스프린트가_존재하지_않으면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> sprintService.deleteSprint(2L))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(SprintErrorCode.SPRINT_NOT_FOUND.getMessage());
        }

        @Test
        void 프로젝트_리더가_삭제할_경우_성공한다() {
            // given
            sprintRepository.save(
                    Sprint.createSprint(project, "testTitle", "testDescription", dueDt));

            // when
            sprintService.deleteSprint(1L);

            // then
            assertThat(sprintRepository.findAll()).isEmpty();
            assertThat(sprintRepository.count()).isEqualTo(0);
        }
    }

    @Nested
    class 스프린트_기본정보를_조회할_떄 {
        @Test
        void 스프린트가_존재하지_않으면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> sprintService.deleteSprint(2L))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(SprintErrorCode.SPRINT_NOT_FOUND.getMessage());
        }

        @Test
        void 스프린트_기본_정보를_조회한다() {
            // given
            List<Sprint> sprintList =
                    List.of(
                            Sprint.createSprint(project, "1", "testDescription1", dueDt),
                            Sprint.createSprint(project, "2", "testDescription2", dueDt),
                            Sprint.createSprint(project, "3", "testDescription3", dueDt));
            sprintRepository.saveAll(sprintList);

            SprintInfoResponse response = sprintService.findSprint(1L);

            assertThat(response.goal()).isEqualTo("testDescription1");
        }
    }

    @Nested
    class 프로젝트별_스프린트_목록을_조회할_때 {
        @Test
        void 프로젝트가_존재하지_않으면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> sprintService.findAllSprint(999L, null, null))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(ProjectErrorCode.PROJECT_NOT_FOUND.getMessage());
        }

        @Test
        @Transactional
        void 프로젝트가_존재한다면_첫_번째_스프린트를_반환한다() {
            // given
            List<Sprint> sprintList =
                    List.of(
                            Sprint.createSprint(project, "1", "testDescription1", dueDt),
                            Sprint.createSprint(project, "2", "testDescription2", dueDt),
                            Sprint.createSprint(project, "3", "testDescription3", dueDt));
            sprintRepository.saveAll(sprintList);

            Sprint sprint = sprintRepository.findById(3L).get();

            taskRepository.save(Task.createTask(sprint, "태스크 조회 기능 구현1", TaskDifficulty.MID));
            taskRepository.save(Task.createTask(sprint, "태스크 조회 기능 구현2", TaskDifficulty.MID));

            Task task = taskRepository.findById(1L).get();
            ProjectParticipant participant = projectParticipantRepository.findById(1L).get();

            task.assignTask(participant);
            task.updateTaskStatus();

            // when
            Slice<SprintDetailResponse> results =
                    sprintService.findAllSprint(project.getId(), null, null);

            // then
            assertThat(results.getSize()).isEqualTo(1);
            assertThat(results)
                    .extracting("id", "title", "goal")
                    .containsExactlyInAnyOrder(tuple(3L, "3", "testDescription3"));

            assertThat(results.getContent().get(0).taskList().get(0).description())
                    .isEqualTo(task.getDescription());
            assertThat(results.getContent().get(0).taskList().size()).isEqualTo(2);
            assertThat(results.getContent().get(0).taskList().get(0).taskStatus())
                    .isEqualTo(TaskStatus.COMPLETED);
        }
    }

    @Nested
    class 프로젝트별_스프린트아이디_목록을_조회할_때 {
        @Test
        void 프로젝트가_존재하지_않으면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> sprintService.findAllSprint(999L, null, null))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(ProjectErrorCode.PROJECT_NOT_FOUND.getMessage());
        }

        @Test
        void 프로젝트가_존재한다면_모든_스프린트아이디를_반환한다() {
            // given
            List<Sprint> sprintList =
                    List.of(
                            Sprint.createSprint(project, "1", "testDescription1", dueDt),
                            Sprint.createSprint(project, "2", "testDescription2", dueDt),
                            Sprint.createSprint(project, "3", "testDescription3", dueDt));
            sprintRepository.saveAll(sprintList);

            // when
            List<SprintIdResponse> results = sprintService.findAllSprintId(1L);

            // given
            assertThat(results.size()).isEqualTo(3);
            assertThat(results).extracting("title").containsExactly("1", "2", "3"); // 순서까지 확인
        }
    }

    @Nested
    class 회원별_스프린트_목록을_조회할_떄 {
        @Test
        void 프로젝트_참가자가_아니면_예외가_발생한다() {
            // given
            loginAs(newMember);

            // when & then
            assertThatThrownBy(() -> sprintService.findAllSprintByMember(1L, null, null))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(ProjectErrorCode.PROJECT_PARTICIPATION_REQUIRED.getMessage());
        }

        @Test
        @Transactional
        void 프로젝트가_존재한다면_마지막_스프린트를_반환한다() {
            // given
            List<Sprint> sprintList =
                    List.of(
                            Sprint.createSprint(
                                    project, "1", "testDescription1", LocalDate.of(2026, 1, 1)),
                            Sprint.createSprint(
                                    project, "2", "testDescription2", LocalDate.of(2026, 2, 1)),
                            Sprint.createSprint(
                                    project, "3", "testDescription3", LocalDate.of(2026, 3, 1)));
            sprintRepository.saveAll(sprintList);

            Sprint sprint = sprintRepository.findById(3L).get();

            taskRepository.save(Task.createTask(sprint, "태스크 조회 기능 구현1", TaskDifficulty.MID));
            taskRepository.save(Task.createTask(sprint, "태스크 조회 기능 구현2", TaskDifficulty.MID));

            Task task = taskRepository.findById(1L).get();
            ProjectParticipant participant = projectParticipantRepository.findById(1L).get();

            task.assignTask(participant);
            task.updateTaskStatus();

            // when
            Slice<SprintDetailResponse> results =
                    sprintService.findAllSprintByMember(project.getId(), null, null);

            // then
            assertThat(results.getSize()).isEqualTo(1);
            assertThat(results)
                    .extracting("id", "title", "goal")
                    .containsExactlyInAnyOrder(tuple(3L, "3", "testDescription3"));

            assertThat(results.getContent().get(0).taskList().get(0).description())
                    .isEqualTo(task.getDescription());
            assertThat(results.getContent().get(0).taskList().size()).isEqualTo(1);

            assertThat(results.getContent().get(0).progress()).isInstanceOf(Integer.class);
        }
    }
}
