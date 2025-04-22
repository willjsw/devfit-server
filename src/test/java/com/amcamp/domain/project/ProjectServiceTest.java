package com.amcamp.domain.project;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import com.amcamp.IntegrationTest;
import com.amcamp.domain.member.dao.MemberRepository;
import com.amcamp.domain.member.domain.Member;
import com.amcamp.domain.member.domain.OauthInfo;
import com.amcamp.domain.project.application.ProjectService;
import com.amcamp.domain.project.dao.ProjectParticipantRepository;
import com.amcamp.domain.project.dao.ProjectRegistrationRepository;
import com.amcamp.domain.project.dao.ProjectRepository;
import com.amcamp.domain.project.domain.*;
import com.amcamp.domain.project.dto.request.ProjectCreateRequest;
import com.amcamp.domain.project.dto.request.ProjectUpdateRequest;
import com.amcamp.domain.project.dto.response.ProjectInfoResponse;
import com.amcamp.domain.project.dto.response.ProjectListInfoResponse;
import com.amcamp.domain.project.dto.response.ProjectParticipantInfoResponse;
import com.amcamp.domain.project.dto.response.ProjectRegisterDetailResponse;
import com.amcamp.domain.project.dto.response.ProjectRegistrationInfoResponse;
import com.amcamp.domain.team.application.TeamService;
import com.amcamp.domain.team.dao.TeamParticipantRepository;
import com.amcamp.domain.team.dao.TeamRepository;
import com.amcamp.domain.team.domain.Team;
import com.amcamp.domain.team.domain.TeamParticipant;
import com.amcamp.domain.team.dto.request.TeamCreateRequest;
import com.amcamp.domain.team.dto.request.TeamInviteCodeRequest;
import com.amcamp.global.exception.CommonException;
import com.amcamp.global.exception.errorcode.ProjectErrorCode;
import com.amcamp.global.exception.errorcode.TeamErrorCode;
import com.amcamp.global.security.PrincipalDetails;
import com.amcamp.global.util.MemberUtil;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class ProjectServiceTest extends IntegrationTest {
    @Autowired private MemberUtil memberUtil;
    @Autowired private ProjectService projectService;
    @Autowired private TeamService teamService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private TeamParticipantRepository teamParticipantRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private ProjectRegistrationRepository projectRegistrationRepository;
    @Autowired private ProjectParticipantRepository projectParticipantRepository;
    @Autowired private ProjectRepository projectRepository;

    private Member memberAdmin;
    private Member member1;
    private Member member2;
    private final String title = "projectTitle";
    private final String description = "projectDescription";
    private final LocalDate dueDt = LocalDate.of(2026, 12, 1);

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

    private Long getTeamId() {
        TeamCreateRequest teamCreateRequest = new TeamCreateRequest("팀 이름", "팀 설명");
        String inviteCode = teamService.createTeam(teamCreateRequest).inviteCode();
        teamInviteCodeRequest = new TeamInviteCodeRequest(inviteCode);
        return teamService.getTeamByCode(teamInviteCodeRequest).teamId();
    }

    Project createTestProject() {
        Member member = memberUtil.getCurrentMember();
        Long teamId = getTeamId();
        Team team = teamRepository.findById(teamId).orElseThrow();
        TeamParticipant participant = teamParticipantRepository.findById(1L).get();
        Project project =
                projectRepository.save(Project.createProject(team, title, description, dueDt));
        projectParticipantRepository.save(
                ProjectParticipant.createProjectParticipant(
                        participant, project, ProjectParticipantRole.ADMIN));
        return project;
    }

    Project createTestProject(Long teamId, Long teamParticipantId) {
        Member member = memberUtil.getCurrentMember();
        Team team = teamRepository.findById(teamId).orElseThrow();
        TeamParticipant participant = teamParticipantRepository.findById(teamParticipantId).get();
        Project project =
                projectRepository.save(Project.createProject(team, title, description, dueDt));
        projectParticipantRepository.save(
                ProjectParticipant.createProjectParticipant(
                        participant, project, ProjectParticipantRole.ADMIN));
        return project;
    }

    Project createTestProject(
            Long teamId,
            Long teamParticipantId,
            String title,
            LocalDate dueDt,
            String description) {
        Member member = memberUtil.getCurrentMember();
        Team team = teamRepository.findById(teamId).orElseThrow();
        TeamParticipant participant = teamParticipantRepository.findById(teamParticipantId).get();
        Project project =
                projectRepository.save(Project.createProject(team, title, description, dueDt));
        projectParticipantRepository.save(
                ProjectParticipant.createProjectParticipant(
                        participant, project, ProjectParticipantRole.ADMIN));
        return project;
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

        member1 =
                Member.createMember(
                        "member1",
                        "testProfileImageUrl",
                        OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"));
        memberRepository.save(member1);

        member2 =
                Member.createMember(
                        "member2",
                        "testProfileImageUrl",
                        OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"));
        memberRepository.save(member2);
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
    }

    @Test
    void 프로젝트를_생성하면_정상적으로_저장된다() {
        // given
        Long teamId = getTeamId();
        // when
        ProjectCreateRequest request = new ProjectCreateRequest(teamId, title, dueDt, description);

        projectService.createProject(request);

        // then
        Project project = projectRepository.findById(1L).get();
        assertThat(project.getId()).isEqualTo(1L);
        assertThat(project)
                .extracting("id", "title", "description")
                .containsExactlyInAnyOrder(1L, title, description);
    }

    @Nested
    class 프로젝트_조회 {
        @Test
        void 팀_ID로_조회하면_전체_프로젝트가_정상적으로_반환된다() {
            // given
            Long teamId = getTeamId();
            createTestProject(teamId, 1L, "project1", dueDt, description);
            Project member1JoinProject =
                    createTestProject(teamId, 1L, "project2", dueDt, description);

            // member logout 후 anotherMember 로그인
            logout();
            loginAs(member1);
            // 팀 참가
            teamService.joinTeam(teamInviteCodeRequest);
            // anotherMember 새 프로젝트 생성
            createTestProject(teamId, 2L, "project3", dueDt, description);
            // Project1에 일반 멤버로 참여
            TeamParticipant participant = teamParticipantRepository.findById(2L).get();
            projectParticipantRepository.save(
                    ProjectParticipant.createProjectParticipant(
                            participant, member1JoinProject, ProjectParticipantRole.MEMBER));
            // when
            Slice<ProjectListInfoResponse> response =
                    projectService.getProjectListInfo(teamId, null, 10);
            response.getContent().forEach(System.out::println);
            assertThat(
                            response.getContent().stream()
                                    .filter(r -> !r.isParticipant())
                                    //				.filter(ProjectListInfoResponse::isAdmin)
                                    .map(ProjectListInfoResponse::projectInfo)
                                    .findAny()
                                    .get()
                                    .projectTitle())
                    .isEqualTo("project1");
            assertThat(
                            response.getContent().stream()
                                    .filter(ProjectListInfoResponse::isParticipant)
                                    .filter(f -> !f.isAdmin())
                                    .map(ProjectListInfoResponse::projectInfo)
                                    .findAny()
                                    .get()
                                    .projectTitle())
                    .isEqualTo("project2");
            assertThat(
                            response.getContent().stream()
                                    .filter(ProjectListInfoResponse::isParticipant)
                                    .filter(ProjectListInfoResponse::isAdmin)
                                    .map(ProjectListInfoResponse::projectInfo)
                                    .findAny()
                                    .get()
                                    .projectTitle())
                    .isEqualTo("project3");
        }

        @Test
        void 프로젝트를_ID로_조회하면_정상적으로_반환된다() {
            // given
            createTestProject();
            Project project = projectRepository.findById(1L).get();
            // when
            ProjectInfoResponse foundResponse = projectService.getProjectInfo(1L);
            // then

            assertThat(foundResponse)
                    .extracting(
                            "projectId", "projectTitle", "projectDescription", "startDt", "dueDt")
                    .containsExactlyInAnyOrder(
                            project.getId(),
                            project.getTitle(),
                            project.getDescription(),
                            project.getStartDt(),
                            project.getDueDt());
        }

        @Test
        void ID가_유효하지_않으면_예외가_발생한다() {
            // given
            Long invalidProjectId = Long.MAX_VALUE;
            // when, then
            assertThatThrownBy(() -> projectService.getProjectInfo(invalidProjectId))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(ProjectErrorCode.PROJECT_NOT_FOUND.getMessage());
        }
    }

    @Nested
    class 프로젝트_업데이트 {
        String originalTitle = "originalProjectTitle";
        String originalDescription = "originalProjectGoal";
        String updatedTitle = "updatedProjectTitle";
        String updatedDescription = "updatedProjectDescription";

        void createOriginalProject() {
            Long teamId = getTeamId();
            createTestProject(teamId, 1L, originalTitle, dueDt, originalDescription);
        }

        @Test
        void 프로젝트_기본정보를_수정하면_정상적으로_수정된다() {
            // given
            createOriginalProject();
            // when
            projectService.updateProject(
                    1L, new ProjectUpdateRequest(updatedTitle, updatedDescription, null));
            Project updatedProject = projectRepository.findById(1L).get();
            // then
            assertThat(updatedProject.getTitle()).isEqualTo(updatedTitle);
            assertThat(updatedProject.getDescription()).isEqualTo(updatedDescription);
        }

        @Test
        void 팀_참여자가_아닌_사용자는_프로젝트_수정이_제한된다() {
            // given
            createOriginalProject();
            logout();
            // 팀에 속하지 않은 사용자 로그인
            loginAs(member1);

            // when, then
            assertThatThrownBy(
                            () ->
                                    projectService.updateProject(
                                            1L,
                                            new ProjectUpdateRequest(
                                                    updatedTitle, updatedDescription, null)))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(TeamErrorCode.TEAM_PARTICIPANT_REQUIRED.getMessage());
        }

        @Test
        void 프로젝트_참여자가_아닌_팀_참가자는_수정이_제한된다() {
            // given
            createOriginalProject();
            logout();
            // 다른 팀 멤버
            loginAs(member1);
            teamService.joinTeam(teamInviteCodeRequest);

            // when, then
            assertThatThrownBy(
                            () ->
                                    projectService.updateProject(
                                            1L,
                                            new ProjectUpdateRequest(
                                                    updatedTitle, updatedDescription, null)))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(
                            ProjectErrorCode.PROJECT_PARTICIPATION_REQUIRED.getMessage());
        }

        @Test
        void 프로젝트_정보를_타이틀만_수정하면_타이틀만_수정된다() {
            // given
            createOriginalProject();
            // when
            projectService.updateProject(1L, new ProjectUpdateRequest(updatedTitle, null, null));
            Project updatedProject = projectRepository.findById(1L).get();
            // then
            assertThat(updatedProject.getTitle()).isEqualTo(updatedTitle);
            assertThat(updatedProject.getDescription()).isEqualTo(originalDescription);
        }

        @Test
        void 프로젝트_정보를_상세설명만_수정하면_상세설명만_수정된다() {
            // given
            createOriginalProject();
            // when
            projectService.updateProject(
                    1L, new ProjectUpdateRequest(null, updatedDescription, null));
            Project updatedProject = projectRepository.findById(1L).get();
            // then
            assertThat(updatedProject.getTitle()).isEqualTo(originalTitle);
            assertThat(updatedProject.getDescription()).isEqualTo(updatedDescription);
        }

        @Test
        void 프로젝트_정보를_마감일자만_수정하면_마감일자만_수정된다() {
            // given
            createOriginalProject();
            // when
            LocalDate updatedDueDt = LocalDate.of(2027, 12, 1);
            projectService.updateProject(1L, new ProjectUpdateRequest(null, null, updatedDueDt));
            Project updatedProject = projectRepository.findById(1L).get();
            // then
            assertThat(updatedProject.getDueDt()).isEqualTo(updatedDueDt);
        }

        @Test
        void 잘못된_날짜를_입력하면_오류가_발생한다() {
            // given
            createOriginalProject();
            // when
            LocalDate updatedDueDt = LocalDate.of(2024, 1, 1);
            ProjectUpdateRequest request = new ProjectUpdateRequest(null, null, updatedDueDt);

            // then
            assertThatThrownBy(() -> projectService.updateProject(1L, request))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(
                            ProjectErrorCode.PROJECT_DUE_DATE_BEFORE_START.getMessage());
        }
    }

    @Nested
    class 프로젝트_가입_신청 {
        @Test
        void 프로젝트_가입신청을_하면_정상적으로_요청이_생성된다() {
            // given
            Long teamId = getTeamId();
            createTestProject(teamId, 1L);
            logout();
            loginAs(member1);
            teamService.joinTeam(teamInviteCodeRequest);

            // when
            projectService.requestToProjectRegistration(1L);
            Team team = teamRepository.findById(teamId).get();
            TeamParticipant teamParticipant =
                    teamParticipantRepository.findByMemberAndTeam(member1, team).get();

            // then
            logout();
            loginAs(memberAdmin);
            ProjectRegistrationInfoResponse registrationInfo =
                    projectService.getProjectRegistration(1L, 1L);
            assertThat(registrationInfo.requesterId()).isEqualTo(teamParticipant.getId());
            assertThat(
                    registrationInfo.projectRegistrationStatus()
                            == ProjectRegistrationStatus.PENDING);
        }

        @Test
        void 프로젝트_멤버_수가_15명을_초과하면_가입신청이_제한된다() {
            // given
            Long teamId = getTeamId();
            createTestProject(teamId, 1L);
            logout();

            for (int i = 0; i < 14; i++) {
                Member tmpMember =
                        Member.createMember(
                                "tmpMember" + i,
                                "testProfileImageUrl",
                                OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"));

                memberRepository.save(tmpMember);
                loginAs(tmpMember);
                teamService.joinTeam(teamInviteCodeRequest);
                projectService.requestToProjectRegistration(1L);
                logout();
            }

            loginAs(memberAdmin);
            for (long registrationId = 1L; registrationId < 15L; registrationId++) {
                projectService.approveProjectRegistration(1L, (registrationId));
            }
            logout();

            loginAs(member1);
            teamService.joinTeam(teamInviteCodeRequest);

            // when
            assertThatThrownBy(() -> projectService.requestToProjectRegistration(1L))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(
                            ProjectErrorCode.PROJECT_PARTICIPANT_LIMIT_EXCEED.getMessage());
        }

        @Test
        void 프로젝트_가입신청_목록을_조회하면_정상적으로_조회된다() {
            // given
            Long teamId = getTeamId();
            Team team = teamRepository.findById(teamId).get();
            // 프로젝트 생성, 프로젝트 ID 1L 할당
            createTestProject(teamId, 1L);
            logout();

            // when: member1이 가입 신청
            loginAs(member1);
            teamService.joinTeam(teamInviteCodeRequest);
            TeamParticipant teamParticipant1 =
                    teamParticipantRepository.findByMemberAndTeam(member1, team).get();
            projectService.requestToProjectRegistration(1L);

            logout();

            // when: member2도 가입 신청
            loginAs(member2);
            teamService.joinTeam(teamInviteCodeRequest);
            TeamParticipant teamParticipant2 =
                    teamParticipantRepository.findByMemberAndTeam(member2, team).get();
            projectService.requestToProjectRegistration(1L);

            // then: 팀 관리자인 memberAdmin이 가입 신청 목록을 조회
            logout();
            loginAs(memberAdmin);
            Slice<ProjectRegisterDetailResponse> response =
                    projectService.getProjectRegistrationList(1L, null, 10);
            List<Long> requesterIds =
                    response.stream().map(ProjectRegisterDetailResponse::requesterId).toList();
            assertThat(new HashSet<>(requesterIds))
                    .isEqualTo(Set.of(teamParticipant1.getId(), teamParticipant2.getId()));
        }

        @Test
        void 이미_가입신청한_팀참여자는_신청하면_예외가_발생한다() {
            // given
            Long teamId = getTeamId();
            createTestProject(teamId, 1L);
            logout();
            loginAs(member1);
            teamService.joinTeam(teamInviteCodeRequest);

            // when: 최초 가입 신청
            projectService.requestToProjectRegistration(1L);

            // then: 이미 가입 신청한 팀 참여자가 다시 신청하면 예외 발생
            assertThatThrownBy(() -> projectService.requestToProjectRegistration(1L))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(
                            ProjectErrorCode.PROJECT_REGISTRATION_ALREADY_EXISTS.getMessage());
        }

        @Test
        void 이미_가입된_프로젝트_참여자가_가입신청하면_예외가_발생한다() {
            // given
            Long teamId = getTeamId();
            createTestProject(teamId, 1L);

            // when, then: 아직 가입 신청하지 않은 상태에서(즉, ACTIVE 상태로 이미 가입되어 있는 상태에서) 가입 신청을 시도하면 예외 발생
            assertThatThrownBy(() -> projectService.requestToProjectRegistration(1L))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(
                            ProjectErrorCode.PROJECT_PARTICIPANT_ALREADY_EXISTS.getMessage());
        }

        @Test
        void 프로젝트_가입을_승인하면_정상적으로_승인된다() {
            // given
            Long teamId = getTeamId();
            createTestProject(teamId, 1L);
            logout();
            loginAs(member1);
            teamService.joinTeam(teamInviteCodeRequest);

            // when
            projectService.requestToProjectRegistration(1L);
            Team team = teamRepository.findById(teamId).get();
            TeamParticipant teamParticipant =
                    teamParticipantRepository.findByMemberAndTeam(member1, team).get();

            // then
            logout();
            loginAs(memberAdmin);
            ProjectRegistrationInfoResponse registrationInfo =
                    projectService.getProjectRegistration(1L, 1L);
            projectService.approveProjectRegistration(1L, registrationInfo.registrationId());

            // then
            ProjectRegistrationInfoResponse approvedRegistration =
                    projectService.getProjectRegistration(1L, 1L);
            assertThat(approvedRegistration.requesterId()).isEqualTo(teamParticipant.getId());
            assertThat(
                    approvedRegistration.projectRegistrationStatus()
                            == ProjectRegistrationStatus.APPROVED);
        }

        @Test
        void 프로젝트_가입을_거부하면_정상적으로_거부된다() {
            // given
            Long teamId = getTeamId();
            createTestProject(teamId, 1L);
            logout();
            loginAs(member1);
            teamService.joinTeam(teamInviteCodeRequest);

            // when
            projectService.requestToProjectRegistration(1L);
            Team team = teamRepository.findById(teamId).get();
            TeamParticipant teamParticipant =
                    teamParticipantRepository.findByMemberAndTeam(member1, team).get();

            // then
            logout();
            loginAs(memberAdmin);
            ProjectRegistrationInfoResponse registrationInfo =
                    projectService.getProjectRegistration(1L, 1L);
            projectService.rejectProjectRegistration(1L, registrationInfo.registrationId());

            // then
            ProjectRegistrationInfoResponse approvedRegistration =
                    projectService.getProjectRegistration(1L, 1L);
            assertThat(approvedRegistration.requesterId()).isEqualTo(teamParticipant.getId());
            assertThat(
                    approvedRegistration.projectRegistrationStatus()
                            == ProjectRegistrationStatus.REJECTED);
        }

        @Test
        void 프로젝트_가입을_취소하면_요청이_정상적으로_삭제된다() {
            // given
            Long teamId = getTeamId();
            createTestProject(teamId, 1L);
            logout();
            loginAs(member1);
            teamService.joinTeam(teamInviteCodeRequest);
            // when
            projectService.requestToProjectRegistration(1L);
            projectService.deleteProjectRegistration(1L);
            // then
            logout();
            loginAs(memberAdmin);
            assertThatThrownBy(() -> projectService.getProjectRegistration(1L, 1L))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(
                            ProjectErrorCode.PROJECT_REGISTRATION_NOT_FOUND.getMessage());
        }

        @Test
        void 프로젝트_참여자를_조회하면_정상적으로_조회된다() {
            // given
            Long teamId = getTeamId();
            createTestProject(teamId, 1L);
            logout(); // 어드민 로그아웃
            loginAs(member1);
            teamService.joinTeam(teamInviteCodeRequest);

            // when
            projectService.requestToProjectRegistration(1L);

            // then
            logout(); // member project 가입 신청 후 로그아웃
            loginAs(memberAdmin); // admin 다시 로그인 후 가입 승인
            ProjectRegistrationInfoResponse registrationInfo =
                    projectService.getProjectRegistration(1L, 1L);
            projectService.approveProjectRegistration(1L, registrationInfo.registrationId());

            // then
            logout();
            loginAs(member1);
            projectService.getProjectParticipantList(1L, null, 1).forEach(System.out::println);
            ProjectParticipantInfoResponse myInfo = projectService.getProjectParticipant(1L);
            assertThat(myInfo.nickname()).isEqualTo(member1.getNickname());
            assertThat(myInfo.role()).isEqualTo(ProjectParticipantRole.MEMBER);
        }

        @Test
        void 프로젝트_참여자_목록을_조회하면_정상적으로_조회된다() {
            // given
            Long teamId = getTeamId();
            createTestProject(teamId, 1L);
            logout(); // admin 로그아웃

            // when
            loginAs(member1);
            teamService.joinTeam(teamInviteCodeRequest);
            projectService.requestToProjectRegistration(1L);
            logout(); //

            loginAs(member2);
            teamService.joinTeam(teamInviteCodeRequest);
            projectService.requestToProjectRegistration(1L);
            logout();

            loginAs(memberAdmin);

            projectService.getProjectRegistrationList(1L, null, 10).stream()
                    .map(ProjectRegisterDetailResponse::registrationId)
                    .forEach(i -> projectService.approveProjectRegistration(1L, i));

            // then
            List<String> requesterIds =
                    projectService.getProjectParticipantList(1L, null, 3).stream()
                            .map(ProjectParticipantInfoResponse::nickname)
                            .toList();

            assertThat(new HashSet<>(requesterIds))
                    .isEqualTo(
                            Set.of(
                                    memberAdmin.getNickname(),
                                    member1.getNickname(),
                                    member2.getNickname()));
        }
    }

    @Nested
    class 프로젝트_삭제_및_나가기 {

        void composeProjectMembers() {
            createTestProject();
            logout();
            loginAs(member1);
            teamService.joinTeam(teamInviteCodeRequest);

            // when
            projectService.requestToProjectRegistration(1L);

            // then
            logout();
            loginAs(memberAdmin);
            projectService.approveProjectRegistration(1L, 1L);
        }

        @Test
        void 프로젝트를_삭제하면_정상적으로_삭제된다() {
            // given
            createTestProject();
            // when
            projectService.deleteProject(1L);
            // then
            assertThatThrownBy(() -> projectService.getProjectInfo(1L))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(ProjectErrorCode.PROJECT_NOT_FOUND.getMessage());
        }

        @Test
        void 권한이_없으면_삭제가_거부된다() {
            // given
            composeProjectMembers();
            logout();
            loginAs(member1);
            // when, then
            assertThatThrownBy(() -> projectService.deleteProject(1L))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(ProjectErrorCode.UNAUTHORIZED_ACCESS.getMessage());
        }

        @Test
        void 프로젝트_참가자가_2명_이상이면_admin은_프로젝트를_못나간다() {
            // given
            composeProjectMembers();

            // when, then
            assertThatThrownBy(() -> projectService.deleteProjectParticipant(1L))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(ProjectErrorCode.PROJECT_ADMIN_CANNOT_LEAVE.getMessage());
        }

        @Test
        void 프로젝트_참가자가_admin_1명이면_프로젝트가_삭제된다() {
            // given
            createTestProject();
            // when
            projectService.deleteProjectParticipant(1L);
            // then
            assertThatThrownBy(() -> projectService.getProjectInfo(1L))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(ProjectErrorCode.PROJECT_NOT_FOUND.getMessage());
        }

        @Test
        void admin권한을_양도하면_정상적으로_프로젝트_참여자가_삭제된다() {
            // given
            composeProjectMembers();
            Long newAdminId =
                    projectService.getProjectParticipantList(1L, null, 3).stream()
                            .filter(r -> !r.nickname().equals(memberAdmin.getNickname()))
                            .map(ProjectParticipantInfoResponse::projectParticipantId)
                            .findAny()
                            .get();
            // when
            projectService.changeProjectAdmin(1L, newAdminId);
            projectService.deleteProjectParticipant(1L);
            // then
            assertThat(projectService.getProjectParticipant(1L).status())
                    .isEqualTo(ProjectParticipantStatus.INACTIVE);
        }
    }
}
