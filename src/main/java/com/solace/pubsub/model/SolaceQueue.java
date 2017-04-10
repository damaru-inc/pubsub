package com.solace.pubsub.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SolaceQueue {

	private StringProperty name;
	private StringProperty topic;
	
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
	
	public String toString() {
	    return topic.getValue() + "(" + name.getValue() + ")";
	}
}
