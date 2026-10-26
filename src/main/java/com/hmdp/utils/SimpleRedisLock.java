package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {
    private String name;
    private StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PRFIX = "lock:";
    private static final String ID_PRFIX = UUID.randomUUID().toString() + "-";

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeOutSec) {
        //获取线程提示
        String threadId = ID_PRFIX + Thread.currentThread().getId();
        //获取锁
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PRFIX + name, threadId + " ", timeOutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unLock() {
        //获取线程提示
        String threadId = ID_PRFIX + Thread.currentThread().getId();
        //得到线程id对比
        String id = stringRedisTemplate.opsForValue().get(KEY_PRFIX + name);
        if (threadId.equals(id)) {
            //释放锁
            stringRedisTemplate.delete(KEY_PRFIX + name);
        }

    }
}
