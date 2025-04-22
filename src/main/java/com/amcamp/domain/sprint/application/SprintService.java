package com.amcamp.domain.sprint.application;

import com.amcamp.domain.member.domain.Member;
import com.amcamp.domain.project.dao.ProjectParticipantRepository;
import com.amcamp.domain.project.dao.ProjectRepository;
import com.amcamp.domain.project.domain.Project;
import com.amcamp.domain.project.domain.ProjectParticipant;
import com.amcamp.domain.project.domain.ProjectParticipantRole;
import com.amcamp.domain.sprint.dao.SprintPagingDirection;
import com.amcamp.domain.sprint.dao.SprintRepository;
import com.amcamp.domain.sprint.domain.Sprint;
import com.amcamp.domain.sprint.dto.request.SprintCreateRequest;
import com.amcamp.domain.sprint.dto.request.SprintUpdateRequest;
import com.amcamp.domain.sprint.dto.response.SprintDetailResponse;
import com.amcamp.domain.sprint.dto.response.SprintIdResponse;
import com.amcamp.domain.sprint.dto.response.SprintInfoResponse;
import com.amcamp.domain.team.dao.TeamParticipantRepository;
import com.amcamp.domain.team.domain.Team;
import com.amcamp.domain.team.domain.TeamParticipant;
import com.amcamp.global.exception.CommonException;
import com.amcamp.global.exception.errorcode.ProjectErrorCode;
import com.amcamp.global.exception.errorcode.SprintErrorCode;
import com.amcamp.global.exception.errorcode.TeamErrorCode;
import com.amcamp.global.util.MemberUtil;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class SprintService {

    private final MemberUtil memberUtil;
    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final TeamParticipantRepository teamParticipantRepository;
    private final ProjectParticipantRepository projectParticipantRepository;

    public SprintInfoResponse createSprint(SprintCreateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Project project = findByProjectId(request.projectId());

        validateProjectParticipant(project, project.getTeam(), currentMember);

        validatePreviousSprintEnded(project);
        validateSprintDueDate(request.dueDt(), project.getDueDt());

        long count = sprintRepository.countByProject(project);
        String autoTitle = String.valueOf(count + 1);

        Sprint sprint =
                sprintRepository.save(
                        Sprint.createSprint(project, autoTitle, request.goal(), request.dueDt()));

        return SprintInfoResponse.from(sprint);
    }

    public SprintInfoResponse updateSprint(Long sprintId, SprintUpdateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Sprint sprint = findBySprintId(sprintId);

        validateProjectParticipant(
                sprint.getProject(), sprint.getProject().getTeam(), currentMember);

        if (request.dueDt() != null) {
            validateSprintDueDate(request.dueDt(), sprint.getProject().getDueDt());
            validateDueDtIfNextSprintExists(sprint.getProject(), request.dueDt(), sprintId);
        }

        sprint.updateSprint(request.goal(), request.dueDt());

        return SprintInfoResponse.from(sprint);
    }

    public void deleteSprint(Long sprintId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Sprint sprint = findBySprintId(sprintId);

        ProjectParticipant projectParticipant =
                validateProjectParticipant(
                        sprint.getProject(), sprint.getProject().getTeam(), currentMember);

        validateAdminProjectParticipant(projectParticipant);

        sprintRepository.deleteById(sprintId);

        List<Sprint> sprintList =
                sprintRepository.findAllByProjectOrderByCreatedAt(sprint.getProject());
        for (int i = 0; i < sprintList.size(); i++) {
            sprintList.get(i).updateSprintTitle(String.valueOf(i + 1));
        }
    }

    @Transactional(readOnly = true)
    public SprintInfoResponse findSprint(Long sprintId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Sprint sprint = findBySprintId(sprintId);
        final Project project = findByProjectId(sprint.getProject().getId());

        teamParticipantRepository
                .findByMemberAndTeam(currentMember, project.getTeam())
                .orElseThrow(() -> new CommonException(TeamErrorCode.TEAM_PARTICIPANT_REQUIRED));

        return SprintInfoResponse.from(sprint);
    }

    @Transactional(readOnly = true)
    public Slice<SprintDetailResponse> findAllSprint(
            Long projectId, Long baseSprintId, SprintPagingDirection direction) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Project project = findByProjectId(projectId);

        teamParticipantRepository
                .findByMemberAndTeam(currentMember, project.getTeam())
                .orElseThrow(() -> new CommonException(TeamErrorCode.TEAM_PARTICIPANT_REQUIRED));

        validatePagingRequest(baseSprintId, direction);

        return sprintRepository.findAllSprintByProjectId(projectId, baseSprintId, direction);
    }

    @Transactional(readOnly = true)
    public List<SprintIdResponse> findAllSprintId(Long projectId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Project project = findByProjectId(projectId);
        ProjectParticipant participant =
                validateProjectParticipant(project, project.getTeam(), currentMember);

        return sprintRepository.findAllSprintIdByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Slice<SprintDetailResponse> findAllSprintByMember(
            Long projectId, Long baseSprintId, SprintPagingDirection direction) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Project project = findByProjectId(projectId);

        ProjectParticipant participant =
                validateProjectParticipant(project, project.getTeam(), currentMember);

        validatePagingRequest(baseSprintId, direction);

        return sprintRepository.findAllSprintByProjectIdAndAssignee(
                projectId, baseSprintId, direction, participant);
    }

    private Sprint findBySprintId(Long sprintId) {
        return sprintRepository
                .findById(sprintId)
                .orElseThrow(() -> new CommonException(SprintErrorCode.SPRINT_NOT_FOUND));
    }

    private Project findByProjectId(Long projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() -> new CommonException(ProjectErrorCode.PROJECT_NOT_FOUND));
    }

    private ProjectParticipant validateProjectParticipant(
            Project project, Team team, Member currentMember) {
        TeamParticipant teamParticipant =
                teamParticipantRepository
                        .findByMemberAndTeam(currentMember, team)
                        .orElseThrow(
                                () -> new CommonException(TeamErrorCode.TEAM_PARTICIPANT_REQUIRED));

        return projectParticipantRepository
                .findByProjectAndTeamParticipant(project, teamParticipant)
                .orElseThrow(
                        () -> new CommonException(ProjectErrorCode.PROJECT_PARTICIPATION_REQUIRED));
    }

    private void validateAdminProjectParticipant(ProjectParticipant projectParticipant) {
        if (!projectParticipant.getProjectRole().equals(ProjectParticipantRole.ADMIN)) {
            throw new CommonException(SprintErrorCode.SPRINT_DELETE_FORBIDDEN);
        }
    }

    private void validateSprintDueDate(LocalDate sprintDueDt, LocalDate projectDueDt) {
        if (sprintDueDt.isAfter(projectDueDt)) {
            throw new CommonException(SprintErrorCode.SPRINT_DUE_DATE_EXCEEDS_PROJECT_END);
        }
    }

    private void validatePreviousSprintEnded(Project project) {
        sprintRepository
                .findTopByProjectOrderByCreatedDtDesc(project)
                .filter(sprint -> !sprint.getDueDt().isBefore(LocalDate.now()))
                .ifPresent(
                        sprint -> {
                            throw new CommonException(SprintErrorCode.PREVIOUS_SPRINT_NOT_ENDED);
                        });
    }

    private void validateDueDtIfNextSprintExists(Project project, LocalDate dueDt, Long sprintId) {
        Optional<Sprint> nextSprint =
                sprintRepository.findNextSprintAfterDueDate(project.getId(), dueDt, sprintId);

        if (nextSprint.isPresent()) {
            if (!dueDt.isBefore(nextSprint.get().getStartDt())) {
                throw new CommonException(SprintErrorCode.SPRINT_DUE_DATE_CONFLICT_WITH_NEXT);
            }
        }
    }

    private void validatePagingRequest(Long baseSprintId, SprintPagingDirection direction) {
        boolean onlyOnePresent = (baseSprintId == null) != (direction == null);
        if (onlyOnePresent) {
            throw new CommonException(SprintErrorCode.INVALID_PAGING_REQUEST);
        }
    }
}
