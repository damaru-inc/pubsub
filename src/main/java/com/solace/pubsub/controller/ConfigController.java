package com.solace.pubsub.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.solace.pubsub.model.SolaceClient;
import com.solace.pubsub.model.SolaceQueue;
import com.solace.pubsub.service.Solace;

import io.swagger.client.ApiException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

@Component
public class ConfigController implements Initializable {
	
	private Logger log = LogManager.getLogger(ConfigController.class);
	
	private boolean connected; // set true once we've connected to a router.
	
	@FXML
	Button configureButton;
	@FXML
	Label configureResult;
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

	List<SolaceClient> clients;
	ObservableList<String> topics;
	ObservableList<SolaceQueue> solaceQueues;

	private String vpnName = "default";

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void initialize(URL location, ResourceBundle resources) {

		configureResult.setText("");
		username.setText("admin");
		password.setText("admin");
		host.setText("192.168.133.44");
		testResult.setText("");

		topics = FXCollections.observableArrayList();
		topics.add("topics/topic1");
		topics.add("topics/topic2");
		topicListView.setItems(topics);

		solaceQueues = FXCollections.observableArrayList();
		solaceQueues.add(new SolaceQueue("queue1", "topics/topic1"));
		solaceQueues.add(new SolaceQueue("queue2", "topics/topic2"));
		queueTableView.setItems(solaceQueues);

		TableColumn<SolaceQueue, String> nameCol = new TableColumn<SolaceQueue, String>("Name");
		nameCol.setCellValueFactory(new PropertyValueFactory("name"));
		TableColumn<SolaceQueue, String> topicCol = new TableColumn<SolaceQueue, String>("Topic");
		topicCol.setCellValueFactory(new PropertyValueFactory("topic"));

		queueTableView.getColumns().setAll(nameCol, topicCol);

	}

	public void test(ActionEvent event) {
		solace.init(host.getText(), username.getText(), password.getText());
		connected = solace.test();
		if (connected) {
			testResult.setText("Connection okay.");
			configureButton.setDisable(false);
			deleteQueuesButton.setDisable(false);
		} else {
			testResult.setText("Connection failed.");
		}
	}
	
	
	// TODO solace should throw, we catch and report.
	public void configure(ActionEvent event) {
		log.debug("configure: connected: " + connected);
		if (connected) {
			try {
				for (SolaceQueue queue : solaceQueues) {
					log.debug("Creating queue: " + queue);
					solace.createQueue(queue.getName(), queue.getTopic());
				}
			} catch (ApiException e) {
				log.error(e);
				configureResult.setText(e.getMessage());
			}
		}
	}

    public void deleteQueues(ActionEvent event) {
        if (connected) {
            solace.deleteQueues();
        }
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

	public void close() {
		log.debug("Closing clients...");
		if (clients != null) {
			for (SolaceClient client : clients) {
				client.close();
			}
		}
	}

}
