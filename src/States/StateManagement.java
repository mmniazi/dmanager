/*
 * To change dataObject license header, choose License Headers in Project Properties.
 * To change dataObject template file, choose Tools | Templates
 * and open the template in the editor.
 */
package States;

import Util.State;
import Util.Utilities;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author muhammad
 */
public class StateManagement {

  private static StateManagement instance = null;
  private BlockingQueue<QueueData> writingQueue;
  private RandomAccessFile file;
  private Map<URI, Long> locationMap = new HashMap<>();

  protected StateManagement() {
    writingQueue = new LinkedBlockingDeque<>();
    try {
      file = new RandomAccessFile(System.getProperty("user.home") + "/download.state", "rw");
    } catch (FileNotFoundException ex) {
      Logger.getLogger(StateManagement.class.getName()).log(Level.SEVERE, null, ex);
    }
    startWritingStates();
  }

  public static StateManagement getInstance() {
    if (instance == null) {
      instance = new StateManagement();
    }
    return instance;
  }

  public boolean stateExsists(URI uri) {
    return locationMap.containsKey(uri);
  }

  public void changeState(StateData data, String purpose) {
    writingQueue.offer(new QueueData(data, purpose));
  }

  public ArrayList<StateData> readFromFile() {

    ArrayList<StateData> downloadsList = new ArrayList<>();
    String stateString;
    String stateVariables[];
    long location = 0;
    try {
      while ((stateString = file.readLine()) != null) {
        //Split the line read into variableStrings
        stateVariables = stateString.split("::");
        //Set locationMap
        locationMap.put(URI.create(stateVariables[1]), location);
        //add new object to downloadList
        downloadsList.add(new StateData(
            stateVariables[0],
            URI.create(stateVariables[1]),
            stateVariables[2],
            Utilities.getArrayFromString(stateVariables[3]),
            Utilities.getArrayFromString(stateVariables[4]),
            new AtomicLong(new Long(stateVariables[5])),
            new Integer(stateVariables[6]),
            new Long(stateVariables[7]),
            State.valueOf(stateVariables[8])
        ));
        location = file.getFilePointer();
      }
    } catch (IOException ex) {
      Logger.getLogger(StateManagement.class.getName()).log(Level.SEVERE, null, ex);
    }
    return downloadsList;
  }

  private void startWritingStates() {
    Thread writer = new Thread(() -> {
      QueueData queueData;
      StateData data;
      long location;
      while (true) {
        try {
          queueData = writingQueue.take();
          data = queueData.data;
          switch (queueData.purpose) {
            case "createState":
              file.seek(file.length());
              location = file.getFilePointer();
              file.writeBytes(data.toString());
              file.seek(file.getFilePointer()
                  + 40960 - data.toString().length());
              file.writeBytes("\n");
              locationMap.put(data.uri, location);
              break;
            case "saveState":
              location = locationMap.get(data.uri);
              file.seek(location);
              file.writeBytes(data.toString());
              break;
            case "deleteState":
              location = locationMap.get(data.uri);
              byte[] buffer = new byte[40960];
              int read;
              file.seek(location + 40960);
              while ((read = file.read(buffer)) > -1) {
                file.seek(file.getFilePointer() - read - 40960);
                file.write(buffer, 0, read);
                file.seek(file.getFilePointer() + 40960);
              }
              file.setLength(file.length() - 40960);
              locationMap.remove(data.uri);
              break;
          }
        } catch (InterruptedException | IOException ex) {
          Logger.getLogger(StateManagement.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    });
    writer.start();
  }

  private class QueueData {

    StateData data;
    String purpose;

    public QueueData(StateData data, String purpose) {
      this.data = data;
      this.purpose = purpose;
    }

  }
}
