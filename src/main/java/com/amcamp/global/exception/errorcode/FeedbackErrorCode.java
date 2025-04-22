package com.amcamp.global.exception.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FeedbackErrorCode implements BaseErrorCode {
    RECEIVER_NOT_FOUND(HttpStatus.NOT_FOUND, "피드백을 받을 프로젝트 참여자를 찾을 수 없습니다."),

    INVALID_PROJECT_PARTICIPANT(HttpStatus.BAD_REQUEST, "같은 프로젝트에 속한 사람에게만 피드백을 보낼 수 있습니다."),
    CANNOT_SEND_FEEDBACK_TO_SELF(HttpStatus.BAD_REQUEST, "본인에게 피드백을 보낼 수 없습니다."),

    FEEDBACK_ALREADY_SENT(HttpStatus.CONFLICT, "이미 해당 사용자에게 피드백을 보냈습니다."),

    FEEDBACK_DUE_DATE_ONLY(HttpStatus.BAD_REQUEST, "스프린트 마감 당일에만 피드백을 전송할 수 있습니다."),

    FEEDBACK_NOT_EXISTS(HttpStatus.NOT_FOUND, "해당 스프린트에서 받은 피드백이 존재하지 않습니다."),

    PARTICIPANT_IS_UNKNOWN(HttpStatus.BAD_REQUEST, "프로젝트에서 나간 사용자에게는 피드백을 전송할 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String errorClassName() {
        return this.name();
    }
}
