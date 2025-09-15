package com.pnu.momeet.domain.common.dto.response;

import lombok.Getter;

import java.util.List;
import java.util.function.Function;

@Getter
public class CursorInfo<T> {
    private final List<T> content;
    private final int size;
    private final String nextId;
    private final boolean hasNext;

    public CursorInfo(List<T> content, Object nextId) {
        this.content = content;
        this.size = content.size();
        if (nextId == null) {
            this.nextId = null;
            this.hasNext = false;
        } else {
            this.nextId = String.valueOf(nextId);
            this.hasNext = true;
        }
    }
    public CursorInfo(List<T> content) {
        this(content, null);
    }

    public static <T, R> CursorInfo<R> convert(CursorInfo<T> source, Function<T, R> converter) {
        List<R> convertedContent = source.getContent().stream()
                .map(converter)
                .toList();
        return new CursorInfo<>(convertedContent, source.getNextId());
    }

}
