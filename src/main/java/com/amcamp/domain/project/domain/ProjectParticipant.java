package com.amcamp.domain.project.domain;

import com.amcamp.domain.common.model.BaseTimeEntity;
import com.amcamp.domain.team.domain.TeamParticipant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
public class ProjectParticipant extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_participant_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_participant_id")
    private TeamParticipant teamParticipant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    // 프로젝트 내 권한
    @Enumerated(EnumType.STRING)
    private ProjectParticipantRole projectRole;

    @Enumerated(EnumType.STRING)
    private ProjectParticipantStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private ProjectParticipant(
            TeamParticipant teamParticipant,
            Project project,
            ProjectParticipantRole projectRole,
            ProjectParticipantStatus status) {
        this.teamParticipant = teamParticipant;
        this.project = project;
        this.projectRole = projectRole;
        this.status = status;
    }

    public static ProjectParticipant createProjectParticipant(
            TeamParticipant teamParticipant, Project project, ProjectParticipantRole projectRole) {
        return ProjectParticipant.builder()
                .teamParticipant(teamParticipant)
                .project(project)
                .projectRole(projectRole)
                .status(ProjectParticipantStatus.ACTIVE)
                .build();
    }

    public void changeRole(ProjectParticipantRole projectRole) {
        this.projectRole = projectRole;
    }

    public void changeStatus(ProjectParticipantStatus status) {
        this.status = status;
    }
}
