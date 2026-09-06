package com.rag.nexusrag.document.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DocumentUploadResult {
    private String bucket;
    private String objectName;
    private String originalFilename;
    private String contentType;
    private long size;
}
