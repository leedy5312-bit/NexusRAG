package com.rag.nexusrag.document.service;

import com.rag.nexusrag.common.config.MinioProperties;
import com.rag.nexusrag.document.dto.DocumentUploadResult;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@Slf4j
@Service
public class DocumentStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public DocumentStorageService(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    public DocumentUploadResult upload(MultipartFile file){
        if (file == null || file.isEmpty()){
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String bucket = minioProperties.bucket();
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String uuid = UUID.randomUUID().toString().replace("-","");
        String objectName = "documents/" + uuid + "-" + originalFilename;

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

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1L)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("文档已成功上传到 MinIO 文件名：{}",objectName);
            return new DocumentUploadResult(
                    bucket,
                    objectName,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize()
            );
        } catch (Exception e) {
            throw new RuntimeException("上传文档到 MinIO 失败", e);
        }
    }
}
