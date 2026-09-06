package com.rag.nexusrag.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document_file")
public class DocumentFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String documentId;

    private String originalFilename;

    private String bucketName;

    private String objectName;

    private String contentType;

    private String fileExtension;

    private Long fileSize;

    private String fileHash;

    private String storageProvider;

    private String uploadStatus;

    private String parseStatus;

    private String errorMessage;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}