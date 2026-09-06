package com.rag.nexusrag.document.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.rag.nexusrag.document.dto.DocumentUploadResult;
import com.rag.nexusrag.document.entity.DocumentFile;
import com.rag.nexusrag.document.mapper.DocumentFileMapper;
import com.rag.nexusrag.document.service.DocumentMetadataService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class DocumentMetadataServiceImpl extends ServiceImpl<DocumentFileMapper, DocumentFile>
        implements DocumentMetadataService {



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
         boolean ok = update(null, Wrappers.lambdaUpdate(DocumentFile.class)
                .eq(DocumentFile::getId,id)
                .set(DocumentFile::getDeleted,x));
         if (ok){
             log.warn("数据库deleted更新成功");
         } else {
             log.error("数据库deleted更新失败");
         }
    }

    @Override
    public DocumentUploadResult queryByID(Long id){
        DocumentFile documentFile = getById(id);
        DocumentUploadResult dto = new DocumentUploadResult();
        if (documentFile != null){
            log.warn("数据库中查到数据:" + documentFile);
            BeanUtils.copyProperties(documentFile, dto);
        } else {
            log.warn("数据库中查询为空:" + documentFile);
            dto = null;
        }
        return dto;
    }

}
