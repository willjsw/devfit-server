package com.amcamp.domain.task.dao;

import com.amcamp.domain.project.domain.ProjectParticipant;
import com.amcamp.domain.sprint.domain.Sprint;
import com.amcamp.domain.task.domain.Task;
import com.amcamp.domain.task.domain.TaskDifficulty;
import com.amcamp.domain.task.domain.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TaskRepository extends JpaRepository<Task, Long>, TaskRepositoryCustom {

    int countBySprintAndTaskDifficulty(Sprint sprint, TaskDifficulty taskDifficulty);

    int countBySprintAndAssigneeAndTaskDifficulty(
            Sprint sprint, ProjectParticipant assignee, TaskDifficulty taskDifficulty);

    int countBySprint(Sprint sprint);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.sprint = :sprint AND t.taskStatus = :taskStatus")
    int countBySprintAndTaskStatus(Sprint sprint, TaskStatus taskStatus);
}
