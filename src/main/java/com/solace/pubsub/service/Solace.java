package com.solace.pubsub.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.MsgVpnApi;
import io.swagger.client.model.MsgVpnClientUsername;
import io.swagger.client.model.MsgVpnClientUsernameResponse;
import io.swagger.client.model.MsgVpnClientUsernamesResponse;
import io.swagger.client.model.MsgVpnQueue;
import io.swagger.client.model.MsgVpnQueue.PermissionEnum;
import io.swagger.client.model.MsgVpnQueueResponse;
import io.swagger.client.model.MsgVpnQueueSubscription;
import io.swagger.client.model.MsgVpnQueueSubscriptionsResponse;
import io.swagger.client.model.MsgVpnQueuesResponse;

@Component
public class Solace {
	private Logger log = LogManager.getLogger(Solace.class);

	private MsgVpnApi sempApiInstance;
	public static final String MSG_VPN_NAME = "default";
	private String host;

	public void init(String host, String username, String password) {
	    this.host = host;
		ApiClient client = new ApiClient();
		client.setBasePath("http://" + host + ":8080/SEMP/v2/config");
		client.setUsername(username);
		client.setPassword(password);
		sempApiInstance = new MsgVpnApi(client);
	}

	public boolean test() {
		boolean ret = true;
		try {
			listClientUsernames();
		} catch (ApiException e) {
			handleError(e);
			ret = false;
		}
		return ret;
	}

	public void listClientUsernames() throws ApiException {
		// Ignore paging and selectors in this example. So set to null.
		MsgVpnClientUsernamesResponse resp = sempApiInstance.getMsgVpnClientUsernames(MSG_VPN_NAME, null, null, null,
				null);
		List<MsgVpnClientUsername> clientUsernamesList = resp.getData();
		for (MsgVpnClientUsername user : clientUsernamesList) {
			log.info("user: " + user.getClientUsername() + " password: " + user.getPassword());
		}
		log.info("Retrieved " + clientUsernamesList.size() + " Client Usernames.");
	}

	public MsgVpnClientUsername getClientUsername(String username) {
		MsgVpnClientUsername ret = null;
		try {
			MsgVpnClientUsernameResponse resp = sempApiInstance.getMsgVpnClientUsername(MSG_VPN_NAME, username, null);
			ret = resp.getData();
			System.out.println("getClientUsername: " + ret);
		} catch (ApiException e) {
			System.out.println("getClientUsername error: " + ret);
			handleError(e);
		}
		return ret;
	}

	public void createUsername(String username) {

		MsgVpnClientUsername clientUsername = getClientUsername(username);
		log.info("createUsername: checking if " + username + " exists: " + clientUsername);

		if (clientUsername == null) {
			MsgVpnClientUsername newClientUsername = new MsgVpnClientUsername();
			newClientUsername.setClientUsername(username);
			newClientUsername.setEnabled(true);
			MsgVpnClientUsernameResponse resp = null;
			try {
				resp = sempApiInstance.createMsgVpnClientUsername(MSG_VPN_NAME, newClientUsername, null);
			} catch (ApiException e) {
				handleError(e);
				return;
			}

			MsgVpnClientUsername createdClientUsername = resp.getData();
			log.info("Created Client Username: " + createdClientUsername);
		} else {
			log.info("User already existed: " + username);
		}
	}

	public void createUsernames() {
		createUsername("user1");
		createUsername("user2");
	}

	public void deleteQueue(String queueName) {
		try {
			sempApiInstance.deleteMsgVpnQueue(MSG_VPN_NAME, queueName);
		} catch (ApiException e) {
			handleError(e);
		}
	}
	
	public void deleteQueues() {
	    List<MsgVpnQueue> queues = getQueues();

	    for (MsgVpnQueue queue : queues) {
            deleteQueue(queue.getQueueName());
        }
	}

	public MsgVpnQueue getQueue(String queueName) {
		MsgVpnQueue queue = null;
		try {
			MsgVpnQueueResponse resp = sempApiInstance.getMsgVpnQueue(MSG_VPN_NAME, queueName, null);
			queue = resp.getData();
		} catch (ApiException e) {
			handleError(e);
		}
		return queue;
	}

    public List<MsgVpnQueue> getQueues() {
        List<MsgVpnQueue> queues = null;
        try {
            MsgVpnQueuesResponse resp = sempApiInstance.getMsgVpnQueues(MSG_VPN_NAME, 100, null, null, null);
            queues = resp.getData();
            for (MsgVpnQueue queue : queues) {
                log.info("Retrieved queue " + queue.getQueueName());
            }
        } catch (ApiException e) {
            handleError(e);
        }
        return queues;
    }

    public List<MsgVpnQueueSubscription> getSubscriptions() {
        List<MsgVpnQueueSubscription> ret = new ArrayList<>();
        try {
            MsgVpnQueuesResponse resp = sempApiInstance.getMsgVpnQueues(MSG_VPN_NAME, 100, null, null, null);
            List<MsgVpnQueue> queues = resp.getData();
            for (MsgVpnQueue queue : queues) {
                MsgVpnQueueSubscriptionsResponse sresp = sempApiInstance.getMsgVpnQueueSubscriptions(MSG_VPN_NAME, 
                        queue.getQueueName(), 100, null, null, null);
                List<MsgVpnQueueSubscription> subscriptions = sresp.getData();
                for (MsgVpnQueueSubscription sub : subscriptions) {
                    log.info("Subscription: " + sub.getQueueName() + " " + sub.getSubscriptionTopic());
                    ret.add(sub);
                }                
            }
        } catch (ApiException e) {
            handleError(e);
        }
        return ret;
    }

	public void createQueue(String queueName, String topic) throws ApiException {
		MsgVpnQueue queue = getQueue(queueName);
		if (queue == null) {
			log.info("Creating queue: " + queueName);
			queue = new MsgVpnQueue();
			queue.setQueueName(queueName);
			queue.setIngressEnabled(true);
			queue.setEgressEnabled(true);
			queue.permission(PermissionEnum.CONSUME);
			MsgVpnQueueSubscription subscription = new MsgVpnQueueSubscription();
			subscription.setSubscriptionTopic(topic);
			sempApiInstance.createMsgVpnQueue(MSG_VPN_NAME, queue, null);
			log.info("Creating queue subscription: " + topic);
			sempApiInstance.createMsgVpnQueueSubscription(MSG_VPN_NAME, queueName, subscription, null);
			log.info("Finished creating queue and subscription.");
		}
	}

	private void handleError(ApiException ae) {
		String responseString = ae.getResponseBody();
		log.error("ApiException: " + responseString);
	}
	
	public String getHost() {
	    return host;
	}
	
}
