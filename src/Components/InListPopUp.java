/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Components;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Popup;
import javafx.stage.Window;

/**
 *
 * @author muhammad
 */
public class InListPopUp {

    public Popup popupWindow;
    // TODO: working here
    @FXML
    private AnchorPane pane;

    public InListPopUp(Window window) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/InListPopUp.fxml"));
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
// TODO: use call backs to do the appropriate action or otherwise you can pass download list

    @FXML
    private void replaceButtonController(ActionEvent event) {

    }

    @FXML
    private void resumeButtonController(ActionEvent event) {

    }

    @FXML
    private void ignoreButtonController(ActionEvent event) {

    }
}
