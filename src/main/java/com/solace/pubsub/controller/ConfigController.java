package com.solace.pubsub.controller;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    TextField managementHostField;
    @FXML
    TextField managementPortField;
    @FXML
    TextField messagingHostField;
    @FXML
    TextField messagingPortField;
    @FXML
    TextField managementUsernameField;
    @FXML
    TextField managementPasswordField;
    @FXML
    TextField clientUsernameField;
    @FXML
    TextField clientPasswordField;
    @FXML
    TextField vpnField;
    @FXML
    TableView<SolaceQueue> queueTableView;
    @FXML
    Label connectResult;
    @FXML
    ListView<String> topicListView;
    @Autowired
    Solace solace;
    @Autowired
    MainController mainController;
    
    @Value("${managementHost:192.168.133.44}")
    String managementHost;

    @Value("${managementPort:8080}")
    Integer managementPort;

    @Value("${managementUsername:admin}")
    String managementUsername;

    @Value("${managementPassword:admin}")
    String managementPassword;

    @Value("${messagingHost:192.168.133.44}")
    String messagingHost;

    @Value("${messagingPort:7000}")
    Integer messagingPort;

    @Value("${clientUsername:admin}")
    String clientUsername;

    @Value("${clientPassword:admin}")
    String clientPassword;

    @Value("${vpn:default}")
    String vpn;

    private ObservableList<String> topics;
    private ObservableList<SolaceQueue> solaceQueues;

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void initialize(URL location, ResourceBundle resources) {

        managementHostField.setText(managementHost);
        managementUsernameField.setText(managementUsername);
        managementPasswordField.setText(managementPassword);
        messagingHostField.setText(messagingHost);
        clientUsernameField.setText(clientUsername);
        clientPasswordField.setText(clientPassword);
        vpnField.setText(vpn);
        
        if (managementPort != null) {
            managementPortField.setText(managementPort.toString());
        }
        
        if (messagingPort != null) {
            messagingPortField.setText(messagingPort.toString());
        }
        
        connectResult.setText("");

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
        solaceQueues.clear();
        topics.clear();
        if (subscriptions != null) {
            log.info("Subscriptions: " + subscriptions.size());
            for (MsgVpnQueueSubscription sub : subscriptions) {
                String queueName = sub.getQueueName();
                String topic = sub.getSubscriptionTopic();
                if (!topic.contains(">") && !topic.contains("*")) {
                    topics.add(topic);
                }
                solaceQueues.add(new SolaceQueue(queueName, topic));
                log.debug("connect: added queue " + queueName);
            }
        }
    }

    public void connect(ActionEvent event) {
        solace.init(managementHostField.getText(), Integer.valueOf(managementPortField.getText()), vpnField.getText(), managementUsernameField.getText(), managementPasswordField.getText());
        connected = solace.test();
        if (connected) {
            connectResult.setText("Connected");
            deleteQueuesButton.setDisable(false);
            addQueueButton.setDisable(false);
            mainController.enableModules();
            loadConfig();
        } else {
            connectResult.setText("Connection failed.");
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

    public String getVpn() {
        return vpnField.getText();
    }

    public String getMessagingHost() {
        return messagingHostField.getText();
    }
    
    public Integer getMessagingPort() {
        return Integer.valueOf(messagingPortField.getText());
    }
    
    public String getClientUsername() {
        return clientUsernameField.getText();
    }

    public String getClientPassword() {
        return clientPasswordField.getText();
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
