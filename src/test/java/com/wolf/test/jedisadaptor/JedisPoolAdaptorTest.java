package com.wolf.test.jedisadaptor;

import com.wolf.redis.common.jedisadaptor.JedisAdaptor;
import com.wolf.redis.common.jedisadaptor.JedisPoolAdaptor;
import com.wolf.redis.entity.RoleInfo;
import com.wolf.redis.entity.UserInfo;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.util.*;


@ContextConfiguration(locations = {"classpath*:jedis-sharded-pool.xml"})
public class JedisPoolAdaptorTest extends AbstractJUnit4SpringContextTests {
	
	@Autowired
	JedisPoolAdaptor shardedJedisPool;

	JedisAdaptor jedis = null;

	@Before
	public void getSource() {
		jedis = shardedJedisPool.getResource();
	}


	@Test
	public void setString_Test() {
		try {
			jedis.set("key_001", "wolf.bin2");
			String val = jedis.get("key_001");
			System.out.println(val);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(jedis!=null){
				jedis.close();
			}
		}
	}
	
	@Test
	public void setObject() {//存储单个对象
		try {
			for (int i = 1; i <= 3; i++) {
                String key = UUID.randomUUID().toString();
                UserInfo user = new UserInfo(String.valueOf(i), "lisi", "shenzhen","110");
                System.out.println("key is : "+key);
                jedis.setObject(key, user);
            }
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(jedis!=null){
				jedis.close();
			}
		}
	}
	@Test
	public void getObject(){//获取Object对象
		try {
			String key = "bcba1f9f-6788-4018-bc0b-9ea844b3428b";
			UserInfo user = jedis.getObject(key, UserInfo.class);
			System.out.println("\n"+user+"\n");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(jedis!=null){
				jedis.close();
			}
		}
	}
	
	@Test
	public void setObjects(){//存储Set<对象>集合
		try {
			UserInfo lisi = new UserInfo("001", "lisi", "Shenzhen","18145645652");
			UserInfo wangwu = new UserInfo("002", "wangwu", "Wuhan","18345645672");
			UserInfo sanmao = new UserInfo("003", "sanmao", "Guangzhou","17845645652");
			Set<UserInfo> set = new HashSet<UserInfo>();
			set.add(lisi);
			set.add(wangwu);
			set.add(sanmao);
			jedis.setSetObjects("0001-1000", set);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(jedis!=null){
				jedis.close();
			}
		}
	}
	
	@Test
	public void getObjects(){//获取Set集合对象
		try {
			Set<UserInfo> set = jedis.getSetObjects("0001-1000", UserInfo.class);
			for (UserInfo u : set) {
                System.out.println(u);
            }
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(jedis!=null){
				jedis.close();
			}
		}
	}
	
	@Test
	public void setListObjects(){//存储List集合对象
		try {
			RoleInfo worker = new RoleInfo("100", "worker", "worker");
			RoleInfo student = new RoleInfo("101", "student", "student");
			RoleInfo engineer = new RoleInfo("102", "engineer", "engineer");
			List<RoleInfo> list = new ArrayList<RoleInfo>();
			list.add(worker);
			list.add(student);
			list.add(engineer);
			jedis.setListObjects("role-list", list);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(jedis!=null){
				jedis.close();
			}
		}
	}
	
	@Test
	public void setMapObjects(){//存储Map集合对象
		try {
			RoleInfo worker = new RoleInfo("0011", "worker", "worker");
			RoleInfo student = new RoleInfo("0022", "student", "student");
			RoleInfo engineer = new RoleInfo("0033", "engineer", "engineer");
			Map<String,RoleInfo> map = new HashMap<String,RoleInfo>();
			map.put("001_key",worker);
			map.put("002_key",student);
			map.put("003_key",engineer);
			jedis.setMapObject("role-map", map);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(jedis!=null){
				jedis.close();
			}
		}
	}
	
	@Test
	public void getMapObject(){
		try {
			Map<String, RoleInfo> map = jedis.getMapObject("role-map", RoleInfo.class);
			for (String key : map.keySet()) {
                System.out.println("key : "+key+" role info : "+map.get(key));
            }
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(jedis!=null){
				jedis.close();
			}
		}
	}
	
	@Test
	public void getListObjects(){//获取List集合对象
		try {
			List<RoleInfo> list = jedis.getListObjects("role-list", RoleInfo.class);
			for (RoleInfo r : list) {
                System.out.println(r);
            }
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(jedis!=null){
				jedis.close();
			}
		}
	}

	@Test
	public void setExpire(){//设置过期时间(30秒)
		try {
			jedis.setExpire("role-map", 30);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(jedis!=null){
				jedis.close();
			}
		}
	}

}