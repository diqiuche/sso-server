package com.lyc.sso.server.controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.lyc.common.CookieUtils;
import com.lyc.common.StringUtils;
import com.lyc.sso.server.Config;
import com.lyc.sso.server.TokenManager;
import com.lyc.sso.server.entity.ClientSystem;
import com.lyc.sso.server.entity.Credential;
import com.lyc.sso.server.entity.LoginUser;
import com.lyc.sso.server.service.IPreLoginHandler;


@Controller
public class LoginController {

	@Autowired
	private Config config;

	/**
	 * 登录入口
	 * 
	 * @param request
	 * @param backUrl
	 * @param response
	 * @param map
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public String login(HttpServletRequest request, String backUrl,
			HttpServletResponse response, ModelMap map) throws Exception {

		String vt = CookieUtils.getCookie("VT", request);

		if (vt == null) { // VT不存在
			String lt = CookieUtils.getCookie("LT", request);
			if (lt == null) { // VT不存在，LT也不存在
				return config.getLoginViewName();
			} else { // VT不存在， LT存在
				LoginUser loginUser = config.getAuthenticationHandler()
						.autoLogin(lt);

				if (loginUser == null) {
					return config.getLoginViewName();
				} else {
					vt = authSuccess(response, loginUser, true);
					return validateSuccess(backUrl, vt, loginUser, response,
							map);
				}
			}
		} else { // VT存在
			LoginUser loginUser = TokenManager.validate(vt);
			if (loginUser != null) { // VT有效
				return validateSuccess(backUrl, vt, loginUser, response, map); // 验证成功后操作
			} else { // VT 失效，转入登录页
				return config.getLoginViewName();
			}
		}
	}

	@RequestMapping("/preLogin")
	@ResponseBody
	public Object preLogin(HttpSession session) throws Exception {
		IPreLoginHandler preLoginHandler = config.getPreLoginHandler();
		if (preLoginHandler == null) {
			throw new Exception("没有配置preLoginHandler,无法执行预处理");
		}

		return preLoginHandler.handle(session);
	}

	// VT验证成功或登录成功后的操作
	private String validateSuccess(String backUrl, String vt,
			LoginUser loginUser, HttpServletResponse response, ModelMap map)
			throws Exception {

		if (backUrl != null) {
			response.sendRedirect(StringUtils.appendUrlParameter(backUrl, "VT",
					vt));
			return null;
		} else {
			map.put("sysList", config.getClientSystems(loginUser));
			map.put("vt", vt);
			map.put("loginUser", loginUser);
			return config.getLoginViewName();
		}

	}

	/**
	 * 登录验证
	 * 
	 * @param backUrl
	 * @param rememberMe
	 * @param request
	 * @param session
	 * @param response
	 * @param map
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/login")
	public String login(String backUrl, Boolean rememberMe,
			HttpServletRequest request, HttpSession session,
			HttpServletResponse response, ModelMap map) throws Exception {

		final Map<String, String[]> params = request.getParameterMap();

		final Object sessionVal = session
				.getAttribute(IPreLoginHandler.SESSION_ATTR_NAME);

		Credential credential = new Credential() {

			@Override
			public String getParameter(String name) {
				String[] tmp = params.get(name);
				return tmp != null && tmp.length > 0 ? tmp[0] : null;
			}

			@Override
			public String[] getParameterValue(String name) {
				return params.get(name);
			}

			@Override
			public Object getSettedSessionValue() {
				return sessionVal;
			}
		};

		LoginUser loginUser = config.getAuthenticationHandler().authenticate(
				credential);

		if (loginUser == null) {
			map.put("errorMsg", credential.getError());
			return config.getLoginViewName();
		} else {
			String vt = authSuccess(response, loginUser, rememberMe);
			return validateSuccess(backUrl, vt, loginUser, response, map);
		}
	}

	/**
	 * 用户退出
	 * 
	 * @param backUrl
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@RequestMapping("/logout")
	public String logout(String backUrl, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		String vt = CookieUtils.getCookie("VT", request);

		// 清除自动登录信息
		LoginUser loginUser = TokenManager.validate(vt);
		if (loginUser != null) {
			// 清除服务端自动登录状态
			config.getAuthenticationHandler().clearLoginToken(loginUser);
			// 清除自动登录cookie
			Cookie ltCookie = new Cookie("LT", null);
			ltCookie.setMaxAge(0);
			response.addCookie(ltCookie);
		}

		// 移除token
		TokenManager.invalid(vt);

		// 移除server端vt cookie
		Cookie cookie = new Cookie("VT", null);
		cookie.setMaxAge(0);
		response.addCookie(cookie);

		// 通知各客户端logout
		for (ClientSystem clientSystem : config.getClientSystems()) {
			clientSystem.noticeLogout(vt);
		}

		if (backUrl == null) {
			return "/logout";
		} else {
			response.sendRedirect(backUrl);
			return null;
		}
	}

	// 授权成功后的操作
	private String authSuccess(HttpServletResponse response,
			LoginUser loginUser, Boolean rememberMe) throws Exception {
		// 生成VT
		String vt = StringUtils.uniqueKey();
		// 生成LT？
		if (rememberMe != null && rememberMe) {

			// String lt = loginUser.loginToken();
			String lt = config.getAuthenticationHandler().loginToken(loginUser);
			setLtCookie(lt, response);
		}

		// 存入Map
		TokenManager.addToken(vt, loginUser);
		// 写 Cookie
		Cookie cookie = new Cookie("VT", vt);

		// 是否仅https模式，如果是，设置cookie secure为true
		if (config.isSecureMode()) {
			cookie.setSecure(true);
		}

		response.addCookie(cookie);
		return vt;
	}

	// 写lt cookie
	private void setLtCookie(String lt, HttpServletResponse response) {
		Cookie ltCookie = new Cookie("LT", lt);
		ltCookie.setMaxAge(config.getAutoLoginExpDays() * 24 * 60 * 60);
		if (config.isSecureMode()) {
			ltCookie.setSecure(true);
		}
		response.addCookie(ltCookie);
	}
}
