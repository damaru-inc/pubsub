package com.solace.pubsub.service;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.solace.pubsub.model.SolaceClient;
import com.solace.pubsub.service.Solace;

public class SolaceTest {

	private Logger log = LogManager.getLogger(SolaceTest.class);

	protected static final String HOST = "192.168.133.44";
	protected static final String VPN_NAME = "default";
	protected static final String USERNAME = "admin";
	protected static final String PASSWORD = "admin";
	
	protected static final String QUEUE_NAME_1 = "queue1";
	protected static final String QUEUE_NAME_2 = "queue2";
	protected static final String TOPIC_NAME_1 = "topics/topic1";
	protected static final String TOPIC_NAME_2 = "topics/topic2";

	//@Test
	public void testSolace() throws Exception {
		log.info("SolaceTest");
		SolaceClient client1 = null;
		SolaceClient client2 = null;
		Solace solace = new Solace();
		solace.init(HOST, USERNAME, PASSWORD);
		try {
			solace.createUsernames();
			//log.info("Deleting queue...");
			//solace.deleteQueue(Solace.QUEUE_NAME);
			solace.deleteQueue("testQueue");
			log.info("Creating queue...");
			solace.createQueue(QUEUE_NAME_1, TOPIC_NAME_1);
			log.info("Creating clients...");
			client1 = new SolaceClient(HOST, VPN_NAME, "client1", null);
			client2 = new SolaceClient(HOST, VPN_NAME, "client2", null);
			log.info("Subscribing...");
			client2.subscribe(QUEUE_NAME_1);
			log.info("Sending message...");
			client1.sendMessage(TOPIC_NAME_1, "Hello There! " + (new Date()));
			log.info("Done.");
		} finally {
			
			try {
				Thread.sleep(2000);
				client1.close();
			} finally {
				client2.close();
			}
		}
	}
	
	@Test
	public void showConfig() throws Exception {
        Solace solace = new Solace();
        solace.init(HOST, USERNAME, PASSWORD);
        solace.getQueues();
        solace.listClientUsernames();
        solace.getSubscriptions();
	}
}
