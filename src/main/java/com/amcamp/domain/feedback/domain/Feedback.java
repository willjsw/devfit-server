package com.amcamp.domain.feedback.domain;

import com.amcamp.domain.common.model.BaseTimeEntity;
import com.amcamp.domain.project.domain.ProjectParticipant;
import com.amcamp.domain.sprint.domain.Sprint;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private ProjectParticipant sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private ProjectParticipant receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @Column(length = 600)
    private String message;

    @Builder(access = AccessLevel.PRIVATE)
    private Feedback(
            ProjectParticipant sender, ProjectParticipant receiver, Sprint sprint, String message) {
        this.sender = sender;
        this.receiver = receiver;
        this.sprint = sprint;
        this.message = message;
    }

    public static Feedback createFeedback(
            ProjectParticipant sender, ProjectParticipant receiver, Sprint sprint, String message) {
        return Feedback.builder()
                .sender(sender)
                .receiver(receiver)
                .sprint(sprint)
                .message(message)
                .build();
    }
}
