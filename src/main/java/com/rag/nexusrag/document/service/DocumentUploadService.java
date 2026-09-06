package com.rag.nexusrag.document.service;

import com.rag.nexusrag.common.config.MinioProperties;
import com.rag.nexusrag.common.enums.ErrorCode;
import com.rag.nexusrag.common.exception.BusinessException;
import com.rag.nexusrag.common.utils.FileNameUtils;
import com.rag.nexusrag.common.utils.IdGenerator;
import com.rag.nexusrag.document.dto.DocumentUploadResult;
import com.rag.nexusrag.document.entity.DocumentFile;
import com.rag.nexusrag.document.enums.ParseStatus;
import com.rag.nexusrag.document.enums.StorageProvider;
import com.rag.nexusrag.document.enums.UploadStatus;
import jakarta.annotation.Resource;
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

        // 文件合格性校验
        if (file == null || file.isEmpty()){
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }

        String originalFilename = file.getOriginalFilename();

        if (!StringUtils.hasText(originalFilename)){
            throw new BusinessException(ErrorCode.FILE_NAME_EMPTY);
        }

        FileNameUtils.validateOriginalFilename(originalFilename);

        // 存入MinIO前置工作
        String bucket = minioProperties.bucket();
        originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        Long id = IdGenerator.Id();
        String documentId = "DOC" + id;
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
        documentFile.setParseStatus(ParseStatus.PENDING.name());

        LocalDateTime now = LocalDateTime.now();

        documentFile.setCreatedAt(now);
        documentFile.setUpdatedAt(now);
        documentFile.setDeleted(0);
        try {
            // 存入MinIO
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

            // 存入数据库
            boolean saveFileMetadata = documentMetadataService.saveFileMetadata(documentFile);

            if (!saveFileMetadata) {
                try {
                    documentStorageService.deleteMinio(bucket, objectName);
                } catch (BusinessException deleteException) {
                    throw new BusinessException(ErrorCode.DOCUMENT_UPLOAD_FAILED, "文档元数据保存失败，且 MinIO 文件补偿删除失败", deleteException);
                }
                throw new BusinessException(ErrorCode.DOCUMENT_UPLOAD_FAILED, "文档元数据保存失败");
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DOCUMENT_UPLOAD_FAILED, "文档上传失败", e);
        }

        return documentMetadataService.queryByID(id);

    }
}
