package com.amcamp.global.exception.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ProjectErrorCode implements BaseErrorCode {
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "project 를 찾을 수 없습니다."),
    PROJECT_PARTICIPANT_LIMIT_EXCEED(HttpStatus.BAD_REQUEST, "project 별 최대 참여 가능 참여자 수를 초과했습니다."),
    PROJECT_PARTICIPATION_REQUIRED(HttpStatus.FORBIDDEN, "해당 프로젝트 참여자가 아닙니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    PROJECT_PARTICIPANT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 참여중인 프로젝트 참가자입니다."),
    PROJECT_ADMIN_CANNOT_LEAVE(
            HttpStatus.FORBIDDEN, "프로젝트 Admin은 다른 팀원에게 권한을 넘긴 후에만 프로젝트를 나갈 수 있습니다."),
    PROJECT_REGISTRATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 프로젝트 가입 요청이 생성되어 있습니다"),
    PROJECT_REGISTRATION_NOT_FOUND(HttpStatus.NOT_FOUND, "project 가입 요청을 찾을 수 없습니다."),
    PROJECT_SPRINT_MISMATCH(HttpStatus.FORBIDDEN, "요청한 스프린트는 현재 참여 중인 프로젝트의 스프린트가 아닙니다."),

    PROJECT_PARTICIPANT_NOT_EXISTS(HttpStatus.NOT_FOUND, "프로젝트 참가자가 존재하지 않습니다."),

    PROJECT_DUE_DATE_BEFORE_START(HttpStatus.BAD_REQUEST, "프로젝트 마감일자는 시작일자 이후여야 합니다.");
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String errorClassName() {
        return this.name();
    }
}
