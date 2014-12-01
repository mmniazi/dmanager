package Controllers;

import Components.AddPopUp;
import Components.DownloaderCell;
import States.StateData;
import States.StateManagement;
import Util.State;
import Util.Utilities;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Paint;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.scene.input.MouseEvent;

/**
 * @author muhammad
 */
public class layoutController implements Initializable {

    ExecutorService threadService;
    ObservableList<DownloaderCell> downloadsList;
    PoolingHttpClientConnectionManager connectionManager;
    CloseableHttpClient client;
    StateManagement stateManager;
    @FXML
    private Button exitButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button prButton;
    @FXML
    private ListView<DownloaderCell> listView;
    @FXML
    private Button addButton;
    @FXML
    private AnchorPane MainWindow;
    @FXML
    private TreeView<String> treeView;

    ButtonState prButtonState = ButtonState.RESUMED;

    @FXML
    private void addButtonController(ActionEvent actionEvent) {
        AddPopUp popUp = new AddPopUp(MainWindow.getScene().getWindow());
        popUp.startButton.addEventHandler(ActionEvent.ACTION, (ActionEvent event) -> {
            String uri = popUp.uriField.getText();
            popUp.popupWindow.hide();
            StateData data = new StateData(System.getProperty("user.home") + "/", URI.create(uri),
                    Utilities.getFromURI(uri, Util.URI.FILENAME_EXT), 10);
            DownloaderCell downloader = new DownloaderCell(data, client, threadService);
            downloadsList.add(downloader);
            stateManager.changeState(data, "createState");
            downloader.set();
        });
    }

    @FXML
    private void prButtonController(ActionEvent actionEvent) {
        switch (prButtonState) {
            case PAUSED:
                for (DownloaderCell cell : listView.getSelectionModel().getSelectedItems()) {
                    cell.stopDownload();
                }
                prButtonState = ButtonState.RESUMED;
                prButton.setId("ResumeButton");
                break;
            case RESUMED:
                for (DownloaderCell cell : listView.getSelectionModel().getSelectedItems()) {
                    cell.startDownload();
                }
                prButtonState = ButtonState.PAUSED;
                prButton.setId("PauseButton");
                break;
        }
    }

    @FXML
    private void deleteButtonController(ActionEvent actionEvent) {
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        threadService = Executors.newCachedThreadPool();
        stateManager = StateManagement.getInstance();
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(1000);
        connectionManager.setDefaultMaxPerRoute(100);
        RequestConfig requestConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.BEST_MATCH)
                .build();
        client = HttpClients.custom().
                setConnectionManager(connectionManager).
                setDefaultRequestConfig(requestConfig).
                build();
        initDownloadsList();
        initCategoriesTree();
    }

    // TODO: create post about how to create a treeView with only single expanded treeItem
    // TODO: add animations to treeView
    // TODO: make pr and delete button work and make mechanism for toggling check box when selecting list cells
    private void initCategoriesTree() {

        TreeItem<String> root = new TreeItem<>("Root Node");
        root.setExpanded(true);

        TreeItem<String> allDownloads
                = new TreeItem<>("All Downloads", new ImageView(new Image(
                                        getClass().getResourceAsStream("/resources/White.png"))));
        allDownloads.setExpanded(true);
        TreeItem<String> inProgress
                = new TreeItem<>("Downloading", new ImageView(new Image(
                                        getClass().getResourceAsStream("/resources/Green.png"))));
        TreeItem<String> completed
                = new TreeItem<>("Completed", new ImageView(new Image(
                                        getClass().getResourceAsStream("/resources/Blue.png"))));
        TreeItem<String> paused
                = new TreeItem<>("Paused", new ImageView(new Image(
                                        getClass().getResourceAsStream("/resources/Orange.png"))));
        TreeItem<String> failed
                = new TreeItem<>("Failed", new ImageView(new Image(
                                        getClass().getResourceAsStream("/resources/Red.png"))));
// Adding all tree items to root item
        root.getChildren().addAll(allDownloads, completed, failed, inProgress, paused);

// Defining branches for each tree item
        for (TreeItem<String> parentItem : root.getChildren()) {
            TreeItem<String> programs = new TreeItem<>("Programs");
            TreeItem<String> compressed = new TreeItem<>("Compressed");
            TreeItem<String> documents = new TreeItem<>("Documents");
            TreeItem<String> videos = new TreeItem<>("Videos");
            TreeItem<String> audio = new TreeItem<>("Audio");
            TreeItem<String> images = new TreeItem<>("Images");
            TreeItem<String> others = new TreeItem<>("Others");

// Adding all tree items to parent tree item
            parentItem.getChildren().addAll(programs, compressed, documents, videos, audio, images, others);
        }

        /*----- Setting the root tree item and hiding root -----*/
        treeView.setRoot(root);
        treeView.setShowRoot(false);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        /*----- When one treeItem is selected expand it and collapse all others -----*/
        treeView.setOnMouseClicked(event -> {
            TreeItem selectedItem = treeView.getSelectionModel().getSelectedItem();

            if (treeView.getSelectionModel().getSelectedIndices().isEmpty()) {
                event.consume();
            } else if (root.getChildren().contains(selectedItem)) {
                for (TreeItem<String> treeItem : root.getChildren()) {
                    treeItem.setExpanded(false);
                }
                selectedItem.setExpanded(true);

                if (selectedItem.equals(allDownloads)) {
                    listView.setItems(downloadsList);
                } else {
                    listView.setItems(downloadsList.filtered(cell -> cell.getData().state.getValue().equals(selectedItem.getValue())));
                }
            } else {
                if (selectedItem.getParent().equals(allDownloads)) {
                    listView.setItems(downloadsList.filtered(cell -> selectedItem.getValue().equals(cell.getType())));
                } else {
                    listView.setItems(downloadsList.filtered(cell -> selectedItem.getParent().getValue().equals(cell.getData().state.getValue())
                            && selectedItem.getValue().equals(cell.getType())));
                }
            }
        });
    }

    private void initDownloadsList() {
        downloadsList = FXCollections.observableArrayList();
        stateManager.readFromFile().stream().forEach((next) -> {
            DownloaderCell downloader = new DownloaderCell(next, client, threadService);
            downloadsList.add(downloader);
            downloader.set();
        });
        listView.setItems(downloadsList);
        AnchorPane emptyCell = new AnchorPane();
        emptyCell.setBackground(new Background(new BackgroundFill(
                Paint.valueOf("#f9f9f9"), CornerRadii.EMPTY, Insets.EMPTY)));
        emptyCell.setPrefSize(800, 60);
        listView.setCellFactory(ListView -> new ListCell<DownloaderCell>() {
            @Override
            protected void updateItem(DownloaderCell item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(emptyCell);
                } else {
                    setGraphic(item.getCell());
                }
            }
        });
        // inshaAllah going to build that hover effect I first designed
        // TODO: if chck box is selected then cell is selected
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setOnMouseClicked((MouseEvent event) -> {
            ObservableList<DownloaderCell> selectedCells = listView.getSelectionModel().getSelectedItems();
            if (!selectedCells.isEmpty()) {
                for (DownloaderCell cell : downloadsList) {
                    if (listView.getSelectionModel().getSelectedItems().contains(cell)) {
                        cell.setCheckBoxValue(true);
                    } else {
                        cell.setCheckBoxValue(false);
                    }
                }
                int activeCounter = 0;
                for (DownloaderCell cell : selectedCells) {
                    if (cell.getData().state == State.ACTIVE) {
                        activeCounter++;
                        prButtonState = ButtonState.PAUSED;
                        prButton.setId("PauseButton");
                        break;
                    }
                }
                if (activeCounter == 0) {
                    prButtonState = ButtonState.RESUMED;
                    prButton.setId("ResumeButton");
                }
            }
        });
    }
}

/* ----- Download Links -----*/
// http://softlayer-sng.dl.sourceforge.net/project/elementaryos/unstable/elementaryos-unstable-amd64.20140810.iso
// https://www.google.com/calendar/ical/tk5scqbfe80ffdcfj86uppbhvk%40group.calendar.google.com/private-0dfe7855fa8b78365041b2b27261446e/basic.ics
// http://downloads.sourceforge.net/project/sevenzip/7-Zip/9.22/7z922.tar.bz2?r=http%3A%2F%2Fsourceforge.net%2Fprojects%2Fsevenzip%2F&ts=1415925898&use_mirror=kaz
// http://software-files-a.cnet.com/s/software/13/91/24/11/avast_free_antivirus_setup_online.exe?token=1417263074_0969cea50d1231fcf1d9c961806461d7&fileName=avast_free_antivirus_setup_online.exe
// http://downloads.sourceforge.net/project/openofficeorg.mirror/4.1.1/binaries/en-US/Apache_OpenOffice_4.1.1_Linux_x86-64_install-rpm_en-US.tar.gz?r=http%3A%2F%2Fsourceforge.net%2Fprojects%2Fopenofficeorg.mirror%2F%3Fsource%3Ddirectory-featured&ts=1417228120&use_mirror=softlayer-sng
// http://downloads.sourceforge.net/project/nagios/nagios-4.x/nagios-4.0.8/nagios-4.0.8.tar.gz?r=http%3A%2F%2Fsourceforge.net%2Fdirectory%2Fbusiness-enterprise%2Fos%3Alinux%2Ffreshness%3Arecently-updated%2F&ts=1417228511&use_mirror=softlayer-sng

