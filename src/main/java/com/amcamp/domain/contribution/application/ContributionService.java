package com.amcamp.domain.contribution.application;

import com.amcamp.domain.contribution.dao.ContributionRepository;
import com.amcamp.domain.contribution.domain.Contribution;
import com.amcamp.domain.contribution.dto.response.BasicContributionInfoResponse;
import com.amcamp.domain.contribution.dto.response.ContributionInfoResponse;
import com.amcamp.domain.member.domain.Member;
import com.amcamp.domain.project.dao.ProjectParticipantRepository;
import com.amcamp.domain.project.dao.ProjectRepository;
import com.amcamp.domain.project.domain.Project;
import com.amcamp.domain.project.domain.ProjectParticipant;
import com.amcamp.domain.sprint.dao.SprintRepository;
import com.amcamp.domain.sprint.domain.Sprint;
import com.amcamp.domain.team.dao.TeamParticipantRepository;
import com.amcamp.domain.team.domain.Team;
import com.amcamp.domain.team.domain.TeamParticipant;
import com.amcamp.global.exception.CommonException;
import com.amcamp.global.exception.errorcode.ContributionErrorCode;
import com.amcamp.global.exception.errorcode.ProjectErrorCode;
import com.amcamp.global.exception.errorcode.SprintErrorCode;
import com.amcamp.global.exception.errorcode.TeamErrorCode;
import com.amcamp.global.util.MemberUtil;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class ContributionService {
    private final MemberUtil memberUtil;
    private final SprintRepository sprintRepository;
    private final ContributionRepository contributionRepository;
    private final ProjectRepository projectRepository;
    private final TeamParticipantRepository teamParticipantRepository;
    private final ProjectParticipantRepository projectParticipantRepository;

    public BasicContributionInfoResponse getContributionByMember(Long projectId, Long sprintId) {
        Member member = memberUtil.getCurrentMember();
        Project project =
                projectRepository
                        .findById(projectId)
                        .orElseThrow(() -> new CommonException(ProjectErrorCode.PROJECT_NOT_FOUND));

        ProjectParticipant currentParticipant =
                validateProjectParticipant(project, project.getTeam(), member);
        Sprint sprint = validateSprint(sprintId);
        validateProjectSprintMismatch(project, sprint);

        Contribution contribution = validateContribution(sprint, currentParticipant);
        return BasicContributionInfoResponse.of(contribution, contribution.getScore());
    }

    public List<ContributionInfoResponse> getContributionBySprint(Long sprintId) {
        Member member = memberUtil.getCurrentMember();
        Sprint sprint = validateSprint(sprintId);
        Project project = sprint.getProject();
        validateTeamParticipant(project.getTeam(), member);

        List<Contribution> contributionList =
                contributionRepository.findBySprintOrderByScoreDesc(sprint);

        List<ContributionInfoResponse> result = new ArrayList<>();
        for (Contribution contribution : contributionList) {
            result.add(ContributionInfoResponse.of(contribution, contribution.getScore()));
        }

        return result;
    }

    private void validateProjectSprintMismatch(Project project, Sprint sprint) {
        if (!project.equals(sprint.getProject())) {
            throw new CommonException(ProjectErrorCode.PROJECT_SPRINT_MISMATCH);
        }
    }

    private void validateTeamParticipant(Team team, Member member) {
        TeamParticipant teamParticipant =
                teamParticipantRepository
                        .findByMemberAndTeam(member, team)
                        .orElseThrow(
                                () -> new CommonException(TeamErrorCode.TEAM_PARTICIPANT_REQUIRED));
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

    private Sprint validateSprint(Long sprintId) {
        return sprintRepository
                .findById(sprintId)
                .orElseThrow(() -> new CommonException(SprintErrorCode.SPRINT_NOT_FOUND));
    }

    private Contribution validateContribution(Sprint sprint, ProjectParticipant participant) {
        return contributionRepository
                .findBySprintAndParticipant(sprint, participant)
                .orElseThrow(
                        () -> new CommonException(ContributionErrorCode.CONTRIBUTION_NOT_FOUND));
    }
}
