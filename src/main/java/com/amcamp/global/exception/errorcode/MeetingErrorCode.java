package com.amcamp.global.exception.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MeetingErrorCode implements BaseErrorCode {
    MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "팀 미팅을 찾을 수 없습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    INVALID_MEETING_TIME_RANGE(HttpStatus.BAD_REQUEST, "유효한 범위 내의 시간을 입력해야합니다."),
    MEETING_DATE_OUT_OF_SPRINT(HttpStatus.BAD_REQUEST, "팀 미팅은 스프린트 기간 내에 생성되어야 합니다."),
    MEETING_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 해당 시간대에 일정이 존재합니다.");
    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String errorClassName() {
        return this.name();
    }
}
