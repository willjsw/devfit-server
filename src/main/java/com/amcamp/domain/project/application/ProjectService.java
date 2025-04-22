package com.amcamp.domain.project.application;

import com.amcamp.domain.member.dao.MemberRepository;
import com.amcamp.domain.member.domain.Member;
import com.amcamp.domain.project.dao.*;
import com.amcamp.domain.project.domain.*;
import com.amcamp.domain.project.domain.ProjectRegistration;
import com.amcamp.domain.project.dto.request.*;
import com.amcamp.domain.project.dto.response.ProjectInfoResponse;
import com.amcamp.domain.project.dto.response.ProjectListInfoResponse;
import com.amcamp.domain.project.dto.response.ProjectParticipantInfoResponse;
import com.amcamp.domain.project.dto.response.ProjectRegisterDetailResponse;
import com.amcamp.domain.project.dto.response.ProjectRegistrationInfoResponse;
import com.amcamp.domain.team.dao.TeamParticipantRepository;
import com.amcamp.domain.team.dao.TeamRepository;
import com.amcamp.domain.team.domain.Team;
import com.amcamp.domain.team.domain.TeamParticipant;
import com.amcamp.global.exception.CommonException;
import com.amcamp.global.exception.errorcode.ProjectErrorCode;
import com.amcamp.global.exception.errorcode.TeamErrorCode;
import com.amcamp.global.util.MemberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final MemberUtil memberUtil;
    private final TeamParticipantRepository teamParticipantRepository;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final MemberRepository memberRepository;
    private final ProjectRegistrationRepository projectRegistrationRepository;

    public void createProject(ProjectCreateRequest request) {
        Member member = memberUtil.getCurrentMember();
        Team team = getTeam(request.teamId());
        TeamParticipant teamParticipant = getValidTeamParticipant(member, team);
        Project project =
                projectRepository.save(
                        Project.createProject(
                                team,
                                normalizeProjectTitle(request.projectTitle()),
                                request.projectDescription(),
                                request.dueDt()));

        projectParticipantRepository.save(
                ProjectParticipant.createProjectParticipant(
                        teamParticipant, project, ProjectParticipantRole.ADMIN));
    }

    @Transactional(readOnly = true)
    public ProjectInfoResponse getProjectInfo(Long projectId) {
        Member member = memberUtil.getCurrentMember();
        Project project = getProjectById(projectId);
        getValidTeamParticipant(member, project.getTeam());
        return ProjectInfoResponse.from(project);
    }

    @Transactional(readOnly = true)
    public Slice<ProjectListInfoResponse> getProjectListInfo(
            Long teamId, Long lastProjectId, int pageSize) {

        Member member = memberUtil.getCurrentMember();
        Team team = getTeam(teamId);
        TeamParticipant teamParticipant = getValidTeamParticipant(member, team);
        return projectRepository.findAllByTeamIdWithPagination(
                teamId, lastProjectId, pageSize, teamParticipant);
    }

    public ProjectInfoResponse updateProject(Long projectId, ProjectUpdateRequest request) {
        final Member member = memberUtil.getCurrentMember();
        final Project project = getProjectById(projectId);

        getValidProjectParticipant(member, project);
        project.updateProject(request.title(), request.description(), request.dueDt());

        return ProjectInfoResponse.from(project);
    }

    public void deleteProject(Long projectId) {
        Member member = memberUtil.getCurrentMember();
        Project project = getProjectById(projectId);
        validateProjectAdmin(member, project);
        projectParticipantRepository.deleteAllByProject(project);
        projectRepository.delete(project);
    }

    public void deleteProjectParticipant(Long projectId) {
        Member member = memberUtil.getCurrentMember();
        Project project = getProjectById(projectId);
        ProjectParticipant participant = getValidProjectParticipant(member, project);
        if (isProjectAdmin(member, project)) {
            boolean hasOtherParticipants =
                    projectParticipantRepository.existsByProjectAndProjectRoleNot(
                            project, ProjectParticipantRole.ADMIN);
            // 다른 팀원 있을 떄 -> 예외 발생
            if (hasOtherParticipants) {
                throw new CommonException(ProjectErrorCode.PROJECT_ADMIN_CANNOT_LEAVE);
            }
            projectParticipantRepository.deleteAllByProject(project);
            projectRepository.delete(project);
        } else {
            participant.changeStatus(ProjectParticipantStatus.INACTIVE);
        }
    }

    public void changeProjectAdmin(Long projectId, Long newAdminId) {
        Member currentMember = memberUtil.getCurrentMember(); // 현재 사용자 (기존 Admin)
        Project project = getProjectById(projectId);
        ProjectParticipant currentAdmin = validateProjectAdmin(currentMember, project);
        ProjectParticipant newAdmin = getProjectParticipantById(newAdminId); // 양도할 프로젝트멤버

        currentAdmin.changeRole(ProjectParticipantRole.MEMBER);
        newAdmin.changeRole(ProjectParticipantRole.ADMIN);
    }

    // project Registration
    public void requestToProjectRegistration(Long projectId) {
        Member member = memberUtil.getCurrentMember();
        Project project = getProjectById(projectId);
        if (projectParticipantRepository.findAllByProject(project).size() >= 15) {
            throw new CommonException(ProjectErrorCode.PROJECT_PARTICIPANT_LIMIT_EXCEED);
        }

        TeamParticipant requester = validateProjectRegistrationAlreadyExists(member, project);

        projectRegistrationRepository.save(ProjectRegistration.createRequest(project, requester));
    }

    @Transactional(readOnly = true)
    public ProjectRegistrationInfoResponse getProjectRegistration(
            Long projectId, Long registrationId) {
        Member member = memberUtil.getCurrentMember();
        Project project = getProjectById(projectId);
        validateProjectAdmin(member, project);
        return ProjectRegistrationInfoResponse.from(getProjectRegistrationById(registrationId));
    }

    @Transactional(readOnly = true)
    public Slice<ProjectRegisterDetailResponse> getProjectRegistrationList(
            Long projectId, Long lastRegistrationId, int pageSize) {
        Member member = memberUtil.getCurrentMember();
        Project project = getProjectById(projectId);
        validateProjectAdmin(member, project);
        return projectRegistrationRepository.findAllByProjectIdWithPagination(
                projectId, lastRegistrationId, pageSize);
    }

    public void approveProjectRegistration(Long projectId, Long registrationId) {
        Member member = memberUtil.getCurrentMember();
        Project project = getProjectById(projectId);
        validateProjectAdmin(member, project);
        ProjectRegistration registration = getProjectRegistrationById(registrationId);
        projectParticipantRepository.save(
                ProjectParticipant.createProjectParticipant(
                        registration.getRequester(), project, ProjectParticipantRole.MEMBER));
        registration.updateStatus(ProjectRegistrationStatus.APPROVED);
    }

    public void rejectProjectRegistration(Long projectId, Long registrationId) {
        Member member = memberUtil.getCurrentMember();
        Project project = getProjectById(projectId);
        validateProjectAdmin(member, project);
        getProjectRegistrationById(registrationId).updateStatus(ProjectRegistrationStatus.REJECTED);
    }

    public void deleteProjectRegistration(Long projectId) {
        Member member = memberUtil.getCurrentMember();
        Project project = getProjectById(projectId);
        TeamParticipant teamParticipant = getValidTeamParticipant(member, project.getTeam());
        ProjectRegistration registration =
                getProjectRegistrationByProjectAndRequester(project, teamParticipant);

        if (teamParticipant.equals(registration.getRequester())) {
            projectRegistrationRepository.delete(registration);
        } else {
            throw new CommonException(ProjectErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    // project participant info

    @Transactional(readOnly = true)
    public ProjectParticipantInfoResponse getProjectParticipant(Long projectId) {
        final Member member = memberUtil.getCurrentMember();
        final Project project = getProjectById(projectId);

        ProjectParticipant participant = getValidProjectParticipant(member, project);

        return ProjectParticipantInfoResponse.from(participant);
    }

    @Transactional(readOnly = true)
    public Slice<ProjectParticipantInfoResponse> getProjectParticipantList(
            Long projectId, Long lastProjectParticipantId, int pageSize) {
        final Member member = memberUtil.getCurrentMember();
        final Project project = getProjectById(projectId);

        getValidTeamParticipant(member, project.getTeam());

        return projectRepository.findAllProjectParticipantByProject(
                projectId, lastProjectParticipantId, pageSize);
    }

    // project utils

    private ProjectParticipant getProjectParticipantById(Long projectMemberId) {
        return projectParticipantRepository
                .findById(projectMemberId)
                .orElseThrow(
                        () -> new CommonException(ProjectErrorCode.PROJECT_PARTICIPANT_NOT_EXISTS));
    }

    private String normalizeProjectTitle(String name) {
        return name.trim().replaceAll("[^0-9a-zA-Z가-힣 ]", "_");
    }

    private Project getProjectById(Long projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() -> new CommonException(ProjectErrorCode.PROJECT_NOT_FOUND));
    }

    private TeamParticipant getValidTeamParticipant(Member member, Team team) {
        return teamParticipantRepository
                .findByMemberAndTeam(member, team)
                .orElseThrow(() -> new CommonException(TeamErrorCode.TEAM_PARTICIPANT_REQUIRED));
    }

    private ProjectParticipant getValidProjectParticipant(Member member, Project project) {
        TeamParticipant teamParticipant = getValidTeamParticipant(member, project.getTeam());
        return projectParticipantRepository
                .findByProjectAndTeamParticipant(project, teamParticipant)
                .orElseThrow(
                        () -> new CommonException(ProjectErrorCode.PROJECT_PARTICIPATION_REQUIRED));
    }

    private Team getTeam(Long teamId) {
        return teamRepository
                .findById(teamId)
                .orElseThrow(() -> new CommonException(TeamErrorCode.TEAM_NOT_FOUND));
    }

    private TeamParticipant validateProjectRegistrationAlreadyExists(
            Member member, Project project) {
        TeamParticipant participant = getValidTeamParticipant(member, project.getTeam());
        if (projectParticipantRepository
                .findByProjectAndTeamParticipant(project, participant)
                .filter(
                        r ->
                                r.getStatus()
                                        .equals(
                                                ProjectParticipantStatus
                                                        .ACTIVE)) // inactive 인 경우 신청 가능하도록
                .isPresent()) {
            throw new CommonException(ProjectErrorCode.PROJECT_PARTICIPANT_ALREADY_EXISTS);
        }
        if (projectRegistrationRepository
                .findByProjectAndRequester(project, participant)
                .isPresent()) {
            throw new CommonException(ProjectErrorCode.PROJECT_REGISTRATION_ALREADY_EXISTS);
        }
        return participant;
    }

    private ProjectRegistration getProjectRegistrationById(Long registrationId) {
        return projectRegistrationRepository
                .findById(registrationId)
                .orElseThrow(
                        () -> new CommonException(ProjectErrorCode.PROJECT_REGISTRATION_NOT_FOUND));
    }

    private ProjectRegistration getProjectRegistrationByProjectAndRequester(
            Project project, TeamParticipant requester) {
        return projectRegistrationRepository
                .findByProjectAndRequester(project, requester)
                .orElseThrow(
                        () -> new CommonException(ProjectErrorCode.PROJECT_REGISTRATION_NOT_FOUND));
    }

    private ProjectParticipant validateProjectAdmin(Member member, Project project) {
        ProjectParticipant participant = getValidProjectParticipant(member, project);
        if (!participant.getProjectRole().equals(ProjectParticipantRole.ADMIN)) {
            throw new CommonException(ProjectErrorCode.UNAUTHORIZED_ACCESS);
        }
        return participant;
    }

    private boolean isProjectAdmin(Member member, Project project) {
        ProjectParticipant participant = getValidProjectParticipant(member, project);
        return participant.getProjectRole().equals(ProjectParticipantRole.ADMIN);
    }
}
