/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author muhammad
 */
public class DownloaderCell extends ListCell {

    // TODO: Check for user permissions for file(in fact there is a method to add administrator rights to your application)
    // TODO: Check performance of program.
    // TODO: create a mechanism that will stop download being paused and resumed to quickly && resuming of already completed downloads
    // TODO: -1 is returned when i try to download calendar data from link.
    // TODO: if download is paused and resumed instantly then java.io.IOException: Stream Closed is thrown
    // TODO: if download is paused while connecting.
    // TODO: safe guard against controller prbutton
    // TODO: Use some method of accurate time instead of sleep for exact speed calculation
    // TODO: Download should not be resumed until all threads are closed. Some counter may be used ** imp
    // imp: ok make a blocking queue which accepts changes and then make changes appear on all threads when all is updated then implement next change

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
        exiting = false;
        speedCalc = TotalSpeedCalc.getInstance();
        stateManager = StateManagement.getInstance();
        this.data = data;
        this.currentBytes = data.bytesDone.get();
        this.controller = controller;
        this.client = controller.getClient();
        this.threadService = Executors.newCachedThreadPool();
        type = Utilities.findType(Utilities.getFromURI(data.uri.toString(), UriPart.EXT));
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ListCell.fxml"));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void change(StateAction action) {
        switch (action) {
            case DELETE:
                delete();
                break;
            case INITIALIZE:
                initializeCell();
                break;
            case PAUSE:
                if (data.state.equals(State.ACTIVE)) {
                    pause();
                }
                break;
            case RESET:
                resetData();
                break;
            case SHUTDOWN:
                exit();
                break;
        }
    }

    private void initializeCell() {
        preSetGUI();
        switch (data.state) {

            case SHDLED:
                defaultButton.setText("Start");
                defaultButton.setOnAction((ActionEvent event) -> {
                    connect();
                    update();
                });
                break;

            case ACTIVE:
                cell.getStylesheets().clear();
                cell.getStylesheets().add(getClass().
                        getResource("/css/ActiveCell.css").toExternalForm());
                defaultButton.setText("Pause");
                connect();
                update();
                defaultButton.setOnAction((ActionEvent event) -> pause());
                break;

            case CMPLTD:
                cell.getStylesheets().clear();
                cell.getStylesheets().add(getClass().
                        getResource("/css/CompletedCell.css").toExternalForm());
                defaultButton.setText("Open");
                defaultButton.setOnMouseClicked((MouseEvent event) -> {
                    if (event.isControlDown()) {
                        try {
                            Desktop.getDesktop().open(new File(data.downloadDirectory));
                        } catch (IOException ex) {
                            Logger.getLogger(DownloaderCell.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        try {
                            Desktop.getDesktop().open(new File(data.downloadDirectory + data.fileName));
                        } catch (IOException ex) {
                            Logger.getLogger(DownloaderCell.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
                break;

            case FAILED:
                cell.getStylesheets().clear();
                cell.getStylesheets().add(getClass().
                        getResource("/css/FailedCell.css").toExternalForm());
                defaultButton.setText("Restart");
                defaultButton.setOnAction((ActionEvent event) -> {
                    resetData();
                    connect();
                    update();
                });
                break;

            case PAUSED:
                cell.getStylesheets().clear();
                cell.getStylesheets().add(getClass().
                        getResource("/css/PausedCell.css").toExternalForm());
                defaultButton.setText("Resume");
                defaultButton.setOnAction((ActionEvent event) -> {
                    data.state = State.ACTIVE;
                    Platform.runLater(this::initializeCell);
                    threadService = Executors.newCachedThreadPool();
                });
                break;
        }
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
                Logger.getLogger(
                        DownloaderCell.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (!data.initialized && !exiting) {
                try {
                    HttpGet sizeGet = new HttpGet(data.uri);
                    CloseableHttpResponse sizeResponse = client.execute(sizeGet);
                    data.sizeOfFile = sizeResponse.getEntity().getContentLength();
                    file.setLength(data.sizeOfFile);
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

                if (data.segments == 1 && !exiting) {
                    data.initialState = new AtomicLongArray(1);
                    data.finalState = new AtomicLongArray(1);
                    data.initialState.set(0, 0);
                    data.finalState.set(0, data.sizeOfFile);
                } else if (!exiting) {
                    long sizeOfEachSegment = data.sizeOfFile / data.segments;
                    data.initialState = new AtomicLongArray(data.segments);
                    data.finalState = new AtomicLongArray(data.segments);
                    for (int i = 0; i < data.segments - 1; i++) {
                        data.initialState.set(i, i * sizeOfEachSegment);
                        data.finalState.set(i, (i + 1) * sizeOfEachSegment);
                    }

                    data.initialState.set(
                            data.segments - 1, data.segments * sizeOfEachSegment);
                    data.finalState.set(data.segments - 1, data.sizeOfFile);
                }
                if (!exiting) {
                    data.state = State.ACTIVE;
                    data.initialized = true;
                    stateManager.changeState(data, StateActivity.SAVE);
                }
            }

            start();
        });
    }

    private void update() {
        threadService.execute(() -> {
            boolean initialPhase = true;
            int averageSize = 5;
            controller.updateActiveDownloads(true);
            List<Float> list = new ArrayList<>();
            for (int counter = 0; data.state == State.ACTIVE && !exiting; counter++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
                stateManager.changeState(data, StateActivity.SAVE);
                float averageSpeed = 0;
                float speed = (data.bytesDone.get() - currentBytes);
                currentBytes = data.bytesDone.get();
                list.add(speed);
                // TODO: check changes
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
                        timeLabel.setText(Utilities.timeConverter(data.sizeOfFile - data.bytesDone.get(), finalAverageSpeed));
                        statusLabel.setText("(" + (data.bytesDone.get() * 100) / data.sizeOfFile + "%" + ")");
                    }
                });
            }
        });
    }

    private void start() {
        for (int i = 0; i < data.segments && !exiting; i++) {
            if (data.initialState.get(i) < data.finalState.get(i)) {
                threadService.execute(new Segment(i));
            }
        }
    }

    private void pause() {
        data.state = State.PAUSED;
        stateManager.changeState(data, StateActivity.SAVE);
        controller.updateActiveDownloads(false);
        exit();
        Platform.runLater(this::initializeCell);
    }

    private void complete() {
        data.state = State.CMPLTD;
        stateManager.changeState(data, StateActivity.SAVE);
        controller.updateActiveDownloads(false);
        exit();
        Platform.runLater(this::initializeCell);
    }

    private void delete() {
        try {
            Files.deleteIfExists(Paths.get(data.downloadDirectory, data.fileName));
        } catch (IOException ex) {
            Logger.getLogger(DownloaderCell.class.getName()).log(Level.SEVERE, null, ex);
        }
        stateManager.changeState(data, StateActivity.DELETE);
    }

    private void resetData() {
        data = new StateData(data.downloadDirectory, data.uri,
                data.downloadDirectory, data.segments);
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
        try {
            threadService.awaitTermination(5000, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            Logger.getLogger(DownloaderCell.class.getName()).log(Level.SEVERE, null, ex);
        }
        exiting = false;
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

        long delta;
        int name;

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

                ByteBuffer buff = ByteBuffer.allocate(4096);
                while (data.state.equals(State.ACTIVE) && !exiting) {
                    if ((data.finalState.get(name) - data.initialState.get(name))
                            >= buff.capacity()) {
                        inputChannel.read(buff);
                        buff.flip();
                        fileChannel.write(buff);
                        buff.compact();
                        data.initialState.addAndGet(name, buff.capacity());
                        data.bytesDone.addAndGet(buff.capacity());
                    } else {
                        buff = ByteBuffer.allocate((int) (data.finalState.get(name)
                                - data.initialState.get(name)));
                        inputChannel.read(buff);
                        buff.flip();
                        fileChannel.write(buff);
                        buff.compact();
                        data.initialState.addAndGet(name, buff.capacity());
                        data.bytesDone.addAndGet(buff.capacity());
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
                        if (delta > 5242880) {
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
