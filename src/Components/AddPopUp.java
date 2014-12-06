/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Components;

import States.Defaults;
import Util.UriPart;
import Util.Utilities;
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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;
import org.apache.commons.validator.UrlValidator;

/**
 * @author muhammad
 */
public class AddPopUp {

    public Popup popupWindow;
    Defaults defaults = new Defaults();
    UrlValidator urlValidator = new UrlValidator();
    @FXML
    public Button startButton;
    @FXML
    public TextField uriField;
    @FXML
    private TextField locationField;
    @FXML
    private ImageView image;
    @FXML
    private TextField nameField;
    @FXML
    private TextField segmentField;
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

        segmentField.setPromptText(String.valueOf(defaults.getSegments()));
        locationField.setPromptText(defaults.getDownloadLocation());
        uriField.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (urlValidator.isValid(newValue)) {
                nameField.setPromptText(Utilities.getFromURI(newValue, UriPart.FILENAME_EXT));
                startButton.setDisable(false);
            } else {
                nameField.setText("");
                startButton.setDisable(true);
            }
        });
    }
}
