package com.wolf.redis.common.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.wolf.redis.common.Constant;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import com.alibaba.fastjson.JSON;


@Repository
public class RedisClientsUtil {
	
	static Logger logger = Logger.getLogger(RedisClientsUtil.class);
	
//	@Autowired
	private JedisPool jedisPool; //注入redis客户端连接池

    
    /** 
     * 添加单个值 
     * @param key 键
     * @param value 值
     * @return 
     */  
    public boolean set(String key, String value) {  
    	Jedis jedis = null;  
        try {  
            jedis = jedisPool.getResource(); 
            jedis.set(key, value);  
            return true;  
        } catch (Exception ex) {  
            logger.error("set error; key is : "+key, ex);  
            return false; 
        } finally {  
        	//释放资源
        	returnResource(jedis);
        }  
    }  
    
    
    /**
    * 向缓存中设置对象
    * @Title: set
    * @param key 键
    * @param object 值
    * @return boolean    
    * @throws
   */
    public <V> boolean setObject(String key,V object){
    	//将对象转换成json数据格式
    	String value = JSON.toJSONString(object);
    	Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            jedis.set(key, value);  
            return true;  
        } catch (Exception ex) {  
            logger.error("setObject error; key is : "+key+" value object is :"+object, ex);  
            return false; 
        } finally {  
            returnResource(jedis);
        }  
    }
    
    /**
     * 
     * 向缓存中设置Map<K, V>对象
     * @Title: set
     * @param key
     * @param map<Key,Value>  
     * @return boolean    
     * @throws
    */
     public <V> boolean setMapObject(String key,Map<String, V> map){
    	 Jedis jedis = null; 
    	 boolean flag = false;
     	//将对象转换成json数据格式
     	if (map!=null && !map.isEmpty()) {
     		List<String> jsonList = new ArrayList<String>(map.size());
			for (String element_key : map.keySet()) {
				V object = map.get(element_key);
				//*******采用分隔符处理有一定的风险,如果map中的key含有SPLIT_CHAR则会导致拆分出错*******
				String element_content = JSON.toJSONString(object)+ Constant.SPLIT_CHAR+element_key;
    			jsonList.add(element_content);
			}
			//将Set<String>转化成json数组
	    	String[] jsonArray = new String[jsonList.size()];
	    	jsonList.toArray(jsonArray);
			try {  
	             jedis = jedisPool.getResource(); 
	             jedis.sadd(key, jsonArray);  
	             flag = true;  
	         } catch (Exception ex) {  
	             logger.error("setMapObject error; key is : "+key+" value map is :"+map, ex);  
	         } finally {  
	             returnResource(jedis);
	         }  
		}
     	 return flag; 
     }
     
     /**
      * @Title: 根据key获取Set无序集合对象值
      * @param key
      * @param clazz 集合中装载对象所属的类   
      * @return Set<E>    
      * @throws
     */
    public <V> Map<String,V> getMapObject(String key,Class<V> clazz) {  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            //获取json集合数据                                                                         
            Set<String> jsonSet =jedis.smembers(key);
            //转化集合中的字符串为对象格式
            Map<String,V> objectMap = new HashMap<String,V>(jsonSet.size());
            for (String json : jsonSet) {
            	//数据格式： "{\"descrition\":\"worker\",\"id\":\"0011\",\"roleName\":\"worker\"}^001_key"
            	String[] contentArray = json.split("\\"+Constant.SPLIT_CHAR);
				V entity = JSON.parseObject(contentArray[0], clazz);
				objectMap.put(contentArray[1],entity);
			}
            return objectMap;
        } catch (Exception ex) {  
            logger.error("get getObjects error key is :"+key+" class is :"+clazz, ex);    
            return null;  
        } finally {  
            returnResource(jedis);
        }  
    }  
    
    /**
     * 
     * 向缓存中设置List集合对象
     * 从最后一个元素开始添加
     * @Title: set
     * @param key
     * @param list 
     * @return boolean    
     * @throws
    */
     public <E> boolean setListObjects(String key,List<E> list){
     	//将List集合中的对象转换成json数据格式的数组
    	List<String> jsonList = new ArrayList<String>(list.size());
    	if(list!=null && !list.isEmpty()){
    		for (E object : list) {
    			String element_content = JSON.toJSONString(object);
    			jsonList.add(element_content);
			}
    	}
    	
    	//将List<String>转化成json数组
    	String[] jsonArray = new String[jsonList.size()];
    	jsonList.toArray(jsonArray);
     	Jedis jedis = null; 
         try {  
             jedis = jedisPool.getResource(); 
             //添加一个字符串值到List容器的底部（右侧）如果KEY不存在，则创建一个List容器，如果KEY存在并且不是一个List容器，那么返回FLASE
             jedis.rpush(key, jsonArray);  
             return true;  
         } catch (Exception ex) {  
             logger.error("setListObjects error; key is : "+key, ex);  
             return false; 
         } finally {  
             returnResource(jedis);
         }  
     }
     
     /**
      * @Title: 根据key获取List有序集合对象值
      * @param key
      * @param clazz 集合中装载对象所属的类   
      * @return List<E>    
      * @throws
     */
    public <E> List<E> getListObjects(String key,Class<E> clazz) {  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            //规律: 左数从0开始,右数从-1开始(0: the first element, 1: the second ... -1: the last element, -2: the penultimate ...)                                                            
            List<String> jsonList =jedis.lrange(key, 0, -1);
            //转化集合中的字符串为对象格式
            List<E> objectList = new ArrayList<E>(jsonList.size());
            for (String json : jsonList) {
				E objec = JSON.parseObject(json, clazz);
				objectList.add(objec);
			}
            return objectList;
        } catch (Exception ex) {  
            logger.error("get getListObjects error key is :"+key+" class is :"+clazz, ex);    
            return null;  
        } finally {  
            returnResource(jedis);
        }  
    }  
    
    
    /**
     * 向缓存中设置Set集合对象
     * @Title: set
     * @param key 键
     * @param set 值  
     * @return boolean    
     * @throws
    */
     public <E> boolean setSetObjects(String key,Set<E> set){
     	//将Set集合中的对象转换成json数据格式的数组
    	Set<String> jsonSet = new HashSet<String>(set.size());
    	if(set!=null && !set.isEmpty()){
    		for (E object : set) {
    			String element_content = JSON.toJSONString(object);
    			jsonSet.add(element_content);
			}
    	}
    	
    	//将Set<String>转化成json数组
    	String[] jsonArray = new String[jsonSet.size()];
    	jsonSet.toArray(jsonArray);
     	Jedis jedis = null; 
         try {  
             jedis = jedisPool.getResource(); 
             //存放集合数据
             jedis.sadd(key, jsonArray);  
             return true;  
         } catch (Exception ex) {  
             logger.error("setObjects error; key is : "+key, ex);  
             return false; 
         } finally {  
             returnResource(jedis);
         }  
     }
     
     /**
      * @Title: 根据key获取Set无序集合对象值
      * @param key
      * @param clazz 集合中装载对象所属的类   
      * @return Set<E>    
      * @throws
     */
    public <E> Set<E> getSetObjects(String key,Class<E> clazz) {  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            //获取json集合数据                                                                         
            Set<String> jsonSet =jedis.smembers(key);
            //转化集合中的字符串为对象格式
            Set<E> objectSet = new HashSet<E>(jsonSet.size());
            for (String json : jsonSet) {
				E objec = JSON.parseObject(json, clazz);
				objectSet.add(objec);
			}
            return objectSet;
        } catch (Exception ex) {  
            logger.error("get getObjects error key is :"+key+" class is :"+clazz, ex);    
            return null;  
        } finally {  
            returnResource(jedis);
        }  
    }  
    
    /**
     * 根据key获取value值
      * @Title: get
      * @param  key
      * @return String    
      * @throws
     */
    public String get(String key) {  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            return jedis.get(key);  
        } catch (Exception ex) {  
            logger.error("get error.", ex);    
            return null;  
        } finally {  
            returnResource(jedis);
        }  
    }  
    
    /**
     * 通过key获取对象值
      * @Title: getObject
      * @param key(推荐类的全路径+id值)
      * @param clazz 要返回的对象所属的类   
      * @return String    
      * @throws
     */
    public <E> E getObject(String key,Class<E> clazz) {  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            String value = jedis.get(key);  
            return JSON.parseObject(value, clazz);
        } catch (Exception ex) {  
            logger.error("get getObject error key is :"+key+" class is :"+clazz, ex);    
            return null;  
        } finally {  
            returnResource(jedis);
        }  
    }  
    
    
    /** 
     * 添加到Set中 
     * @param key 
     * @param members (值成员)
     * @return 
     */  
    public boolean addSet(String key, String... members) {  
        if(key == null || members == null){  
            return false;  
        }  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            jedis.sadd(key, members);  
            return true;  
        } catch (Exception ex) {  
            logger.error("addSet error; key is :"+key, ex);     
            return false;  
        } finally {  
            returnResource(jedis);
        }  
    }  
    
    /** 
     * 获取Set 
     * @param key 
     * @return Set<String>
     */  
    public Set<String> getSet(String key){  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            return jedis.smembers(key);  
        } catch (Exception ex) {  
            logger.error("getSet error.", ex);  
            return null;  
        } finally {  
            returnResource(jedis);
        }  
    }  
  
    /** 
     * 添加到Set中（同时设置过期时间） 
     * @param key key值 
     * @param expire_time 过期时间 单位(秒:s)
     * @param value 
     * @return 
     */  
    public boolean addSet(String key,int expire_time, String... value) {  
        boolean result = addSet(key, value);  
        if(result){  
            long i = setExpire(key, expire_time);  
            return i==1;  
        }  
        return false;  
    }  
    
    /** 
     * 设置一个key的过期时间（单位：秒） 
     * @param key key值 
     * @param expire_time 多少秒后过期 
     * @return 1：设置了过期时间  0：没有设置过期时间/不能设置过期时间 
     */  
    public long setExpire(String key, int expire_time) {  
        if (key==null || key.equals("")) {  
            return 0;  
        }  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            return jedis.expire(key, expire_time);  
        } catch (Exception ex) {  
            logger.error("expire error[key=" + key + " seconds=" + expire_time + "]" + ex.getMessage(), ex); 
            return 0; 
        } finally {  
        	returnResource(jedis);
        }   
    }  
  
    /** 
     * 设置一个key在某个时间点过期 
     * @param key key值 
     * @param unixTimestamp unix时间戳，从1970-01-01 00:00:00开始到现在的秒数 
     * @return 1：设置了过期时间  0：没有设置过期时间/不能设置过期时间 
     */  
    public long setExpireAtUnixTimestamp(String key, int unixTimestamp) {  
        if (key==null || key.equals("")) {  
            return 0;  
        }  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            return jedis.expireAt(key, unixTimestamp);  
        } catch (Exception ex) {  
            logger.error("expireAt error[key=" + key + " unixTimestamp=" + unixTimestamp + "]" + ex.getMessage(), ex);
            return 0;  
        } finally {  
        	returnResource(jedis);
        }  
    }  
  
    /** 
     * 截断一个List 
     * @param key 列表key 
     * @param start 开始位置 从0开始 
     * @param end 结束位置 
     * @return 状态码 
     */  
    public String trimList(String key, long start, long end) {  
        if (key == null || key.equals("")) {  
            return "-";  
        }  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            return jedis.ltrim(key, start, end);  
        } catch (Exception ex) {  
            logger.error("trimList 出错[key=" + key + " start=" + start + " end=" + end + "]" + ex.getMessage() , ex); 
            return "-"; 
        } finally {  
            returnResource(jedis);
        }   
    }  
    
    /** 
     * 检查Set长度 
     * @param key 
     * @return 
     */  
    public long countSet(String key){  
        if(key == null ){  
            return 0;  
        }  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            return jedis.scard(key);  
        } catch (Exception ex) {  
            logger.error("countSet error: key is :"+key, ex);  
            return 0;  
        } finally {  
            returnResource(jedis);
        }  
    }  
    
    
    /** 
     * @param key 
     * @param value 
     * @return 判断值是否包含在set中 
     */  
    public boolean containsInSet(String key, String value) {  
        if(key == null || value == null){  
            return false;  
        }  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            return jedis.sismember(key, value);  
        } catch (Exception ex) {  
            logger.error("containsInSet error.", ex);  
            return false;  
        } finally {  
            returnResource(jedis);
        }  
    }  
    
   
  
    /** 
     * 从set中删除value 
     * @param key 
     * @return 
     */  
    public  boolean removeSetValue(String key,String... value){  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            jedis.srem(key, value);  
            return true;  
        } catch (Exception ex) {  
            logger.error("removeSetValue error.", ex);   
            return false;  
        } finally {  
            returnResource(jedis);
        }  
    }  
      
    /** 
     * 从list中删除value 默认count 1 
     * @param key 
     * @param values 值list 
     * @return 
     */  
    public int removeListValue(String key,List<String> values){  
        return removeListValue(key, 1, values);  
    }  
    /** 
     * 从list中删除value 
     * @param key 
     * @param count  
     * @param values 值list 
     * @return 
     */  
    public int removeListValue(String key,long count,List<String> values){  
        int result = 0;  
        if(values != null && values.size()>0){  
            for(String value : values){  
                if(removeListValue(key, count, value)){  
                    result++;  
                }  
            }  
        }  
        return result;  
    }  
    
    /** 
     *  从list中删除value 
     * @param key 
     * @param count 要删除个数 
     * @param value 
     * @return 
     */  
    public  boolean removeListValue(String key,long count,String value){  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            jedis.lrem(key, count, value);  
            return true;  
        } catch (Exception ex) {  
            logger.error("removeListValue error.", ex);  
            return false;  
        } finally {  
            returnResource(jedis);
        }  
    }  
      
    /** 
     * 截取List 
     * @param key  
     * @param start 起始位置 
     * @param end 结束位置 
     * @return 
     */  
    public List<String> rangeList(String key, long start, long end) {  
        if (key == null || key.equals("")) {  
            return null;  
        }  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            return jedis.lrange(key, start, end);  
        } catch (Exception ex) {  
            logger.error("rangeList 出错[key=" + key + " start=" + start + " end=" + end + "]" + ex.getMessage() , ex); 
            return null; 
        } finally {  
            returnResource(jedis);
        }  
    }  
      
    /** 
     * 检查List长度 
     * @param key 
     * @return 
     */  
    public long countList(String key){  
        if(key == null ){  
            return 0;  
        }  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            return jedis.llen(key);  
        } catch (Exception ex) {  
            logger.error("countList error.", ex);  
            return 0;
        } finally {  
            returnResource(jedis);
        }   
    }  
      
    /** 
     * 添加到List中（同时设置过期时间） 
     * @param key key值 
     * @param seconds 过期时间 单位s 
     * @param value  
     * @return  
     */  
    public boolean addList(String key,int seconds, String... value){  
        boolean result = addList(key, value);  
        if(result){  
            long i = setExpire(key, seconds);  
            return i==1;  
        }  
        return false;  
    }  
    
    /** 
     * 添加到List 
     * @param key 
     * @param value 
     * @return 
     */  
    public boolean addList(String key, String... value) {  
        if(key == null || value == null){  
            return false;  
        }  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            jedis.lpush(key, value);  
            return true;  
        } catch (Exception ex) {  
            logger.error("addList error.", ex);   
            return false; 
        } finally {  
            returnResource(jedis);
        }    
    }  
    
    /** 
     * 添加到List(只新增) 
     * @param key 
     * @param value 
     * @return 
     */  
    public boolean addList(String key, List<String> list) {  
        if(key == null || list == null || list.size() == 0){  
            return false;  
        }  
        for(String value : list){  
            addList(key, value);  
        }  
        return true;  
    }  
      
    /** 
     * 获取List 
     * @param key 
     * @return 
     */  
    public  List<String> getList(String key){  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            //规律: 左数从0开始,右数从-1开始(0: the first element, 1: the second ... -1: the last element, -2: the penultimate ...)                                                            
            return jedis.lrange(key, 0, -1);  
        } catch (Exception ex) {  
            logger.error("getList error.", ex);    
            return null;  
        } finally {  
            returnResource(jedis);
        }  
    }  
    
    /** 
     * 设置HashSet对象 
     * @param key   键
     * @param field 属性名
     * @param value 属性值
     * @return 
     */  
    public boolean setHashSet(String key, String field, String value) {  
        if (value == null) return false;  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            //hset "001" "name" "李四"
            jedis.hset(key,field,value);  
            return true;  
        } catch (Exception ex) {  
            logger.error("setHashSet error; key is  : "+key, ex);  
            return false;  
        } finally {  
            returnResource(jedis);
        }  
    }  
    
  
    /** 
     * 获得HashSet对象 
     * @param key    键值 
     * @param field  属性
     * @return 返回field属性对应的值
     */  
    public String getHashSet(String key,String field) {  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            return jedis.hget(key,field);  
        } catch (Exception ex) {  
            logger.error("getHashSet error.", ex);     
            return null; 
        } finally {  
            returnResource(jedis);
        }  
    }  
  
    /** 
     * 删除HashSet对象 
     * @param key   键 
     * @param field 属性名 
     * @return 删除field对应的value值 
     */  
    public long delHSet(String key,String field) {  
        Jedis jedis = null; 
        long count = 0;  
        try {  
            jedis = jedisPool.getResource(); 
            count = jedis.hdel(key,field);  
        } catch (Exception ex) {  
            logger.error("delHSet error.", ex);          
        } finally {  
            returnResource(jedis);
        }  
        return count;  
    }  
  
    /** 
     * 删除HashSet对象 
     * @param key   键
     * @param field 属性名
     * @return 删除field(一个到多个)对应的value值 
     */  
    public long delHSets(String key,String... field) {  
        Jedis jedis = null; 
        long count = 0;  
        try {  
            jedis = jedisPool.getResource(); 
            count = jedis.hdel(key,field);  
        } catch (Exception ex) {  
            logger.error("delHSets error.", ex);  
        } finally {  
            returnResource(jedis);
        }  
        return count;  
    }  
  
    /** 
     * 判断key是否存在 
     * @param key   键
     * @param field 属性名
     * @return 
     */  
    public boolean existsHashSet(String key,String field) {  
        Jedis jedis = null; 
        boolean isExist = false;  
        try {  
            jedis = jedisPool.getResource(); 
            isExist = jedis.hexists(key,field);  
        } catch (Exception ex) {  
            logger.error("existsHashSet error.", ex);   
        } finally {  
            returnResource(jedis);
        }  
        return isExist;  
    } 
  
    /** 
     * 返回 key 指定的哈希集中所有字段的value值 
     * @param key 键
     * @return 
     */  
  
    public List<String> hvals(String key) {  
        Jedis jedis = null; 
        List<String> retList = null;  
        try {  
            jedis = jedisPool.getResource(); 
            retList = jedis.hvals(key);  
        } catch (Exception ex) {  
            logger.error("hvals error.", ex);       
        } finally {  
            returnResource(jedis);
        }  
        return retList;  
    }  
  
    /** 
     * 返回 key 指定的哈希集中所有字段的key值 
     * @param key 
     * @return 
     */  
    public Set<String> hkeys(String key) {  
        Jedis jedis = null; 
        Set<String> retSet = null;  
        try {  
            jedis = jedisPool.getResource(); 
            retSet = jedis.hkeys(key);  
        } catch (Exception ex) {  
            logger.error("hkeys error.", ex);        
        } finally {  
            returnResource(jedis);
        }  
        return retSet;  
    }  
  
    /** 
     * 返回 key 指定的哈希key值总数 
     * 
     * @param key 
     * @return 
     */  
    public long lenHset(String key) {  
        Jedis jedis = null; 
        long count = 0;  
        try {  
            jedis = jedisPool.getResource(); 
            count = jedis.hlen(key);  
        } catch (Exception ex) {  
            logger.error("lenHset error.", ex);    
        } finally {  
            returnResource(jedis);
        }  
        return count;  
    }  
  
    /** 
     * 设置排序集合 
     * 
     * @param key 
     * @param score 
     * @param value 
     * @return 
     */  
    public boolean setSortedSet(String key, long score, String value) {  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            jedis.zadd(key, score, value);  
            return true;  
        } catch (Exception ex) {  
            logger.error("setSortedSet error.", ex);  
            return false;  
        } finally {  
            returnResource(jedis);
        }  
    }  
  
    /** 
     * 获得排序集合 
     * 
     * @param key 
     * @param startScore 
     * @param endScore 
     * @param orderByDesc 
     * @return 
     */  
    public Set<String> getSoredSet(String key, long startScore, long endScore, boolean orderByDesc) {  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            if (orderByDesc) {  
                return jedis.zrevrangeByScore(key, endScore, startScore);  
            } else {  
                return jedis.zrangeByScore(key, startScore, endScore);  
            }  
        } catch (Exception ex) {  
            logger.error("getSoredSet error.", ex);  
            return null;  
        } finally {  
            returnResource(jedis);
        }   
    }  
  
    /** 
     * 计算排序长度 
     * 
     * @param key 
     * @param startScore 
     * @param endScore 
     * @return 
     */  
    public long countSoredSet(String key, long startScore, long endScore) {  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            Long count = jedis.zcount(key, startScore, endScore);  
            return count == null ? 0L : count;  
        } catch (Exception ex) {  
            logger.error("countSoredSet error.", ex);     
            return 0L;
        } finally {  
            returnResource(jedis);
        }    
    }  
  
    /** 
     * 删除排序集合 
     * 
     * @param key 
     * @param value 
     * @return 
     */  
    public boolean delSortedSet(String key, String value) {  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            long count = jedis.zrem(key, value);  
            return count > 0;  
        } catch (Exception ex) {  
            logger.error("delSortedSet error.", ex);  
            return false;  
        } finally {  
            returnResource(jedis);
        }  
    }  
  
    /** 
     * 获得排序集合  
     * @param key 
     * @param startRange 
     * @param endRange 
     * @param orderByDesc 
     * @return 
     */  
    public Set<String> getSoredSetByRange(String key, int startRange, int endRange, boolean orderByDesc) {  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            if (orderByDesc) {  
                return jedis.zrevrange(key, startRange, endRange);  
            } else {  
                return jedis.zrange(key, startRange, endRange);  
            }  
        } catch (Exception ex) {  
            logger.error("getSoredSetByRange error.", ex);     
            return null; 
        } finally {  
            returnResource(jedis);
        }  
    }  
  
    /** 
     * 获得排序打分 
     * 
     * @param key 
     * @return 
     */  
    public Double getScore(String key, String member) {  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            return jedis.zscore(key, member);  
        } catch (Exception ex) {  
            logger.error("getScore error.", ex);   
            return null;  
        } finally {  
            returnResource(jedis);
        }  
    }  
  
    /** 
     * 添加单个值 ,并设置过期时间
     * @param key 键
     * @param value 值
     * @param expire_time 过期时间(多少秒后过期) 
     * @return 
     */  
    public boolean set(String key, String value, int expire_time) {  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            jedis.setex(key, expire_time, value);  
            return true;  
        } catch (Exception ex) {  
            logger.error("set error.", ex);  
            return false; 
        } finally {  
            returnResource(jedis);
        }  
    }  

  
    /**
     * 删除一个key
      * @Title: del
      * @param @param key
      * @param @return    
      * @return boolean    
      * @throws
     */
    public boolean del(String key) {  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            jedis.del(key);  
            return true;  
        } catch (Exception ex) {  
            logger.error("del error.", ex);  
            return false; 
        } finally {  
            returnResource(jedis);
        }  
    }  
  
    /**
     * 
      * 根据指定的key的值加1,并返回加1后的值
      * @Title: incr
      * @param @param key
      * @param @return    
      * @return long    
      * @throws
     */
    public long incr(String key) {  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            return jedis.incr(key);  
        } catch (Exception ex) {  
            logger.error("incr error.", ex);  
            return 0;
        } finally {  
        	returnResource(jedis);  
        }  
    }  
  
    /**
     * 
      * 根据指定的key的值减1,并返回减1后的值
      * @Title: incr
      * @param @param key
      * @param @return    
      * @return long    
      * @throws
     */
    public long decr(String key) {  
        Jedis jedis = null; 
        try {  
            jedis = jedisPool.getResource(); 
            return jedis.decr(key);  
        } catch (Exception ex) {  
            logger.error("decr error.", ex);  
            return 0; 
        } finally {  
        	returnResource(jedis);  
        }  
    }  
  
   
    /**
     * 释放jedis资源(从Jedis 3.0开始使用jedis.close()方法释放资源,不再使用returnResource()函数)
     * @param jedis
     */
    private void returnResource(final Jedis jedis) {
        if (jedis != null) {
        	//starting from Jedis 3.0 returnResource method won't exist. Resouce cleanup should be done using @see {@link redis.clients.jedis.Jedis#close()
        	jedis.close();
        }
    }
}
  