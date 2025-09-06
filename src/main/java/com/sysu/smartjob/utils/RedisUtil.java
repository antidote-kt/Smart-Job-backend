package com.sysu.smartjob.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis操作工具类 - 纯工具方法
 */
@Slf4j
@Component
public class RedisUtil {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 设置缓存
     * @param key 键
     * @param value 值
     * @param timeout 超时时间
     * @param unit 时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("Redis缓存设置成功，key: {}", key);
        } catch (Exception e) {
            log.error("Redis缓存设置失败，key: {}", key, e);
        }
    }
    
    /**
     * 设置缓存（默认分钟）
     * @param key 键
     * @param value 值
     * @param timeout 超时时间（分钟）
     */
    public void set(String key, Object value, long timeout) {
        set(key, value, timeout, TimeUnit.MINUTES);
    }
    
    /**
     * 获取缓存
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis缓存获取失败，key: {}", key, e);
            return null;
        }
    }
    
    /**
     * 删除缓存
     * @param key 键
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Redis缓存删除，key: {}", key);
        } catch (Exception e) {
            log.error("Redis缓存删除失败，key: {}", key, e);
        }
    }
    
    /**
     * 根据pattern查找key
     * @param pattern 模式
     * @return key集合
     */
    public Set<String> keys(String pattern) {
        try {
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            log.error("Redis查找key失败，pattern: {}", pattern, e);
            return new HashSet<>();
        }
    }
    
    /**
     * 批量获取缓存
     * @param keys key集合
     * @return 值列表
     */
    public List<Object> multiGet(Collection<String> keys) {
        try {
            if (keys == null || keys.isEmpty()) {
                return new ArrayList<>();
            }
            return redisTemplate.opsForValue().multiGet(keys);
        } catch (Exception e) {
            log.error("Redis批量获取缓存失败，keys: {}", keys, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 批量删除缓存
     * @param keys key集合
     */
    public void delete(Collection<String> keys) {
        try {
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Redis批量删除缓存，数量: {}", keys.size());
            }
        } catch (Exception e) {
            log.error("Redis批量删除缓存失败，keys: {}", keys, e);
        }
    }

}