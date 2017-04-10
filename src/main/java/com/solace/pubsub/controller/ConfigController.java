package com.solace.pubsub.controller;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.solace.pubsub.model.SolaceQueue;
import com.solace.pubsub.service.Solace;

import io.swagger.client.ApiException;
import io.swagger.client.model.MsgVpnQueueSubscription;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

@Component
public class ConfigController implements Initializable {

    private Logger log = LogManager.getLogger(ConfigController.class);

    private boolean connected; // set true once we've connected to a router.

    @FXML
    Button addQueueButton;
    @FXML
    Button deleteQueuesButton;
    @FXML
    TextField host;
    @FXML
    TextField password;
    @FXML
    TableView<SolaceQueue> queueTableView;
    @FXML
    Label testResult;
    @FXML
    ListView<String> topicListView;
    @FXML
    TextField username;
    @Autowired
    Solace solace;
    @Autowired
    MainController mainController;

    private ObservableList<String> topics;
    private ObservableList<SolaceQueue> solaceQueues;

    private String vpnName = "default";

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void initialize(URL location, ResourceBundle resources) {

        username.setText("admin");
        password.setText("admin");
        host.setText("192.168.133.44");
        testResult.setText("");

        topics = FXCollections.observableArrayList();
        // topics.add("topics/topic1");
        // topics.add("topics/topic2");
        topicListView.setItems(topics);

        solaceQueues = FXCollections.observableArrayList();
        // solaceQueues.add(new SolaceQueue("queue1", "topics/topic1"));
        // solaceQueues.add(new SolaceQueue("queue2", "topics/topic2"));
        queueTableView.setItems(solaceQueues);

        TableColumn<SolaceQueue, String> nameCol = new TableColumn<SolaceQueue, String>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory("name"));
        TableColumn<SolaceQueue, String> topicCol = new TableColumn<SolaceQueue, String>("Topic");
        topicCol.setCellValueFactory(new PropertyValueFactory("topic"));

        queueTableView.getColumns().setAll(nameCol, topicCol);

    }

    private void loadConfig() {
        List<MsgVpnQueueSubscription> subscriptions = solace.getSubscriptions();
        if (subscriptions != null) {
            log.info("Subscriptions: " + subscriptions.size());
            for (MsgVpnQueueSubscription sub : subscriptions) {
                String queueName = sub.getQueueName();
                String topic = sub.getSubscriptionTopic();
                topics.add(topic);
                solaceQueues.add(new SolaceQueue(queueName, topic));
                log.info("test: added topic " + topic);
            }
        }
    }

    public void test(ActionEvent event) {
        solace.init(host.getText(), username.getText(), password.getText());
        connected = solace.test();
        if (connected) {
            testResult.setText("Connection okay.");
            deleteQueuesButton.setDisable(false);
            addQueueButton.setDisable(false);
            mainController.enableModules();
            loadConfig();
        } else {
            testResult.setText("Connection failed.");
        }
    }

    public void deleteQueues(ActionEvent event) {
        if (connected) {
            solace.deleteQueues();
            loadConfig();
        }
    }

    public void addQueue(ActionEvent event) {
        // Create the custom dialog.
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Add Queue");
        dialog.setHeaderText("Add a queue");

        // Set the button types.
        ButtonType addQueueButtonType = new ButtonType("Add", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addQueueButtonType, ButtonType.CANCEL);

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField queueName = new TextField();
        queueName.setPromptText("Queue name");
        TextField topicName = new TextField();
        topicName.setPromptText("Subscription Topic");

        grid.add(new Label("Queue name:"), 0, 0);
        grid.add(queueName, 1, 0);
        grid.add(new Label("Subscription Topic:"), 0, 1);
        grid.add(topicName, 1, 1);

        // Enable/Disable login button depending on whether a username was
        // entered.
        Node addQueueButton = dialog.getDialogPane().lookupButton(addQueueButtonType);
        addQueueButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        queueName.textProperty().addListener((observable, oldValue, newValue) -> {
            addQueueButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(() -> queueName.requestFocus());

        // Convert the result to a username-password-pair when the login button
        // is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addQueueButtonType) {
                return new Pair<>(queueName.getText(), topicName.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(queue -> {
            log.info("queueName=" + queue.getKey() + ", TopicName=" + queue.getValue());
            try {
                solace.createQueue(queue.getKey(), queue.getValue());
                loadConfig();
            } catch (ApiException e) {
                log.error(e);
            }
        });
    }

    public String getVpnName() {
        return vpnName;
    }

    public void setVpnName(String vpnName) {
        this.vpnName = vpnName;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public ObservableList<String> getTopics() {
        return topics;
    }

    public ObservableList<SolaceQueue> getSolaceQueues() {
        return solaceQueues;
    }

}
