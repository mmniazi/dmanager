/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Components;

import States.Defaults;
import Util.UriPart;
import Util.Utilities;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Popup;
import javafx.stage.Window;
import org.apache.commons.validator.routines.UrlValidator;

import java.io.File;
import java.io.IOException;

/**
 * @author muhammad
 */
public class AddPopUp {

    public Popup popupWindow;
    //TODO: use callbacks to finalize this class
    @FXML
    public Button startButton;
    @FXML
    public Button cancelButton;
    @FXML
    public TextField uriField;
    Defaults defaults = new Defaults();
    UrlValidator urlValidator = new UrlValidator();
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

    // TODO: automatically paste from clip board when popup starts
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

        segmentField.setText(String.valueOf(defaults.getSegments()));
        locationField.setText(defaults.getDownloadLocation());
        uriField.textProperty()
                .addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                    if (urlValidator.isValid(newValue)) {
                        uriField.setId("");
                        nameField.setText(Utilities.getFromURI(newValue, UriPart.FILENAME_EXT));
                        startButton.setDisable(false);
                    } else {
                        uriField.setId("Error-Field");
                        nameField.setText("");
                        startButton.setDisable(true);
                    }
                });
    }

    @FXML
    private void cancelButtonController(ActionEvent event) {
        popupWindow.hide();
    }

    @FXML
    private void browseButtonController(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Download Directory");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        locationField.setText(chooser.showDialog(popupWindow).toString());
    }

    public String getUri() {
        return uriField.getText();
    }

    public String getName() {
        return nameField.getText();
    }

    public String getLocation() {
        return locationField.getText();
    }

    public String getSegments() {
        return segmentField.getText();
    }
}
