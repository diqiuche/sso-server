package com.lyc.sso.server.service;

import java.io.FileInputStream;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.lyc.common.MD5;
import com.lyc.common.StringUtils;
import com.lyc.sso.server.entity.Credential;
import com.lyc.sso.server.entity.DemoLoginUser;
import com.lyc.sso.server.entity.LoginUser;
import com.lyc.sso.server.persistence.UserPersistenceObject;


public class CaptchaAuthenticationHandler implements IAuthenticationHandler {

	@Autowired
	private UserPersistenceObject userPersistenceObject;

	@Override
	public LoginUser authenticate(Credential credential) throws Exception {

		// 获取session中保存的验证码
		String sessionCode = (String) credential.getSettedSessionValue();
		String captcha = credential.getParameter("captcha");

		if (!captcha.equalsIgnoreCase(sessionCode)) {
			credential.setError("验证码错误");
			return null;
		}

		// 从持久化中查询登录账号对应的用户对象
		DemoLoginUser loginUser = userPersistenceObject.getUser(credential
				.getParameter("name"));
		
		if (loginUser != null) {
			String passwd = credential.getParameter("passwd");
			String passwd2 = MD5.encode(MD5.encode(loginUser.getPasswd())
					+ sessionCode);
			if (passwd2.equals(passwd)) {
				return loginUser;
			}
		}
		
		credential.setError("帐号或密码错误");
		return null;

		// String passwd = credential.getParameter("passwd");
		// String passwd2 = MD5.encode(MD5.encode("admin") + sessionCode);
		// if ("admin".equals(credential.getParameter("name"))
		// && passwd2.equals(passwd)) {
		//
		// DemoLoginUser user = new DemoLoginUser();
		// user.setLoginName("admin");
		// return user;
		// } else {
		// credential.setError("帐号或密码错误");
		// return null;
		// }
	}

	@Override
	public Set<String> authedSystemIds(LoginUser loginUser) throws Exception {
		return null;
	}

	// 自动登录
	@Override
	public LoginUser autoLogin(String lt) throws Exception {

		// String[] tmp = lt.split(",");
		// if (tmp.length == 2) {
		// String uname = tmp[0];
		// String passwd = tmp[1];
		//
		// if ("admin".equals(uname) && "admin".equals(passwd)) {
		// DemoLoginUser user = new DemoLoginUser();
		// user.setLoginName("admin");
		// return user;
		// }
		// }
		//
		// return null;

		// lt = DES.decrypt(lt, "test==");
		// String[] tmp = lt.split(",");
		// if (tmp.length == 2) {
		// String uname = tmp[0];
		// String passwd = tmp[1];
		//
		// if ("admin".equals(uname) && MD5.encode("admin").equals(passwd)) {
		// DemoLoginUser user = new DemoLoginUser();
		// user.setLoginName("admin");
		// return user;
		// }
		// }

		// 从持久化存储中按lt查找对应loginUser
		FileInputStream fis = new FileInputStream("d:/test");
		byte[] buff = new byte[fis.available()];
		fis.read(buff);
		fis.close();
				
		String tmp = new String(buff);
		String[] tmps = tmp.split("=");
		
		// 相当于从存储中找个了与lt匹配的数据记录
		if (lt.equals(tmps[0])) {
			// 将匹配的数据装配成loginUser对象
			DemoLoginUser loginUser = userPersistenceObject.getUser(tmps[1]);
			return loginUser;
		}
		
		// 没有匹配项则表示自动登录标识无效
		return null;
	}

	// 生成自动登录标识
	@Override
	public String loginToken(LoginUser loginUser) throws Exception {

		DemoLoginUser demoLoginUser = (DemoLoginUser) loginUser;

		// 生成一个唯一标识用作lt
		String lt = StringUtils.uniqueKey();

		// 将新lt更新到当前user对应字段
		userPersistenceObject
				.updateLoginToken(demoLoginUser.getLoginName(), lt);

		return lt;
	}

	// 更新持久化的lt
	@Override
	public void clearLoginToken(LoginUser loginUser)
			throws Exception {
		DemoLoginUser demoLoginUser = (DemoLoginUser) loginUser;	
		userPersistenceObject.updateLoginToken(demoLoginUser.getLoginName(), null);
	}
}
