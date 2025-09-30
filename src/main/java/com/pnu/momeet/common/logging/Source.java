package com.pnu.momeet.common.logging;

public record Source(String name) {
    public static <S> Source of(Class<S> clazz) {
        return new Source(clazz.getSimpleName());
    }
    public static Source of(Object instance) {
        return of(instance.getClass());
    }
}
