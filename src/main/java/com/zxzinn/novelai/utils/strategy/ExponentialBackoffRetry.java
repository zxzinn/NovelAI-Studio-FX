package com.zxzinn.novelai.utils.strategy;

import lombok.extern.log4j.Log4j2;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Log4j2
public class ExponentialBackoffRetry implements RetryStrategy {
    private final int maxRetries;
    private final long initialDelayMs;

    public ExponentialBackoffRetry(int maxRetries, long initialDelayMs) {
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
    }

    @Override
    public <T> Optional<T> execute(ThrowingSupplier<T> supplier) {
        for (int retry = 0; retry < maxRetries; retry++) {
            try {
                return Optional.of(supplier.get());
            } catch (Exception e) {
                if (retry == maxRetries - 1) {
                    log.error("操作失敗，已達到最大重試次數", e);
                    return Optional.empty();
                }
                long delayMs = initialDelayMs * (long) Math.pow(2, retry);
                log.warn("操作失敗，將在{}毫秒後重試. 錯誤: {}", delayMs, e.getMessage());
                try {
                    TimeUnit.MILLISECONDS.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }
}