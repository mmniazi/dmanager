/*
 * To change dataObject license header, choose License Headers in Project Properties.
 * To change dataObject template file, choose Tools | Templates
 * and open the template in the editor.
 */
package States;

import Util.State;
import Util.Utilities;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author muhammad
 */
public class StateManagement {

    private static StateManagement instance = null;
    private ObservableList<dataChange> changeList;
    private RandomAccessFile file;
    private Map<URI, Long> locationMap;

    protected StateManagement() {
        locationMap = new HashMap<>();
        changeList = FXCollections.observableArrayList();
        try {
            file = new RandomAccessFile(System.getProperty("user.home") + "/download.state", "rw");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(StateManagement.class.getName()).log(Level.SEVERE, null, ex);
        }

        changeList.addListener((ListChangeListener<dataChange>) change -> {
            if (change.next()) {
                change.getAddedSubList().forEach(dataChange -> {
                    StateData data;
                    long location;
                    data = dataChange.data;
                    try {
                        switch (dataChange.purpose) {
                            case CREATE:
                                file.seek(file.length());
                                location = file.getFilePointer();
                                file.writeBytes(data.toString());
                                file.seek(file.getFilePointer()
                                        + 40960 - data.toString().length());
                                file.writeBytes("\n");
                                locationMap.put(data.uri, location);
                                break;
                            case SAVE:
                                location = locationMap.get(data.uri);
                                file.seek(location);
                                file.writeBytes(data.toString());
                                break;
                            case DELETE:
                                location = locationMap.get(data.uri);
                                byte[] buffer = new byte[40961];
                                int read;
                                file.seek(location + 40961);
                                while ((read = file.read(buffer)) > -1) {
                                    file.seek(file.getFilePointer() - read - 40961);
                                    file.write(buffer, 0, read);
                                    file.seek(file.getFilePointer() + 40961);
                                }
                                file.setLength(file.length() - 40961);
                                locationMap.remove(data.uri);
                                break;
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(StateManagement.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
            }
        });
    }

    public static StateManagement getInstance() {
        if (instance == null) {
            instance = new StateManagement();
        }
        return instance;
    }

    public void changeState(StateData data, StateActivity purpose) {
        changeList.add(new dataChange(data, purpose));
    }

    public ArrayList<StateData> readFromFile() {

        ArrayList<StateData> downloadsList = new ArrayList<>();
        String stateString;
        String stateVariables[];
        long location = 0;
        try {
            while ((stateString = file.readLine()) != null) {
                stateVariables = stateString.split("::");
                locationMap.put(URI.create(stateVariables[1]), location);
                downloadsList.add(new StateData(
                        stateVariables[0],
                        URI.create(stateVariables[1]),
                        stateVariables[2],
                        Utilities.getArrayFromString(stateVariables[3]),
                        Utilities.getArrayFromString(stateVariables[4]),
                        new AtomicLong(new Long(stateVariables[5])),
                        new Integer(stateVariables[6]),
                        new Long(stateVariables[7]),
                        State.valueOf(stateVariables[8]),
                        Boolean.valueOf(stateVariables[9])
                ));
                location = file.getFilePointer();
            }
        } catch (IOException ex) {
            Logger.getLogger(StateManagement.class.getName()).log(Level.SEVERE, null, ex);
        }
        return downloadsList;
    }

    private class dataChange {

        StateData data;
        StateActivity purpose;

        public dataChange(StateData data, StateActivity purpose) {
            this.data = data;
            this.purpose = purpose;
        }
    }

}
