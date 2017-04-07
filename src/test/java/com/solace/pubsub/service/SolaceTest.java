package com.solace.pubsub.service;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.solace.pubsub.model.SolaceClient;
import com.solace.pubsub.service.Solace;

public class SolaceTest {

	private Logger log = LogManager.getLogger(SolaceTest.class);

	public String host = "192.168.132.27";
	public String vpnName = "default";
	public String username = "admin";
	public String password = "admin";

	@Test
	public void testSolace() throws Exception {
		log.info("SolaceTest");
		SolaceClient client1 = null;
		SolaceClient client2 = null;
		Solace solace = new Solace();
		solace.init(host, username, password);
		try {
			solace.createUsernames();
			//log.info("Deleting queue...");
			//solace.deleteQueue(Solace.QUEUE_NAME);
			log.info("Creating queue...");
			solace.createQueue(Solace.QUEUE_NAME, Solace.TOPIC);
			log.info("Creating clients...");
			client1 = new SolaceClient(host, vpnName, "client1", null);
			client2 = new SolaceClient(host, vpnName, "client2", null);
			log.info("Subscribing...");
			client2.subscribe(Solace.QUEUE_NAME);
			log.info("Sending message...");
			client1.sendMessage(Solace.TOPIC, "Hello There! " + (new Date()));
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
}
