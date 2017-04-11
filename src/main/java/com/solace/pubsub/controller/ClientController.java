package com.solace.pubsub.controller;

import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.solace.pubsub.model.SolaceClient;
import com.solace.pubsub.model.SolaceQueue;
import com.solace.pubsub.service.Solace;
import com.solacesystems.jcsmp.JCSMPException;

import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

@Component
public class ClientController implements Initializable {

    private Logger log = LogManager.getLogger(ClientController.class);

    @FXML
    TableView<SolaceQueue> browserTableView;
    @FXML
    TextField delay;
    @FXML
    TextField numMessages;
    @FXML
    ObservableList<String> receivedMessages;
    @FXML
    ListView<String> receivedMessagesListView;
    @FXML
    Label sendResult;
    @FXML
    Label subscribeResult;
    @FXML
    ComboBox<String> sendTopicComboBox;
    @FXML
    ComboBox<SolaceQueue> subscribeComboBox;

    @Autowired
    Solace solace;
    @Autowired
    ConfigController configController;

    private SolaceClient sender;
    private SolaceClient receiver;
    private HashMap<SolaceQueue, SolaceClient> browsers = new HashMap<>();
    //List<Object> objList = Collections.synchronizedList(new ArrayList<Object>());
    private long lastUpdate = 0L;
    private long updateInterval = 500_000_000L; // in nanoseconds - .25 seconds
    private Random rand = new Random();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            sender = new SolaceClient(solace.getHost(), Solace.MSG_VPN_NAME, "user1", null);
            receiver = new SolaceClient(solace.getHost(), Solace.MSG_VPN_NAME, "user2", null);
            receivedMessages = FXCollections.observableArrayList();
            receivedMessagesListView.setItems(receivedMessages);
            log.debug("configController: " + configController);
            sendTopicComboBox.setItems(configController.getTopics());
            subscribeComboBox.setItems(configController.getSolaceQueues());
            sendResult.setText("");
            subscribeResult.setText("");
            numMessages.setText("100");
            delay.setText("0");

            TableColumn<SolaceQueue, String> nameCol = new TableColumn<SolaceQueue, String>("Queue");
            nameCol.setCellValueFactory(new PropertyValueFactory("name"));
            TableColumn<SolaceQueue, String> topicCol = new TableColumn<SolaceQueue, String>("Topic");
            topicCol.setCellValueFactory(new PropertyValueFactory("topic"));
            TableColumn<SolaceQueue, Integer> countCol = new TableColumn<SolaceQueue, Integer>("Messages");
            countCol.setCellValueFactory(new PropertyValueFactory("numMessages"));

            browserTableView.getColumns().setAll(nameCol, topicCol, countCol);
            
            AnimationTimer timer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    long gap = now - lastUpdate;
                    //log.debug("now: " + now + " gap: " + gap);
                    if (gap > updateInterval) {
                        lastUpdate = now;
                        List<String> messages = receiver.getMessages();
                        int lastSize = receivedMessages.size();
                        receivedMessages.setAll(messages);
                        int size = receivedMessages.size();
                        if (size > 0 && size > lastSize) {
                            receivedMessagesListView.scrollTo(receivedMessages.size() - 1);
                        }

                        try {
                            ObservableList<SolaceQueue> queues = configController.getSolaceQueues();
                            for (SolaceQueue queue : queues) {
                                SolaceClient browser = browsers.get(queue);
                                if (browser == null) {
                                    browser = new SolaceClient(solace.getHost(), Solace.MSG_VPN_NAME, "user1", null);
                                    browsers.put(queue, browser);
                                }
                                int count = browser.getMessageCount(queue.getName());
                                queue.setNumMessages(count);
                                //log.info("queue: " + queue.getName() + ": " + count);
                            }
                            browserTableView.setItems(queues);
                            browserTableView.refresh();
                        } catch (JCSMPException e) {
                            log.error(e);
                        }
                    }
                }
            };

            timer.start();

        } catch (JCSMPException e) {
            // TODO Auto-generated catch block
            log.error(e);
        }
    }

    public void send(ActionEvent event) {
        sendResult.setText("");
        String topic = sendTopicComboBox.getValue();
            try {
                int num = Integer.parseInt(numMessages.getText());
                int del = Integer.parseInt(delay.getText());
                List<String> topics = configController.getTopics();
                int numTopics = (topics != null ? topics.size() : 0);
                Runnable task = () -> {
                for (int i = 0; i < num; i++) {
                    String top = topic;
                    if (topic == null || topic.equals("") && numTopics > 0) {
                        int pick = rand.nextInt(numTopics);
                        top = topics.get(pick);
                    }

                    Date now = new Date();
                    String message = top + " " + i + " : " + now;
                    try {
                        sender.sendMessage(top, message);
                        if (del != 0) {
                            Thread.sleep(del);
                        }
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                };
                Thread thread = new Thread(task);
                thread.start();
            } catch (Exception e) {
                log.error(e);
                sendResult.setText(e.getMessage());
            }
    }

    public void subscribe(ActionEvent event) {
        SolaceQueue queue = subscribeComboBox.getValue();
        if (queue != null) {
            try {
                receiver.clearMessages();
                receiver.subscribe(queue.getName());
                subscribeResult.setText("Subscribed to " + queue);
            } catch (JCSMPException e) {
                log.error(e);
                subscribeResult.setText(e.getMessage());
            }
        }
    }

    public void close() {
        if (receiver != null) {
            receiver.close();
            sender.close();
        }
    }

    public void clear(ActionEvent event) {
        receiver.clearMessages();
    }

}
