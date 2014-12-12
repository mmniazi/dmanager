/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Components;

import Controllers.layoutController;
import States.StateData;
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

    @FXML
    private AnchorPane pane;

    public InListPopUp(Window window, layoutController controller, DownloaderCell cell, StateData data) {
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
        this.controller = controller;
        this.cell = cell;
        this.data = data;
    }

    @FXML
    private void replaceButtonController(ActionEvent event) {
        cell.stop();
        cell.setData(data);
        cell.initializeCell();
        popupWindow.hide();
    }

    @FXML
    private void viewButtonController(ActionEvent event) {
        controller.showDownload(cell);
        popupWindow.hide();
    }

    @FXML
    private void cancelButtonController(ActionEvent event) {
        popupWindow.hide();
    }
}
