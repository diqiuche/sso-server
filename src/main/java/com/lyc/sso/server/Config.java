package com.lyc.sso.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.lyc.sso.server.entity.ClientSystem;
import com.lyc.sso.server.entity.LoginUser;
import com.lyc.sso.server.service.IAuthenticationHandler;
import com.lyc.sso.server.service.IPreLoginHandler;

/**
 * 应用配置信息
 * 
 * @author Administrator
 *
 */
public class Config implements ResourceLoaderAware {

	private static Logger logger = LoggerFactory.getLogger(Config.class);

	private ResourceLoader resourceLoader;

	private IAuthenticationHandler authenticationHandler; // 鉴权处理器
	private IPreLoginHandler preLoginHandler; // 登录前预处理器
	private String loginViewName = "/login"; // 登录页面视图名称

	private int tokenTimeout = 30; // 令牌有效期，单位为分钟，默认30分钟

	private boolean secureMode = false; // 是否必须为https

	private int autoLoginExpDays = 365; // 自动登录状态有效期限，默认一年

	private List<ClientSystem> clientSystems = new ArrayList<ClientSystem>();

	/**
	 * 重新加载配置，以支持热部署
	 * 
	 * @throws Exception
	 */
	public void refreshConfig() throws Exception {

		// 加载config.properties
		Properties configProperties = new Properties();

		try {
			Resource resource = resourceLoader
					.getResource("classpath:config.properties");
			configProperties.load(resource.getInputStream());
		} catch (IOException e) {
			logger.warn("在classpath下未找到配置文件config.properties");
		}

		// vt有效期参数
		String configTokenTimeout = (String) configProperties
				.get("tokenTimeout");
		if (configTokenTimeout != null) {
			try {
				tokenTimeout = Integer.parseInt(configTokenTimeout);
				logger.debug("config.properties设置tokenTimeout={}", tokenTimeout);
			} catch (NumberFormatException e) {
				logger.warn("tokenTimeout参数配置不正确");
			}
		}

		// 是否仅https安全模式下运行
		String configScureMode = configProperties.getProperty("secureMode");
		if (configScureMode != null) {
			this.secureMode = Boolean.parseBoolean(configScureMode);
			logger.debug("config.properties设置secureMode={}", this.secureMode);
		}

		// 自动登录有效期
		String configAutoLoginExpDays = configProperties
				.getProperty("autoLoginExpDays");
		if (configAutoLoginExpDays != null) {
			try {
				autoLoginExpDays = Integer.parseInt(configAutoLoginExpDays);
				logger.debug("config.properties设置autoLoginExpDays={}",
						autoLoginExpDays);
			} catch (NumberFormatException e) {
				logger.warn("autoLoginExpDays参数配置不正确");
			}
		}

		// 加载客户端系统配置列表
		try {
			loadClientSystems();
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("加载client system配置失败");
		}
	}

	// 加载客户端系统配置列表
	@SuppressWarnings("unchecked")
	private void loadClientSystems() throws Exception {
		Resource resource = resourceLoader
				.getResource("classpath:client_systems.xml");
		// dom4j
		SAXReader reader = new SAXReader();
		Document doc = reader.read(resource.getInputStream());

		Element rootElement = doc.getRootElement();
		List<Element> systemElements = rootElement.elements();

		clientSystems.clear();
		for (Element element : systemElements) {
			ClientSystem clientSystem = new ClientSystem();

			clientSystem.setId(element.attributeValue("id"));
			clientSystem.setName(element.attributeValue("name"));
			clientSystem.setBaseUrl(element.elementText("baseUrl"));
			clientSystem.setHomeUri(element.elementText("homeUri"));
			clientSystem.setInnerAddress(element.elementText("innerAddress"));

			clientSystems.add(clientSystem);
		}
	}

	/**
	 * 应用停止时执行，做清理性工作，如通知客户端logout
	 */
	public void destroy() {
		for (ClientSystem clientSystem : clientSystems) {
			clientSystem.noticeShutdown();
		}
	}

	/**
	 * 获取当前鉴权处理器
	 * 
	 * @return
	 */
	public IAuthenticationHandler getAuthenticationHandler() {
		return authenticationHandler;
	}

	public void setAuthenticationHandler(
			IAuthenticationHandler authenticationHandler) {
		this.authenticationHandler = authenticationHandler;
	}

	/**
	 * 获取登录前预处理器
	 * 
	 * @return
	 */
	public IPreLoginHandler getPreLoginHandler() {
		return preLoginHandler;
	}

	public void setPreLoginHandler(IPreLoginHandler preLoginHandler) {
		this.preLoginHandler = preLoginHandler;
	}

	/**
	 * 获取登录页面视图名称
	 * 
	 * @return
	 */
	public String getLoginViewName() {
		return loginViewName;
	}

	public void setLoginViewName(String loginViewName) {
		this.loginViewName = loginViewName;
	}

	/**
	 * 获取令牌有效期，单位为分钟
	 * 
	 * @return
	 */
	public int getTokenTimeout() {
		return tokenTimeout;
	}

	public void setTokenTimeout(int tokenTimeout) {
		this.tokenTimeout = tokenTimeout;
	}

	/**
	 * 客户端系统列表
	 * 
	 * @return
	 */
	public List<ClientSystem> getClientSystems() {
		return clientSystems;
	}

	public void setClientSystems(List<ClientSystem> clientSystems) {
		this.clientSystems = clientSystems;
	}

	/**
	 * 获取指定用户的可用系统列表
	 * 
	 * @param loginUser
	 * @return
	 * @throws Exception
	 */
	public List<ClientSystem> getClientSystems(LoginUser loginUser)
			throws Exception {
		Set<String> authedSysIds = getAuthenticationHandler().authedSystemIds(
				loginUser);

		// null表示允许全部
		if (authedSysIds == null) {
			return clientSystems;
		}

		List<ClientSystem> auhtedSystems = new ArrayList<ClientSystem>();
		for (ClientSystem clientSystem : clientSystems) {
			if (authedSysIds.contains(clientSystem.getId())) {
				auhtedSystems.add(clientSystem);
			}
		}

		return auhtedSystems;
	}

	@Override
	public void setResourceLoader(ResourceLoader loader) {
		this.resourceLoader = loader;
	}

	public boolean isSecureMode() {
		return secureMode;
	}

	public int getAutoLoginExpDays() {
		return autoLoginExpDays;
	}

}
