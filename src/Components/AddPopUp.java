package Components;

import Controllers.layoutController;
import States.Defaults;
import States.StateData;
import Util.UriPart;
import Util.Utilities;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.effect.GaussianBlur;
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


/* Version 2
* TODO: start while asking for start
* TODO: automatically paste from clip board when popup starts
* TODO: change image based on file type
*/

public class AddPopUp {

    private final Popup popupWindow;
    Defaults defaults = new Defaults();
    UrlValidator urlValidator = new UrlValidator();
    layoutController controller;
    Window window;
    ChangeListener<Boolean> focusListener;

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

    public AddPopUp(layoutController controller, AnchorPane mainWindow) {
        this.controller = controller;
        this.window = mainWindow.getScene().getWindow();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/AddPopUp.fxml"));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        popupWindow = new Popup();
        popupWindow.getContent().add(pane);

        popupWindow.setOnShown(event -> mainWindow.setEffect(new GaussianBlur(7)));
        popupWindow.setOnHidden(event -> mainWindow.setEffect(null));

        popupWindow.show(window);
        popupWindow.centerOnScreen();
        popupWindow.setAutoFix(true);
        popupWindow.setAnchorLocation(PopupWindow.AnchorLocation.WINDOW_TOP_RIGHT);
        focusListener = (observable, oldValue, newValue) -> {
            if (newValue) popupWindow.show(window);
            else popupWindow.hide();
        };
        window.focusedProperty().addListener(focusListener);

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
        window.focusedProperty().removeListener(focusListener);
        popupWindow.hide();
    }

    @FXML
    private void cancelButtonController(ActionEvent event) {
        window.focusedProperty().removeListener(focusListener);
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
