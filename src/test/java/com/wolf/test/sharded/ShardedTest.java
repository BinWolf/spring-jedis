package com.wolf.test.sharded;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.wolf.redis.common.sharded.BaseShardedJedis;
import com.wolf.redis.entity.RoleInfo;
import com.wolf.redis.entity.UserInfo;

/**
 * 一致性hash测试
  *
 */

@ContextConfiguration(locations = {"classpath*:redis-shardedpool.xml"})
public class ShardedTest extends AbstractJUnit4SpringContextTests {


//	@Autowired
	private BaseShardedJedis baseShardedJedis;

/*	@Autowired
	private TaskExecutor taskExecutor;*/


	@Test
	public void setObject() {//存储单个对象
        for (int i = 1; i <= 1; i++) {
        	String key = UUID.randomUUID().toString();
        	UserInfo user = new UserInfo(String.valueOf(i), "lisi", "shenzhen","110");
        	System.out.println("key is : "+key);
        	baseShardedJedis.setObject(key, user);
		}
    }

	@Test
	public void getObject(){//获取Object对象
		String key = "e8eafa89-db81-4fc1-b8e6-b16edee58a67";
		UserInfo user = baseShardedJedis.getObject(key, UserInfo.class);
		System.out.println("\n"+user+"\n");
	}
	
	@Test
	public void setObjects(){//存储Set<对象>集合
		UserInfo lisi = new UserInfo("001", "lisi", "Shenzhen","110");
		UserInfo wangwu = new UserInfo("002", "wangwu", "Wuhan","110");
		UserInfo sanmao = new UserInfo("003", "sanmao", "Guangzhou","110");
		Set<UserInfo> set = new HashSet<UserInfo>();
		set.add(lisi);
		set.add(wangwu);
		set.add(sanmao);
		baseShardedJedis.setSetObjects("0001-1000", set);
	}
	
	@Test
	public void getObjects(){//获取Set集合对象
		Set<UserInfo> set = baseShardedJedis.getSetObjects("0001-1000", UserInfo.class);
		for (UserInfo u : set) {
			System.out.println(u);
		}
	}
	
	@Test
	public void setListObjects(){//存储List集合对象
		RoleInfo worker = new RoleInfo("100", "worker", "worker");
		RoleInfo student = new RoleInfo("101", "student", "student");
		RoleInfo engineer = new RoleInfo("102", "engineer", "engineer");
		List<RoleInfo> list = new ArrayList<RoleInfo>();
		list.add(worker);
		list.add(student);
		list.add(engineer);
		baseShardedJedis.setListObjects("role-list", list);
	}
	
	@Test
	public void setMapObjects(){//存储Map集合对象
		RoleInfo worker = new RoleInfo("0011", "worker", "worker");
		RoleInfo student = new RoleInfo("0022", "student", "student");
		RoleInfo engineer = new RoleInfo("0033", "engineer", "engineer");
		Map<String,RoleInfo> map = new HashMap<String,RoleInfo>();
		map.put("001_key",worker);
		map.put("002_key",student);
		map.put("003_key",engineer);
		baseShardedJedis.setMapObject("role-map", map);
	}
	
	@Test
	public void getMapObject(){
		Map<String, RoleInfo> map = baseShardedJedis.getMapObject("role-map", RoleInfo.class);
		for (String key : map.keySet()) {
			System.out.println("key : "+key+" role info : "+map.get(key));
		}
	}
	
	@Test
	public void getListObjects(){//获取List集合对象
		List<RoleInfo> list = baseShardedJedis.getListObjects("role-list", RoleInfo.class);
		for (RoleInfo r : list) {
			System.out.println(r);
		}
	}
	
	@Test
	public void setExpire(){//设置过期时间(30秒)
		baseShardedJedis.setExpire("role-map", 30);
	}
	
	/*@Test
	public void getObjectByThread(){//测试资源释放
		int count = 10;
		for (int i = 0; i <= count; i++) {
			taskExecutor.execute(new Runnable() {
				@Override
				public void run() {
					String key = "bcba1f9f-6788-4018-bc0b-9ea844b3428b";
					UserInfo user = baseShardedJedis.getObject(key, UserInfo.class);
					System.out.println("user info :  "+user);
				}
			});
		}
	}*/
}