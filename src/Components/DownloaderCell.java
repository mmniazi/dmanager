/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Components;

import States.StateData;
import States.StateManagement;
import Util.State;
import Util.Utilities;
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
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author muhammad
 */
public class DownloaderCell extends ListCell {

  private StateManagement stateManager = StateManagement.getInstance();
  private StateData data;
  private RandomAccessFile file;
  private CloseableHttpClient client;
  private FileChannel fileChannel;
  private ExecutorService threadService;
  private long currentBytes;
  private String type;

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

  public DownloaderCell(StateData data, CloseableHttpClient client) {
    this.data = data;
    this.currentBytes = data.bytesDone.get();
    this.client = client;
    this.threadService = Executors.newCachedThreadPool();
    type = Utilities.findType(Utilities.getFromURI(data.uri.toString(), "ext"));
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ListCell.fxml"));
    fxmlLoader.setController(this);
    try {
      fxmlLoader.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // TODO: Check for user permissions for file(in fact there is a method to add administrator rights to your application)
  // TODO: Check performance of program.
  public void set() {
    // Setting data
    preSetGui();
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
        defaultButton.setOnAction((ActionEvent event) -> stopDownload());
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
            } catch (IOException e) {
              e.printStackTrace();
            }
          } else {
            try {
              Desktop.getDesktop().open(new File(data.downloadDirectory + data.fileName));
            } catch (IOException e) {
              e.printStackTrace();
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
          Platform.runLater(this::set);
        });
        break;
    }
  }

  public void stopDownload() {
    threadService.execute(() -> {
      try {
        data.state = State.PAUSED;
        fileChannel.close();
        file.close();
        stateManager.changeState(data, "saveState");
        Platform.runLater(this::set);
      } catch (IOException ex) {
        Logger.getLogger(DownloaderCell.class.getName()).log(Level.SEVERE, null, ex);
      }
    });
  }

  //TODO: make sure every thread ends on end what ever the case
  //TODO: On resuming the download speed haves, maybe using a separate client will help
  /*TODO: InshaAllah I wil be creating a separate threadservice for each cell and then ending that service using application thread and then as usual set method will be called on application thread*/
  private void connect() {
    threadService.execute(() -> {
      // create an empty file
      try {
        file = new RandomAccessFile(new File(data.downloadDirectory + data.fileName), "rwd");
      } catch (FileNotFoundException ex) {
        Logger.getLogger(
                DownloaderCell.class.getName()).log(Level.SEVERE, null, ex);
      }

      fileChannel = file.getChannel();
      // Get request for checking size of file.
      HttpGet checkingGet = new HttpGet(data.uri);

      // TODO: -1 is returned when i try to download calendar data from link.
      // TODO: if download is paused and resumed instantly then java.io.IOException: Stream Closed is thrown
      // TODO: if download is paused while connecting.
      CloseableHttpResponse checkingResponse;
      try {
        checkingResponse = client.execute(checkingGet);
        data.sizeOfFile = checkingResponse.getEntity().getContentLength();
        file.setLength(data.sizeOfFile);
        if (checkingResponse.getStatusLine().getStatusCode() != 206) {
          data.segments = 1;
        }
      } catch (IOException ex) {
        Logger.getLogger(DownloaderCell.class.getName()).log(Level.SEVERE, null, ex);
      }


      // Set range header for checking server support for partial content.
      checkingGet.setHeader("Range", "bytes=" + 0 + "-" + 1);

      // If server support partial content then download using multiple threads.
      // Initialize initial and final State array.
      if (data.segments == 1) {
        data.initialState = new AtomicLongArray(1);
        data.finalState = new AtomicLongArray(1);
        data.initialState.set(0, 0);
        data.finalState.set(0, data.sizeOfFile);
      } else {
        long sizeOfEachSegment = data.sizeOfFile / data.segments;
        data.initialState = new AtomicLongArray(data.segments + 1);
        data.finalState = new AtomicLongArray(data.segments + 1);
        for (int i = 0; i < data.segments; i++) {
          data.initialState.set(i, i * sizeOfEachSegment);
          data.finalState.set(i, (i + 1) * sizeOfEachSegment);
        }
        // assign remaining bytes to last segment.
        data.initialState.set(
                data.segments, data.segments * sizeOfEachSegment);
        data.finalState.set(data.segments, data.sizeOfFile);
      }
      data.state = State.ACTIVE;
      stateManager.changeState(data, "saveState");
      start();
    });
  }

  private void start() {
    threadService.execute(() -> {
      //Download each segment independently.
      for (int i = 0; i <= data.segments; i++) {
        if (data.initialState.get(i) < data.finalState.get(i)) {
          threadService.execute(new Segment(i));
        }
      }
    });
  }

  private void preSetGui() {
    if (data.sizeOfFile == 0) {
      fileLabel.setText(data.fileName);
    } else if (data.bytesDone.get() == data.sizeOfFile) {
      fileLabel.setText(data.fileName);
      sTotalLabel.setText(Utilities.sizeConverter(data.sizeOfFile));
      progressBar.setProgress(1);
      statusLabel.setText("Done");
    } else {
      fileLabel.setText(data.fileName);
      sDoneLabel.setText(Utilities.sizeConverter(data.bytesDone.get()));
      sTotalLabel.setText(Utilities.sizeConverter(data.sizeOfFile));
      progressBar.setProgress(data.bytesDone.get() / data.sizeOfFile);
      statusLabel.setText("(" + (data.bytesDone.get() / data.sizeOfFile) * 100 + "%" + ")");
    }
  }

  // TODO: speed calculation giving a bit low results. It can be jugated by increasing the sleep time.
  private void update() {
    threadService.execute(() -> {
      List<Float> list = new ArrayList<>();
      do {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        stateManager.changeState(data, "saveState");
        float averageSpeed = 0;
        // Calculating
        float speed = (data.bytesDone.get() - currentBytes);
        currentBytes = data.bytesDone.get();
        list.add(speed);
        if (list.size() > 5) {
          list.remove(0);
        }
        for (Float increment : list) {
          averageSpeed += increment;
        }
        averageSpeed /= list.size();
        // Updating Gui //
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
            progressBar.setProgress(data.bytesDone.get() / data.sizeOfFile);
            timeLabel.setText(Utilities.timeConverter(data.sizeOfFile - data.bytesDone.get(), finalAverageSpeed));
            statusLabel.setText("(" + (data.bytesDone.get() / data.sizeOfFile) * 100 + "%" + ")");
          }
        });
      } while (data.state.equals(State.ACTIVE) && data.bytesDone.get() != data.sizeOfFile);
    });
  }

  private void complete() {
    data.state = State.CMPLTD;
    try {
      fileChannel.close();
      file.close();
    } catch (IOException ex) {
      Logger.getLogger(DownloaderCell.class.getName()).log(Level.SEVERE, null, ex);
    }
    stateManager.changeState(data, "saveState");
    Platform.runLater(this::set);
  }

  private void resetData() {
    data = new StateData(data.downloadDirectory, data.uri,
            data.downloadDirectory, data.segments);
  }

  public AnchorPane getCell() {
    return cell;
  }

  public void selectCheckBox(boolean bool) {
    checkBox.setSelected(bool);
  }

  public String getType() {
    return type;
  }

  public StateData getData() {
    return data;
  }

  private class Segment implements Runnable {

    long delta;
    int name;

    public Segment(int name) {
      this.name = name;
    }

    @Override
    public void run() {
      Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
      // TODO: complete method is not being called
      try {
        HttpGet get = new HttpGet(data.uri);
        // Range header for defining which segment of file we want to receive.
        String byteRange = data.initialState.get(name) + "-" + data.finalState.get(name);
        get.setHeader("Range", "bytes=" + byteRange);
        CloseableHttpResponse response = client.execute(get);
        ReadableByteChannel inputChannel
                = Channels.newChannel(response.getEntity().getContent());
        while (data.state.equals(State.ACTIVE)) {
          long BUFFER_SIZE = 1024 * 8;
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
        System.out.println("Alhamdullilah");
        if (data.bytesDone.get() == data.sizeOfFile) {
          complete();
        }
        // Algorithm for dynamic segmentation.
        if (data.state.equals(State.ACTIVE)) {
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

      } catch (IOException ex) {
        Logger.getLogger(DownloaderCell.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
}
