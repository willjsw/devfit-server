package com.amcamp.domain.feedback.dao;

import com.amcamp.domain.feedback.domain.Feedback;
import com.amcamp.domain.project.domain.ProjectParticipant;
import com.amcamp.domain.sprint.domain.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository
        extends JpaRepository<Feedback, Long>, FeedbackRepositoryCustom {
    boolean existsBySenderAndReceiverAndSprint(
            ProjectParticipant sender, ProjectParticipant receiver, Sprint sprint);
}
