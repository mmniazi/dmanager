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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.*;
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
* TODO: change image based on file type
* TODO: keyboard focus is not on popup on shown
*/

public class AddPopUp {

    private final Stage stage;
    Defaults defaults = new Defaults();
    UrlValidator urlValidator = new UrlValidator();
    layoutController controller;
    Window window;
    AnchorPane mainWindow;

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

    public AddPopUp(layoutController controller) {
        this(controller, null);
    }

    public AddPopUp(layoutController controller, String url) {
        this.controller = controller;
        mainWindow = controller.getMainWindow();
        window = mainWindow.getScene().getWindow();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/AddPopUp.fxml"));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        stage = new Stage();
        Scene scene = new Scene(pane);
        stage.getIcons().add(new Image("resources/icon.png"));
        scene.setFill(Color.TRANSPARENT);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(window);
        stage.setScene(scene);

        stage.setOnShown(event -> mainWindow.setEffect(new GaussianBlur(7)));
        stage.setOnHidden(event -> mainWindow.setEffect(null));

        stage.setAlwaysOnTop(true);
        stage.centerOnScreen();
        stage.requestFocus();
        stage.show();

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

        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (url != null) {
            uriField.setText(url);
        } else if (clipboard.hasString() && urlValidator.isValid(clipboard.getString())) {
            uriField.setText(clipboard.getString());
        }

        segmentField.setText(String.valueOf(defaults.getSegments()));
        locationField.setText(defaults.getDownloadLocation());
    }

    @FXML
    private void startButtonController(ActionEvent event) {
        StateData data = new StateData(locationField.getText(),
                URI.create(uriField.getText()), nameField.getText(), 10);
        controller.addDownload(data);
        stage.hide();
    }

    @FXML
    private void cancelButtonController(ActionEvent event) {
        stage.hide();
    }

    @FXML
    private void browseButtonController(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Download Directory");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File selectedDirectory = chooser.showDialog(stage);
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
