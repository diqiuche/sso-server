package com.lyc.sso.server;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 通过静态成员持有applicationContext，以便在应用任意代码中可访问context
 * 
 * @author Administrator
 *
 */
@Component
public class SpringContextUtil implements ApplicationContextAware {

	private static ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		SpringContextUtil.applicationContext = applicationContext;
	}

	/**
	 * 获取ApplicationContext
	 * 
	 * @return
	 */
	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	/**
	 * 按名称获取spring bean实例
	 * 
	 * @param beanName
	 * @return
	 */
	public static Object getBean(String beanName) {
		return applicationContext.getBean(beanName);
	}

	/**
	 * 按类型获取spring bean 实例
	 * 
	 * @param beanType
	 * @return
	 */
	public static <T> T getBean(Class<T> beanType) {
		return applicationContext.getBean(beanType);
	}
}
