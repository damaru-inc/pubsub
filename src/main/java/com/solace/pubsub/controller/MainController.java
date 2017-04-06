package com.solace.pubsub.controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by mike on 2017-01-29.
 */
@Component
public class MainController implements Initializable {

    Logger log = LogManager.getLogger(MainController.class);
    private VBox rootNode;
    @FXML
    ComboBox moduleCombo;
    Object controller;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        log.debug("init start");
        try {
            ObservableList<String> modules = FXCollections.observableArrayList();
            modules.add("Config");
            modules.add("Clients");
            moduleCombo.setItems(modules);
            moduleCombo.getSelectionModel().select(0);
            moduleCombo.valueProperty().addListener(new ModuleChangeListener());
        } catch (Exception e) {
            log.error("Can't initialize MainController", e);
            quit(null);
        }
        log.debug("init end");
    }

    public void setRootNode(ApplicationContext springContext, Parent rootNode) throws IOException {
        this.rootNode = (VBox) rootNode;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Config.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);
        Node rangeModule = fxmlLoader.load();
        ObservableList<Node> children = this.rootNode.getChildren();
        children.set(1, rangeModule);
        controller = fxmlLoader.getController();
    }

    public void moduleSelected() {
        log.debug("module selected.");
    }

    class ModuleChangeListener implements ChangeListener<String> {

        @Override
        public void changed(ObservableValue ov, String old, String newOne) {
            log.debug(String.format("changed: %s %s " + ov, old, newOne));
            String resourceName = "/fxml/" + newOne + ".fxml";
            log.debug("Changing module to " + resourceName);
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(resourceName));
            Node module = null;
            try {
                module = fxmlLoader.load();
            } catch (IOException e) {
                log.error("Failed to load module resourceName", e);
                quit(null);
            }
            ObservableList<Node> children = rootNode.getChildren();
            children.set(1, module);
            controller = fxmlLoader.getController();
        }
    }

    public void quit(ActionEvent event) {
        Platform.exit();
    }


}
