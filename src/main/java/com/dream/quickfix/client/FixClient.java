package com.dream.quickfix.client;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.NewOrderSingle;

import java.io.FileNotFoundException;

public class FixClient implements Application {

	private static volatile SessionID sessionID;

	@Override
	public void onCreate(SessionID sessionID) {
		System.out.println("当一个Fix Session 建立时调用");
	}

	@Override
	public void onLogon(SessionID sessionID) {
		System.out.println("当一个Fix Session 登陆成功时调用");
		FixClient.sessionID = sessionID;
	}

	@Override
	public void onLogout(SessionID sessionID) {
		System.out.println("客户端断开连接时候调用此方法");
		FixClient.sessionID = null;
	}

	@Override
	public void toAdmin(Message message, SessionID sessionID) {
		System.out.println("toAdmin");
	}

	@Override
	public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		System.out.println("fromAdmin");
	}

	@Override
	public void toApp(Message message, SessionID sessionID) throws DoNotSend {
		System.out.println("toApp: " + message);
	}

	@Override
	public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
		System.out.println("FromApp");
	}

	public static void main(String[] args) throws ConfigError, FileNotFoundException, InterruptedException, SessionNotFound {
		SessionSettings settings = new SessionSettings("src/main/resources/quickfix-client.properties");

		Application clientApplication = new FixClient();
		MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
		LogFactory logFactory = new ScreenLogFactory(true, true, true);
		MessageFactory messageFactory = new DefaultMessageFactory();

		ThreadedSocketInitiator threadedSocketInitiator = new ThreadedSocketInitiator(clientApplication, messageStoreFactory, settings, logFactory, messageFactory);
		threadedSocketInitiator.start();

		while(sessionID ==null){
			Thread.sleep(1000);
		}

		sendMockMessage();

		Thread.sleep(5000);
	}

	private static void sendMockMessage() throws SessionNotFound{
		final String orderId = "342";
		NewOrderSingle newOrderSingle = new NewOrderSingle();
		newOrderSingle.set(new ClOrdID("qsd"));
		newOrderSingle.set(new OrderQty(1));
		newOrderSingle.set(new OrdType('2'));
		newOrderSingle.set(new Price(10));
		newOrderSingle.set(new Side('1'));
		newOrderSingle.set(new Symbol("LTC/CNY"));
		newOrderSingle.set(new TransactTime());

		Session.sendToTarget(newOrderSingle, sessionID);

	}
}
