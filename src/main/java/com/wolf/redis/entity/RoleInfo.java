package com.wolf.redis.entity;

import java.io.Serializable;

public class RoleInfo implements Serializable {
	
	private static final long serialVersionUID = -6011241820070393952L;

	private String id;
	
	private String roleName;
	
	private String descrition;

	
	public RoleInfo() {
		
	}

	public RoleInfo(String id, String roleName, String descrition) {
		this.id = id;
		this.roleName = roleName;
		this.descrition = descrition;
	}


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	
	
	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public String getDescrition() {
		return descrition;
	}

	public void setDescrition(String descrition) {
		this.descrition = descrition;
	}

	public String toString(){
		return "id :"+id+" roleName :"+roleName+" descrition:"+descrition;
	}
}