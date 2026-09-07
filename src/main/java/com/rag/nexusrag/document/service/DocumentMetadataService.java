package com.rag.nexusrag.document.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.rag.nexusrag.common.response.CursorPageResponse;
import com.rag.nexusrag.common.response.PageResponse;
import com.rag.nexusrag.document.dto.DocumentInfo;
import com.rag.nexusrag.document.entity.DocumentFile;

import java.time.LocalDateTime;

public interface DocumentMetadataService extends IService<DocumentFile> {

    boolean saveFileMetadata(DocumentFile documentFile);

    void updateDeleteStatus(Long id, int x);

    DocumentFile queryByID(Long id);

    boolean isFileHashDuplicate(String fileHash);

    PageResponse<DocumentInfo> pageDocuments(long pageNo, long pageSize);

    CursorPageResponse<DocumentInfo> scrollDocuments(LocalDateTime cursorCreatedAt, Long cursorId, long pageSize);

}
