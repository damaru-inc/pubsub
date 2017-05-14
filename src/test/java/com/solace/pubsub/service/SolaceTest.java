package com.solace.pubsub.service;

import java.io.IOException;
import java.net.Proxy;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.solace.pubsub.model.SolaceClient;
import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

public class SolaceTest {

	private Logger log = LogManager.getLogger(SolaceTest.class);

	protected static final String HOST = "192.168.133.44";
    protected static final Integer PORT = 7000;
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
		solace.init(HOST, PORT, VPN_NAME, USERNAME, PASSWORD);
		try {
			solace.createUsernames();
			//log.info("Deleting queue...");
			//solace.deleteQueue(Solace.QUEUE_NAME);
			solace.deleteQueue("testQueue");
			log.info("Creating queue...");
			solace.createQueue(QUEUE_NAME_1, TOPIC_NAME_1);
			log.info("Creating clients...");
			client1 = new SolaceClient(HOST, PORT, VPN_NAME, "client1", null);
			client2 = new SolaceClient(HOST,PORT,  VPN_NAME, "client2", null);
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
	
	//@Test
	public void showConfig() throws Exception {
        Solace solace = new Solace();
        solace.init(HOST, PORT, VPN_NAME, USERNAME, PASSWORD);
        solace.getQueues();
        solace.listClientUsernames();
        solace.getSubscriptions();
	}
	
	@Test
	public void testSemp() throws Exception {
	    String semp = "<rpc semp-version=\"soltr/8_2VMR\"><show><queue><name>queue1</name></queue></show></rpc>";
	    MediaType JSON
	    = MediaType.parse("application/json; charset=utf-8");

	OkHttpClient client = new OkHttpClient();
	client.setAuthenticator(new Authenticator() {
	    public Request authenticate(Response response) throws IOException {
            String credential = Credentials.basic("admin", "admin");
            return response.request().newBuilder()
                .header("Authorization", credential)
                .build();
          }

        @Override
        public Request authenticate(Proxy proxy, Response response) throws IOException {
            
            return authenticate(response);
        }

        @Override
        public Request authenticateProxy(Proxy proxy, Response response) throws IOException {
            return authenticate(response);
        }
	});

	String url = "http://192.168.133.44:8080/SEMP";
	        
	  RequestBody body = RequestBody.create(JSON, semp);
	  Request request = new Request.Builder()
	      .url(url)
	      .post(body)
	      .build();
	  Response response = client.newCall(request).execute();
	  String resp = response.body().string();
	  log.info(resp);
	  int spooled = resp.indexOf("num-messages-spooled");
	  int start = resp.indexOf(">", spooled);
	  int end = resp.indexOf("<", start);
	  String num = resp.substring(start+1, end);
	  log.info("spooled: " + num);

	}
}
