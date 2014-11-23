/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Components;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.io.IOException;

/**
 * @author muhammad
 */
public class AddPopUp {

    @FXML
    public Button startButton;
    @FXML
    public TextField uriField;
    public Popup popupWindow;
    @FXML
    private Label uriLabel;
    @FXML
    private TextField locationField;
    @FXML
    private ImageView image;
    @FXML
    private TextField nameField;
    @FXML
    private Label locationLabel;
    @FXML
    private Label segmentsLabel;
    @FXML
    private TextField segmentField;
    @FXML
    private Label nameLabel;
    @FXML
    private AnchorPane pane;

    public AddPopUp(Window window) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/AddPopUp.fxml"));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        popupWindow = new Popup();
        popupWindow.getContent().add(pane);
        popupWindow.show(window);
    }

    @FXML
    private void uriFieldController(Event event) {
        //TODO:develop a proper link checking mechanism
        if (!uriField.getText().isEmpty()) {
            startButton.setDisable(false);
        } else {
            startButton.setDisable(true);
        }
    }
}
