package com.amcamp.domain.task.domain;

import com.amcamp.domain.common.model.BaseTimeEntity;
import com.amcamp.domain.project.domain.ProjectParticipant;
import com.amcamp.domain.sprint.domain.Sprint;
import com.amcamp.domain.task.dto.request.TaskBasicInfoUpdateRequest;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @Lob private String description;

    @Enumerated(value = EnumType.STRING)
    private TaskStatus taskStatus;

    private LocalDate dueDt;

    @Enumerated(EnumType.STRING)
    private TaskDifficulty taskDifficulty;

    @Enumerated(EnumType.STRING)
    private AssignedStatus assignedStatus;

    @Enumerated(EnumType.STRING)
    private SOSStatus sosStatus;

    // 태스크 수행 멤버
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private ProjectParticipant assignee;

    @Builder(access = AccessLevel.PRIVATE)
    private Task(
            Sprint sprint,
            String description,
            TaskDifficulty taskDifficulty,
            LocalDate dueDt,
            TaskStatus taskStatus,
            AssignedStatus assignedStatus,
            ProjectParticipant assignee,
            SOSStatus sosStatus) {
        this.sprint = sprint;
        this.description = description;
        this.dueDt = dueDt;
        this.taskStatus = taskStatus;
        this.taskDifficulty = taskDifficulty;
        this.assignedStatus = assignedStatus;
        this.assignee = assignee;
        this.sosStatus = sosStatus;
    }

    public static Task createTask(
            Sprint sprint, String description, TaskDifficulty taskDifficulty) {
        return Task.builder()
                .sprint(sprint)
                .description(description)
                .taskDifficulty(taskDifficulty)
                .assignedStatus(AssignedStatus.NOT_ASSIGNED)
                .dueDt(null)
                .taskStatus(TaskStatus.NOT_STARTED)
                .assignee(null)
                .sosStatus(SOSStatus.NOT_SOS)
                .build();
    }

    public void updateTaskBasicInfo(TaskBasicInfoUpdateRequest request) {
        if (request.description() != null) {
            this.description = request.description();
        }
        if (request.taskDifficulty() != null) {
            this.taskDifficulty = request.taskDifficulty();
        }
    }

    public void updateTaskStatus() {
        if (this.taskStatus != TaskStatus.COMPLETED) {
            this.taskStatus = TaskStatus.COMPLETED;
            this.dueDt = LocalDate.now();
        } else {
            this.taskStatus = TaskStatus.ON_GOING;
            this.dueDt = null;
        }
    }

    public void assignTask(ProjectParticipant projectParticipant) {
        this.assignedStatus = AssignedStatus.ASSIGNED;
        this.assignee = projectParticipant;
        this.taskStatus = TaskStatus.ON_GOING;
        this.sosStatus = SOSStatus.NOT_SOS;
    }

    public void updateTaskSOS() {
        if (this.sosStatus != SOSStatus.SOS) {
            this.sosStatus = SOSStatus.SOS;
        } else {
            this.sosStatus = SOSStatus.NOT_SOS;
        }
    }
}
