package Controllers;

import Components.AddPopUp;
import Components.DownloaderCell;
import Components.InListPopUp;
import States.StateActivity;
import States.StateData;
import States.StateManagement;
import Util.State;
import Util.StateAction;
import Util.TotalSpeedCalc;
import javafx.application.Platform;
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
import javafx.stage.Stage;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;

/**
 * @author muhammad
 */

public class layoutController implements Initializable {

    ObservableList<DownloaderCell> downloadsList;
    PoolingHttpClientConnectionManager connectionManager;
    CloseableHttpClient client;
    StateManagement stateManager;
    ButtonState prButtonState = ButtonState.RESUMED;
    TotalSpeedCalc speedCalc;
    int activeDownloads;
    // TODO: 2nd download is not deleting in case of 3
    // TODO: if paused all downloads get deleted
    // TODO: delete module is working correctly and location is getting deleted
    @FXML
    private Button prButton;
    @FXML
    private ListView<DownloaderCell> listView;
    @FXML
    private AnchorPane MainWindow;
    @FXML
    private TreeView<String> treeView;
    @FXML
    private Label totalDownloadsLabel, totalSpeedLabel;

    @FXML
    private void addButtonController(ActionEvent actionEvent) {
        new AddPopUp(this, MainWindow);
    }

    @FXML
    private void prButtonController(ActionEvent actionEvent) {
        if (!listView.getSelectionModel().isEmpty()) {
            switch (prButtonState) {
                case PAUSED:
                    listView.getSelectionModel().getSelectedItems().stream().forEach((cell) -> {
                        if (cell.getData().state.equals(State.ACTIVE)) {
                            cell.change(StateAction.PAUSE);
                        }
                    });
                    prButtonState = ButtonState.RESUMED;
                    prButton.setId("ResumeButton");
                    break;
                case RESUMED:
                    listView.getSelectionModel().getSelectedItems().stream().forEach((cell) -> {
                        if (cell.getData().state.equals(State.PAUSED)) {
                            cell.change(StateAction.START);
                        } else if (cell.getData().state.equals(State.FAILED)) {
                            cell.change(StateAction.RESTART);
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
        List<DownloaderCell> deleteList = new LinkedList<>(listView.getSelectionModel().getSelectedItems());
        listView.getSelectionModel().clearSelection();
        deleteList.forEach(cell -> {
            listView.getItems().remove(cell);
            cell.change(StateAction.DELETE);
        });
    }

    @FXML
    private void minimizeButtonController(ActionEvent event) {
        Stage stage = (Stage) MainWindow.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void exitButtonController(ActionEvent event) {
        downloadsList.forEach(cell -> cell.change(StateAction.SHUTDOWN));
        connectionManager.shutdown();
        Platform.exit();
        System.exit(0);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        activeDownloads = 0;
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

        allDownloads.expandedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) allDownloads.setGraphic(new ImageView(new Image(
                    getClass().getResourceAsStream("/resources/Black.png"))));
            else allDownloads.setGraphic(new ImageView(new Image(
                    getClass().getResourceAsStream("/resources/White.png"))));
        });
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

        root.getChildren().addAll(allDownloads, completed, failed, inProgress, paused);

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

        treeView.setRoot(root);
        treeView.setShowRoot(false);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

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
        stateManager.readFromFile().stream().forEach((data) -> {
            DownloaderCell cell = new DownloaderCell(data, this);
            downloadsList.add(cell);
        });
        listView.setItems(downloadsList);
    }

    public void addDownload(StateData data) {

        Predicate<DownloaderCell> predicate = cell -> cell.getData().uri.equals(data.uri);
        Optional<DownloaderCell> optionalCell = downloadsList.stream().filter(predicate).findFirst();

        if (optionalCell.isPresent()) {
            new InListPopUp(MainWindow, this, optionalCell.get(), data);
        } else {
            stateManager.changeState(data, StateActivity.CREATE);
            DownloaderCell cell = new DownloaderCell(data, this);
            downloadsList.add(cell);
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

    public void updateActiveDownloads(boolean isIncremented) {
        if (isIncremented) {
            Platform.runLater(() -> totalDownloadsLabel.setText(String.valueOf(++activeDownloads)));
            speedCalc.updateActiveDownloads(activeDownloads);
        } else {
            Platform.runLater(() -> totalDownloadsLabel.setText(String.valueOf(--activeDownloads)));
            speedCalc.updateActiveDownloads(activeDownloads);
        }
    }

    public CloseableHttpClient getClient() {
        return client;
    }
}

/*----- Download Links -----*/
// http://softlayer-sng.dl.sourceforge.net/project/elementaryos/unstable/elementaryos-unstable-amd64.20140810.iso
// https://www.google.com/calendar/ical/tk5scqbfe80ffdcfj86uppbhvk%40group.calendar.google.com/private-0dfe7855fa8b78365041b2b27261446e/basic.ics
// http://downloads.sourceforge.net/project/sevenzip/7-Zip/9.22/7z922.tar.bz2?r=http%3A%2F%2Fsourceforge.net%2Fprojects%2Fsevenzip%2F&ts=1415925898&use_mirror=kaz
// http://software-files-a.cnet.com/s/software/13/91/24/11/avast_free_antivirus_setup_online.exe?token=1417263074_0969cea50d1231fcf1d9c961806461d7&fileName=avast_free_antivirus_setup_online.exe
// http://downloads.sourceforge.net/project/openofficeorg.mirror/4.1.1/binaries/en-US/Apache_OpenOffice_4.1.1_Linux_x86-64_install-rpm_en-US.tar.gz?r=http%3A%2F%2Fsourceforge.net%2Fprojects%2Fopenofficeorg.mirror%2F%3Fsource%3Ddirectory-featured&ts=1417228120&use_mirror=softlayer-sng
// http://downloads.sourceforge.net/project/nagios/nagios-4.x/nagios-4.0.8/nagios-4.0.8.tar.gz?r=http%3A%2F%2Fsourceforge.net%2Fdirectory%2Fbusiness-enterprise%2Fos%3Alinux%2Ffreshness%3Arecently-updated%2F&ts=1417228511&use_mirror=softlayer-sng

