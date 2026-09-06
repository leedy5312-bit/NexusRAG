package com.rag.nexusrag.common.utils;

import com.rag.nexusrag.common.enums.ErrorCode;
import com.rag.nexusrag.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.regex.Pattern;

public final class FileNameUtils {

    private FileNameUtils(){
    }

    private static final Pattern SAFE_FILENAME_PATTERN =
            Pattern.compile("^[\\u4e00-\\u9fa5a-zA-Z0-9._()\\- ]+$");

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "txt", "md"
    );

    public static void validateAndCleanOriginalFilename(String fileName){

        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")){
            throw new BusinessException(ErrorCode.FILE_NAME_INVALID);
        }
        if (fileName.length() > 255){
            throw new BusinessException(ErrorCode.FILE_NAME_TOO_LONG);
        }
        if (!SAFE_FILENAME_PATTERN.matcher(fileName).matches()){
            throw new BusinessException(ErrorCode.FILE_NAME_INVALID);
        }

        String extension = StringUtils.getFilenameExtension(fileName);

        if (!StringUtils.hasText(extension) || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())){
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }

    }

}
