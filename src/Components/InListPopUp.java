/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Components;

import Controllers.layoutController;
import States.StateData;
import Util.StateAction;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.io.IOException;

/**
 * @author muhammad
 */
public class InListPopUp {

    private final Popup popupWindow;
    private final layoutController controller;
    private final DownloaderCell cell;
    private final StateData data;
    private final Window window;
    ChangeListener<Boolean> focusListener;

    @FXML
    private AnchorPane pane;

    public InListPopUp(AnchorPane mainWindow, layoutController controller, DownloaderCell cell, StateData data) {
        window = mainWindow.getScene().getWindow();
        this.controller = controller;
        this.cell = cell;
        this.data = data;

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

        focusListener = (observable, oldValue, newValue) -> {
            if (newValue) popupWindow.show(window);
            else popupWindow.hide();
        };
        window.focusedProperty().addListener(focusListener);
    }

    @FXML
    private void replaceButtonController(ActionEvent event) {
        cell.change(StateAction.DELETE);
        cell.setData(data);
        cell.change(StateAction.START);
        window.focusedProperty().removeListener(focusListener);
        popupWindow.hide();
    }

    @FXML
    private void viewButtonController(ActionEvent event) {
        controller.showDownload(cell);
        window.focusedProperty().removeListener(focusListener);
        popupWindow.hide();
    }

    @FXML
    private void cancelButtonController(ActionEvent event) {
        window.focusedProperty().removeListener(focusListener);
        popupWindow.hide();
    }
}
