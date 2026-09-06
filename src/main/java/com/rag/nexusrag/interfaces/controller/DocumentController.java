package com.rag.nexusrag.interfaces.controller;

import com.rag.nexusrag.common.enums.ErrorCode;
import com.rag.nexusrag.common.exception.BusinessException;
import com.rag.nexusrag.common.utils.FileNameUtils;
import com.rag.nexusrag.document.dto.DocumentUploadResult;
import com.rag.nexusrag.common.response.ApiResponse;
import com.rag.nexusrag.document.service.DocumentStorageService;
import com.rag.nexusrag.document.service.DocumentUploadService;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static cn.hutool.core.io.file.FileMode.r;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Resource
    private DocumentUploadService documentUploadService;

    @PostMapping("/upload")
    public ApiResponse<DocumentUploadResult> upload(@RequestPart("file") MultipartFile file){

        if (file == null || file.isEmpty()){
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }

        String originalFilename = file.getOriginalFilename();

        if (!StringUtils.hasText(originalFilename)){
            throw new BusinessException(ErrorCode.FILE_NAME_EMPTY);
        }

        FileNameUtils.validateAndCleanOriginalFilename(originalFilename);

        DocumentUploadResult documentUploadResult = documentUploadService.uploadDocument(file);
        if (documentUploadResult == null || documentUploadResult.getDeleted() == 1){
            return ApiResponse.fail("上传 MinIO 失败");
        }
        return ApiResponse.success(documentUploadResult);
    }

}
