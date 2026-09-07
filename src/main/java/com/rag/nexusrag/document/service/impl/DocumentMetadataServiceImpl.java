package com.rag.nexusrag.document.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.rag.nexusrag.common.enums.ErrorCode;
import com.rag.nexusrag.common.exception.BusinessException;
import com.rag.nexusrag.common.response.CursorPageResponse;
import com.rag.nexusrag.common.response.PageResponse;
import com.rag.nexusrag.document.dto.DocumentInfo;
import com.rag.nexusrag.document.entity.DocumentFile;
import com.rag.nexusrag.document.mapper.DocumentFileMapper;
import com.rag.nexusrag.document.service.DocumentMetadataService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentMetadataServiceImpl extends ServiceImpl<DocumentFileMapper, DocumentFile>
        implements DocumentMetadataService {

    private static final long DEFAULT_PAGE_NO = 1L;
    private static final long DEFAULT_PAGE_SIZE = 20L;
    private static final long MAX_PAGE_SIZE = 100L;

    public boolean isFileHashDuplicate(String fileHash){
        return lambdaQuery()
                .eq(DocumentFile::getFileHash, fileHash)
                .exists();

    }


    /*
    * 文档上传到 MinIO 之后将元数据写入MySQL
    * */
    @Override
    public boolean saveFileMetadata(DocumentFile documentFile){
        return save(documentFile);
    }

    /*
    *  上传MinIO失败将数据库中deleted改为1
    * */
    @Override
    public void updateDeleteStatus(Long id,int x){
        LocalDateTime now = LocalDateTime.now();
        boolean ok = update(null, Wrappers.lambdaUpdate(DocumentFile.class)
                .eq(DocumentFile::getId, id)
                .set(DocumentFile::getUpdatedAt, now)
                .set(DocumentFile::getDeleted, x));
         if (ok){
             log.warn("数据库deleted更新成功");
         } else {
             log.error("数据库deleted更新失败");
             throw new BusinessException(ErrorCode.DB_ERROR);
         }
    }

    @Override
    public DocumentFile queryByID(Long id){
        DocumentFile documentFile = getById(id);

        if (documentFile != null){
            log.warn("数据库中查到数据:" + documentFile);
            return documentFile;
        } else {
            log.warn("数据库中查询为空:" + documentFile);
            return documentFile = null;
        }

    }

    @Override
    public PageResponse<DocumentInfo> pageDocuments(long pageNo, long pageSize) {
        long safePageNo = pageNo < 1 ? DEFAULT_PAGE_NO : pageNo;
        long safePageSize = pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        Page<DocumentFile> page = lambdaQuery()
                .orderByDesc(DocumentFile::getCreatedAt)
                .orderByDesc(DocumentFile::getId)
                .page(Page.of(safePageNo, safePageSize));

        List<DocumentInfo> records = page.getRecords().stream()
                .map(this::toDocumentInfo)
                .toList();

        return new PageResponse<>(
                records,
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.hasNext()
        );
    }

    @Override
    public CursorPageResponse<DocumentInfo> scrollDocuments(
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            long pageSize
    ) {
        long safePageSize = pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        List<DocumentFile> documentFiles = lambdaQuery()
                .and(cursorCreatedAt != null && cursorId != null, wrapper -> wrapper
                        .lt(DocumentFile::getCreatedAt, cursorCreatedAt)
                        .or()
                        .eq(DocumentFile::getCreatedAt, cursorCreatedAt)
                        .lt(DocumentFile::getId, cursorId))
                .orderByDesc(DocumentFile::getCreatedAt)
                .orderByDesc(DocumentFile::getId)
                .last("LIMIT " + (safePageSize + 1))
                .list();

        boolean hasNext = documentFiles.size() > safePageSize;
        List<DocumentFile> currentPage = hasNext
                ? documentFiles.subList(0, (int) safePageSize)
                : documentFiles;
        List<DocumentInfo> records = currentPage.stream()
                .map(this::toDocumentInfo)
                .toList();

        LocalDateTime nextCursorCreatedAt = null;
        Long nextCursorId = null;
        if (hasNext && !currentPage.isEmpty()) {
            DocumentFile lastDocument = currentPage.get(currentPage.size() - 1);
            nextCursorCreatedAt = lastDocument.getCreatedAt();
            nextCursorId = lastDocument.getId();
        }

        return new CursorPageResponse<>(
                records,
                safePageSize,
                hasNext,
                nextCursorCreatedAt,
                nextCursorId
        );
    }

    private DocumentInfo toDocumentInfo(DocumentFile documentFile) {
        DocumentInfo dto = new DocumentInfo();
        BeanUtils.copyProperties(documentFile, dto);
        return dto;
    }

}
