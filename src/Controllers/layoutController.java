package Controllers;

import Components.AddPopUp;
import Components.DownloaderCell;
import States.StateData;
import States.StateManagement;
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

/**
 * @author muhammad
 */
public class layoutController implements Initializable {
    ObservableList<DownloaderCell> downloadsList;
    //Back End
    PoolingHttpClientConnectionManager connectionManager;
    CloseableHttpClient client;
    StateManagement stateManager;
    // Front End
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
    private String prButtonState = "pause";

    // TODO: check stackoverflow for answer to cookie problem
    // TODO: so nothing is stopping the whole downloading code keeps on running even after paused
    // TODO: may speed being halved is caused by the same above problem, because now there are two update threads and speed is distributed between them
    @FXML
    private void addButtonController(ActionEvent actionEvent) {
//http://softlayer-sng.dl.sourceforge.net/project/elementaryos/unstable/elementaryos-unstable-amd64.20140810.iso
//https://www.google.com/calendar/ical/tk5scqbfe80ffdcfj86uppbhvk%40group.calendar.google.com/private-0dfe7855fa8b78365041b2b27261446e/basic.ics
//http://downloads.sourceforge.net/project/sevenzip/7-Zip/9.22/7z922.tar.bz2?r=http%3A%2F%2Fsourceforge.net%2Fprojects%2Fsevenzip%2F&ts=1415925898&use_mirror=kaz
        AddPopUp popUp = new AddPopUp(MainWindow.getScene().getWindow());
        popUp.startButton.addEventHandler(ActionEvent.ACTION, (ActionEvent event) -> {
            String uri = popUp.uriField.getText();
            System.out.println(URI.create(uri));
            popUp.popupWindow.hide();
            StateData data = new StateData(System.getProperty("user.home") + "/", URI.create(uri),
                    Utilities.getFromURI(uri, "filename.ext"), 10);
            DownloaderCell downloader = new DownloaderCell(data, client
            );
            downloadsList.add(downloader);
            stateManager.changeState(data, "createState");
            downloader.set();
        });
    }

    @FXML
    private void prButtonController(ActionEvent actionEvent) {
        switch (prButtonState) {
            case "pause":
                prButtonState = "resume";
                prButton.setText(prButtonState);
                listView.getSelectionModel().getSelectedIndices();
                break;
            case "resume":
                prButtonState = "pause";
                prButton.setText(prButtonState);
                break;
        }
    }

    @FXML
    private void deleteButtonController(ActionEvent actionEvent) {
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
// initializing Back End
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

        TreeItem<String> allDownloads =
                new TreeItem<>("All Downloads", new ImageView(new Image(
                        getClass().getResourceAsStream("/resources/White.png"))));
        allDownloads.setExpanded(true);
        TreeItem<String> inProgress =
                new TreeItem<>("Downloading", new ImageView(new Image(
                        getClass().getResourceAsStream("/resources/Green.png"))));
        TreeItem<String> completed =
                new TreeItem<>("Completed", new ImageView(new Image(
                        getClass().getResourceAsStream("/resources/Blue.png"))));
        TreeItem<String> paused =
                new TreeItem<>("Paused", new ImageView(new Image(
                        getClass().getResourceAsStream("/resources/Orange.png"))));
        TreeItem<String> failed =
                new TreeItem<>("Failed", new ImageView(new Image(
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

            if (treeView.getSelectionModel().getSelectedIndices().isEmpty()) event.consume();
            else if (root.getChildren().contains(selectedItem)) {
                for (TreeItem<String> treeItem : root.getChildren()) {
                    treeItem.setExpanded(false);
                }
                selectedItem.setExpanded(true);

                if (selectedItem.equals(allDownloads)) listView.setItems(downloadsList);
                else
                    listView.setItems(downloadsList.filtered(cell -> cell.getData().state.getValue().equals(selectedItem.getValue())));
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
            DownloaderCell downloader = new DownloaderCell(next, client);
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
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }
}