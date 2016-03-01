package com.wolf.test.template;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import com.wolf.redis.common.spring.BaseRedisTemplate;


@ContextConfiguration(locations = {"classpath*:applicationContext.xml"})
public class RedisTemplateTest extends AbstractJUnit4SpringContextTests {
	
//	@Autowired
	BaseRedisTemplate<String,String> baseRedisTemplate;
	
	/*@Test
	public void setObject() {//存储单个对象
        for (int i = 1; i <= 10; i++) {
        	String key = "0772";
        	UserInfo user = new UserInfo(String.valueOf(i), "lisi", "shenzhen","18145645652");
        	baseRedisTemplate.setObject(key, user);
		}
    }
	@Test
	public void getObject(){//获取Object对象
		String key = "0772";
		UserInfo user = baseRedisTemplate.getObject(key, UserInfo.class);
		System.out.println("\n"+user+"\n");
	}*/
	
	@Test
	public void push() {
		baseRedisTemplate.push("0755", "shenzhen.code");
	}
	
}