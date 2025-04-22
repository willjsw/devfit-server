package com.amcamp.domain.project.domain;

import com.amcamp.domain.common.model.BaseTimeEntity;
import com.amcamp.domain.team.domain.TeamParticipant;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
public class ProjectRegistration extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_registration_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_participant_id", unique = true)
    private TeamParticipant requester;

    @Enumerated(EnumType.STRING)
    private ProjectRegistrationStatus requestStatus;

    @Builder
    public ProjectRegistration(
            Project project, TeamParticipant requester, ProjectRegistrationStatus requestStatus) {
        this.project = project;
        this.requester = requester;
        this.requestStatus = requestStatus;
    }

    public static ProjectRegistration createRequest(Project project, TeamParticipant requester) {
        return ProjectRegistration.builder()
                .project(project)
                .requester(requester)
                .requestStatus(ProjectRegistrationStatus.PENDING)
                .build();
    }

    public void updateStatus(ProjectRegistrationStatus requestStatus) {
        this.requestStatus = requestStatus;
    }
}
