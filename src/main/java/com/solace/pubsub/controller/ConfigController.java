package com.solace.pubsub.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.solace.pubsub.model.SolaceClient;
import com.solace.pubsub.model.SolaceQueue;
import com.solace.pubsub.service.Solace;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

@Component
public class ConfigController {

	@FXML
	TextField host;
	@FXML
	TextField password;
	@FXML
	Label testResult;
	@FXML
	TextField username;
	@Autowired
	Solace solace;
	
	List<SolaceClient> clients;
	List<String> topics;
	List<SolaceQueue> queues;
	
	private String vpnName = "default";
	
    public void test(ActionEvent event) {
        solace.init(host.getText(), username.getText(), password.getText());
        if (solace.test()) {
        	testResult.setText("Connection okay.");
        } else {
        	testResult.setText("Connection failed.");
        }
    }
	public String getVpnName() {
		return vpnName;
	}
	public void setVpnName(String vpnName) {
		this.vpnName = vpnName;
	}
	
}
