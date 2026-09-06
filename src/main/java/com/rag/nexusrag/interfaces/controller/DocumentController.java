package com.rag.nexusrag.interfaces.controller;

import com.rag.nexusrag.document.service.DocumentStorageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentStorageService documentStorageService;

    public DocumentController(DocumentStorageService documentStorageService) {
        this.documentStorageService = documentStorageService;
    }

    @PostMapping("/upload")
    public String upload(@RequestPart("file") MultipartFile file){
        return documentStorageService.upload(file);
    }

}
