package com.dream.quickfix.server;

import quickfix.*;
import quickfix.field.MsgType;


/**
 * MessageCracker 是一个工具类, 通过继承MessageCracker可以覆盖onMessage方法
 *
 * @author Harry
 */
public class FixServerApplication extends MessageCracker implements Application {

	@Override
	protected void onMessage(Message message, SessionID sessionID) {
		System.out.println("业务逻辑实现统一写在此方法中...");
		try {
			String msgType = message.getHeader().getString(35);
			System.out.println("服务器收到的用户信息订阅:" + msgType);

			Session session = Session.lookupSession(sessionID);

			switch (msgType) {
				case MsgType.LOGON:
					session.logon();
					session.sentLogon();
					break;
				case MsgType.HEARTBEAT:
					session.generateHeartbeat();
					;
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 当一个Fix Session 建立时调用
	 *
	 * @param sessionID
	 */
	@Override
	public void onCreate(SessionID sessionID) {

	}

	/**
	 * 当一个Fix Session 登陆成功时调用
	 *
	 * @param sessionID
	 */
	@Override
	public void onLogon(SessionID sessionID) {
		System.out.println("客户端登陆成功时候调用此方法");
	}

	/**
	 * 当一个Fix Session推出时调用
	 *
	 * @param sessionID
	 */
	@Override
	public void onLogout(SessionID sessionID) {
		System.out.println("客户端断开连接时候调用此方法");
	}

	/**
	 * 当发送一个admin类型消息调用toApp -> 当发送一个非admin(业务型)消息调用
	 *
	 * @param message
	 * @param sessionID
	 */
	@Override
	public void toAdmin(Message message, SessionID sessionID) {
		System.out.println("发送会话消息时候调用此方法");
	}

	/**
	 * 当收到一个消息, 经过一系列检查合格后. 属于Admin类型的时候调用
	 *
	 * @param message
	 * @param sessionID
	 * @throws FieldNotFound
	 * @throws IncorrectDataFormat
	 * @throws IncorrectTagValue
	 * @throws RejectLogon
	 */
	@Override
	public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		System.out.println("接收会话类型消息时调用此方法...");

		try {
			crack(message, sessionID);
		} catch (UnsupportedMessageType | FieldNotFound | IncorrectTagValue e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param message
	 * @param sessionID
	 * @throws DoNotSend
	 */
	@Override
	public void toApp(Message message, SessionID sessionID) throws DoNotSend {
		System.out.println("发送业务消息时候调用此方法");
	}

	/**
	 * 当收到一个消息时，经过一系列的检查合格后，不属于admin类型的时候调用
	 *
	 * @param message
	 * @param sessionID
	 * @throws FieldNotFound
	 * @throws IncorrectDataFormat
	 * @throws IncorrectTagValue
	 * @throws UnsupportedMessageType
	 */
	@Override
	public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
		System.out.println("接收业务消息时调用此方法...");
		crack(message, sessionID);
	}
}
