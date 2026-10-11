package com.dhoon.transfertracker.internal.common;

import com.dhoon.transfertracker.internal.admin.exception.AdminSyncFailedException;
import com.dhoon.transfertracker.internal.member.exception.AlreadyRegisteredFavoriteTeamException;
import com.dhoon.transfertracker.internal.member.exception.DuplicateLoginIdException;
import com.dhoon.transfertracker.internal.member.exception.InvalidFavoriteTeamRequestException;
import com.dhoon.transfertracker.internal.member.exception.LoginFailException;
import com.dhoon.transfertracker.internal.member.exception.NotFoundMemberException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateLoginIdException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateLoginId(DuplicateLoginIdException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(LoginFailException.class)
    public ResponseEntity<ApiErrorResponse> handleLoginFail(LoginFailException exception) {
        return error(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler({
            InvalidFavoriteTeamRequestException.class,
            AlreadyRegisteredFavoriteTeamException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(NotFoundMemberException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(AdminSyncFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleAdminSyncFailed(AdminSyncFailedException exception) {
        return error(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " 값을 확인해 주세요.")
                .orElse("요청 값을 확인해 주세요.");

        return error(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .body(ApiErrorResponse.of(message, status.value()));
    }
}
