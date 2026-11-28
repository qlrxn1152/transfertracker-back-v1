package com.dhoon.transfertracker.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.TypeMismatchException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {

        ErrorCode errorCode = ex.getErrorCode();

        ErrorResponse response = new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage()
        );

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

        ErrorResponse errorResponse = resolveErrorResponse(ex, statusCode);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.addAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        return super.handleExceptionInternal(ex, errorResponse, responseHeaders, statusCode, request);
    }

    private ErrorResponse resolveErrorResponse(Exception ex, HttpStatusCode statusCode) {
        ErrorCode errorCode = switch (ex) {
            case TypeMismatchException e ->
                    CommonErrorCode.INVALID_PARAMETER_TYPE;

            case MissingServletRequestParameterException e ->
                    CommonErrorCode.MISSING_PARAMETER;

            case HttpMessageNotReadableException e ->
                    CommonErrorCode.INVALID_REQUEST_BODY;

            case NoResourceFoundException e ->
                    CommonErrorCode.RESOURCE_NOT_FOUND;

            case HttpRequestMethodNotSupportedException e ->
                    CommonErrorCode.METHOD_NOT_ALLOWED;

            case HttpMediaTypeNotSupportedException e ->
                    CommonErrorCode.UNSUPPORTED_MEDIA_TYPE;

            default -> null;
        };

        if (errorCode != null && errorCode.getStatus().value() == statusCode.value()) {
            return new ErrorResponse(
                    errorCode.getCode(),
                    errorCode.getMessage()
            );
        }

        if (statusCode.is5xxServerError()) {
            return new ErrorResponse(
                    "HTTP_" + statusCode.value(),
                    "서버 내부 오류가 발생했습니다."
            );
        }

        return new ErrorResponse(
                "HTTP_" + statusCode.value(),
                "요청을 처리할 수 없습니다."
        );
    }
}
