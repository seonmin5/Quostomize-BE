package com.quostomize.quostomize_be.common.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class IdempotencyRedisRepository {

    private static final String KEY_PREFIX = "$Idempotency ";
    private static final int RESPONSE_EXPIRATION = 30;

    @Qualifier("objectRedisTemplate")
    private final RedisTemplate<String, Object> redisTemplate;


    public boolean hasSucceedResult(String idempotencyKey) {
        String key = KEY_PREFIX + idempotencyKey;
        log.info("[key] hasSucceedResult: {}", key);
        IdempotencyResponse response = (IdempotencyResponse) redisTemplate.opsForValue().get(key);
        log.info("[response] hasSucceedResult: {}", response);
        if (response == null) {
            return false;
        }
        return response.isSucceed();
    }

    public void saveStatusProcessing(String idempotencyKey) {
        log.info("[saveStatusProcessing] idempotencyKey: {}", idempotencyKey);
        String key = KEY_PREFIX + idempotencyKey;
        IdempotencyResponse response = IdempotencyResponse.builder()
                .processStatus(ProcessStatus.PROCESSING)
                .build();

        redisTemplate.opsForValue().set(
                key,
                response,
                RESPONSE_EXPIRATION,
                TimeUnit.MINUTES
        );
    }

    public boolean isProcessing(String idempotencyKey) {
        String key = KEY_PREFIX + idempotencyKey;
        IdempotencyResponse response = (IdempotencyResponse) redisTemplate.opsForValue().get(key);
        log.info("[처리 중]");
        if (response == null) {
            return false;
        }
        return response.isProcessing();
    }

    public Object getSucceedResponse(String idempotencyKey) {
        String key = KEY_PREFIX + idempotencyKey;
        IdempotencyResponse savedData = (IdempotencyResponse) redisTemplate.opsForValue().get(key);
        return savedData.getResult();
    }

    public void saveSucceedResult(String idempotencyKey, Object result) {
        final String key = KEY_PREFIX + idempotencyKey;

        IdempotencyResponse response = IdempotencyResponse.builder()
                .processStatus(ProcessStatus.SUCCESS)
                .result(result)
                .build();

        redisTemplate.opsForValue().set(
                key,
                response,
                RESPONSE_EXPIRATION,
                TimeUnit.MINUTES
        );
    }

    public void saveFailedStatus(String idempotencyKey) {
        String key = KEY_PREFIX + idempotencyKey;

        IdempotencyResponse failedResponse = IdempotencyResponse.builder()
                .processStatus(ProcessStatus.FAILED)
                .build();

        redisTemplate.opsForValue().set(
                key,
                failedResponse,
                RESPONSE_EXPIRATION,
                TimeUnit.MINUTES
        );
    }


    public boolean canRetry(String idempotencyKey) {
        String key = KEY_PREFIX + idempotencyKey;
        IdempotencyResponse response = (IdempotencyResponse) redisTemplate.opsForValue().get(key);
        log.info("[실패 상태]");
        if (response == null) {
            return false;
        }

        return response.isFailed();
    }

}
