package com.zxzinn.novelai.utils.strategy;

import lombok.extern.log4j.Log4j2;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Log4j2
public class ExponentialBackoffRetry implements RetryStrategy {
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_RETRY_DELAY_MS = 20000;

    public ExponentialBackoffRetry() {
    }

    @Override
    public <T> Optional<T> execute(ThrowingSupplier<T> supplier) {
        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            try {
                return Optional.of(supplier.get());
            } catch (Exception e) {
                if (retry == MAX_RETRIES - 1) {
                    log.error("操作失敗，已達到最大重試次數", e);
                    return Optional.empty();
                }
                long delayMs = INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, retry);
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