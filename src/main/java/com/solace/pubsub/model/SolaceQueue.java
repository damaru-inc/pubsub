package com.solace.pubsub.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SolaceQueue {

	private StringProperty name;
	private StringProperty topic;
	private IntegerProperty numMessages = new SimpleIntegerProperty();
	
	public SolaceQueue(String name, String topic) {
		this.name = new SimpleStringProperty(name);
		this.topic = new SimpleStringProperty(topic);
	}
	
	public String getName() {
		return name.get();
	}

	public StringProperty getNameProperty() {
		return name;
	}

	public String getTopic() {
		return topic.get();
	}
	
	public StringProperty getTopicProperty() {
		return topic;
	}
	
	public int getNumMessages() {
	    return numMessages.get();
	}
	
	public void setNumMessages(int num) {
	    numMessages.set(num);
	}
	
	public IntegerProperty getNumMessagesProperty() {
	    return numMessages;
	}
	
	public String toString() {
	    return topic.getValue() + "(" + name.getValue() + ")";
	}
}
