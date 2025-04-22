package com.amcamp.global.exception.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TaskErrorCode implements BaseErrorCode {
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "task를 찾을 수 없습니다."),
    TASK_MODIFY_FORBIDDEN(HttpStatus.FORBIDDEN, "task 수정·삭제 권한이 없습니다. "),
    TASK_ALREADY_ASSIGNED(HttpStatus.FORBIDDEN, "이미 담당자가 존재하는 task입니다."),
    TASK_NOT_ASSIGNED(HttpStatus.FORBIDDEN, "아직 할당되지 않은 task입니다."),
    TASK_ASSIGN_FORBIDDEN(HttpStatus.BAD_REQUEST, "본인에게 task를 할당할 수 없습니다"),
    TASK_COMPLETE_FORBIDDEN(HttpStatus.BAD_REQUEST, "SOS 상태인 task는 완료 처리할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String errorClassName() {
        return this.name();
    }
}
