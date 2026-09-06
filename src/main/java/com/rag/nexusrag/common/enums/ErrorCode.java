package com.rag.nexusrag.common.enums;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    SUCCESS(200, "success", HttpStatus.OK),

    PARAM_ERROR(400, "请求参数错误", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(401, "未登录或认证失败", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "无权限访问", HttpStatus.FORBIDDEN),
    NOT_FOUND(404, "资源不存在", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(405, "请求方法不支持", HttpStatus.METHOD_NOT_ALLOWED),
    MEDIA_TYPE_NOT_SUPPORTED(415, "请求媒体类型不支持", HttpStatus.UNSUPPORTED_MEDIA_TYPE),

    FILE_EMPTY(1001, "上传文件不能为空", HttpStatus.BAD_REQUEST),
    FILE_NAME_EMPTY(10011, "上传文件名称不能为空", HttpStatus.BAD_REQUEST),
    FILE_NAME_INVALID(10012, "文件名不合法", HttpStatus.BAD_REQUEST),
    FILE_NAME_TOO_LONG(10013, "文件名过长", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(1002, "上传文件大小超过限制", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED(1003, "文件上传失败", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_TYPE_NOT_SUPPORTED(1004, "文件类型不合法", HttpStatus.BAD_REQUEST),
    MINIO_ERROR(1101, "对象存储服务异常", HttpStatus.INTERNAL_SERVER_ERROR),

    DOCUMENT_UPLOAD_FAILED(1201, "文档上传失败", HttpStatus.INTERNAL_SERVER_ERROR),

    AI_MODEL_ERROR(2001, "模型调用失败", HttpStatus.INTERNAL_SERVER_ERROR),
    VECTOR_STORE_ERROR(3001, "向量库服务异常", HttpStatus.INTERNAL_SERVER_ERROR),

    SYSTEM_ERROR(500, "系统异常", HttpStatus.INTERNAL_SERVER_ERROR);

    private final Integer code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(Integer code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public Integer code() {
        return code;
    }

    public String message() {
        return message;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
