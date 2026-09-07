package com.rag.nexusrag.document.service;

import com.rag.nexusrag.common.enums.ErrorCode;
import com.rag.nexusrag.common.exception.BusinessException;
import com.rag.nexusrag.document.dto.StoredObjectInfo;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDate;

@Slf4j
@Service
public class DocumentStorageService {

    private final MinioClient minioClient;

    public DocumentStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public StoredObjectInfo uploadMinio(MultipartFile file, String bucket, String originalFilename, String documentId) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String datePath = LocalDate.now().toString().replace("-", "/");

        String objectName = "documents/" + datePath + "/" + documentId + "." + extension;

        try {
            ensureBucketExists(bucket);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream inputStream = file.getInputStream();
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {

                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectName)
                                .stream(digestInputStream, file.getSize(), -1L)
                                .contentType(resolveContentType(file))
                                .build()
                );
            }

            String fileHash = toHex(digest.digest());

            log.info("Document uploaded to MinIO, bucket={}, objectName={}, size={}, sha256={}",
                    bucket, objectName, file.getSize(), fileHash);

            return new StoredObjectInfo(bucket, objectName, fileHash);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "上传文档到 MinIO 失败", e);
        }
    }

    private void ensureBucketExists(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucket)
                            .build()
            );

            if (!exists) {
                log.info("MinIO bucket does not exist, creating bucket={}", bucket);
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucket)
                                .build()
                );
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.MINIO_ERROR, "检查或创建 MinIO bucket 失败", e);
        }
    }

    private String resolveContentType(MultipartFile file) {
        if (StringUtils.hasText(file.getContentType())) {
            return file.getContentType();
        }
        return "application/octet-stream";
    }

    private static String toHex(byte[] bytes) {
        char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];

        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = hexArray[v >>> 4];
            hexChars[i * 2 + 1] = hexArray[v & 0x0F];
        }

        return new String(hexChars);
    }

    public void deleteMinio(String bucket, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            log.info("删除 MinIO 文件成功");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.MINIO_ERROR, "删除 MinIO 文件失败", e);
            //TODO 后期添加消息队列，删除失败的重新删除
        }
    }

}
