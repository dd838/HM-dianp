package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    // 缓存重建线程池，就是你代码里的 CACHE_REBNILD_EXECUTOR
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 普通写入，设置TTL（物理过期）
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    // 写入，带逻辑过期时间（Redis不设置TTL）
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 缓存穿透方案：缓存空值
     *
     * @param keyPrefix  key前缀
     * @param id         查询id
     * @param type       返回类型
     * @param dbFallback 查数据库函数
     * @param time       过期时间
     * @param unit       单位
     */
    public <R, ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1.查redis
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.命中且不为空
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }
        // 命中是空字符串（缓存空值）
        if (json != null) {
            return null;
        }
        // 3.查数据库
        R r = dbFallback.apply(id);
        // 数据库不存在，写入空字符串
        if (r == null) {
            stringRedisTemplate.opsForValue().set(key, "", 2L, TimeUnit.MINUTES);
            return null;
        }
        // 4.写入缓存
        this.set(key, r, time, unit);
        return r;
    }

    /**
     * 缓存击穿方案：互斥锁
     */
    public <R, ID> R queryWithMutex(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1.查缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }
        if (json != null) {
            return null;
        }
        // 2.获取锁
        String lockKey = "lock:" + keyPrefix + id;
        R r = null;
        try {
            boolean isLock = tryLock(lockKey);
            if (!isLock) {
                // 没拿到锁，休眠重试
                Thread.sleep(50);
                return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit);
            }
            // 拿到锁，查库
            r = dbFallback.apply(id);
            if (r == null) {
                stringRedisTemplate.opsForValue().set(key, "", 2L, TimeUnit.MINUTES);
                return null;
            }
            this.set(key, r, time, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unLock(lockKey);
        }
        return r;
    }

    /**
     * 缓存击穿方案：逻辑过期（就是你刚才写的异步重建）
     */
    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        // 没命中，直接返回null（不会去查库）
        if (StrUtil.isBlank(json)) {
            return null;
        }
        // 命中，转RedisData
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 没过期，直接返回
        if (expireTime.isAfter(LocalDateTime.now())) {
            return r;
        }
        // 已经过期，尝试获取锁，开启异步线程重建
        String lockKey = "lock:" + keyPrefix + id;
        boolean isLock = tryLock(lockKey);
        if (isLock) {
            // 提交任务到线程池
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库，写入逻辑过期缓存
                    R newR = dbFallback.apply(id);
                    this.setWithLogicalExpire(key, newR, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(lockKey);
                }
            });
        }
        // 不管拿没拿到锁，直接返回旧数据
        return r;
    }

    // 获取锁
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10L, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    // 释放锁
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }
}
