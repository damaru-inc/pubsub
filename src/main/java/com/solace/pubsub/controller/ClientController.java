package com.solace.pubsub.controller;

import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.solace.pubsub.model.SolaceClient;
import com.solace.pubsub.model.SolaceQueue;
import com.solace.pubsub.service.Solace;
import com.solacesystems.jcsmp.JCSMPException;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;

@Component
public class ClientController implements Initializable {

    private Logger log = LogManager.getLogger(ClientController.class);

    private SolaceClient sender;
    private SolaceClient receiver;

    @FXML
    public ObservableList<String> receivedMessages;

    @FXML
    public ListView<String> receivedMessagesListView;

    @FXML
    ComboBox<String> sendTopicComboBox;

    @FXML
    ComboBox<SolaceQueue> subscribeComboBox;

    @Autowired
    Solace solace;

    @Autowired
    ConfigController configController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            sender = new SolaceClient(solace.getHost(), Solace.MSG_VPN_NAME, "user1", null);
            receiver = new SolaceClient(solace.getHost(), Solace.MSG_VPN_NAME, "user2", null);
            receivedMessages = receiver.getMessages();
            receivedMessagesListView.setItems(receivedMessages);
            log.debug("configController: " + configController);
            sendTopicComboBox.setItems(configController.getTopics());
            subscribeComboBox.setItems(configController.getSolaceQueues());
        } catch (JCSMPException e) {
            // TODO Auto-generated catch block
            log.error(e);
        }
    }

    public void send(ActionEvent event) {
        String topic = sendTopicComboBox.getValue();
        if (topic != null) {
            for (int i = 0; i < 10; i++) {
                Date now = new Date();
                try {
                    String message = topic + " " + i + " : " + now;
                    sender.sendMessage(topic, message);
                } catch (JCSMPException e) {
                    log.error(e);
                }
            }
        } else {
            log.warn("Must pick a topic.");
        }
    }

    public void subscribe(ActionEvent event) {
        SolaceQueue queue = subscribeComboBox.getValue();
        if (queue != null) {
            try {
                receiver.subscribe(queue.getName());
            } catch (JCSMPException e) {
                log.error(e);
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
