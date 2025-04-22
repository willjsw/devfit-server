package com.amcamp.domain.contribution.dao;

import com.amcamp.domain.contribution.domain.Contribution;
import com.amcamp.domain.project.domain.ProjectParticipant;
import com.amcamp.domain.sprint.domain.Sprint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContributionRepository extends JpaRepository<Contribution, Long> {
    Optional<Contribution> findBySprintAndParticipant(
            Sprint sprint, ProjectParticipant participant);

    List<Contribution> findBySprintOrderByScoreDesc(Sprint sprint);
}
