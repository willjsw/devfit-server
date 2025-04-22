package com.amcamp.infra.scheduler;

import com.amcamp.domain.meeting.application.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetingStatusScheduler {

    private final MeetingService meetingService;

    @Scheduled(fixedRate = 24 * 60 * 60000) // 하루 한번 체크(시간당 한번..?)
    public void updateExpiredMeetings() {
        meetingService.updateExpiredMeetings();
    }
}
