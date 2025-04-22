package com.amcamp.domain.sprint.domain;

import com.amcamp.domain.common.model.BaseTimeEntity;
import com.amcamp.domain.contribution.domain.Contribution;
import com.amcamp.domain.feedback.domain.Feedback;
import com.amcamp.domain.meeting.domain.Meeting;
import com.amcamp.domain.project.domain.Project;
import com.amcamp.domain.task.domain.Task;
import com.amcamp.global.exception.CommonException;
import com.amcamp.global.exception.errorcode.SprintErrorCode;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sprint extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sprint_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    private String title;

    @Lob private String goal;

    private LocalDate startDt;

    private LocalDate dueDt;

    @OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Feedback> feedbacks = new ArrayList<>();

    // Task
    @OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();

    // Meeting
    @OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Meeting> meetings = new ArrayList<>();

    // 기여도
    @OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Contribution> contribution = new ArrayList<>();

    // 진척도
    private Double progress;

    @Builder(access = AccessLevel.PRIVATE)
    private Sprint(
            Project project,
            String title,
            String goal,
            LocalDate startDt,
            LocalDate dueDt,
            Double progress) {
        this.project = project;
        this.title = title;
        this.goal = goal;
        this.startDt = startDt;
        this.dueDt = dueDt;
        this.progress = progress;
    }

    public static Sprint createSprint(Project project, String title, String goal, LocalDate dueDt) {
        validateDueDt(LocalDate.now(), dueDt);
        return Sprint.builder()
                .project(project)
                .title(title)
                .goal(goal)
                .progress(0.0)
                .startDt(LocalDate.now())
                .dueDt(dueDt)
                .build();
    }

    public void updateSprint(String goal, LocalDate dueDt) {
        if (goal != null) this.goal = goal;
        if (dueDt != null) {
            validateDueDt(this.startDt, dueDt);
            this.dueDt = dueDt;
        }
    }

    public void updateSprintTitle(String title) {
        this.title = title;
    }

    public void updateProgress(Double progress) {
        this.progress = progress;
    }

    private static void validateDueDt(LocalDate startDt, LocalDate dueDt) {
        if (dueDt.isBefore(startDt)) {
            throw new CommonException(SprintErrorCode.SPRINT_DUE_DATE_BEFORE_START);
        }
    }
}
