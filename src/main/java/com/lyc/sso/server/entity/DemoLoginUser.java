package com.lyc.sso.server.entity;

@SuppressWarnings("serial")
public class DemoLoginUser extends LoginUser {

	private String loginName;

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getLoginName() {
		return loginName;
	}
	
	public String getPasswd() {
		return "admin";
	}

	@Override
	public String toString() {
		return loginName;
	}
}
