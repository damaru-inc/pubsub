package com.solace.pubsub.controller;

import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentLinkedDeque;

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
import javafx.scene.control.Button;
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
    Label numReceived1;
    @FXML
    Label numReceived2;
    @FXML
    ObservableList<String> receivedMessages1;
    @FXML
    ObservableList<String> receivedMessages2;
    @FXML
    ListView<String> receivedMessagesListView1;
    @FXML
    ListView<String> receivedMessagesListView2;
    @FXML
    Label sendResult;
    @FXML
    Button startButton;
    @FXML
    Button stopButton;
    @FXML
    Label subscribeResult1;
    @FXML
    Label subscribeResult2;
    @FXML
    ComboBox<String> sendTopicComboBox;
    @FXML
    ComboBox<SolaceQueue> subscribeComboBox1;
    @FXML
    ComboBox<SolaceQueue> subscribeComboBox2;

    @Autowired
    Solace solace;
    @Autowired
    ConfigController configController;

    private SolaceClient sender;
    private SolaceClient receiver1;
    private SolaceClient receiver2;
    private long lastUpdate = 0L;
    private long updateInterval = 100_000_000L; // in nanoseconds - .1 seconds
    private Random rand = new Random();
    private Thread sendThread;
    private SendRunner sendRunner;
    private boolean finishedSending = false;
    private String sendResultMessage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            sender = new SolaceClient(configController.getMessagingHost(), configController.getMessagingPort(), configController.getVpn(), configController.getClientUsername(), configController.getClientPassword());
            receiver1 = new SolaceClient(configController.getMessagingHost(), configController.getMessagingPort(), configController.getVpn(), configController.getClientUsername(), configController.getClientPassword());
            receiver2 = new SolaceClient(configController.getMessagingHost(), configController.getMessagingPort(), configController.getVpn(), configController.getClientUsername(), configController.getClientPassword());
            receivedMessages1 = FXCollections.observableArrayList();
            receivedMessagesListView1.setItems(receivedMessages1);
            receivedMessages2 = FXCollections.observableArrayList();
            receivedMessagesListView2.setItems(receivedMessages2);
            log.debug("configController: " + configController);
            sendTopicComboBox.setItems(configController.getTopics());
            subscribeComboBox1.setItems(configController.getSolaceQueues());
            subscribeComboBox2.setItems(configController.getSolaceQueues());
            sendResult.setText("");
            subscribeResult1.setText("");
            subscribeResult2.setText("");
            numMessages.setText("10");
            delay.setText("1000");

            TableColumn<SolaceQueue, String> nameCol = new TableColumn<SolaceQueue, String>("Queue");
            nameCol.setCellValueFactory(new PropertyValueFactory("name"));
            TableColumn<SolaceQueue, String> topicCol = new TableColumn<SolaceQueue, String>("Topic");
            topicCol.setCellValueFactory(new PropertyValueFactory("topic"));
            TableColumn<SolaceQueue, Integer> countCol = new TableColumn<SolaceQueue, Integer>("Messages");
            countCol.setCellValueFactory(new PropertyValueFactory("numMessages"));
            TableColumn<SolaceQueue, Double> usageCol = new TableColumn<SolaceQueue, Double>("Current Usage (MB)");
            usageCol.setCellValueFactory(new PropertyValueFactory("currentUsage"));
            TableColumn<SolaceQueue, Double> highWaterCol = new TableColumn<SolaceQueue, Double>(
                    "High Water Mark (MB)");
            highWaterCol.setCellValueFactory(new PropertyValueFactory("highWaterMark"));

            browserTableView.getColumns().setAll(nameCol, topicCol, countCol, usageCol, highWaterCol);

            AnimationTimer timer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    long gap = now - lastUpdate;
                    // log.debug("now: " + now + " gap: " + gap);
                    if (gap > updateInterval) {
                        lastUpdate = now;
                        updateReceiverList(receiver1, receivedMessages1, receivedMessagesListView1, numReceived1);
                        updateReceiverList(receiver2, receivedMessages2, receivedMessagesListView2, numReceived2);
                        
                        if (finishedSending) {
                            sendResult.setText(sendResultMessage);
                            finishedSending = false;
                        }

                        // log.info("getNumMessages start");
                        try {
                            ObservableList<SolaceQueue> queues = configController.getSolaceQueues();
                            for (SolaceQueue queue : queues) {
                                solace.getStats(queue);
                            }
                            browserTableView.setItems(queues);
                            browserTableView.refresh();
                        } catch (Exception e) {
                            stopSending();
                            log.error(e);
                        }
                        // log.info("getNumMessages end");
                    }
                }
            };

            timer.start();

        } catch (JCSMPException e) {
            // TODO Auto-generated catch block
            log.error(e);
        }
    }

    private void updateReceiverList(SolaceClient receiver, ObservableList<String> list, ListView<String> listView,
            Label label) {
        ConcurrentLinkedDeque<String> messages = receiver.getMessages();
        int numMessages = messages.size();
        label.setText(String.valueOf(numMessages));
        int oldSize = list.size();
        list.clear();
        // only display the last 1000 messages.
        if (numMessages > 1000) {
            Object[] arr = messages.toArray();
            for (int i = numMessages - 1000; i < numMessages; i++) {
                list.add(arr[i].toString());
            }
        } else {
            list.addAll(messages);
        }

        int newSize = list.size();
        if (newSize > oldSize && newSize > 0) {
            listView.scrollTo(newSize - 1);
        }
    }

    public void startSending(ActionEvent event) {
        sendResult.setText("");
        String topic = sendTopicComboBox.getValue();
        finishedSending = false;
        try {
            int num = Integer.parseInt(numMessages.getText());
            int dely = Integer.parseInt(delay.getText());
            List<String> topics = configController.getTopics();
            sendRunner = new SendRunner(topic, topics, num, dely);
            sendThread = new Thread(sendRunner);
            sendThread.start();
            startButton.setDisable(true);
            stopButton.setDisable(false);
        } catch (Exception e) {
            stopSending();
            log.error(e);
            startButton.setDisable(false);
            stopButton.setDisable(false);
            sendResult.setText(e.getMessage());
        }
    }

    public void stopSending() {
        if (sendRunner != null) {
            sendRunner.quit();
        }
    }

    private void subscribe(SolaceClient receiver, ComboBox<SolaceQueue> combo, Label label) {
        SolaceQueue queue = combo.getValue();
        if (queue != null) {
            try {
                receiver.clearMessages();
                receiver.subscribe(queue.getName());
                label.setText("Subscribed to " + queue);
            } catch (JCSMPException e) {
                log.error(e);
                label.setText(e.getMessage());
            }
        }
    }

    public void subscribe1(ActionEvent event) {
        subscribe(receiver1, subscribeComboBox1, subscribeResult1);
    }

    public void subscribe2(ActionEvent event) {
        subscribe(receiver2, subscribeComboBox2, subscribeResult2);
    }

    private void unsubscribe(SolaceClient receiver, Label label) {
        receiver.clearMessages();
        receiver.unsubscribe();
        label.setText("");
    }

    public void unsubscribe1(ActionEvent event) {
        unsubscribe(receiver1, subscribeResult1);
    }

    public void unsubscribe2(ActionEvent event) {
        unsubscribe(receiver2, subscribeResult2);
    }

    public void close() {
        if (receiver1 != null) {
            receiver1.close();
            receiver2.close();
            sender.close();
        }
        stopSending();
    }

    public void clear1(ActionEvent event) {
        receiver1.clearMessages();
    }

    public void clear2(ActionEvent event) {
        receiver2.clearMessages();
    }

    private class SendRunner implements Runnable {

        private volatile boolean running = true;
        private String topic;
        private List<String> topics;
        private int delay;
        private int num;

        public SendRunner(String topic, List<String> topics, int num, int delay) {
            super();
            this.topic = topic;
            this.topics = topics;
            this.delay = delay;
            this.num = num;
        }

        @Override
        public void run() {
            int numTopics = (topics != null ? topics.size() : 0);

            Date start = new Date();
            for (int i = 1; i <= num && running; i++) {
                
                try {
                    String top = topic;
                    if (topic == null || topic.equals("") && numTopics > 0) {
                        int pick = rand.nextInt(numTopics);
                        top = topics.get(pick);
                    }

                    Date now = new Date();
                    String message = String.format("%s message %05d   Time: %tT ", top, i, now);
                    sender.sendMessage(top, message);
                    
                    if (delay != 0) {
                        Thread.sleep(delay);
                    }
                } catch (Exception e) {
                    log.error(e);
                    break;
                }
            }

            Date stop = new Date();
            double millis = stop.getTime() - start.getTime();
            double secs = millis / 1000;
            sendResultMessage = String.format("Elapsed time: %f seconds.", secs);
            finishedSending = true;
            startButton.setDisable(false);
            stopButton.setDisable(true);
        }

        public void quit() {
            running = false;
        }

    }

}
