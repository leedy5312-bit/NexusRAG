package com.rag.nexusrag.document.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class StoredObjectInfo {
    private String bucketName;
    private String objectName;
    private String fileHash;
}