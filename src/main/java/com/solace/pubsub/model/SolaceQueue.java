package com.solace.pubsub.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SolaceQueue {

	private StringProperty name;
	private StringProperty topic;
	private IntegerProperty numMessages = new SimpleIntegerProperty();
	private DoubleProperty currentUsage = new SimpleDoubleProperty();
	private DoubleProperty highWaterMark = new SimpleDoubleProperty();
	
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
	
	public double getCurrentUsage() {
	    return currentUsage.doubleValue();
	}
	
	public void setCurrentUsage(double usage) {
	    currentUsage.set(usage);
	}
	
	public DoubleProperty getCurrentUsageProperty() {
	    return currentUsage;
	}
	
    public double getHighWaterMark() {
        return highWaterMark.doubleValue();
    }
    
    public void setHighWaterMark(double mark) {
        highWaterMark.set(mark);
    }
    
    public DoubleProperty getHighWaterMarkProperty() {
        return highWaterMark;
    }
    
	public String toString() {
	    return topic.getValue() + "(" + name.getValue() + ")";
	}
}
