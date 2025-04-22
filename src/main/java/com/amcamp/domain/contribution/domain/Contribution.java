package com.amcamp.domain.contribution.domain;

import com.amcamp.domain.common.model.BaseTimeEntity;
import com.amcamp.domain.project.domain.ProjectParticipant;
import com.amcamp.domain.sprint.domain.Sprint;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Contribution extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contribution_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    // 기여한 멤버
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    private ProjectParticipant participant;

    // 기여도 점수/비율
    private Double score;

    @Builder(access = AccessLevel.PRIVATE)
    Contribution(Sprint sprint, ProjectParticipant participant, Double score) {
        this.sprint = sprint;
        this.participant = participant;
        this.score = score;
    }

    public static Contribution createContribution(
            Sprint sprint, ProjectParticipant participant, Double score) {
        return Contribution.builder().sprint(sprint).participant(participant).score(score).build();
    }

    public void updateScore(Double score) {
        this.score = score;
    }
}
