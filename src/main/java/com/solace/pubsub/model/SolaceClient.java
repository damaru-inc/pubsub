package com.solace.pubsub.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.solace.pubsub.service.Solace;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;

public class SolaceClient {
	private Logger log = LogManager.getLogger(SolaceClient.class);
	
	private String username;
	private String password;
	private String vpnName;
	private String host;
	private JCSMPSession session;
	private XMLMessageProducer producer;
	private TextMessage lastReceivedMessage;
	private FlowReceiver receiver;
	
	public SolaceClient(String host, String vpnName, String username, String password) throws JCSMPException {
		this.host = host;
		this.vpnName = vpnName;
		this.username = username;
		this.password = password;
		connect();
	}
	
	private class SimplePublisherEventHandler implements JCSMPStreamingPublishEventHandler {
		@Override
		public void responseReceived(String messageID) {
			log.info("Producer received response for msg: " + messageID);
		}

		@Override
		public void handleError(String messageID, JCSMPException e, long timestamp) {
			log.error("Producer received error for msg: " + messageID + " - " + timestamp, e);
		}

	}

	private class SimpleMessageListener implements XMLMessageListener {

		@Override
		public void onReceive(BytesXMLMessage receivedMessage) {

			if (receivedMessage instanceof TextMessage) {
				lastReceivedMessage = (TextMessage) receivedMessage;
				log.info("Received message : " + lastReceivedMessage.getText());
				receivedMessage.ackMessage();
			} else {
				log.error("Received message that was not a TextMessage: " + receivedMessage.dump());
			}
		}

		@Override
		public void onException(JCSMPException e) {
			log.error("Consumer received exception: %s%n", e);
		}
	}

	public void connect() throws JCSMPException {
		final JCSMPProperties properties = new JCSMPProperties();
		properties.setProperty(JCSMPProperties.HOST, host);
		properties.setProperty(JCSMPProperties.VPN_NAME, vpnName);
		properties.setProperty(JCSMPProperties.USERNAME, username);
		properties.setProperty(JCSMPProperties.PASSWORD, password);
		session = JCSMPFactory.onlyInstance().createSession(properties);
		//session.connect();
		//Queue endpoint = new Queue();

		//session.cre
		//final XMLMessageConsumer cons = session.getMessageConsumer(new SimpleMessageListener());
		//cons.start();
	}
	
	public void subscribe(String queueName) throws JCSMPException {
		Queue testQueue = JCSMPFactory.onlyInstance().createQueue(Solace.QUEUE_NAME);
		ConsumerFlowProperties props = new ConsumerFlowProperties();
		props.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);
		props.setEndpoint(testQueue);
		//receiver = session.createFlow(testQueue, null, new SimpleMessageListener());
		receiver = session.createFlow(new SimpleMessageListener(), props);
		receiver.start();		
	}

	public void close() {
		if (receiver != null) {
			receiver.close();
		}
		session.closeSession();
	}

	public void sendMessage(String topic, String message) throws JCSMPException {
		
		if (producer == null) {
			producer = session.getMessageProducer(new SimplePublisherEventHandler());			
		}
		
		final Topic tp = JCSMPFactory.onlyInstance().createTopic(topic);
		TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
		msg.setText(message);
		producer.send(msg, tp);
	}

	public String getUsername() {
		return username;
	}

}
