/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Components;

import Controllers.layoutController;
import States.Defaults;
import States.StateData;
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
import javafx.stage.PopupWindow;
import javafx.stage.Window;
import org.apache.commons.validator.routines.UrlValidator;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author muhammad
 */
public class AddPopUp {

    private final Popup popupWindow;
    Defaults defaults = new Defaults();
    UrlValidator urlValidator = new UrlValidator();
    layoutController controller;

    @FXML
    private Button startButton;
    @FXML
    private TextField uriField;
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

    // start while asking for start
    // automatically paste from clip board when popup starts
    // change image based on file type
    // if file is not found on server change text field to red.
    public AddPopUp(Window window, layoutController controller) {

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
        popupWindow.centerOnScreen();
        popupWindow.setAutoFix(true);
        popupWindow.setAnchorLocation(PopupWindow.AnchorLocation.WINDOW_TOP_RIGHT);

        this.controller = controller;

        segmentField.setText(String.valueOf(defaults.getSegments()));
        locationField.setText(defaults.getDownloadLocation());
        uriField.textProperty()
                .addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                    if (urlValidator.isValid(newValue)) {
                        uriField.setId("");
                        nameField.setText(validFileName());
                        startButton.setDisable(false);
                    } else {
                        uriField.setId("Error-Field");
                        nameField.setText("");
                        startButton.setDisable(true);
                    }
                });
    }

    @FXML
    private void startButtonController(ActionEvent event) {
        StateData data = new StateData(locationField.getText(),
                URI.create(uriField.getText()), nameField.getText(), 10);
        controller.addDownload(data);
        popupWindow.hide();
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
        File selectedDirectory = chooser.showDialog(popupWindow);
        if (!(selectedDirectory == null)) {
            locationField.setText(selectedDirectory.toString());
            nameField.setText(validFileName());
        }
    }

    private String validFileName() {
        String location = locationField.getText();
        String fileName = Utilities.getFromURI(uriField.getText(), UriPart.FILENAME_EXT);
        boolean fileExists = Files.exists(Paths.get(location + fileName));
        if (fileExists) {
            int counter = 0;
            while (fileExists) {
                fileName = Utilities.getFromURI(uriField.getText(), UriPart.FILENAME) +
                        "(" + ++counter + ")" + "." +
                        Utilities.getFromURI(uriField.getText(), UriPart.EXT);
                fileExists = Files.exists(Paths.get(location + fileName));
            }
            return fileName;
        } else {
            return Utilities.getFromURI(uriField.getText(), UriPart.FILENAME_EXT);
        }
    }
}
