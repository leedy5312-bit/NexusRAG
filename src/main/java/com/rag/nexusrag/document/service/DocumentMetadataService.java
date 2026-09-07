package com.rag.nexusrag.document.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.rag.nexusrag.document.dto.DocumentUploadResult;
import com.rag.nexusrag.document.entity.DocumentFile;

public interface DocumentMetadataService extends IService<DocumentFile> {

    boolean saveFileMetadata(DocumentFile documentFile);

    void updateDeleteStatus(Long id, int x);

    DocumentUploadResult queryByID(Long id);

    boolean isFileHashDuplicate(String fileHash);

}
