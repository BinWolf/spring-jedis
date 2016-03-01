package com.wolf.redis.common.spring;

import java.util.List;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Repository;
import com.alibaba.fastjson.JSON;

/**
 * 使用Spring对RedisTemplate的封装
  * @param <K>
  * @param <V>
 */

@Repository
public class BaseRedisTemplate<K, V> {
	
//	@Autowired
	protected RedisTemplate<K, V> redisTemplate;
	
	static Logger logger = Logger.getLogger(BaseRedisTemplate.class);
	
	/**
	 * 
	  * setObject(存放对象到redis)
	  * @Title: setObject
	  * @param @param key 键
	  * @param @param entity 值     
	  * @return void    
	  * @throws
	 */
	public void setObject(final String key,final V entity) {  
		final String value = JSON.toJSONString(entity);
	    redisTemplate.execute(new RedisCallback<Object>() {  
	        @Override
	        public Object doInRedis(RedisConnection connection)throws DataAccessException {
	            connection.set(redisTemplate.getStringSerializer().serialize(key),redisTemplate.getStringSerializer().serialize(value));  
	            return null;  
	        }  
	    });  
	}  
	
	/**
	 * 
	  * getObject(根据key获取redis中存储的对象)
	  * @Title: getObject
	  * @param key 键
	  * @param clazz 要返回的对象对应的类
	  * @return V 要返回的对象   
	  * @throws
	 */
	public V getObject(final String key,final Class<V> clazz) {  
	    return redisTemplate.execute(new RedisCallback<V>() {  
	        @Override  
	        public V doInRedis(RedisConnection connection)throws DataAccessException {  
	            byte[] key_byte = redisTemplate.getStringSerializer().serialize(key);  
	            if (connection.exists(key_byte)) {  
	                byte[] value = connection.get(key_byte);  
	                String json = redisTemplate.getStringSerializer().deserialize(value);  
	                V entity = JSON.parseObject(json,clazz);
	                return entity;  
	            }  
	            return null;  
	        }  
	    });  
	}  
	
	/** 
     * 压栈 
     *  
     * @param key 
     * @param value 
     * @return 
     */  
    public Long push(K key, V value) {  
        return redisTemplate.opsForList().leftPush(key, value);  
    }  
  
    /** 
     * 出栈 
     *  
     * @param key 
     * @return 
     */  
    public V pop(K key) {  
        return redisTemplate.opsForList().leftPop(key);  
    }  
  
    /** 
     * 入队 
     *  
     * @param key 
     * @param value 
     * @return 
     */  
    public Long in(K key, V value) {  
        return redisTemplate.opsForList().rightPush(key, value);  
    }  
  
    /** 
     * 出队 
     *  
     * @param key 
     * @return 
     */  
    public V out(K key) {  
        return redisTemplate.opsForList().leftPop(key);  
    }  
  
    /** 
     * 栈/队列长 
     *  
     * @param key 
     * @return 
     */  
    public Long length(K key) {  
        return redisTemplate.opsForList().size(key);  
    }  
  
    /** 
     * 范围检索 
     *  
     * @param key 
     * @param start 
     * @param end 
     * @return 
     */  
    public List<V> range(K key, int start, int end) {  
        return redisTemplate.opsForList().range(key, start, end);  
    }  
  
    /** 
     * 移除 
     *  
     * @param key 
     * @param i 
     * @param value 
     */  
    public void remove(K key, long i, V value) {  
        redisTemplate.opsForList().remove(key, i, value);  
    }  
  
    /** 
     * 检索 
     *  
     * @param key 
     * @param index 
     * @return 
     */  
    public V index(K key, long index) {  
        return redisTemplate.opsForList().index(key, index);  
    }  
  
    /** 
     * 置值 
     *  
     * @param key 
     * @param index 
     * @param value 
     */  
    public void set(K key, long index, V value) {  
        redisTemplate.opsForList().set(key, index, value);  
    }  
  
    /** 
     * 裁剪 
     *  
     * @param key 
     * @param start 
     * @param end 
     */  
    public void trim(K key, long start, int end) {  
        redisTemplate.opsForList().trim(key, start, end);  
    }  
	/**
	 * 获取 RedisSerializer
	 * 
	 */
	protected RedisSerializer<String> getRedisSerializer() {
		return redisTemplate.getStringSerializer();
	}
	
	
}