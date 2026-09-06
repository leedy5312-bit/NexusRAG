package com.rag.nexusrag.document.service;

import com.rag.nexusrag.common.config.MinioProperties;
import com.rag.nexusrag.common.enums.ErrorCode;
import com.rag.nexusrag.common.exception.BusinessException;
import com.rag.nexusrag.common.utils.IdGenerator;
import com.rag.nexusrag.document.dto.DocumentUploadResult;
import com.rag.nexusrag.document.entity.DocumentFile;
import com.rag.nexusrag.document.enums.StorageProvider;
import com.rag.nexusrag.document.enums.UploadStatus;
import com.rag.nexusrag.document.service.impl.DocumentMetadataServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
public class DocumentUploadService {

    private final MinioProperties minioProperties;

    @Resource
    private DocumentStorageService documentStorageService;

    @Resource
    private DocumentMetadataService documentMetadataService;

    public DocumentUploadService(MinioProperties minioProperties) {
        this.minioProperties = minioProperties;
    }

    public DocumentUploadResult uploadDocument(MultipartFile file){

        String bucket = minioProperties.bucket();
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        Long id = IdGenerator.Id();
        String documentId = "DOC" + IdGenerator.Id();
        DocumentFile documentFile = new DocumentFile();
        String objectName =  null;
        documentFile.setId(id);
        documentFile.setDocumentId(documentId);
        documentFile.setOriginalFilename(file.getOriginalFilename());
        documentFile.setBucketName(bucket);
        documentFile.setObjectName(objectName);
        documentFile.setContentType(file.getContentType());
        documentFile.setFileExtension(StringUtils.getFilenameExtension(originalFilename));
        documentFile.setFileSize(file.getSize());
        documentFile.setFileHash(null); //TODO 文件哈希值计算未做 SHA-256
        documentFile.setStorageProvider(StorageProvider.MINIO.name());
        documentFile.setUploadStatus(UploadStatus.UPLOADING.name());
        // TODO parseStatus解析状态未做

        LocalDateTime now = LocalDateTime.now();

        documentFile.setCreatedAt(now);
        documentFile.setUpdatedAt(now);
        documentFile.setDeleted(0);
        try {
            objectName = documentStorageService.uploadMinio(file, bucket, originalFilename);

            if (objectName == null){
                documentFile.setUploadStatus(UploadStatus.UPLOAD_FAILED.name());
                documentFile.setErrorMessage("上传文档到MinIO失败.." + ErrorCode.FILE_UPLOAD_FAILED);
                now = LocalDateTime.now();
                documentFile.setUpdatedAt(now);
            }
            else {
                documentFile.setObjectName(objectName);
                documentFile.setUploadStatus(UploadStatus.UPLOADED.name());
                now = LocalDateTime.now();
                documentFile.setUpdatedAt(now);
            }
            boolean saveFileMetadata = documentMetadataService.saveFileMetadata(documentFile);

            if (!saveFileMetadata){
                // 调用minio中删除方法,将数据库中setDeleted(1)
                try {
                    documentStorageService.deleteMinio(bucket, objectName);
                    documentMetadataService.updateDeleteStatus(id,1);
                } catch (Exception e) {
                    throw new BusinessException(ErrorCode.MINIO_ERROR, "删除 MinIO 文件失败", e);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return documentMetadataService.queryByID(id);

    }
}
