package com.zxzinn.novelai.utils.strategy;

import java.util.Optional;

public interface RetryStrategy {
    <T> Optional<T> execute(ThrowingSupplier<T> supplier);

    @FunctionalInterface
    interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}