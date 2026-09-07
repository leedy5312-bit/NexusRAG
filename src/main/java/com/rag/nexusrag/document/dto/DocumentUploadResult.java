package com.rag.nexusrag.document.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DocumentUploadResult {

    private String documentId;

    private String originalFilename;

    private String contentType;

    private long fileSize;

    private String uploadStatus;

    private String parseStatus;

    private Integer deleted;

}
