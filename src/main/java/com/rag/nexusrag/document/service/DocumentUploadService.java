package com.rag.nexusrag.document.service;

import com.rag.nexusrag.common.config.MinioProperties;
import com.rag.nexusrag.common.enums.ErrorCode;
import com.rag.nexusrag.common.exception.BusinessException;
import com.rag.nexusrag.common.utils.FileNameUtils;
import com.rag.nexusrag.common.utils.IdGenerator;
import com.rag.nexusrag.document.dto.DocumentUploadResult;
import com.rag.nexusrag.document.dto.StoredObjectInfo;
import com.rag.nexusrag.document.entity.DocumentFile;
import com.rag.nexusrag.document.enums.ParseStatus;
import com.rag.nexusrag.document.enums.StorageProvider;
import com.rag.nexusrag.document.enums.UploadStatus;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
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
        documentFile.setStorageProvider(StorageProvider.MINIO.name());
        documentFile.setUploadStatus(UploadStatus.UPLOADING.name());
        documentFile.setParseStatus(ParseStatus.PENDING.name());

        LocalDateTime now = LocalDateTime.now();

        documentFile.setCreatedAt(now);
        documentFile.setUpdatedAt(now);
        documentFile.setDeleted(0);
        boolean objectNeedsCleanup = false;
        try {
            // 存入MinIO
            StoredObjectInfo objectInfo = documentStorageService.uploadMinio(file, bucket, originalFilename, documentId);

            if (objectInfo == null){
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "MinIO 未返回对象信息");
            }

            objectName = objectInfo.getObjectName();
            objectNeedsCleanup = true;

            //文件hash值
            String fileHash = objectInfo.getFileHash();

            //根据文件的hash值查重
            if (documentMetadataService.isFileHashDuplicate(fileHash)) {
                try {
                    documentStorageService.deleteMinio(bucket, objectName);
                } catch (BusinessException e) {
                    throw new BusinessException(ErrorCode.MINIO_ERROR, "文件重复，但临时文件删除失败", e);
                }
                objectNeedsCleanup = false;
                throw new BusinessException(ErrorCode.FILE_DUPLICATE, ErrorCode.FILE_DUPLICATE.message() + "：" + originalFilename);
            }

            documentFile.setFileHash(fileHash);
            documentFile.setObjectName(objectName);
            documentFile.setUploadStatus(UploadStatus.UPLOADED.name());
            now = LocalDateTime.now();
            documentFile.setUpdatedAt(now);


            // 存入数据库
            boolean saveFileMetadata;
            try {
                saveFileMetadata = documentMetadataService.saveFileMetadata(documentFile);
            } catch (DataIntegrityViolationException e) {
                // 预检查和插入之间可能有并发请求，唯一索引是最终裁决者。
                if (documentMetadataService.isFileHashDuplicate(fileHash)) {
                    throw new BusinessException(
                            ErrorCode.FILE_DUPLICATE,
                            ErrorCode.FILE_DUPLICATE.message() + "：" + originalFilename,
                            e
                    );
                }
                throw e;
            }

            if (!saveFileMetadata) {
                try {
                    documentStorageService.deleteMinio(bucket, objectName);
                    objectNeedsCleanup = false;
                } catch (BusinessException deleteException) {
                    throw new BusinessException(ErrorCode.DOCUMENT_UPLOAD_FAILED, "文档元数据保存失败，且 MinIO 文件补偿删除失败", deleteException);
                }
                throw new BusinessException(ErrorCode.DOCUMENT_UPLOAD_FAILED, "文档元数据保存失败");
            }
            objectNeedsCleanup = false;

        } catch (BusinessException e) {
            cleanupUploadedObjectIfNecessary(objectNeedsCleanup, bucket, objectName);
            throw e;
        } catch (Exception e) {
            cleanupUploadedObjectIfNecessary(objectNeedsCleanup, bucket, objectName);
            throw new BusinessException(ErrorCode.DOCUMENT_UPLOAD_FAILED, "文档上传失败", e);
        }

        DocumentFile result = documentMetadataService.queryByID(id);
        DocumentUploadResult documentUploadResult = new DocumentUploadResult();
        BeanUtils.copyProperties(result, documentUploadResult);

        return documentUploadResult;

    }

    public void deleteDocument(Long id){
        // 先在数据库中查询数据是否存在
        DocumentFile documentFile = documentMetadataService.queryByID(id);
        if (documentFile == null){
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // 先删除 MinIO 对象，成功后再释放 file_hash 的唯一占用。
        try {
            documentStorageService.deleteMinio(documentFile.getBucketName(), documentFile.getObjectName());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.MINIO_ERROR);
        }

        try {
            documentMetadataService.updateDeleteStatus(id, 1, null);
        } catch (BusinessException e) {
            // 此时对象已删除，无法依赖数据库事务恢复对象；应由后续补偿任务处理该记录。
            throw new BusinessException(
                    ErrorCode.DB_ERROR,
                    "MinIO 文件已删除，但数据库删除状态更新失败，请执行补偿处理",
                    e
            );
        }
    }

    private void cleanupUploadedObjectIfNecessary(
            boolean objectNeedsCleanup,
            String bucket,
            String objectName
    ) {
        if (!objectNeedsCleanup || !StringUtils.hasText(objectName)) {
            return;
        }
        try {
            documentStorageService.deleteMinio(bucket, objectName);
        } catch (BusinessException cleanupException) {
            throw new BusinessException(
                    ErrorCode.MINIO_ERROR,
                    "文档处理失败，且 MinIO 临时文件清理失败",
                    cleanupException
            );
        }
    }

}
