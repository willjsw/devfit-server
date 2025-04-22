package com.amcamp.domain.task.application;

import com.amcamp.domain.contribution.dao.ContributionRepository;
import com.amcamp.domain.contribution.domain.Contribution;
import com.amcamp.domain.member.domain.Member;
import com.amcamp.domain.project.dao.ProjectParticipantRepository;
import com.amcamp.domain.project.dao.ProjectRepository;
import com.amcamp.domain.project.domain.Project;
import com.amcamp.domain.project.domain.ProjectParticipant;
import com.amcamp.domain.project.domain.ProjectParticipantRole;
import com.amcamp.domain.sprint.dao.SprintRepository;
import com.amcamp.domain.sprint.domain.Sprint;
import com.amcamp.domain.task.dao.TaskRepository;
import com.amcamp.domain.task.domain.*;
import com.amcamp.domain.task.dto.request.TaskBasicInfoUpdateRequest;
import com.amcamp.domain.task.dto.request.TaskCreateRequest;
import com.amcamp.domain.task.dto.response.TaskBasicInfoResponse;
import com.amcamp.domain.task.dto.response.TaskInfoResponse;
import com.amcamp.domain.team.dao.TeamParticipantRepository;
import com.amcamp.domain.team.domain.Team;
import com.amcamp.domain.team.domain.TeamParticipant;
import com.amcamp.global.exception.CommonException;
import com.amcamp.global.exception.errorcode.*;
import com.amcamp.global.util.MemberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class TaskService {
    private final MemberUtil memberUtil;
    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final TeamParticipantRepository teamParticipantRepository;
    private final ProjectRepository projectRepository;
    private final ContributionRepository contributionRepository;

    public TaskInfoResponse createTask(TaskCreateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Sprint sprint = findBySprintId(request.sprintId());
        final Project project = sprint.getProject();
        validateProjectParticipant(project, project.getTeam(), currentMember);
        Task task =
                taskRepository.save(
                        Task.createTask(sprint, request.description(), request.taskDifficulty()));

        return findProjectParticipantMember(task) != null
                ? TaskInfoResponse.from(task, findProjectParticipantMember(task))
                : TaskInfoResponse.from(task);
    }

    public TaskInfoResponse updateTaskBasicInfo(Long taskId, TaskBasicInfoUpdateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Task task = findByTaskId(taskId);
        final Sprint sprint = findBySprintId(task.getSprint().getId());
        final Project project = sprint.getProject();

        ProjectParticipant participant =
                validateProjectParticipant(project, project.getTeam(), currentMember);
        validateTaskModify(participant, task);
        task.updateTaskBasicInfo(request);

        return findProjectParticipantMember(task) != null
                ? TaskInfoResponse.from(task, findProjectParticipantMember(task))
                : TaskInfoResponse.from(task);
    }

    public TaskInfoResponse updateTaskStatus(Long taskId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Task task = findByTaskId(taskId);
        final Sprint sprint = findBySprintId(task.getSprint().getId());
        final Project project = sprint.getProject();

        ProjectParticipant participant =
                validateProjectParticipant(project, project.getTeam(), currentMember);
        validateTaskStatusModify(participant, task);

        task.updateTaskStatus();
        Contribution contribution = validateContribution(sprint, participant);
        sprint.updateProgress(getSprintProgress(sprint));
        contribution.updateScore(getScore(sprint, participant));

        return findProjectParticipantMember(task) != null
                ? TaskInfoResponse.from(task, findProjectParticipantMember(task))
                : TaskInfoResponse.from(task);
    }

    public TaskInfoResponse updateTaskSOS(Long taskId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Task task = findByTaskId(taskId);
        final Sprint sprint = findBySprintId(task.getSprint().getId());
        final Project project = sprint.getProject();

        ProjectParticipant participant =
                validateProjectParticipant(project, project.getTeam(), currentMember);
        validateTaskNotAssignedForSos(task);
        validateTaskModify(participant, task);
        task.updateTaskSOS();

        return findProjectParticipantMember(task) != null
                ? TaskInfoResponse.from(task, findProjectParticipantMember(task))
                : TaskInfoResponse.from(task);
    }

    public TaskInfoResponse assignTask(Long taskId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Task task = findByTaskId(taskId);
        final Sprint sprint = findBySprintId(task.getSprint().getId());
        final Project project = sprint.getProject();

        ProjectParticipant projectParticipant =
                validateProjectParticipant(project, project.getTeam(), currentMember);

        if (task.getAssignedStatus() != AssignedStatus.NOT_ASSIGNED
                && task.getAssignee() != null
                && task.getSosStatus() != SOSStatus.SOS) {
            throw new CommonException(TaskErrorCode.TASK_ALREADY_ASSIGNED);
        }

        if (task.getAssignedStatus() != AssignedStatus.NOT_ASSIGNED
                && task.getAssignee() == projectParticipant
                && task.getSosStatus() == SOSStatus.SOS) {
            throw new CommonException(TaskErrorCode.TASK_ASSIGN_FORBIDDEN);
        }

        task.assignTask(projectParticipant);
        return findProjectParticipantMember(task) != null
                ? TaskInfoResponse.from(task, findProjectParticipantMember(task))
                : TaskInfoResponse.from(task);
    }

    public void deleteTask(Long taskId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Task task = findByTaskId(taskId);
        final Sprint sprint = findBySprintId(task.getSprint().getId());
        final Project project = sprint.getProject();

        ProjectParticipant participant =
                validateProjectParticipant(project, project.getTeam(), currentMember);
        validateTaskModify(participant, task);
        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public Slice<TaskInfoResponse> getTasksBySprint(Long sprintId, Long lastTaskId, int size) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Sprint sprint = findBySprintId(sprintId);
        final Project project = sprint.getProject();
        validateTeamParticipant(project.getTeam(), currentMember);
        return taskRepository.findBySprint(sprintId, lastTaskId, size);
    }

    @Transactional(readOnly = true)
    public Slice<TaskBasicInfoResponse> getTasksByMember(Long sprintId, Long lastTaskId, int size) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Sprint sprint = findBySprintId(sprintId);
        final Project project = sprint.getProject();
        ProjectParticipant projectParticipant =
                validateProjectParticipant(project, project.getTeam(), currentMember);
        return taskRepository.findBySprintAndAssignee(
                sprintId, projectParticipant, lastTaskId, size);
    }

    private Double getScore(Sprint sprint, ProjectParticipant participant) {
        int highTask = taskRepository.countBySprintAndTaskDifficulty(sprint, TaskDifficulty.HIGH);
        int midTask = taskRepository.countBySprintAndTaskDifficulty(sprint, TaskDifficulty.MID);
        int lowTask = taskRepository.countBySprintAndTaskDifficulty(sprint, TaskDifficulty.LOW);

        int highTaskCompleted =
                taskRepository.countBySprintAndAssigneeAndTaskDifficulty(
                        sprint, participant, TaskDifficulty.HIGH);
        int midTaskCompleted =
                taskRepository.countBySprintAndAssigneeAndTaskDifficulty(
                        sprint, participant, TaskDifficulty.MID);
        int lowTaskCompleted =
                taskRepository.countBySprintAndAssigneeAndTaskDifficulty(
                        sprint, participant, TaskDifficulty.LOW);

        int maxScore = 20 * highTask + 10 * midTask + lowTask * 5;
        if (maxScore == 0) {
            throw new CommonException(SprintErrorCode.TASK_NOT_CREATED_YET);
        }

        double total =
                (20 * highTaskCompleted + 10 * midTaskCompleted + 5 * lowTaskCompleted) * 100;
        return total / maxScore;
    }

    private Contribution validateContribution(Sprint sprint, ProjectParticipant participant) {
        return contributionRepository
                .findBySprintAndParticipant(sprint, participant)
                .orElseGet(
                        () ->
                                contributionRepository.save(
                                        Contribution.createContribution(sprint, participant, 0.0)));
    }

    private Double getSprintProgress(Sprint sprint) {
        int totalTasks = taskRepository.countBySprint(sprint);
        double completedTasks =
                taskRepository.countBySprintAndTaskStatus(sprint, TaskStatus.COMPLETED);
        Double progress = completedTasks * 100 / totalTasks;
        return progress;
    }

    private void validateTaskNotAssignedForSos(Task task) {
        if (task.getAssignedStatus() == AssignedStatus.NOT_ASSIGNED) {
            throw new CommonException(TaskErrorCode.TASK_NOT_ASSIGNED);
        }
    }

    private void validateTeamParticipant(Team team, Member currentMember) {
        TeamParticipant teamParticipant =
                teamParticipantRepository
                        .findByMemberAndTeam(currentMember, team)
                        .orElseThrow(
                                () -> new CommonException(TeamErrorCode.TEAM_PARTICIPANT_REQUIRED));
    }

    private void validateTaskModify(ProjectParticipant participant, Task task) {
        validateTaskModifyAccess(participant, task);
        if (task.getTaskStatus() == TaskStatus.COMPLETED) {
            throw new CommonException(TaskErrorCode.TASK_MODIFY_FORBIDDEN);
        }
    }

    private void validateTaskStatusModify(ProjectParticipant participant, Task task) {
        validateTaskModifyAccess(participant, task);
        if (task.getSosStatus() == SOSStatus.SOS) {
            throw new CommonException(TaskErrorCode.TASK_COMPLETE_FORBIDDEN);
        }
    }

    private void validateTaskModifyAccess(ProjectParticipant participant, Task task) {
        if (task.getAssignedStatus() != AssignedStatus.NOT_ASSIGNED || task.getAssignee() != null) {
            if (!participant.getProjectRole().equals(ProjectParticipantRole.ADMIN)
                    && !participant.equals(task.getAssignee())) {
                throw new CommonException(TaskErrorCode.TASK_MODIFY_FORBIDDEN);
            }
        }
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

    private Sprint findBySprintId(Long sprintId) {
        return sprintRepository
                .findById(sprintId)
                .orElseThrow(() -> new CommonException(SprintErrorCode.SPRINT_NOT_FOUND));
    }

    private Task findByTaskId(Long taskId) {
        return taskRepository
                .findById(taskId)
                .orElseThrow(() -> new CommonException(TaskErrorCode.TASK_NOT_FOUND));
    }

    private Member findProjectParticipantMember(Task task) {
        Member member = null;
        if (task.getAssignee() != null && task.getAssignedStatus() != AssignedStatus.NOT_ASSIGNED) {
            member = task.getAssignee().getTeamParticipant().getMember();
        }
        return member;
    }
}
