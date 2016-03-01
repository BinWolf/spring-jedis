package com.wolf.redis.common.jedisadaptor;

import com.alibaba.fastjson.JSON;
import com.wolf.redis.common.Constant;
import org.apache.log4j.Logger;
import redis.clients.jedis.BinaryClient.LIST_POSITION;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.SafeEncoder;

import java.util.*;

/**
 * 这个类是对Jedis和ShardedJedis的适配器。 Jedis和ShardedJedis未实现同一接口， 是Jedis设计上的失误
 * 大部分方法Jedis和ShardedJedis天然都支持
 * ，有一部分在JedisAdaptor中做了兼容，有一部分方法是ShardedJedis无法支持，这些方法在调用时（如果是sharded）会抛出异常。
 * 
 * @author wolf
 * 
 */
public class JedisAdaptor {
    static Logger logger = Logger.getLogger(JedisAdaptor.class);

    private boolean sharded;
    private ShardedJedis shardedJedis;
    private Jedis jedis;
    private JedisCommands jedisCommands;
    private BinaryJedisCommands binaryJedisCommands;

    public JedisAdaptor(Jedis jedis) {
        super();
        this.jedis = jedis;
        this.jedisCommands = jedis;
        this.binaryJedisCommands = jedis;
        this.sharded = false;
    }

    public JedisAdaptor(ShardedJedis shardedJedis) {
        super();
        this.shardedJedis = shardedJedis;
        this.jedisCommands = shardedJedis;
        this.binaryJedisCommands = shardedJedis;
        this.sharded = true;
    }

    public Object getJedis() {
        return sharded ? shardedJedis : jedis;
    }

    public String ping() {
        if (sharded) {
            throw new JedisException("ping is not supported if sharded.");
        }
        return jedis.ping();
    }

    public String set(String key, String value) {
        return jedisCommands.set(key, value);
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
        try {
            //将对象转换成json数据格式
            String value = JSON.toJSONString(object);
            jedis.set(key, value);
            return true;
        } catch (Exception e) {
            logger.error("setObject error .....");
            return false;
        }
    }

    /**
     * 通过key获取对象值
     * @Title: getObject
     * @param key(推荐类的全路径+id值)
     * @param clazz 要返回的对象所属的类
     * @return Object
     * @throws
     */
    public <E> E getObject(String key,Class<E> clazz) {
        try {
            String value = jedis.get(key);
            return JSON.parseObject(value, clazz);
        } catch (Exception ex) {
            logger.error("get getObject error key is :"+key+" class is :"+clazz, ex);
            return null;
        }
    }

    public String get(String key) {
        return jedisCommands.get(key);
    }

    public String set(byte[] key, byte[] value) {
        return binaryJedisCommands.set(key, value);
    }

    public byte[] get(byte[] key) {
        return binaryJedisCommands.get(key);
    }

    public String quit() {
        if (sharded) {
            throw new JedisException("quit is not supported if sharded.");
        }
        return jedis.quit();
    }

    public Boolean exists(String key) {
        return jedisCommands.exists(key);
    }

    public Boolean exists(byte[] key) {
        return binaryJedisCommands.exists(key);
    }

    public Long del(String... keys) {
        if (sharded) {
            long count = 0;
            for (String key : keys) {
                long del = shardedJedis.del(key);
                if (del > 0) {
                    count++;
                }
            }
            return count;
        } else {
            return jedis.del(keys);
        }
    }

    /**
     * 避免使用此方法进行删除，效率低下
     * 
     * @param keys
     * @return
     */
    public Long del(byte[]... keys) {
        if (sharded) {
            long count = 0;
            for (byte[] key : keys) {
                long del = shardedJedis.del(SafeEncoder.encode(key));
                if (del > 0) {
                    count++;
                }
            }
            return count;
        } else {
            return jedis.del(keys);
        }
    }

    public String type(String key) {
        return jedisCommands.type(key);
    }

    public String type(byte[] key) {
        return binaryJedisCommands.type(key);
    }

    public String flushDB() {
        if (sharded) {
            String code = null;
            Collection<Jedis> allShards = shardedJedis.getAllShards();
            for (Jedis shard : allShards) {
                code = shard.flushDB();
            }
            return code;
        } else {
            return jedis.flushDB();
        }
    }

    public Set<String> keys(String pattern) {
        if (sharded) {
            HashSet<String> keys = new HashSet<String>();
            Collection<Jedis> allShards = shardedJedis.getAllShards();
            for (Jedis shard : allShards) {
                keys.addAll(shard.keys(pattern));
            }
            return keys;
        } else {
            return jedis.keys(pattern);
        }
    }

    public Set<byte[]> keys(byte[] pattern) {
        if (sharded) {
            HashSet<byte[]> keys = new HashSet<byte[]>();
            Collection<Jedis> allShards = shardedJedis.getAllShards();
            for (Jedis shard : allShards) {
                keys.addAll(shard.keys(pattern));
            }
            return keys;
        } else {
            return jedis.keys(pattern);
        }
    }

    public String randomKey() {
        if (sharded) {
            Collection<Jedis> allShards = shardedJedis.getAllShards();
            Random r = new Random();
            int num = r.nextInt(allShards.size());
            return allShards.toArray(new Jedis[0])[num].randomKey();
        } else {
            return jedis.randomKey();
        }
    }

    public String rename(String oldkey, String newkey) {
        if (sharded) {
            throw new JedisException("rename is not supported if sharded.");
        } else {
            return jedis.rename(oldkey, newkey);
        }
    }

    public byte[] randomBinaryKey() {
        if (sharded) {
            Collection<Jedis> allShards = shardedJedis.getAllShards();
            Random r = new Random();
            int num = r.nextInt(allShards.size());
            return allShards.toArray(new Jedis[0])[num].randomBinaryKey();
        } else {
            return jedis.randomBinaryKey();
        }
    }

    public String rename(byte[] oldkey, byte[] newkey) {
        if (sharded) {
            throw new JedisException("rename is not supported if sharded.");
        } else {
            return jedis.rename(oldkey, newkey);
        }
    }

    public Long renamenx(String oldkey, String newkey) {
        if (sharded) {
            throw new JedisException("renamenx is not supported if sharded.");
        } else {
            return jedis.renamenx(oldkey, newkey);
        }

    }

    public Long renamenx(byte[] oldkey, byte[] newkey) {
        if (sharded) {
            throw new JedisException("renamenx is not supported if sharded.");
        } else {
            return jedis.renamenx(oldkey, newkey);
        }
    }

    public Long expire(String key, int seconds) {
        return jedisCommands.expire(key, seconds);
    }

    public Long dbSize() {
        if (sharded) {
            long size = 0;
            Collection<Jedis> allShards = shardedJedis.getAllShards();
            for (Jedis shard : allShards) {
                size += shard.dbSize();
            }
            return size;
        } else {
            return jedis.dbSize();
        }
    }

    public Long expire(byte[] key, int seconds) {
        return binaryJedisCommands.expire(key, seconds);
    }

    public Long expireAt(String key, long unixTime) {
        return jedisCommands.expireAt(key, unixTime);
    }

    public Long expireAt(byte[] key, long unixTime) {
        return binaryJedisCommands.expireAt(key, unixTime);
    }

    public Long ttl(String key) {
        return jedisCommands.ttl(key);
    }

    public String select(int index) {
        if (sharded) {
            throw new JedisException("select is not supported if sharded.");
        } else {
            return jedis.select(index);
        }
    }

    public Long ttl(byte[] key) {
        return binaryJedisCommands.ttl(key);
    }

    public Long move(String key, int dbIndex) {
        if (sharded) {
            throw new JedisException("move is not supported if sharded.");
        } else {
            return jedis.move(key, dbIndex);
        }
    }

    public Long move(byte[] key, int dbIndex) {
        if (sharded) {
            throw new JedisException("move is not supported if sharded.");
        } else {
            return jedis.move(key, dbIndex);
        }
    }

    public String flushAll() {
        if (sharded) {
            String code = null;
            Collection<Jedis> allShards = shardedJedis.getAllShards();
            for (Jedis shard : allShards) {
                code = shard.flushAll();
            }
            return code;
        } else {
            return jedis.flushAll();
        }
    }

    public String getSet(String key, String value) {
        return jedisCommands.getSet(key, value);
    }

    public List<String> mget(String... keys) {
        if (sharded) {
            ArrayList<String> values = new ArrayList<String>();
            for (String key : keys) {
                values.add(shardedJedis.get(key));
            }
            return values;
        } else {
            return jedis.mget(keys);
        }
    }

    public byte[] getSet(byte[] key, byte[] value) {
        return binaryJedisCommands.getSet(key, value);
    }

    public Long setnx(String key, String value) {
        return jedisCommands.setnx(key, value);
    }

    public List<byte[]> mget(byte[]... keys) {
        if (sharded) {
            ArrayList<byte[]> values = new ArrayList<byte[]>();
            for (byte[] key : keys) {
                values.add(shardedJedis.get(key));
            }
            return values;
        } else {
            return jedis.mget(keys);
        }
    }

    public String setex(String key, int seconds, String value) {
        return jedisCommands.setex(key, seconds, value);
    }

    public Long setnx(byte[] key, byte[] value) {
        return binaryJedisCommands.setnx(key, value);
    }

    public String mset(String... keysvalues) {
        if (sharded) {
            for (int i = 0; i < keysvalues.length; i += 2) {
                shardedJedis.set(keysvalues[i], keysvalues[i + 1]);
            }
            return "+OK";
        } else {
            return jedis.mset(keysvalues);
        }

    }

    public String setex(byte[] key, int seconds, byte[] value) {
        return binaryJedisCommands.setex(key, seconds, value);
    }

    public String mset(byte[]... keysvalues) {
        if (sharded) {
            for (int i = 0; i < keysvalues.length; i += 2) {
                shardedJedis.set(keysvalues[i], keysvalues[i + 1]);
            }
            return "+OK";
        } else {
            return jedis.mset(keysvalues);
        }
    }

    public Long msetnx(String... keysvalues) {
        if (sharded) {
            long count = 0;
            for (int i = 0; i < keysvalues.length; i += 2) {
                long num = shardedJedis.setnx(keysvalues[i], keysvalues[i + 1]);
                if (num > 0) {
                    count++;
                }
            }
            return count;
        } else {
            return jedis.msetnx(keysvalues);
        }
    }

    public Long msetnx(byte[]... keysvalues) {
        if (sharded) {
            long count = 0;
            for (int i = 0; i < keysvalues.length; i += 2) {
                long num = shardedJedis.setnx(keysvalues[i], keysvalues[i + 1]);
                if (num > 0) {
                    count++;
                }
            }
            return count;
        } else {
            return jedis.msetnx(keysvalues);
        }
    }

    public Long decrBy(String key, long integer) {
        return jedisCommands.decrBy(key, integer);
    }

    public Long decrBy(byte[] key, long integer) {
        return binaryJedisCommands.decrBy(key, integer);
    }

    public Long decr(String key) {
        return jedisCommands.decr(key);
    }

    public Long decr(byte[] key) {
        return binaryJedisCommands.decr(key);
    }

    public Long incrBy(String key, long integer) {
        return jedisCommands.incrBy(key, integer);
    }

    public Long incrBy(byte[] key, long integer) {
        return binaryJedisCommands.incrBy(key, integer);
    }

    public Long incr(String key) {
        return jedisCommands.incr(key);
    }

    public Long incr(byte[] key) {
        return binaryJedisCommands.incr(key);
    }

    public Long append(String key, String value) {
        return jedisCommands.append(key, value);
    }

    public Long append(byte[] key, byte[] value) {
        return binaryJedisCommands.append(key, value);
    }

    public String substr(String key, int start, int end) {
        return jedisCommands.substr(key, start, end);
    }

    public byte[] substr(byte[] key, int start, int end) {
        return binaryJedisCommands.substr(key, start, end);
    }

    public Long hset(String key, String field, String value) {
        return jedisCommands.hset(key, field, value);
    }

    public String hget(String key, String field) {
        return jedisCommands.hget(key, field);
    }

    public Long hset(byte[] key, byte[] field, byte[] value) {
        return binaryJedisCommands.hset(key, field, value);
    }

    public Long hsetnx(String key, String field, String value) {
        return jedisCommands.hsetnx(key, field, value);
    }

    public byte[] hget(byte[] key, byte[] field) {
        return binaryJedisCommands.hget(key, field);
    }

    public String hmset(String key, Map<String, String> hash) {
        return jedisCommands.hmset(key, hash);
    }

    public Long hsetnx(byte[] key, byte[] field, byte[] value) {
        return binaryJedisCommands.hsetnx(key, field, value);
    }

    public List<String> hmget(String key, String... fields) {
        return jedisCommands.hmget(key, fields);
    }

    public String hmset(byte[] key, Map<byte[], byte[]> hash) {
        return binaryJedisCommands.hmset(key, hash);
    }

    public Long hincrBy(String key, String field, long value) {
        return jedisCommands.hincrBy(key, field, value);
    }

    public List<byte[]> hmget(byte[] key, byte[]... fields) {
        return binaryJedisCommands.hmget(key, fields);
    }

    public Long hincrBy(byte[] key, byte[] field, long value) {
        return binaryJedisCommands.hincrBy(key, field, value);
    }

    public Boolean hexists(String key, String field) {
        return jedisCommands.hexists(key, field);
    }

    public Long hdel(String key, String... fields) {
        return jedisCommands.hdel(key, fields);
    }

    public Boolean hexists(byte[] key, byte[] field) {
        return binaryJedisCommands.hexists(key, field);
    }

    public Long hlen(String key) {
        return jedisCommands.hlen(key);
    }

    public Long hdel(byte[] key, byte[]... fields) {
        return binaryJedisCommands.hdel(key, fields);
    }

    public Set<String> hkeys(String key) {
        return jedisCommands.hkeys(key);
    }

    public Long hlen(byte[] key) {
        return binaryJedisCommands.hlen(key);
    }

    public List<String> hvals(String key) {
        return jedisCommands.hvals(key);
    }

    public Set<byte[]> hkeys(byte[] key) {
        return binaryJedisCommands.hkeys(key);
    }

    public Map<String, String> hgetAll(String key) {
        return jedisCommands.hgetAll(key);
    }

    public Collection<byte[]> hvals(byte[] key) {
        return binaryJedisCommands.hvals(key);
    }

    public Long rpush(String key, String... strings) {
        return jedisCommands.rpush(key, strings);
    }

    public Map<byte[], byte[]> hgetAll(byte[] key) {
        return binaryJedisCommands.hgetAll(key);
    }

    public Long lpush(String key, String... strings) {
        return jedisCommands.lpush(key, strings);
    }

    public Long rpush(byte[] key, byte[]... strings) {
        return binaryJedisCommands.rpush(key, strings);
    }

    public Long llen(String key) {
        return jedisCommands.llen(key);
    }

    public Long lpush(byte[] key, byte[]... strings) {
        return binaryJedisCommands.lpush(key, strings);
    }

    public List<String> lrange(String key, long start, long end) {
        return jedisCommands.lrange(key, start, end);
    }

    public Long llen(byte[] key) {
        return binaryJedisCommands.llen(key);
    }

    public List<byte[]> lrange(byte[] key, int start, int end) {
        return binaryJedisCommands.lrange(key, start, end);
    }

    public String ltrim(String key, long start, long end) {
        return jedisCommands.ltrim(key, start, end);
    }

    public String ltrim(byte[] key, int start, int end) {
        return binaryJedisCommands.ltrim(key, start, end);
    }

    public String lindex(String key, long index) {
        return jedisCommands.lindex(key, index);
    }

    public String lset(String key, long index, String value) {
        return jedisCommands.lset(key, index, value);
    }

    public byte[] lindex(byte[] key, int index) {
        return binaryJedisCommands.lindex(key, index);
    }

    public Long lrem(String key, long count, String value) {
        return jedisCommands.lrem(key, count, value);
    }

    public String lset(byte[] key, int index, byte[] value) {
        return binaryJedisCommands.lset(key, index, value);
    }

    public Long lrem(byte[] key, int count, byte[] value) {
        return binaryJedisCommands.lrem(key, count, value);
    }

    public String lpop(String key) {
        return jedisCommands.lpop(key);
    }

    public String rpop(String key) {
        return jedisCommands.rpop(key);
    }

    public byte[] lpop(byte[] key) {
        return binaryJedisCommands.lpop(key);
    }

    public String rpoplpush(String srckey, String dstkey) {
        if (sharded) {
            throw new JedisException("rpoplpush is not supported if sharded.");
        } else {
            return jedis.rpoplpush(srckey, dstkey);
        }
    }

    public byte[] rpop(byte[] key) {
        return binaryJedisCommands.rpop(key);
    }

    public byte[] rpoplpush(byte[] srckey, byte[] dstkey) {
        if (sharded) {
            throw new JedisException("rpoplpush is not supported if sharded.");
        } else {
            return jedis.rpoplpush(srckey, dstkey);
        }
    }

    public Long sadd(String key, String... members) {
        return jedisCommands.sadd(key, members);
    }

    public Set<String> smembers(String key) {
        return jedisCommands.smembers(key);
    }

    public Long sadd(byte[] key, byte[]... members) {
        return binaryJedisCommands.sadd(key, members);
    }

    public Long srem(String key, String... members) {
        return jedisCommands.srem(key, members);
    }

    public Set<byte[]> smembers(byte[] key) {
        return binaryJedisCommands.smembers(key);
    }

    public String spop(String key) {
        return jedisCommands.spop(key);
    }

    public Long srem(byte[] key, byte[]... member) {
        return binaryJedisCommands.srem(key, member);
    }

    /**
     * 如果是sharded，那么这个操作不是原子的
     * 
     * @param srckey
     * @param dstkey
     * @param member
     * @return
     */
    public Long smove(String srckey, String dstkey, String member) {
        if (sharded) {
            long num = shardedJedis.srem(srckey, member);
            if (num > 0) {
                shardedJedis.sadd(dstkey, member);
            }
        }
        return jedis.smove(srckey, dstkey, member);
    }

    public byte[] spop(byte[] key) {
        return binaryJedisCommands.spop(key);
    }

    /**
     * 如果是sharded，那么这个操作不是原子的
     * 
     * @param srckey
     * @param dstkey
     * @param member
     * @return
     */
    public Long smove(byte[] srckey, byte[] dstkey, byte[] member) {
        if (sharded) {
            long num = shardedJedis.srem(srckey, member);
            if (num > 0) {
                shardedJedis.sadd(dstkey, member);
            }
        }
        return jedis.smove(srckey, dstkey, member);
    }

    public Long scard(String key) {
        return jedisCommands.scard(key);
    }

    public Boolean sismember(String key, String member) {
        return jedisCommands.sismember(key, member);
    }

    public Set<String> sinter(String... keys) {
        if (sharded) {
            throw new JedisException("sinter is not supported if sharded.");
        } else {
            return jedis.sinter(keys);
        }
    }

    public Long scard(byte[] key) {
        return binaryJedisCommands.scard(key);
    }

    public Boolean sismember(byte[] key, byte[] member) {
        return binaryJedisCommands.sismember(key, member);
    }

    public Set<byte[]> sinter(byte[]... keys) {
        if (sharded) {
            throw new JedisException("sinter is not supported if sharded.");
        } else {
            return jedis.sinter(keys);
        }
    }

    public Long sinterstore(String dstkey, String... keys) {
        if (sharded) {
            throw new JedisException("sinterstore is not supported if sharded.");
        } else {
            return jedis.sinterstore(dstkey, keys);
        }
    }

    public Set<String> sunion(String... keys) {
        if (sharded) {
            throw new JedisException("sunion is not supported if sharded.");
        } else {
            return jedis.sunion(keys);
        }
    }

    public Long sinterstore(byte[] dstkey, byte[]... keys) {
        if (sharded) {
            throw new JedisException("sinterstore is not supported if sharded.");
        } else {
            return jedis.sinterstore(dstkey, keys);
        }
    }

    public Long sunionstore(String dstkey, String... keys) {
        if (sharded) {
            throw new JedisException("sunionstore is not supported if sharded.");
        } else {
            return jedis.sunionstore(dstkey, keys);
        }
    }

    public Set<byte[]> sunion(byte[]... keys) {
        if (sharded) {
            throw new JedisException("sunion is not supported if sharded.");
        } else {
            return jedis.sunion(keys);
        }
    }

    public Set<String> sdiff(String... keys) {
        if (sharded) {
            throw new JedisException("sdiff is not supported if sharded.");
        } else {
            return jedis.sdiff(keys);
        }
    }

    public Long sunionstore(byte[] dstkey, byte[]... keys) {
        if (sharded) {
            throw new JedisException("sunionstore is not supported if sharded.");
        } else {
            return jedis.sunionstore(dstkey, keys);
        }
    }

    public Long sdiffstore(String dstkey, String... keys) {
        if (sharded) {
            throw new JedisException("sdiffstore is not supported if sharded.");
        } else {
            return jedis.sdiffstore(dstkey, keys);
        }
    }

    public Set<byte[]> sdiff(byte[]... keys) {
        if (sharded) {
            throw new JedisException("sdiff is not supported if sharded.");
        } else {
            return jedis.sdiff(keys);
        }
    }

    public String srandmember(String key) {
        return jedisCommands.srandmember(key);
    }

    public Long zadd(String key, double score, String member) {
        return jedisCommands.zadd(key, score, member);
    }

    public Long sdiffstore(byte[] dstkey, byte[]... keys) {
        if (sharded) {
            throw new JedisException("sdiffstore is not supported if sharded.");
        } else {
            return jedis.sdiffstore(dstkey, keys);
        }
    }

    public byte[] srandmember(byte[] key) {
        return binaryJedisCommands.srandmember(key);
    }

    public Long zadd(byte[] key, double score, byte[] member) {
        return binaryJedisCommands.zadd(key, score, member);
    }


	public Long zadd(String key, Map<String, Double> scoreMembers) {
		return jedisCommands.zadd(key, scoreMembers);
	}


    public Set<String> zrange(String key, long start, long end) {
        return jedisCommands.zrange(key, start, end);
    }

    public Long zrem(String key, String... members) {
        return jedisCommands.zrem(key, members);
    }

	public Long zadd(byte[] key, Map<byte[], Double> scoreMembers) {
		return binaryJedisCommands.zadd(key, scoreMembers);
	}


    public Double zincrby(String key, double score, String member) {
        return jedisCommands.zincrby(key, score, member);
    }

    public Set<byte[]> zrange(byte[] key, int start, int end) {
        return binaryJedisCommands.zrange(key, start, end);
    }

    public Long zrem(byte[] key, byte[]... members) {
        return binaryJedisCommands.zrem(key, members);
    }

    public Double zincrby(byte[] key, double score, byte[] member) {
        return binaryJedisCommands.zincrby(key, score, member);
    }

    public Long zrank(String key, String member) {
        return jedisCommands.zrank(key, member);
    }

    public Long zrevrank(String key, String member) {
        return jedisCommands.zrevrank(key, member);
    }

    public Long zrank(byte[] key, byte[] member) {
        return binaryJedisCommands.zrank(key, member);
    }

    public Set<String> zrevrange(String key, long start, long end) {
        return jedisCommands.zrevrange(key, start, end);
    }

    public Long zrevrank(byte[] key, byte[] member) {
        return binaryJedisCommands.zrevrank(key, member);
    }

    public Set<Tuple> zrangeWithScores(String key, long start, long end) {
        return jedisCommands.zrangeWithScores(key, start, end);
    }

    public Set<Tuple> zrevrangeWithScores(String key, long start, long end) {
        return jedisCommands.zrevrangeWithScores(key, start, end);
    }

    public Long zcard(String key) {
        return jedisCommands.zcard(key);
    }

    public Set<byte[]> zrevrange(byte[] key, int start, int end) {
        return binaryJedisCommands.zrevrange(key, start, end);
    }

    public Double zscore(String key, String member) {
        return jedisCommands.zscore(key, member);
    }

    public Set<Tuple> zrangeWithScores(byte[] key, int start, int end) {
        return binaryJedisCommands.zrangeWithScores(key, start, end);
    }

    public Set<Tuple> zrevrangeWithScores(byte[] key, int start, int end) {
        return binaryJedisCommands.zrevrangeWithScores(key, start, end);
    }

    public void watch(String... keys) {
        if (sharded) {
            throw new JedisException("watch is not supported if sharded.");
        } else {
            jedis.watch(keys);
        }
    }

    public Long zcard(byte[] key) {
        return binaryJedisCommands.zcard(key);
    }

    public List<String> sort(String key) {
        return jedisCommands.sort(key);
    }

    public Double zscore(byte[] key, byte[] member) {
        return binaryJedisCommands.zscore(key, member);
    }

    public List<String> sort(String key, SortingParams sortingParameters) {
        return jedisCommands.sort(key, sortingParameters);
    }

    public Transaction multi() {
        if (sharded) {
            throw new JedisException("multi is not supported if sharded.");
        } else {
            return jedis.multi();
        }
    }


	
	public String watch(byte[]... keys) {
		if (sharded) {
			throw new JedisException("watch is not supported if sharded.");
		} else {
			return jedis.watch(keys);
		}
	}


    public String unwatch() {
        if (sharded) {
            throw new JedisException("unwatch is not supported if sharded.");
        } else {
            return jedis.unwatch();
        }
    }

    public List<byte[]> sort(byte[] key) {
        return binaryJedisCommands.sort(key);
    }

    public List<String> blpop(int timeout, String... keys) {
        if (sharded) {
            throw new JedisException("blpop is not supported if sharded.");
        } else {
            return jedis.blpop(timeout, keys);
        }
    }

    public List<byte[]> sort(byte[] key, SortingParams sortingParameters) {
        return binaryJedisCommands.sort(key, sortingParameters);
    }

    public List<byte[]> blpop(int timeout, byte[]... keys) {
        if (sharded) {
            throw new JedisException("blpop is not supported if sharded.");
        } else {
            return jedis.blpop(timeout, keys);
        }
    }

    public Long sort(String key, SortingParams sortingParameters, String dstkey) {
        if (sharded) {
            throw new JedisException("sort is not supported if sharded.");
        } else {
            return jedis.sort(key, sortingParameters, dstkey);
        }
    }

    public Long sort(String key, String dstkey) {
        if (sharded) {
            throw new JedisException("sort is not supported if sharded.");
        } else {
            return jedis.sort(key, dstkey);
        }
    }

    public List<String> brpop(int timeout, String... keys) {
        if (sharded) {
            throw new JedisException("brpop is not supported if sharded.");
        } else {
            return jedis.brpop(timeout, keys);
        }
    }

    public Long sort(byte[] key, SortingParams sortingParameters, byte[] dstkey) {
        if (sharded) {
            throw new JedisException("sort is not supported if sharded.");
        } else {
            return jedis.sort(key, sortingParameters, dstkey);
        }
    }

    public Long sort(byte[] key, byte[] dstkey) {
        if (sharded) {
            throw new JedisException("sort is not supported if sharded.");
        } else {
            return jedis.sort(key, dstkey);
        }
    }

    public List<byte[]> brpop(int timeout, byte[]... keys) {
        if (sharded) {
            throw new JedisException("brpop is not supported if sharded.");
        } else {
            return jedis.brpop(timeout, keys);
        }
    }

    // public void subscribe(JedisPubSub jedisPubSub, String... channels) {
    // jedis.subscribe(jedisPubSub, channels);
    // }

    public Long publish(String channel, String message) {
        if (sharded) {
            return shardedJedis.getShard(channel).publish(channel, message);
        } else {
            return jedis.publish(channel, message);
        }
    }

    //
    // public void psubscribe(JedisPubSub jedisPubSub, String... patterns) {
    // jedis.psubscribe(jedisPubSub, patterns);
    // }

    public Long zcount(String key, double min, double max) {
        return jedisCommands.zcount(key, min, max);
    }

    public Long zcount(String key, String min, String max) {
        return jedisCommands.zcount(key, min, max);
    }

    public Set<String> zrangeByScore(String key, double min, double max) {
        return jedisCommands.zrangeByScore(key, min, max);
    }

    public Long zcount(byte[] key, double min, double max) {
        return binaryJedisCommands.zcount(key, min, max);
    }

    public Long zcount(byte[] key, byte[] min, byte[] max) {
        return binaryJedisCommands.zcount(key, min, max);
    }

    public Set<byte[]> zrangeByScore(byte[] key, double min, double max) {
        return binaryJedisCommands.zrangeByScore(key, min, max);
    }

    public Set<String> zrangeByScore(String key, String min, String max) {
        return jedisCommands.zrangeByScore(key, min, max);
    }

    public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        return jedisCommands.zrangeByScore(key, min, max, offset, count);
    }

    public Set<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
        return binaryJedisCommands.zrangeByScore(key, min, max, offset, count);
    }

    public Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
        return jedisCommands.zrangeByScore(key, min, max, offset, count);
    }

    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
        return jedisCommands.zrangeByScoreWithScores(key, min, max);
    }

    public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
        return binaryJedisCommands.zrangeByScoreWithScores(key, min, max);
    }

    public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
        return jedisCommands.zrangeByScoreWithScores(key, min, max);
    }

    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        return jedisCommands.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
        return binaryJedisCommands.zrangeByScoreWithScores(key, min, max);
    }

    public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
        return binaryJedisCommands.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
        return jedisCommands.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    public Set<String> zrevrangeByScore(String key, double max, double min) {
        return jedisCommands.zrevrangeByScore(key, max, min);
    }

    public Set<String> zrevrangeByScore(String key, String max, String min) {
        return jedisCommands.zrevrangeByScore(key, max, min);
    }

    public Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
        return jedisCommands.zrevrangeByScore(key, max, min, offset, count);
    }

    public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
        return jedisCommands.zrevrangeByScoreWithScores(key, max, min);
    }

    public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
        return jedisCommands.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
        return jedisCommands.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    public Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
        return jedisCommands.zrevrangeByScore(key, max, min, offset, count);
    }

    public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return binaryJedisCommands.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
        return jedisCommands.zrevrangeByScoreWithScores(key, max, min);
    }

    public Long zremrangeByRank(String key, long start, long end) {
        return jedisCommands.zremrangeByRank(key, start, end);
    }

    public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min) {
        return binaryJedisCommands.zrevrangeByScore(key, max, min);
    }

    public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
        return binaryJedisCommands.zrevrangeByScore(key, max, min);
    }

    public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
        return binaryJedisCommands.zrevrangeByScore(key, max, min, offset, count);
    }

    public Long zremrangeByScore(String key, double start, double end) {
        return jedisCommands.zremrangeByScore(key, start, end);
    }

    public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return binaryJedisCommands.zrevrangeByScore(key, max, min, offset, count);
    }

    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
        return binaryJedisCommands.zrevrangeByScoreWithScores(key, max, min);
    }

    public Long zremrangeByScore(String key, String start, String end) {
        return jedisCommands.zremrangeByScore(key, start, end);
    }

    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count) {
        return binaryJedisCommands.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    public Long zunionstore(String dstkey, String... sets) {
        if (sharded) {
            throw new JedisException("zunionstore is not supported if sharded.");
        } else {
            return jedis.zunionstore(dstkey, sets);
        }
    }

    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
        return binaryJedisCommands.zrevrangeByScoreWithScores(key, max, min);
    }

    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return binaryJedisCommands.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    public Long zremrangeByRank(byte[] key, int start, int end) {
        return binaryJedisCommands.zremrangeByRank(key, start, end);
    }

    public Long zremrangeByScore(byte[] key, double start, double end) {
        return binaryJedisCommands.zremrangeByScore(key, start, end);
    }

    public Long zunionstore(String dstkey, ZParams params, String... sets) {
        if (sharded) {
            throw new JedisException("zunionstore is not supported if sharded.");
        } else {
            return jedis.zunionstore(dstkey, params, sets);
        }
    }

    public Long zremrangeByScore(byte[] key, byte[] start, byte[] end) {
        return binaryJedisCommands.zremrangeByScore(key, start, end);
    }

    public Long zunionstore(byte[] dstkey, byte[]... sets) {
        if (sharded) {
            throw new JedisException("zunionstore is not supported if sharded.");
        } else {
            return jedis.zunionstore(dstkey, sets);
        }
    }

    public Long zinterstore(String dstkey, String... sets) {
        if (sharded) {
            throw new JedisException("zinterstore is not supported if sharded.");
        } else {
            return jedis.zinterstore(dstkey, sets);
        }
    }

    public Long zunionstore(byte[] dstkey, ZParams params, byte[]... sets) {
        if (sharded) {
            throw new JedisException("zunionstore is not supported if sharded.");
        } else {
            return jedis.zunionstore(dstkey, params, sets);
        }
    }

    public Long zinterstore(String dstkey, ZParams params, String... sets) {
        if (sharded) {
            throw new JedisException("zinterstore is not supported if sharded.");
        } else {
            return jedis.zinterstore(dstkey, params, sets);
        }
    }

    public Long zinterstore(byte[] dstkey, byte[]... sets) {
        if (sharded) {
            throw new JedisException("zinterstore is not supported if sharded.");
        } else {
            return jedis.zinterstore(dstkey, sets);
        }
    }

    public Long strlen(String key) {
        if (sharded) {
            return shardedJedis.getShard(key).strlen(key);
        } else {
            return jedis.strlen(key);
        }

    }

    public Long lpushx(String key, String string) {
        return jedisCommands.lpushx(key, string);
    }

    public Long zinterstore(byte[] dstkey, ZParams params, byte[]... sets) {
        if (sharded) {
            throw new JedisException("zinterstore is not supported if sharded.");
        } else {
            return jedis.zinterstore(dstkey, params, sets);
        }
    }

    public Long persist(String key) {
        if (sharded) {
            return shardedJedis.getShard(key).persist(key);
        } else {
            return jedis.persist(key);
        }
    }

    public Long rpushx(String key, String string) {
        return jedisCommands.rpushx(key, string);
    }

    public Long linsert(String key, LIST_POSITION where, String pivot, String value) {
        return jedisCommands.linsert(key, where, pivot, value);
    }

    public String brpoplpush(String source, String destination, int timeout) {
        if (sharded) {
            List<String> pops = shardedJedis.getShard(source).brpop(timeout, source);
            if (pops.size() == 2) {
                String element = pops.get(1);
                shardedJedis.lpush(destination, element);
                return element;
            } else {
                return null;
            }
        } else {
            return jedis.brpoplpush(source, destination, timeout);
        }
    }

    public Boolean setbit(String key, long offset, boolean value) {
        return jedisCommands.setbit(key, offset, value);
    }

    public Boolean getbit(String key, long offset) {
        return jedisCommands.getbit(key, offset);
    }

    public Long setrange(String key, long offset, String value) {
        return jedisCommands.setrange(key, offset, value);
    }

    public String save() {
        if (sharded) {

        }
        return jedis.save();
    }

    public String getrange(String key, long startOffset, long endOffset) {
        return jedisCommands.getrange(key, startOffset, endOffset);
    }

    // public List<String> configGet(String pattern) {
    // return jedisCommands.configGet(pattern);
    // }
    // public String bgsave() {
    // return jedisCommands.bgsave();
    // }
    // public String bgrewriteaof() {
    // return jedisCommands.bgrewriteaof();
    // }
    // public String configSet(String parameter, String value) {
    // return jedisCommands.configSet(parameter, value);
    // }
    // public Long lastsave() {
    // return jedisCommands.lastsave();
    // }
    // public String shutdown() {
    // return jedisCommands.shutdown();
    // }
    // public Object eval(String script, int keyCount, String... params) {
    // return jedisCommands.eval(script, keyCount, params);
    // }
    // public Object eval(String script, List<String> keys, List<String> args) {
    // return jedisCommands.eval(script, keys, args);
    // }
    // public Object eval(String script) {
    // return jedisCommands.eval(script);
    // }
    public String info() {
        if (sharded) {
            throw new JedisException("info is not supported if sharded.");
        } else {
            return jedis.info();
        }
    }

    public Object evalsha(String script) {
        if (sharded) {
            throw new JedisException("evalsha is not supported if sharded.");
        } else {
            return jedis.evalsha(script);
        }
    }

    public Object evalsha(String sha1, List<String> keys, List<String> args) {
        if (sharded) {
            throw new JedisException("evalsha is not supported if sharded.");
        } else {
            return jedis.evalsha(sha1, keys, args);
        }
    }

    public Object evalsha(String sha1, int keyCount, String... params) {
        if (sharded) {
            throw new JedisException("evalsha is not supported if sharded.");
        } else {
            return jedis.evalsha(sha1, keyCount, params);
        }
    }

    public Boolean scriptExists(String sha1) {
        if (sharded) {
            throw new JedisException("scriptExists is not supported if sharded.");
        } else {
            return jedis.scriptExists(sha1);
        }
    }

    public List<Boolean> scriptExists(String... sha1) {
        if (sharded) {
            throw new JedisException("scriptExists is not supported if sharded.");
        } else {
            return jedis.scriptExists(sha1);
        }
    }

    public String scriptLoad(String script) {
        if (sharded) {
            throw new JedisException("scriptLoad is not supported if sharded.");
        } else {
            return jedis.scriptLoad(script);
        }
    }

    public Long objectRefcount(String key) {
        return sharded ? shardedJedis.objectRefcount(SafeEncoder.encode(key)) : jedis.objectRefcount(key);
    }

    public String objectEncoding(String key) {
        return sharded ? SafeEncoder.encode(shardedJedis.objectEncoding(SafeEncoder.encode(key))) : jedis.objectEncoding(key);
    }

    public Long objectIdletime(String key) {
        return sharded ? shardedJedis.objectIdletime(SafeEncoder.encode(key)) : jedis.objectIdletime(key);
    }

    public List<byte[]> configGet(byte[] pattern) {
        if (sharded) {
            return shardedJedis.getAllShards().iterator().next().configGet(pattern);
        } else {
            return jedis.configGet(pattern);
        }

    }

    public String configResetStat() {
        if (sharded) {
            String code = null;
            for (Jedis shard : shardedJedis.getAllShards()) {
                code = shard.configResetStat();
            }
            return code;
        } else {
            return jedis.configResetStat();
        }
    }

    public byte[] configSet(byte[] parameter, byte[] value) {
        if (sharded) {
            byte[] code = null;
            for (Jedis shard : shardedJedis.getAllShards()) {
                code = shard.configSet(parameter, value);
            }
            return code;
        } else {
            return jedis.configSet(parameter, value);
        }
    }

    public Long strlen(byte[] key) {
        if (sharded) {
            return shardedJedis.getShard(key).strlen(key);
        } else {
            return jedis.strlen(key);
        }
    }

    public void sync() {
        if (sharded) {
            for (Jedis shard : shardedJedis.getAllShards()) {
                shard.sync();
            }
        } else {
            jedis.sync();
        }
    }

    public Long lpushx(byte[] key, byte[] string) {
        return binaryJedisCommands.lpushx(key, string);
    }

    public Long persist(byte[] key) {
        if (sharded) {
            return shardedJedis.getShard(key).persist(key);
        } else {
            return jedis.persist(key);
        }
    }

    public Long rpushx(byte[] key, byte[] string) {
        return binaryJedisCommands.rpushx(key, string);
    }

    public Long linsert(byte[] key, LIST_POSITION where, byte[] pivot, byte[] value) {
        return binaryJedisCommands.linsert(key, where, pivot, value);
    }

    public byte[] brpoplpush(byte[] source, byte[] destination, int timeout) {
        if (sharded) {
            throw new JedisException("brpoplpush is not supported if sharded.");
        } else {
            return jedis.brpoplpush(source, destination, timeout);
        }
    }


	public String getrange(byte[] key, long startOffset, long endOffset) {
		if (sharded) {
			return shardedJedis.getrange(SafeEncoder.encode(key), startOffset,
					endOffset);
		} else {
			byte[] ret = jedis.getrange(key, startOffset, endOffset);
			return SafeEncoder.encode(ret);
		}
	}


    public Long publish(byte[] channel, byte[] message) {
        if (sharded) {
            return shardedJedis.getShard(channel).publish(channel, message);
        } else {
            return jedis.publish(channel, message);
        }
    }

    //
    // public void subscribe(BinaryJedisPubSub jedisPubSub, byte[]... channels)
    // {
    // jedis.subscribe(jedisPubSub, channels);
    // }
    //
    // public void psubscribe(BinaryJedisPubSub jedisPubSub, byte[]... patterns)
    // {
    // jedis.psubscribe(jedisPubSub, patterns);
    // }


    public Object eval(byte[] script, byte[] keyCount, byte[][] params) {
        if (sharded) {
            throw new JedisException("eval is not supported if sharded.");
        } else {
            return jedis.eval(script, keyCount, params);
        }
    }

    public String scriptFlush() {
        if (sharded) {
            throw new JedisException("scriptFlush is not supported if sharded.");
        } else {
            return jedis.scriptFlush();
        }
    }


	public String scriptKill() {
		if (sharded) {
			throw new JedisException("scriptKill is not supported if sharded.");
		} else {
			return jedis.scriptKill();
		}
	}


	public String slowlogReset() {
		if (sharded) {
			throw new JedisException(
					"slowlogReset is not supported if sharded.");
		} else {
			return jedis.slowlogReset();
		}
	}

    public long slowlogLen() {
        if (sharded) {
            throw new JedisException("slowlogLen is not supported if sharded.");
        } else {
            return jedis.slowlogLen();
        }
    }

    public List<byte[]> slowlogGetBinary() {
        if (sharded) {
            throw new JedisException("slowlogGetBinary is not supported if sharded.");
        } else {
            return jedis.slowlogGetBinary();
        }
    }

    public List<byte[]> slowlogGetBinary(long entries) {
        if (sharded) {
            throw new JedisException("slowlogGetBinary is not supported if sharded.");
        } else {
            return jedis.slowlogGetBinary(entries);
        }
    }

    public Long objectRefcount(byte[] key) {
        if (sharded) {
            throw new JedisException("objectRefcount is not supported if sharded.");
        } else {
            return jedis.objectRefcount(key);
        }
    }

    public byte[] objectEncoding(byte[] key) {
        if (sharded) {
            throw new JedisException("objectEncoding is not supported if sharded.");
        } else {
            return jedis.objectEncoding(key);
        }
    }

    public Long objectIdletime(byte[] key) {
        if (sharded) {
            throw new JedisException("objectIdletime is not supported if sharded.");
        } else {
            return jedis.objectIdletime(key);
        }
    }

    public Pipeline pipeline() {
        if (sharded) {
            throw new JedisException("please use shardPipeline method if sharded.");
        }
        return jedis.pipelined();
    }

    public ShardedJedisPipeline shardPipeline() {
        if (!sharded) {
            throw new JedisException("please use pipeline method if not sharded.");
        }
        return shardedJedis.pipelined();
    }

    
    public void subscribe(final JedisPubSub jedisPubSub, final String... channels) {
    	if(sharded) {
    		for(String channel: channels) {
    			shardedJedis.getShard(channel).subscribe(jedisPubSub, channel);
    		}
    	} else {
    		jedis.subscribe(jedisPubSub, channels);
    	}
    }
    
    public void subscribe(final BinaryJedisPubSub jedisPubSub, final byte[]... channels) {
    	if(sharded) {
    		for(byte[] channel: channels) {
    			shardedJedis.getShard(channel).subscribe(jedisPubSub, channel);
    		}
    	} else {
    		jedis.subscribe(jedisPubSub, channels);
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
                jedis.sadd(key, jsonArray);
                flag = true;
            } catch (Exception ex) {
                logger.error("setMapObject error; key is : "+key+" value map is :"+map, ex);
                return flag;
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
        try {
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
        try {
            //添加一个字符串值到List容器的底部（右侧）如果KEY不存在，则创建一个List容器，如果KEY存在并且不是一个List容器，那么返回FLASE
            jedis.rpush(key, jsonArray);
            return true;
        } catch (Exception ex) {
            logger.error("setListObjects error; key is : "+key, ex);
            return false;
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
        try {
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
        try {
            //存放集合数据
            jedis.sadd(key, jsonArray);
            return true;
        } catch (Exception ex) {
            logger.error("setObjects error; key is : "+key, ex);
            return false;
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
        try {
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
        }
    }

    public void close(){
        shardedJedis.close();
    }

    public Long setExpire(String key, long time) {
        return jedis.pexpire(key, time);
    }
}
