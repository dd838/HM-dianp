package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class RedisWorker {
    private static final Long BEGIN_TIMESTAMP = 1767225600L;

    private static final long COUNT_BITS = 32;
    private StringRedisTemplate stringRedisTemplate;

    public Long nextId(String keyPrefix) {
        //1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowhSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = nowhSecond - BEGIN_TIMESTAMP;
        //2.生成序列号
        //2.1.获取当前日期
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:mm:dd"));
        Long count = stringRedisTemplate.opsForValue().increment("icr" + timeStamp + ":" + date);
        //3.拼接返回
        return timeStamp << COUNT_BITS | count;
    }
}

