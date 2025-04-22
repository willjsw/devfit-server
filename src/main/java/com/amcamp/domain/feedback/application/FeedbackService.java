package com.amcamp.domain.feedback.application;

import com.amcamp.domain.feedback.dao.FeedbackRepository;
import com.amcamp.domain.feedback.domain.Feedback;
import com.amcamp.domain.feedback.dto.request.FeedbackSendRequest;
import com.amcamp.domain.feedback.dto.request.OriginalFeedbackRequest;
import com.amcamp.domain.feedback.dto.response.FeedbackInfoResponse;
import com.amcamp.domain.feedback.dto.response.FeedbackRefineResponse;
import com.amcamp.domain.member.domain.Member;
import com.amcamp.domain.project.dao.ProjectParticipantRepository;
import com.amcamp.domain.project.dao.ProjectRepository;
import com.amcamp.domain.project.domain.Project;
import com.amcamp.domain.project.domain.ProjectParticipant;
import com.amcamp.domain.project.domain.ProjectParticipantStatus;
import com.amcamp.domain.project.dto.response.ProjectParticipantFeedbackInfoResponse;
import com.amcamp.domain.sprint.dao.SprintRepository;
import com.amcamp.domain.sprint.domain.Sprint;
import com.amcamp.domain.team.dao.TeamParticipantRepository;
import com.amcamp.domain.team.domain.TeamParticipant;
import com.amcamp.global.exception.CommonException;
import com.amcamp.global.exception.errorcode.FeedbackErrorCode;
import com.amcamp.global.exception.errorcode.ProjectErrorCode;
import com.amcamp.global.exception.errorcode.SprintErrorCode;
import com.amcamp.global.exception.errorcode.TeamErrorCode;
import com.amcamp.global.util.MemberUtil;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final ChatGptService chatGptService;
    private final MemberUtil memberUtil;
    private final FeedbackRepository feedbackRepository;
    private final TeamParticipantRepository teamParticipantRepository;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;

    public FeedbackRefineResponse refineFeedback(OriginalFeedbackRequest request) {
        String chatResponse = chatGptService.getAiFeedback(request.originalMessage());
        return new FeedbackRefineResponse(chatResponse);
    }

    public void sendFeedback(FeedbackSendRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Sprint sprint = findBySprintId(request.sprintId());

        validateSprintDueDate(sprint);

        // 피드백을 보낼 대상
        final ProjectParticipant sender = findSender(currentMember, sprint.getProject());

        // 피드백을 받을 대상
        final ProjectParticipant receiver = findReceiver(request.receiverId());
        validateUnknownUser(receiver);

        validateSenderIsNotReceiver(sender, receiver);
        validateDuplicateFeedback(sender, receiver, sprint);
        validateSameProject(sender, receiver);

        feedbackRepository.save(
                Feedback.createFeedback(sender, receiver, sprint, request.message()));
    }

    @Transactional(readOnly = true)
    public Slice<FeedbackInfoResponse> findSprintFeedbacksByParticipant(
            Long projectId, Long sprintId, Long lastFeedbackId, int pageSize) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Project project = findByProjectId(projectId);
        final Sprint sprint = findBySprintId(sprintId);

        ProjectParticipant projectParticipant = validateProjectParticipant(currentMember, project);
        validateProjectSprintMismatch(project, sprint);

        return feedbackRepository.findSprintFeedbacksByParticipant(
                projectParticipant.getId(), sprintId, lastFeedbackId, pageSize);
    }

    @Transactional(readOnly = true)
    public Slice<ProjectParticipantFeedbackInfoResponse> findFeedbackStatusBySprint(
            Long projectId, Long sprintId, Long lastProjectParticipantId, int pageSize) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Project project = findByProjectId(projectId);
        final Sprint sprint = findBySprintId(sprintId);

        ProjectParticipant projectParticipant = validateProjectParticipant(currentMember, project);
        validateProjectSprintMismatch(project, sprint);

        return feedbackRepository.findSprintFeedbackStatusByParticipant(
                projectParticipant, sprintId, lastProjectParticipantId, pageSize);
    }

    private Project findByProjectId(Long projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() -> new CommonException(ProjectErrorCode.PROJECT_NOT_FOUND));
    }

    private Sprint findBySprintId(Long sprintId) {
        return sprintRepository
                .findById(sprintId)
                .orElseThrow(() -> new CommonException(SprintErrorCode.SPRINT_NOT_FOUND));
    }

    private ProjectParticipant findSender(Member currentMember, Project project) {
        final TeamParticipant teamParticipant =
                teamParticipantRepository
                        .findByMemberAndTeam(currentMember, project.getTeam())
                        .orElseThrow(
                                () -> new CommonException(TeamErrorCode.TEAM_PARTICIPANT_REQUIRED));

        return projectParticipantRepository
                .findByProjectAndTeamParticipant(project, teamParticipant)
                .orElseThrow(
                        () -> new CommonException(ProjectErrorCode.PROJECT_PARTICIPATION_REQUIRED));
    }

    private ProjectParticipant findReceiver(Long receiverId) {
        return projectParticipantRepository
                .findById(receiverId)
                .orElseThrow(() -> new CommonException(FeedbackErrorCode.RECEIVER_NOT_FOUND));
    }

    private void validateSenderIsNotReceiver(
            ProjectParticipant sender, ProjectParticipant receiver) {
        if (sender.getId().equals(receiver.getId())) {
            throw new CommonException(FeedbackErrorCode.CANNOT_SEND_FEEDBACK_TO_SELF);
        }
    }

    private void validateSameProject(ProjectParticipant sender, ProjectParticipant receiver) {
        if (!sender.getProject().equals(receiver.getProject())) {
            throw new CommonException(FeedbackErrorCode.INVALID_PROJECT_PARTICIPANT);
        }
    }

    private void validateDuplicateFeedback(
            ProjectParticipant sender, ProjectParticipant receiver, Sprint sprint) {
        boolean feedbackExists =
                feedbackRepository.existsBySenderAndReceiverAndSprint(sender, receiver, sprint);

        if (feedbackExists) {
            throw new CommonException(FeedbackErrorCode.FEEDBACK_ALREADY_SENT);
        }
    }

    private void validateSprintDueDate(Sprint sprint) {
        if (!sprint.getDueDt().isEqual(LocalDate.now())) {
            throw new CommonException(FeedbackErrorCode.FEEDBACK_DUE_DATE_ONLY);
        }
    }

    private void validateProjectSprintMismatch(Project project, Sprint sprint) {
        if (!project.equals(sprint.getProject())) {
            throw new CommonException(ProjectErrorCode.PROJECT_SPRINT_MISMATCH);
        }
    }

    private ProjectParticipant validateProjectParticipant(Member member, Project project) {
        TeamParticipant teamParticipant =
                teamParticipantRepository
                        .findByMemberAndTeam(member, project.getTeam())
                        .orElseThrow(
                                () -> new CommonException(TeamErrorCode.TEAM_PARTICIPANT_REQUIRED));

        return projectParticipantRepository
                .findByProjectAndTeamParticipant(project, teamParticipant)
                .orElseThrow(
                        () -> new CommonException(ProjectErrorCode.PROJECT_PARTICIPATION_REQUIRED));
    }

    private void validateUnknownUser(ProjectParticipant participant) {
        if (participant.getStatus() == ProjectParticipantStatus.INACTIVE) {
            throw new CommonException(FeedbackErrorCode.PARTICIPANT_IS_UNKNOWN);
        }
    }
}
