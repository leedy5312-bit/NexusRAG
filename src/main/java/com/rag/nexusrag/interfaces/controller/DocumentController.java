package com.rag.nexusrag.interfaces.controller;

import com.rag.nexusrag.common.enums.ErrorCode;
import com.rag.nexusrag.common.exception.BusinessException;
import com.rag.nexusrag.common.response.CursorPageResponse;
import com.rag.nexusrag.document.dto.DocumentInfo;
import com.rag.nexusrag.document.dto.DocumentUploadResult;
import com.rag.nexusrag.common.response.ApiResponse;
import com.rag.nexusrag.common.response.PageResponse;
import com.rag.nexusrag.document.entity.DocumentFile;
import com.rag.nexusrag.document.service.DocumentMetadataService;
import com.rag.nexusrag.document.service.DocumentUploadService;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentUploadService documentUploadService;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;
    private final DocumentMetadataService documentMetadataService;

    public DocumentController(DocumentUploadService documentUploadService, DocumentMetadataService documentMetadataService) {
        this.documentUploadService = documentUploadService;
        this.documentMetadataService = documentMetadataService;
    }

    /*
    *  上传文档接口
    * */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DocumentUploadResult> upload(@RequestPart("file") MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE){
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        DocumentUploadResult result = documentUploadService.uploadDocument(file);
        return ApiResponse.success(result);
    }

    /*
    *  根据文档Id查询文档
    * */
    @PostMapping
    public ApiResponse<DocumentInfo> queryDocumentById(Long id){

        DocumentFile documentFile = documentMetadataService.queryByID(id);
        DocumentInfo documentInfo = new DocumentInfo();
        BeanUtils.copyProperties(documentFile, documentInfo);
        if (documentInfo == null){
            return ApiResponse.fail("文档不存在");
        }

        return ApiResponse.success(documentInfo);
    }

    /*
     *  分页查询文档列表
     * */
    @GetMapping
    public ApiResponse<PageResponse<DocumentInfo>> pageDocuments(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        PageResponse<DocumentInfo> page = documentMetadataService.pageDocuments(pageNo, pageSize);
        return ApiResponse.success(page);
    }

    /*
     *  游标分页查询文档列表，用于大数据量滚动加载
     * */
    @GetMapping("/scroll")
    public ApiResponse<CursorPageResponse<DocumentInfo>> scrollDocuments(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime cursorCreatedAt,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        CursorPageResponse<DocumentInfo> page = documentMetadataService.scrollDocuments(
                cursorCreatedAt,
                cursorId,
                pageSize
        );
        return ApiResponse.success(page);
    }

    /*
    *  删除文档接口
    * */
    @DeleteMapping
    public ApiResponse<Void> deleteDocumentById(Long id){
        documentUploadService.deleteDocument(id);
        return ApiResponse.success();
    }

}
