package Controllers;

import Components.AddPopUp;
import Components.DownloaderCell;
import Components.InListPopUp;
import States.StateData;
import States.StateManagement;
import Util.State;
import Util.TotalSpeedCalc;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
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

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.application.Platform;

/**
 * @author muhammad
 */
public class layoutController implements Initializable {

    ExecutorService threadService;
    ObservableList<DownloaderCell> downloadsList;
    PoolingHttpClientConnectionManager connectionManager;
    CloseableHttpClient client;
    StateManagement stateManager;
    ButtonState prButtonState = ButtonState.RESUMED;
    TotalSpeedCalc speedCalc;

    @FXML
    private Button prButton;
    @FXML
    private ListView<DownloaderCell> listView;
    @FXML
    private AnchorPane MainWindow;
    @FXML
    private TreeView<String> treeView;
    @FXML
    private Label totalDownloadsLabel;
    @FXML
    private Label totalSpeedLabel;

    // TODO: work on delete button
    @FXML
    private void addButtonController(ActionEvent actionEvent) {
        AddPopUp popUp = new AddPopUp(MainWindow.getScene().getWindow(), this);
    }

    @FXML
    private void prButtonController(ActionEvent actionEvent) {
        if (!listView.getSelectionModel().isEmpty()) {
            switch (prButtonState) {
                case PAUSED:
                    listView.getSelectionModel().getSelectedItems().stream().forEach((cell) -> {
                        if (cell.getData().state.equals(State.ACTIVE)) {
                            cell.stop();
                        }
                    });
                    prButtonState = ButtonState.RESUMED;
                    prButton.setId("ResumeButton");
                    break;
                case RESUMED:
                    listView.getSelectionModel().getSelectedItems().stream().forEach((cell) -> {
                        if (cell.getData().state.equals(State.PAUSED)) {
                            cell.getData().state = State.ACTIVE;
                            cell.initializeCell();
                        } else if (cell.getData().state.equals(State.FAILED)) {
                            cell.resetData();
                            cell.getData().state = State.ACTIVE;
                            cell.initializeCell();
                        }
                    });
                    prButtonState = ButtonState.PAUSED;
                    prButton.setId("PauseButton");
                    break;
            }
        }
    }

    @FXML
    private void deleteButtonController(ActionEvent actionEvent) {
        listView.getSelectionModel().getSelectedItems().forEach((DownloaderCell cell) -> {
            listView.getSelectionModel().clearSelection(listView.getItems().indexOf(cell));
            listView.getItems().remove(cell);
            cell.stop();
            cell.delete();
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        threadService = Executors.newCachedThreadPool();
        stateManager = StateManagement.getInstance();
        downloadsList = FXCollections.observableArrayList();
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(1000);
        connectionManager.setDefaultMaxPerRoute(100);
        speedCalc = TotalSpeedCalc.getInstance();
        speedCalc.setController(this);
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
        root.getChildren().stream().forEach((parentItem) -> {
            TreeItem<String> programs = new TreeItem<>("Programs");
            TreeItem<String> compressed = new TreeItem<>("Compressed");
            TreeItem<String> documents = new TreeItem<>("Documents");
            TreeItem<String> videos = new TreeItem<>("Videos");
            TreeItem<String> audio = new TreeItem<>("Audio");
            TreeItem<String> images = new TreeItem<>("Images");
            TreeItem<String> others = new TreeItem<>("Others");
            parentItem.getChildren().addAll(programs, compressed, documents, videos, audio, images, others);
        });

        /*----- Setting the root tree item and hiding root -----*/
        treeView.setRoot(root);
        treeView.setShowRoot(false);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        /*----- When one treeItem is selected expand it and collapse all others -----*/
        treeView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<TreeItem<String>>) change -> {
            if (change.next()) {
                TreeItem selectedItem = change.getAddedSubList().get(0);
                if (!selectedItem.isLeaf()) {
                    root.getChildren().stream().forEach(treeItem -> {
                        if (treeItem.equals(selectedItem)) {
                            treeItem.setExpanded(true);
                        } else if (treeItem.isExpanded()) {
                            treeItem.setExpanded(false);
                        }
                    });
                    if (selectedItem.equals(allDownloads)) {
                        listView.setItems(downloadsList);
                    } else {
                        listView.setItems(downloadsList.filtered(cell -> cell.getData().state.getValue()
                                .equals(selectedItem.getValue())));
                    }
                } else {
                    if (selectedItem.getParent().equals(allDownloads)) {
                        listView.setItems(downloadsList
                                .filtered(cell -> selectedItem.getValue().equals(cell.getType())));
                    } else {
                        listView.setItems(downloadsList
                                .filtered(cell -> selectedItem.getParent().getValue()
                                        .equals(cell.getData().state.getValue())
                                        && selectedItem.getValue().equals(cell.getType())));
                    }
                }
            }
        });
    }

    private void initDownloadsList() {
        downloadsList.addListener((ListChangeListener<DownloaderCell>) change -> {
            if (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(cell -> cell.getCheckBox().setOnMouseClicked(event -> {
                        if (cell.getCheckBoxValue()) {
                            listView.getSelectionModel().select(cell);
                        } else {
                            listView.getSelectionModel().clearSelection(listView.getItems().indexOf(cell));
                        }
                    }));

                }
            }
        });
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.getSelectionModel().getSelectedItems()
                .addListener((ListChangeListener<DownloaderCell>) change -> {
                    if (change.next()) {

                        if (change.wasRemoved()) {
                            change.getRemoved().stream().forEach(cell -> cell.setCheckBoxValue(false));
                        }

                        if (change.wasAdded()) {
                            change.getAddedSubList().stream().forEach(cell -> cell.setCheckBoxValue(true));
                        }
                    }
                });
        listView.getSelectionModel().getSelectedItems()
                .addListener((ListChangeListener<DownloaderCell>) change -> {
                    if (change.next()) {
                        Predicate<DownloaderCell> predicate = cell -> cell.getData().state == State.ACTIVE;
                        if (change.getList().stream().anyMatch(predicate)) {
                            prButtonState = ButtonState.PAUSED;
                            prButton.setId("PauseButton");
                        } else {
                            prButtonState = ButtonState.RESUMED;
                            prButton.setId("ResumeButton");
                        }
                    }
                });
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

        stateManager.readFromFile().stream().forEach((next) -> {
            DownloaderCell cell = new DownloaderCell(next, this);
            downloadsList.add(cell);
            cell.initializeCell();
        });
        listView.setItems(downloadsList);

    }

    // TODO: Handle file name duplicates
    public void addDownload(StateData data) {

        Predicate<DownloaderCell> predicate = cell -> cell.getData().uri.equals(data.uri);
        Optional<DownloaderCell> optionalCell = downloadsList.stream().filter(predicate).findFirst();

        if (optionalCell.isPresent()) {
            InListPopUp inListPopUp = new InListPopUp(MainWindow.getScene().getWindow(), this, optionalCell.get(), data);
        } else {
            DownloaderCell cell = new DownloaderCell(data, this);
            downloadsList.add(cell);
            stateManager.changeState(data, "createState");
            cell.initializeCell();
        }
    }

    public void showDownload(DownloaderCell cell) {
        listView.getSelectionModel().clearAndSelect(listView.getItems().indexOf(cell));
        listView.scrollTo(cell);
    }

    public void updateTotalSpeed(int speed) {
        Platform.runLater(()
                -> totalSpeedLabel.setText(Util.Utilities.speedConverter(speed))
        );
    }

    public void updateActiveDownloads(boolean increment) {
        if (increment) {
            int activeDownloads = Integer.valueOf(totalDownloadsLabel.getText()) + 1;
            Platform.runLater(() -> totalDownloadsLabel.setText(String.valueOf(activeDownloads)));
            speedCalc.updateActiveDownloads(activeDownloads);
        } else {
            int activeDownloads = Integer.valueOf(totalDownloadsLabel.getText()) - 1;
            Platform.runLater(() -> totalDownloadsLabel.setText(String.valueOf(activeDownloads)));
            speedCalc.updateActiveDownloads(activeDownloads);
        }
    }

    public CloseableHttpClient getClient() {
        return client;
    }

    public ExecutorService getThreadService() {
        return threadService;
    }
}

/*----- Download Links -----*/
// http://softlayer-sng.dl.sourceforge.net/project/elementaryos/unstable/elementaryos-unstable-amd64.20140810.iso
// https://www.google.com/calendar/ical/tk5scqbfe80ffdcfj86uppbhvk%40group.calendar.google.com/private-0dfe7855fa8b78365041b2b27261446e/basic.ics
// http://downloads.sourceforge.net/project/sevenzip/7-Zip/9.22/7z922.tar.bz2?r=http%3A%2F%2Fsourceforge.net%2Fprojects%2Fsevenzip%2F&ts=1415925898&use_mirror=kaz
// http://software-files-a.cnet.com/s/software/13/91/24/11/avast_free_antivirus_setup_online.exe?token=1417263074_0969cea50d1231fcf1d9c961806461d7&fileName=avast_free_antivirus_setup_online.exe
// http://downloads.sourceforge.net/project/openofficeorg.mirror/4.1.1/binaries/en-US/Apache_OpenOffice_4.1.1_Linux_x86-64_install-rpm_en-US.tar.gz?r=http%3A%2F%2Fsourceforge.net%2Fprojects%2Fopenofficeorg.mirror%2F%3Fsource%3Ddirectory-featured&ts=1417228120&use_mirror=softlayer-sng
// http://downloads.sourceforge.net/project/nagios/nagios-4.x/nagios-4.0.8/nagios-4.0.8.tar.gz?r=http%3A%2F%2Fsourceforge.net%2Fdirectory%2Fbusiness-enterprise%2Fos%3Alinux%2Ffreshness%3Arecently-updated%2F&ts=1417228511&use_mirror=softlayer-sng

