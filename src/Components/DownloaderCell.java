/*
Version 2.0 :
TODO: Check for user permissions for file(in fact there is a method to add administrator rights to your application)
TODO: Use some method of accurate time instead of sleep for exact speed calculation
TODO: handle failed downloads
TODO: -1 is returned when i try to download calendar data from link.
TODO: Tweak speed calculator to adjust its speed to expected level.
*/

/*
* TODO: Total Speed is not working sometimes doubling and after pause resume not updating
* */
package Components;

import Controllers.layoutController;
import States.StateActivity;
import States.StateData;
import States.StateManagement;
import Util.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DownloaderCell extends ListCell {
    private StateManagement stateManager;
    private TotalSpeedCalc speedCalc;
    private layoutController controller;
    private StateData data;
    private RandomAccessFile file;
    private CloseableHttpClient client;
    private FileChannel fileChannel;
    private ExecutorService threadService;
    private long currentBytes;
    private String type;
    private boolean exiting;

    @FXML
    private AnchorPane cell;
    @FXML
    private Button defaultButton;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private CheckBox checkBox;
    @FXML
    private Label fileLabel, sDoneLabel, sTotalLabel, timeLabel, speedLabel, statusLabel;

    public DownloaderCell(StateData data, layoutController controller) {
        speedCalc = TotalSpeedCalc.getInstance();
        stateManager = StateManagement.getInstance();
        this.data = data;
        this.currentBytes = data.bytesDone.get();
        this.controller = controller;
        this.client = controller.getClient();
        type = Utilities.findType(Utilities.getFromURI(data.uri.toString(), UriPart.EXT));
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ListCell.fxml"));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        initializeCell();
        if (data.state.equals(State.ACTIVE)) {
            change(StateAction.START);
        }
    }

    public void change(StateAction action) {
        switch (action) {
            case DELETE:
                if (data.state.equals(State.ACTIVE)) {
                    pause();
                }
                delete();
                break;
            case PAUSE:
                if (data.state.equals(State.ACTIVE)) {
                    pause();
                }
                break;
            case SHUTDOWN:
                exit();
                break;
            case START:
                start();
                break;
            case RESTART:
                resetData();
                start();
                break;
            case OPEN:
                openInExplorer(data.downloadDirectory + data.fileName);
                break;
            case OPENFOLDER:
                openInExplorer(data.downloadDirectory);
                break;
        }
    }

    private void openInExplorer(String file) {
        String OS = System.getProperty("os.name").toLowerCase();
        if (OS.contains("win")) {
            try {
                Runtime.getRuntime().exec("explorer.exe /select, " + file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (OS.contains("mac")) {
            try {
                Runtime.getRuntime().exec("open -R " + file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (OS.contains("nix") || OS.contains("nux") || OS.contains("aix")) {
            try {
                Runtime.getRuntime().exec("xdg-open " + file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initializeCell() {
        preSetGUI();
        switch (data.state) {

            case SHDLED:
                defaultButton.setText("Start");

                defaultButton.setOnAction((ActionEvent event) -> change(StateAction.START));
                break;

            case ACTIVE:
                cell.getStylesheets().clear();
                cell.getStylesheets().add(getClass().
                        getResource("/css/ActiveCell.css").toExternalForm());
                defaultButton.setText("Pause");

                defaultButton.setOnAction((ActionEvent event) -> change(StateAction.PAUSE));
                break;

            case CMPLTD:
                cell.getStylesheets().clear();
                cell.getStylesheets().add(getClass().
                        getResource("/css/CompletedCell.css").toExternalForm());
                defaultButton.setText("Open");

                defaultButton.setOnMouseClicked((MouseEvent event) -> {
                    if (event.isControlDown()) change(StateAction.OPENFOLDER);
                    else change(StateAction.OPEN);
                });
                break;

            case FAILED:
                cell.getStylesheets().clear();
                cell.getStylesheets().add(getClass().
                        getResource("/css/FailedCell.css").toExternalForm());
                defaultButton.setText("Restart");

                defaultButton.setOnAction((ActionEvent event) -> change(StateAction.RESTART));
                break;

            case PAUSED:
                cell.getStylesheets().clear();
                cell.getStylesheets().add(getClass().
                        getResource("/css/PausedCell.css").toExternalForm());
                defaultButton.setText("Resume");

                defaultButton.setOnAction((ActionEvent event) -> change(StateAction.START));
                break;
        }
    }

    private void start() {
        data.state = State.ACTIVE;
        exiting = false;
        threadService = Executors.newCachedThreadPool();
        Platform.runLater(this::initializeCell);
        connect();
        update();
    }

    private void pause() {
        data.state = State.PAUSED;
        stateManager.changeState(data, StateActivity.SAVE);
        controller.updateActiveDownloads(false);
        Platform.runLater(this::initializeCell);
        exit();
    }

    private void complete() {
        data.state = State.CMPLTD;
        stateManager.changeState(data, StateActivity.SAVE);
        controller.updateActiveDownloads(false);
        Platform.runLater(this::initializeCell);
        exit();
    }

    private void delete() {
        try {
            Files.deleteIfExists(Paths.get(data.downloadDirectory, data.fileName));
        } catch (IOException ex) {
            Logger.getLogger(DownloaderCell.class.getName()).log(Level.SEVERE, null, ex);
        }
        stateManager.changeState(data, StateActivity.DELETE);
    }

    private void exit() {
        exiting = true;
        try {
            fileChannel.close();
            file.close();
        } catch (IOException ex) {
            Logger.getLogger(DownloaderCell.class.getName()).log(Level.SEVERE, null, ex);
        }
        threadService.shutdown();
    }

    private void resetData() {
        data = new StateData(data.downloadDirectory, data.uri,
                data.downloadDirectory, data.segments);
    }

    private void preSetGUI() {
        if (data.sizeOfFile == 0) {
            fileLabel.setText(data.fileName);
            progressBar.setProgress(0);
            statusLabel.setText("");
        } else if (data.bytesDone.get() == data.sizeOfFile) {
            fileLabel.setText(data.fileName);
            sTotalLabel.setText(Utilities.sizeConverter(data.sizeOfFile));
            progressBar.setProgress(1);
            statusLabel.setText("Done");
        } else {
            fileLabel.setText(data.fileName);
            sDoneLabel.setText(Utilities.sizeConverter(data.bytesDone.get()));
            sTotalLabel.setText(Utilities.sizeConverter(data.sizeOfFile));
            progressBar.setProgress(data.bytesDone.floatValue() / data.sizeOfFile);
            statusLabel.setText("(" + ((data.bytesDone.get() * 100) / data.sizeOfFile) + "%" + ")");
        }
    }

    private void connect() {
        threadService.execute(() -> {
            try {
                file = new RandomAccessFile(new File(data.downloadDirectory + data.fileName), "rwd");
                fileChannel = file.getChannel();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(DownloaderCell.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (!data.initialized) {
                try {
                    HttpGet sizeGet = new HttpGet(data.uri);
                    CloseableHttpResponse sizeResponse = client.execute(sizeGet);
                    data.sizeOfFile = sizeResponse.getEntity().getContentLength();
                    if (data.sizeOfFile != -1 && !exiting) {
                        file.setLength(data.sizeOfFile);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(DownloaderCell.class.getName()).log(Level.SEVERE, null, ex);
                }
                try {
                    HttpGet segmentsGet = new HttpGet(data.uri);
                    segmentsGet.setHeader("Range", "bytes=" + 0 + "-" + 1);
                    CloseableHttpResponse segmentsResponse = client.execute(segmentsGet);
                    if (segmentsResponse.getStatusLine().getStatusCode() != 206) {
                        data.segments = 1;
                    }
                } catch (IOException ex) {
                    Logger.getLogger(DownloaderCell.class.getName()).log(Level.SEVERE, null, ex);
                }

                long sizeOfEachSegment = optimizeSegments();

                if (data.segments == 1) {
                    data.initialState = new AtomicLongArray(1);
                    data.finalState = new AtomicLongArray(1);
                    data.initialState.set(0, 0);
                    data.finalState.set(0, data.sizeOfFile);
                } else {
                    data.initialState = new AtomicLongArray(data.segments);
                    data.finalState = new AtomicLongArray(data.segments);
                    for (int i = 0; i < data.segments - 1; i++) {
                        data.initialState.set(i, i * sizeOfEachSegment);
                        data.finalState.set(i, (i + 1) * sizeOfEachSegment);
                    }

                    data.initialState.set(data.segments - 1, data.segments * sizeOfEachSegment);
                    data.finalState.set(data.segments - 1, data.sizeOfFile);
                }
                if (!exiting) {
                    data.state = State.ACTIVE;
                    data.initialized = true;
                    stateManager.changeState(data, StateActivity.SAVE);
                }
            }

            startThreads();
        });
    }

    private void update() {
        threadService.execute(() -> {
            boolean initialPhase = true;
            int averageSize = 5;
            controller.updateActiveDownloads(true);
            List<Float> list = new ArrayList<>();
            for (int counter = 0; data.state == State.ACTIVE && !exiting; counter++) {
                stateManager.changeState(data, StateActivity.SAVE);
                float averageSpeed = 0;
                float speed = (data.bytesDone.get() - currentBytes);
                currentBytes = data.bytesDone.get();
                list.add(speed);

                if (counter > 50) initialPhase = false;
                if (!initialPhase) averageSize = 50;
                if (list.size() > averageSize) {
                    list.remove(0);
                }

                averageSpeed = list.stream()
                        .map((increment) -> increment)
                        .reduce(averageSpeed, (accumulator, _item) -> accumulator + _item);
                averageSpeed /= list.size();
                speedCalc.updateTotalSpeed(averageSpeed);
                if (exiting) break;
                final float finalAverageSpeed = averageSpeed;
                Platform.runLater(() -> {
                    if (data.sizeOfFile == 0) {
                        speedLabel.setText("");
                        sDoneLabel.setText("");
                        sTotalLabel.setText("");
                        progressBar.setProgress(-1);
                        timeLabel.setText("");
                        statusLabel.setText("Connecting");
                    } else if (data.state.equals(State.CMPLTD)) {
                        speedLabel.setText("");
                        sDoneLabel.setText("");
                        progressBar.setProgress(1);
                        timeLabel.setText("");
                        statusLabel.setText("Done");
                    } else if (data.state.equals(State.PAUSED)) {
                        speedLabel.setText("");
                        timeLabel.setText("");
                    } else {
                        speedLabel.setText(Utilities.speedConverter(finalAverageSpeed));
                        sDoneLabel.setText(Utilities.sizeConverter(data.bytesDone.get()));
                        sTotalLabel.setText(Utilities.sizeConverter(data.sizeOfFile));
                        progressBar.setProgress(data.bytesDone.floatValue() / data.sizeOfFile);
                        timeLabel.setText(
                                Utilities.timeConverter(data.sizeOfFile - data.bytesDone.get(), finalAverageSpeed));
                        statusLabel.setText("(" + (data.bytesDone.get() * 100) / data.sizeOfFile + "%" + ")");
                    }
                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        });
    }

    private void startThreads() {
        for (int i = 0; i < data.segments && !exiting; i++) {
            if (data.initialState.get(i) < data.finalState.get(i)) {
                threadService.execute(new Segment(i));
            }
        }
    }

    private long optimizeSegments() {
        long sizeOfEachSegment = data.sizeOfFile / data.segments;
        if (sizeOfEachSegment >= 1024 * 1024 || data.segments == 1) return sizeOfEachSegment;
        else {
            --data.segments;
            return optimizeSegments();
        }
    }

    public AnchorPane getCell() {
        return cell;
    }

    public boolean getCheckBoxValue() {
        return checkBox.isSelected();
    }

    public void setCheckBoxValue(boolean bool) {
        checkBox.setSelected(bool);
    }

    public CheckBox getCheckBox() {
        return checkBox;
    }

    public String getType() {
        return type;
    }

    public StateData getData() {
        return data;
    }

    public void setData(StateData data) {
        this.data = data;
    }

    private class Segment implements Runnable {

        int name;
        long delta;

        public Segment(int name) {
            this.name = name;
        }

        @Override
        public void run() {
            try {
                HttpGet get = new HttpGet(data.uri);
                String byteRange = data.initialState.get(name) + "-" + data.finalState.get(name);
                get.setHeader("Range", "bytes=" + byteRange);
                CloseableHttpResponse response = client.execute(get);
                ReadableByteChannel inputChannel
                        = Channels.newChannel(response.getEntity().getContent());

                long BUFFER_SIZE = 1024 * 8;
                while (data.state.equals(State.ACTIVE) && !exiting) {
                    if ((data.finalState.get(name) - data.initialState.get(name))
                            >= BUFFER_SIZE) {
                        fileChannel.transferFrom(
                                inputChannel, data.initialState.get(name), BUFFER_SIZE);
                        data.initialState.addAndGet(name, BUFFER_SIZE);
                        data.bytesDone.addAndGet(BUFFER_SIZE);
                    } else {
                        long NEW_BUFFER_SIZE = data.finalState.get(name)
                                - data.initialState.get(name);
                        fileChannel.transferFrom(
                                inputChannel, data.initialState.get(name), NEW_BUFFER_SIZE);
                        data.initialState.addAndGet(name, NEW_BUFFER_SIZE);
                        data.bytesDone.addAndGet(NEW_BUFFER_SIZE);
                        break;
                    }
                }
                response.close();
                inputChannel.close();
                if (data.bytesDone.get() == data.sizeOfFile) {
                    complete();
                }

                if (data.state.equals(State.ACTIVE) && !exiting) {
                    for (int i = 0; i < data.initialState.length(); i++) {
                        delta = data.finalState.get(i) - data.initialState.get(i);
                        if (delta > 0.05 * data.sizeOfFile && delta > 1024 * 1024) {
                            data.finalState.set(name, data.finalState.get(i));
                            data.finalState.set(i, data.finalState.get(i) - delta / 2);
                            data.initialState.set(name, data.finalState.get(i));
                            threadService.execute(new Segment(name));
                            break;
                        }
                    }
                }

            } catch (IOException ignored) {
            }
        }
    }
}
