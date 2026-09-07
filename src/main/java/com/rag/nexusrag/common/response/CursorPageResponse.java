package com.rag.nexusrag.common.response;

import java.time.LocalDateTime;
import java.util.List;

public record CursorPageResponse<T>(
        List<T> records,
        long pageSize,
        boolean hasNext,
        LocalDateTime nextCursorCreatedAt,
        Long nextCursorId
) {
}
