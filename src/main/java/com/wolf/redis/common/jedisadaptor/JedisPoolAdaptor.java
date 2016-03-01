package com.wolf.redis.common.jedisadaptor;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.*;
import redis.clients.util.Hashing;
import redis.clients.util.Pool;

import java.util.List;
import java.util.regex.Pattern;
//import org.apache.commons.pool.impl.GenericObjectPool.Config;

/**
 * 这个类是对JedisPool和ShardedJedisPool的适配器。
 * JedisPool和ShardedJedisPool未实现同一接口，
 * 是Jedis设计上的失误
 * 
 * @author wolf
 * 
 */
@Repository
public class JedisPoolAdaptor extends Pool<JedisAdaptor> {

	public JedisPoolAdaptor(){
	}

	// 创建基于Sharded Jedis的Pool
	public JedisPoolAdaptor(final JedisPoolConfig poolConfig,
							List<JedisShardInfo> shards) {
		this(poolConfig, shards, Hashing.MURMUR_HASH);
	}

	// 创建基于Sharded Jedis的Pool
	public JedisPoolAdaptor(final JedisPoolConfig poolConfig,
							List<JedisShardInfo> shards, Hashing algo) {
		this(poolConfig, shards, algo, null);
	}

	// 创建基于Sharded Jedis的Pool
	public JedisPoolAdaptor(final JedisPoolConfig poolConfig,
							List<JedisShardInfo> shards, Pattern keyTagPattern) {
		this(poolConfig, shards, Hashing.MURMUR_HASH, keyTagPattern);
	}

	// 创建基于Sharded Jedis的Pool
	public JedisPoolAdaptor(final JedisPoolConfig poolConfig,
							List<JedisShardInfo> shards, Hashing algo, Pattern keyTagPattern) {
		super(poolConfig, new JedisFactory(shards, algo, keyTagPattern));
	}

	// 创建基于Jedis的Pool
	public JedisPoolAdaptor(final JedisPoolConfig poolConfig,final String host) {
		this(poolConfig, host, Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT,null);
	}

	// 创建基于Jedis的Pool
	public JedisPoolAdaptor(String host, int port) {
		super(new JedisPoolConfig(),
				new JedisFactory(host, port,Protocol.DEFAULT_TIMEOUT, null));
	}

	// 创建基于Jedis的Pool
	public JedisPoolAdaptor(final JedisPoolConfig poolConfig, final String host,
			int port, int timeout, final String password) {
		super(poolConfig, new JedisFactory(host, port, timeout, password));
	}

	// 创建基于Jedis的Pool
	public JedisPoolAdaptor(final JedisPoolConfig poolConfig,
			final String host, final int port) {
		this(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT, null);
	}

	// 创建基于Jedis的Pool
	public JedisPoolAdaptor(final JedisPoolConfig poolConfig,
			final String host, final int port, final int timeout) {
		this(poolConfig, host, port, timeout, null);
	}

	/**
	 * PoolableObjectFactory custom impl.
	 */
	private static class JedisFactory implements PooledObjectFactory<JedisAdaptor> {
		private boolean sharded;

		// for sharded jedis pool
		private List<JedisShardInfo> shards;
		private Hashing algo;
		private Pattern keyTagPattern;

		// for single jedis pool
		private String host;
		private int port;
		private int timeout;
		private String password;

		public JedisFactory(List<JedisShardInfo> shards, Hashing algo,
				Pattern keyTagPattern) {
			this.shards = shards;
			this.algo = algo;
			this.keyTagPattern = keyTagPattern;
			this.sharded = true;
		}

		public JedisFactory(final String host, final int port,
				final int timeout, final String password) {
			super();
			this.host = host;
			this.port = port;
			this.timeout = (timeout > 0) ? timeout : -1;
			this.password = password;
			this.sharded = false;
		}

		public PooledObject<JedisAdaptor> makeObject() throws Exception {
			if (sharded) {
				return new DefaultPooledObject<JedisAdaptor>(new JedisAdaptor(new ShardedJedis(shards, algo,
						keyTagPattern)));
			} else {
				final Jedis jedis;
				if (timeout > 0) {
					jedis = new Jedis(this.host, this.port, this.timeout);
				} else {
					jedis = new Jedis(this.host, this.port);
				}

				jedis.connect();
				if (null != this.password) {
					jedis.auth(this.password);
				}
				return new DefaultPooledObject<JedisAdaptor>(new JedisAdaptor(jedis));
			}
		}



		@Override
		public void destroyObject(PooledObject<JedisAdaptor> p)
				throws Exception {
			Object jedisObj = p.getObject().getJedis();
			if (sharded) {
				ShardedJedis shardedJedis = (ShardedJedis) jedisObj;
				for (Jedis jedis : shardedJedis.getAllShards()) {
					try {
					    try {
					    	jedis.quit();
					    } catch (Exception e) {

					    }
					    jedis.disconnect();
					} catch (Exception e) {

					}
			    }
			} else {
				if (((Jedis) jedisObj).isConnected()) {
					try {
						try {
							((Jedis) jedisObj).quit();
						} catch (Exception e) {
						}
						((Jedis) jedisObj).disconnect();
					} catch (Exception e) {

					}
				}
			}
		}

		@Override
		public boolean validateObject(PooledObject<JedisAdaptor> p) {
			Object jedisObj = p.getObject().getJedis();

			if (sharded) {
				try {
					ShardedJedis jedis = (ShardedJedis) jedisObj;
					for (Jedis shard : jedis.getAllShards()) {
						if (!shard.ping().equals("PONG")) {
							return false;
						}
					}
					return true;
				} catch (Exception ex) {
					return false;
				}
			} else {
				if (jedisObj instanceof Jedis) {
					try {
						return ((Jedis) jedisObj).isConnected()
								&& ((Jedis) jedisObj).ping().equals("PONG");
					} catch (final Exception e) {
						return false;
					}
				} else {
					return false;
				}
			}
		}

		@Override
		public void activateObject(PooledObject<JedisAdaptor> p)
				throws Exception {
			// TODO Auto-generated method stub

		}

		@Override
		public void passivateObject(PooledObject<JedisAdaptor> p)
				throws Exception {
			// TODO Auto-generated method stub
			
		}

		
	}
}