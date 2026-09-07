package com.rag.nexusrag.interfaces.controller;

import com.rag.nexusrag.common.enums.ErrorCode;
import com.rag.nexusrag.common.exception.BusinessException;
import com.rag.nexusrag.document.dto.DocumentUploadResult;
import com.rag.nexusrag.common.response.ApiResponse;
import com.rag.nexusrag.document.service.DocumentUploadService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentUploadService documentUploadService;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    public DocumentController(DocumentUploadService documentUploadService) {
        this.documentUploadService = documentUploadService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DocumentUploadResult> upload(@RequestPart("file") MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE){
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        DocumentUploadResult result = documentUploadService.uploadDocument(file);
        return ApiResponse.success(result);
    }
}
