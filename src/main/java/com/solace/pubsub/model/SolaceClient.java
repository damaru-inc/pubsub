package com.solace.pubsub.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.solacesystems.jcsmp.Browser;
import com.solacesystems.jcsmp.BrowserProperties;
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
    private String vpn;
    private String host;
    private Integer port;
    private JCSMPSession session;
    private XMLMessageProducer producer;
    ConcurrentLinkedDeque<String> messages = new ConcurrentLinkedDeque<>();
    private FlowReceiver receiver;

    public SolaceClient(String host, Integer port, String vpn, String username, String password) throws JCSMPException {
        this.host = host;
        this.port = port;
        this.vpn = vpn;
        this.username = username;
        this.password = password;
        connect();
    }

    private class SimplePublisherEventHandler implements JCSMPStreamingPublishEventHandler {
        @Override
        public void responseReceived(String messageID) {
            log.trace("Producer received response for msg: " + messageID);
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
                messages.add(((TextMessage) receivedMessage).getText());
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
        properties.setProperty(JCSMPProperties.HOST, host + ":" + port);
        properties.setProperty(JCSMPProperties.VPN_NAME, vpn);
        properties.setProperty(JCSMPProperties.USERNAME, username);
        properties.setProperty(JCSMPProperties.PASSWORD, password);
        session = JCSMPFactory.onlyInstance().createSession(properties);
    }

    public void subscribe(String queueName) throws JCSMPException {
        Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);

        ConsumerFlowProperties props = new ConsumerFlowProperties();
        props.setEndpoint(queue);
        unsubscribe();
        receiver = session.createFlow(new SimpleMessageListener(), props);
        receiver.start();
    }
    
    public void unsubscribe() {
        if (receiver != null) {
            receiver.close();
        }        
    }

    public int getMessageCount(String queueName) throws JCSMPException {
        int ret = 0;
        Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);
        BrowserProperties props = new BrowserProperties();
        props.setEndpoint(queue);
        props.setWaitTimeout(50);
        Browser browser = session.createBrowser(props);
        BytesXMLMessage message = browser.getNext();
        while (message != null) {
            ret++;
            message = browser.getNext();
        }
        browser.close();
        return ret;
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
    
    public String getPassword() {
        return password;
    }

    public void clearMessages() {
        messages.clear();
    }

    public ConcurrentLinkedDeque<String> getMessages() {
        return messages;
    }

}
