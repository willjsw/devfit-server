package com.amcamp.domain.meeting.api;

import com.amcamp.domain.meeting.application.MeetingService;
import com.amcamp.domain.meeting.dto.request.MeetingCreateRequest;
import com.amcamp.domain.meeting.dto.request.MeetingDtUpdateRequest;
import com.amcamp.domain.meeting.dto.request.MeetingTitleUpdateRequest;
import com.amcamp.domain.meeting.dto.response.MeetingInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "9. 팀 미팅 API", description = "팀 미팅 관련 API입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    @Operation(summary = "미팅 생성", description = "새로운 미팅을 생성합니다.")
    @PostMapping("/create")
    public ResponseEntity<Void> meetingCreate(@Valid @RequestBody MeetingCreateRequest request) {
        meetingService.createMeeting(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "미팅 일시 업데이트", description = "미팅 일시를 업데이트합니다.")
    @PostMapping("/{meetingId}/date")
    public ResponseEntity<Void> meetingDateUpdate(
            @PathVariable Long meetingId, @Valid @RequestBody MeetingDtUpdateRequest request) {
        meetingService.updateMeetingDt(meetingId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "미팅 타이틀 업데이트", description = "미팅 타이틀을 업데이트합니다.")
    @PostMapping("/{meetingId}/title")
    public ResponseEntity<Void> meetingTitleUpdate(
            @PathVariable Long meetingId, @Valid @RequestBody MeetingTitleUpdateRequest request) {
        meetingService.updateMeetingTitle(meetingId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "미팅 취소", description = "미팅을 삭제합니다.")
    @DeleteMapping("/{meetingId}")
    public ResponseEntity<Void> meetingCancel(@PathVariable Long meetingId) {
        meetingService.deleteMeeting(meetingId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "미팅 조회", description = "미팅 개별 정보를 조회합니다.")
    @GetMapping("/{meetingId}")
    public MeetingInfoResponse meetingGet(@PathVariable Long meetingId) {
        return meetingService.getMeeting(meetingId);
    }

    @Operation(summary = "미팅 목록 조회", description = "스프린트 별 미팅 목록을 조회합니다.")
    @GetMapping("/{sprintId}/list")
    public Slice<MeetingInfoResponse> meetingListGet(
            @PathVariable Long sprintId,
            @RequestParam(required = false) Long lastMeetingId,
            @RequestParam(defaultValue = "10") int pageSize) {
        return meetingService.getMeetingList(sprintId, lastMeetingId, pageSize);
    }
}
