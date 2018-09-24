package com.dream.quickfix.server;

import org.apache.log4j.PropertyConfigurator;
import quickfix.*;
import quickfix.mina.acceptor.DynamicAcceptorSessionProvider;

import java.net.InetSocketAddress;
import java.util.*;

import static quickfix.Acceptor.SETTING_ACCEPTOR_TEMPLATE;
import static quickfix.Acceptor.SETTING_SOCKET_ACCEPT_ADDRESS;
import static quickfix.Acceptor.SETTING_SOCKET_ACCEPT_PORT;

/**
 * quickFix 服务端程序
 *
 * @author Harry
 */
public class FIXServer {

	private static ThreadedSocketAcceptor acceptor = null;

	public static ThreadedSocketAcceptor getAcceptor() {
		return acceptor;
	}

	private final Map<InetSocketAddress, List<DynamicAcceptorSessionProvider.TemplateMapping>> dynamicSessionMappings = new HashMap<>();

	public Map<InetSocketAddress, List<DynamicAcceptorSessionProvider.TemplateMapping>> getDynamicSessionMappings() {
		return dynamicSessionMappings;
	}


	/**
	 * 构造函数 指定配置文件启动
	 *
	 * @throws ConfigError
	 * @throws FieldConvertError
	 */
	public FIXServer(String profile) throws ConfigError, FieldConvertError {
		//1. 设置配置文件
		SessionSettings settings = new SessionSettings(profile);

		//设置一个Application
		Application application = new FixServerApplication();

		/**
		 * quickfix.MessageStore 有2种实现方式: quickfix.JdbcStore, quickfix.FileStore
		 * JdbcStoreFactory 负责创建JdbcStore
		 * FileStoreFactory 负责创建FileStore
		 * quickFix 默认用文件存储, 因为文件存储效率高
		 */
		MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);

		LogFactory logFactory = new FileLogFactory(settings);

		MessageFactory messageFactory = new DefaultMessageFactory();

		acceptor = new ThreadedSocketAcceptor(application, messageStoreFactory, settings, logFactory, messageFactory);

		configureDynamicSessions(settings, application, messageStoreFactory, logFactory, messageFactory);
	}

	private void startServer() throws RuntimeError, ConfigError {
		acceptor.start();
	}

	public void stop() {
		acceptor.stop();
	}

	/**
	 * 调用startServer方法
	 *
	 * @throws ConfigError
	 * @throws FieldConvertError
	 */
	public static void start() throws ConfigError, FieldConvertError {
		FIXServer fixServer = new FIXServer("src/main/resources/quickfix-server.properties");
		fixServer.startServer();
	}

	/**
	 * 测试本地方法
	 *
	 * @param args
	 */
	public static void main(String[] args) throws ConfigError, FieldConvertError {
		//配置LOG日志
		PropertyConfigurator.configure("src/main/resources/log4j.properties");

		//启动
		start();
	}

	/**
	 * 以下几个方法时配置动态SessionProvider
	 * FIX 支持同时支持不同的SessionTemplate, 使用不同的数据处理provider
	 * 体现在配置文件中的session 标签
	 *
	 * @param settings
	 * @param application
	 * @param messageStoreFactory
	 * @param logFactory
	 * @param messageFactory
	 * @throws ConfigError
	 * @throws FieldConvertError
	 */
	private void configureDynamicSessions(SessionSettings settings, Application application, MessageStoreFactory messageStoreFactory, LogFactory logFactory, MessageFactory messageFactory) throws ConfigError, FieldConvertError {
		//获取配置文件中的session标签集合
		Iterator<SessionID> sessionIDIterator = settings.sectionIterator();
		while (sessionIDIterator.hasNext()) {
			SessionID sessionID = sessionIDIterator.next();
			//判断是否使用了AcceptorTemplate
			if (isSessionTemplate(settings, sessionID)) {
				InetSocketAddress address = getAcceptSocketAddress(settings, sessionID);
				getMappings(address).add(new DynamicAcceptorSessionProvider.TemplateMapping(sessionID, sessionID));
			}
		}

		for (Map.Entry<InetSocketAddress, List<DynamicAcceptorSessionProvider.TemplateMapping>> entry : dynamicSessionMappings.entrySet()) {
			acceptor.setSessionProvider(entry.getKey(), new DynamicAcceptorSessionProvider(settings, entry.getValue(), application, messageStoreFactory, logFactory, messageFactory));
		}
	}

	private boolean isSessionTemplate(SessionSettings sessionSettings, SessionID sessionID) throws ConfigError, FieldConvertError {
		return sessionSettings.isSetting(sessionID, SETTING_ACCEPTOR_TEMPLATE) && sessionSettings.getBool(sessionID, SETTING_ACCEPTOR_TEMPLATE);
	}

	private InetSocketAddress getAcceptSocketAddress(SessionSettings settings, SessionID sessionID) throws ConfigError, FieldConvertError {
		String hostname = "0.0.0.0";
		if (settings.isSetting(sessionID, SETTING_SOCKET_ACCEPT_ADDRESS)) {
			hostname = settings.getString(sessionID, SETTING_SOCKET_ACCEPT_ADDRESS);
		}

		int port = (int) settings.getLong(sessionID, SETTING_SOCKET_ACCEPT_PORT);

		InetSocketAddress address = new InetSocketAddress(hostname, port);
		return address;
	}

	private List<DynamicAcceptorSessionProvider.TemplateMapping> getMappings(InetSocketAddress address) {
		List<DynamicAcceptorSessionProvider.TemplateMapping> templateMappings = dynamicSessionMappings.get(address);
		if (templateMappings == null) {
			templateMappings = new ArrayList<>();
			dynamicSessionMappings.put(address, templateMappings);
		}

		return templateMappings;
	}


}
