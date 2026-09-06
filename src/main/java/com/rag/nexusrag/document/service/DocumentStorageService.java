package com.rag.nexusrag.document.service;

import com.rag.nexusrag.common.config.MinioProperties;
import com.rag.nexusrag.common.enums.ErrorCode;
import com.rag.nexusrag.common.exception.BusinessException;
import com.rag.nexusrag.common.utils.FileNameUtils;
import com.rag.nexusrag.common.utils.IdGenerator;
import com.rag.nexusrag.document.dto.DocumentUploadResult;
import com.rag.nexusrag.document.entity.DocumentFile;
import com.rag.nexusrag.document.enums.StorageProvider;
import com.rag.nexusrag.document.enums.UploadStatus;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
public class DocumentStorageService {

    private final MinioClient minioClient;

    public DocumentStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public String uploadMinio(MultipartFile file, String bucket, String originalFilename){

        String objectName = null;
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucket)
                            .build()
            );

            if (!exists){
                log.info("MinIO中bucket不存在...正在创建bucket");
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucket)
                                .build()
                );
            }
            String uuid = UUID.randomUUID().toString().replace("-","");

            try (InputStream inputStream = file.getInputStream()) {
                objectName = "documents/" + LocalDate.now() + "/" + uuid + "-" + originalFilename;
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1L)
                                .contentType(file.getContentType())
                                .build()
                );
            }


            log.info("文档已成功上传到 MinIO 文件名：{}",objectName);

            return objectName;

        } catch (Exception e) {
            log.error("上传文档到 MinIO 失败", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "上传文档到 MinIO 失败", e);
        }
    }

    public void deleteMinio(String bucket, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.MINIO_ERROR, "删除 MinIO 文件失败", e);
        }
    }

}
