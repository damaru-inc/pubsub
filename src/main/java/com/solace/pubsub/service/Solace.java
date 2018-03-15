package com.solace.pubsub.service;

import java.io.IOException;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.solace.pubsub.model.SolaceQueue;
import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

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
    private static final String SEMP_QUEUE_START = "<rpc semp-version=\"soltr/8_2VMR\"><show><queue><name>";
    private static final String SEMP_QUEUE_END = "</name></queue></show></rpc>";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    private MsgVpnApi sempApiInstance;
    private String managementHost;
    private Integer managementPort;
    private String vpn;
    private String username;
    private String password;
    private OkHttpClient httpClient;
    private String sempUrl;

    public void init(String managementHost, Integer managementPort, String vpn, String username, String password, boolean useSecure) {
        this.managementHost = managementHost;
        this.managementPort = managementPort;
        this.username = username;
        this.password = password;
        this.vpn = vpn;
        String scheme = useSecure ? "https" : "http";
        sempUrl = scheme + "://" + managementHost + ":" + managementPort + "/SEMP";
        ApiClient client = new ApiClient();
        client.setBasePath(sempUrl + "/v2/config");
        client.setUsername(username);
        client.setPassword(password);
        sempApiInstance = new MsgVpnApi(client);
        log.debug("init - sempUrl: " + sempUrl);
    }

    private void setupHttpClient() {
        httpClient = new OkHttpClient();
        httpClient.setAuthenticator(new Authenticator() {
            public Request authenticate(Response response) throws IOException {
                String credential = Credentials.basic(username, password);
                return response.request().newBuilder().header("Authorization", credential).build();
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
    }

    public void getStats(SolaceQueue queue) throws IOException {
        if (httpClient == null) {
            setupHttpClient();
        }
        String semp = SEMP_QUEUE_START + queue.getName() + SEMP_QUEUE_END;
        RequestBody body = RequestBody.create(JSON, semp);
        Request request = new Request.Builder().url(sempUrl).post(body).build();
        Response response = httpClient.newCall(request).execute();
        String resp = response.body().string();
        //log.info(resp);

        int entity = resp.indexOf("num-messages-spooled");
        int start = resp.indexOf(">", entity);
        int end = resp.indexOf("<", start);
        String num = resp.substring(start + 1, end);
        queue.setNumMessages(Integer.valueOf(num));

        entity = resp.indexOf("current-spool-usage-in-mb");
        start = resp.indexOf(">", entity);
        end = resp.indexOf("<", start);
        num = resp.substring(start + 1, end);
        queue.setCurrentUsage(Double.valueOf(num));
        
        entity = resp.indexOf("high-water-mark-in-mb");
        start = resp.indexOf(">", entity);
        end = resp.indexOf("<", start);
        num = resp.substring(start + 1, end);
        queue.setHighWaterMark(Double.valueOf(num));
        
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
        MsgVpnClientUsernamesResponse resp = sempApiInstance.getMsgVpnClientUsernames(vpn, null, null, null,
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
            MsgVpnClientUsernameResponse resp = sempApiInstance.getMsgVpnClientUsername(vpn, username, null);
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
                resp = sempApiInstance.createMsgVpnClientUsername(vpn, newClientUsername, null);
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
            sempApiInstance.deleteMsgVpnQueue(vpn, queueName);
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
            MsgVpnQueueResponse resp = sempApiInstance.getMsgVpnQueue(vpn, queueName, null);
            queue = resp.getData();
        } catch (ApiException e) {
            handleError(e);
        }
        return queue;
    }

    public List<MsgVpnQueue> getQueues() {
        List<MsgVpnQueue> queues = null;
        try {
            MsgVpnQueuesResponse resp = sempApiInstance.getMsgVpnQueues(vpn, 100, null, null, null);
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
            MsgVpnQueuesResponse resp = sempApiInstance.getMsgVpnQueues(vpn, 100, null, null, null);
            List<MsgVpnQueue> queues = resp.getData();
            for (MsgVpnQueue queue : queues) {
                MsgVpnQueueSubscriptionsResponse sresp = sempApiInstance.getMsgVpnQueueSubscriptions(vpn,
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
            sempApiInstance.createMsgVpnQueue(vpn, queue, null);
            log.info("Creating queue subscription: " + topic);
            sempApiInstance.createMsgVpnQueueSubscription(vpn, queueName, subscription, null);
            log.info("Finished creating queue and subscription.");
        }
    }

    private void handleError(ApiException ae) {
        String responseString = ae.getResponseBody();
        log.error("ApiException: " + responseString, ae);
    }

    public String getHost() {
        return managementHost;
    }

}
