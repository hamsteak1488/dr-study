package com.nomz.doctorstudy.blockinterpreter;

import com.nomz.doctorstudy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BlockErrorCode implements ErrorCode {
    VALUE_BLOCK_NOT_FOUND(HttpStatus.BAD_REQUEST, "값 블록이 위치해야 합니다."),
    COMMAND_BLOCK_NOT_FOUND(HttpStatus.BAD_REQUEST, "명령 블록이 위치해야 합니다."),
    UNEXPECTED_TOKEN(HttpStatus.BAD_REQUEST, "토큰을 해석할 수 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}

