CREATE TABLE document_file (
                               id BIGINT NOT NULL COMMENT '主键ID',

                               document_id VARCHAR(64) NOT NULL COMMENT '文档业务ID，对外使用',
                               original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
                               bucket_name VARCHAR(128) NOT NULL COMMENT 'MinIO bucket名称',
                               object_name VARCHAR(512) NOT NULL COMMENT 'MinIO对象名称',
                               content_type VARCHAR(128) DEFAULT NULL COMMENT '文件Content-Type',
                               file_extension VARCHAR(32) DEFAULT NULL COMMENT '文件扩展名',
                               file_size BIGINT NOT NULL COMMENT '文件大小，单位字节',
                               file_hash VARCHAR(128) DEFAULT NULL COMMENT '文件哈希值，后续可用于去重',

                               storage_provider VARCHAR(32) NOT NULL DEFAULT 'MINIO' COMMENT '存储提供方',
                               upload_status VARCHAR(32) NOT NULL DEFAULT 'UPLOADED' COMMENT '上传状态：UPLOADING/UPLOADED/UPLOAD_FAILED',
                               parse_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '解析状态：PENDING/PARSING/PARSED/PARSE_FAILED',

                               error_message VARCHAR(1024) DEFAULT NULL COMMENT '失败原因',
                               remark VARCHAR(512) DEFAULT NULL COMMENT '备注',

                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',

                               PRIMARY KEY (id),
                               UNIQUE KEY uk_document_id (document_id),
                               UNIQUE KEY uk_bucket_object (bucket_name, object_name),
                               KEY idx_file_hash (file_hash),
                               KEY idx_upload_status (upload_status),
                               KEY idx_parse_status (parse_status),
                               KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档文件元数据表';