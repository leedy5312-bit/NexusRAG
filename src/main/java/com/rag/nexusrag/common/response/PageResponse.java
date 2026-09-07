package com.rag.nexusrag.common.response;

import java.util.List;

public record PageResponse<T>(
        List<T> records,
        long pageNo,
        long pageSize,
        long total,
        boolean hasNext
) {
}
