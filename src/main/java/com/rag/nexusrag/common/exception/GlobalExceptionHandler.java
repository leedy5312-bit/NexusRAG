package com.rag.nexusrag.common.exception;

import com.rag.nexusrag.common.enums.ErrorCode;
import com.rag.nexusrag.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("Business exception, code={}, message={}, path={}",
                errorCode.code(), e.getMessage(), request.getRequestURI());
        return buildResponse(errorCode, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse(ErrorCode.PARAM_ERROR.message());
        log.warn("Request body validation failed, message={}, path={}", message, request.getRequestURI());
        return buildResponse(ErrorCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(
            BindException e,
            HttpServletRequest request
    ) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse(ErrorCode.PARAM_ERROR.message());
        log.warn("Request parameter binding failed, message={}, path={}", message, request.getRequestURI());
        return buildResponse(ErrorCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .orElse(ErrorCode.PARAM_ERROR.message());
        log.warn("Constraint validation failed, message={}, path={}", message, request.getRequestURI());
        return buildResponse(ErrorCode.PARAM_ERROR, message);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(
            Exception e,
            HttpServletRequest request
    ) {
        String message = resolveBadRequestMessage(e);
        log.warn("Bad request, message={}, path={}", message, request.getRequestURI());
        return buildResponse(ErrorCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e,
            HttpServletRequest request
    ) {
        log.warn("Uploaded file is too large, path={}", request.getRequestURI());
        return buildResponse(ErrorCode.FILE_TOO_LARGE);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(
            NoResourceFoundException e,
            HttpServletRequest request
    ) {
        log.warn("Resource not found, method={}, path={}", request.getMethod(), request.getRequestURI());
        return buildResponse(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request
    ) {
        log.warn("HTTP method not supported, method={}, path={}", request.getMethod(), request.getRequestURI());
        return buildResponse(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e,
            HttpServletRequest request
    ) {
        log.warn("HTTP media type not supported, contentType={}, path={}",
                request.getContentType(), request.getRequestURI());
        return buildResponse(ErrorCode.MEDIA_TYPE_NOT_SUPPORTED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception e,
            HttpServletRequest request
    ) {
        log.error("Unexpected system exception, method={}, path={}",
                request.getMethod(), request.getRequestURI(), e);
        return buildResponse(ErrorCode.SYSTEM_ERROR);
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(ErrorCode errorCode) {
        return buildResponse(errorCode, errorCode.message());
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(ErrorCode errorCode, String message) {
        return ResponseEntity
                .status(errorCode.httpStatus())
                .body(ApiResponse.fail(errorCode, message));
    }

    private String resolveBadRequestMessage(Exception e) {
        if (e instanceof MissingServletRequestParameterException missingParameter) {
            return "缺少必要参数: " + missingParameter.getParameterName();
        }
        if (e instanceof MissingServletRequestPartException missingPart) {
            return "缺少必要文件或表单字段: " + missingPart.getRequestPartName();
        }
        if (e instanceof MethodArgumentTypeMismatchException typeMismatch) {
            Class<?> requiredTypeClass = typeMismatch.getRequiredType();
            String requiredType = requiredTypeClass == null ? "正确类型" : requiredTypeClass.getSimpleName();
            return "参数类型错误: " + typeMismatch.getName() + " 应为 " + requiredType;
        }
        if (e instanceof HttpMessageNotReadableException) {
            return "请求体格式错误";
        }
        return e.getMessage() == null ? ErrorCode.PARAM_ERROR.message() : e.getMessage();
    }
}
