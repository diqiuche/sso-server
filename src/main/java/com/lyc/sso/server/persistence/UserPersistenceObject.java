package com.lyc.sso.server.persistence;

import java.io.FileOutputStream;

import org.springframework.stereotype.Repository;

import com.lyc.sso.server.entity.DemoLoginUser;


/**
 * 登录用户信息持久化服务，相当于DAO对象的模拟
 * @author preach
 *
 */
@Repository
public class UserPersistenceObject {

	/**
	 * 更新当前登录用户的lt标识
	 * @param loginName
	 * @param lt
	 * @throws Exception 
	 */
	public void updateLoginToken(String loginName, String lt) throws Exception {
		
		//将信息写入存储文件test，格式为lt=loginName，如：02564fc6a02a35c689cbdf898458d2da=admin
		FileOutputStream fos = new FileOutputStream("d:/test");
		fos.write((lt + "=" + loginName).getBytes());
		fos.close();
	}

	/**
	 * 按登录账号查询用户信息
	 * @param parameter
	 * @return
	 */
	public DemoLoginUser getUser(String uname) {
		if ("admin".equals(uname)) {
			DemoLoginUser loginUser = new DemoLoginUser();
			loginUser.setLoginName("admin");
			return loginUser;
		}
		return null;
	}

	

}
