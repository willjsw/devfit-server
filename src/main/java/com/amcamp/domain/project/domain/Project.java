package com.amcamp.domain.project.domain;

import com.amcamp.domain.common.model.BaseTimeEntity;
import com.amcamp.domain.sprint.domain.Sprint;
import com.amcamp.domain.team.domain.Team;
import com.amcamp.global.exception.CommonException;
import com.amcamp.global.exception.errorcode.ProjectErrorCode;
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
public class Project extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long id;

    // 팀
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    // 프로젝트 이름
    @Column(name = "project_title")
    private String title;

    // 설명
    @Lob private String description;

    private LocalDate startDt;

    private LocalDate dueDt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sprint> sprints = new ArrayList<>();

    // 캘린더
    @Builder(access = AccessLevel.PRIVATE)
    private Project(
            Team team, String title, String description, LocalDate startDt, LocalDate dueDt) {
        this.team = team;
        this.title = title;
        this.description = description;
        this.startDt = startDt;
        this.dueDt = dueDt;
    }

    public static Project createProject(
            Team team, String title, String description, LocalDate dueDt) {
        validateDueDt(LocalDate.now(), dueDt);
        return Project.builder()
                .team(team)
                .title(title)
                .description(description)
                .startDt(LocalDate.now())
                .dueDt(dueDt)
                .build();
    }

    public void updateProject(String title, String description, LocalDate dueDt) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (dueDt != null) {
            validateDueDt(this.startDt, dueDt);
            this.dueDt = dueDt;
        }
    }

    private static void validateDueDt(LocalDate startDt, LocalDate dueDt) {
        if (dueDt.isBefore(startDt)) {
            throw new CommonException(ProjectErrorCode.PROJECT_DUE_DATE_BEFORE_START);
        }
    }
}
