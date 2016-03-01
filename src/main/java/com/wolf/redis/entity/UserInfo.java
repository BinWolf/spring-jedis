package com.wolf.redis.entity;

import java.io.Serializable;

public class UserInfo implements Serializable {
	
	private static final long serialVersionUID = -6011241820070393952L;

	private String id;
	
	private String name;
	
	private String address;

	private String telephone;

	
	public UserInfo() {
		
	}

	public UserInfo(String id, String name, String address, String telephone) {
		this.id = id;
		this.name = name;
		this.address = address;
		this.telephone = telephone;
	}


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getTelephone() {
		return telephone;
	}

	public void setTelephone(String telephone) {
		this.telephone = telephone;
	}
	
	public String toString(){
		return "id :"+id+" name :"+name+" address:"+address+" telephone :"+telephone;
	}
}