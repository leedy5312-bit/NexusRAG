package com.rag.nexusrag.document.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DocumentInfo {

    private Long id;

    private String documentId;

    private String originalFilename;

    private String contentType;

    private long fileSize;

    private String storageProvider;

    private String uploadStatus;

    private String parseStatus;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer deleted;

}
