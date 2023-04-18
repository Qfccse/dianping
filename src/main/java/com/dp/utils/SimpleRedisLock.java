package com.dp.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{
    private String name;
    private final StringRedisTemplate stringRedisTemplate;
    private static final String LOCK_PREFIX = "lock";

    private static final String THREAD_PREFIX = UUID.fastUUID().toString(true) + "-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(String name,StringRedisTemplate srt){
        this.name = name;
        this.stringRedisTemplate = srt;
    }

    @Override
    public boolean tryLock(Long timeSec) {
        String tid = THREAD_PREFIX + Thread.currentThread().getId();
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(LOCK_PREFIX + name, tid, timeSec, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unLock() {
        //使用lua实现释放锁的原子性
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(LOCK_PREFIX + name),
                THREAD_PREFIX + Thread.currentThread().getId()
        );
    }
    //@Override
    //public void unLock() {
    //    String tid = THREAD_PREFIX + Thread.currentThread().getId();
    //    String id = stringRedisTemplate.opsForValue().get(LOCK_PREFIX + name);
    //    if(tid.equals(id))
    //    {
    //        stringRedisTemplate.delete(LOCK_PREFIX + name);
    //    }
    //}
}
