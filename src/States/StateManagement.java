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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author muhammad
 */
public class StateManagement {

    private static StateManagement instance = null;
    private BlockingQueue<dataChange> changeList;
    private RandomAccessFile file;
    private ConcurrentMap<URI, Long> locationMap;

    protected StateManagement() {
        locationMap = new ConcurrentHashMap<>();
        changeList = new LinkedBlockingQueue<>();
        try {
            file = new RandomAccessFile(System.getProperty("user.home") + "/download.state", "rw");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(StateManagement.class.getName()).log(Level.SEVERE, null, ex);
        }
        start();
    }

    public static StateManagement getInstance() {
        if (instance == null) {
            instance = new StateManagement();
        }
        return instance;
    }

    private void start() {
        new Thread(() -> {
            while (true) try {
                dataChange change;
                StateData data;
                StateActivity purpose;
                long location;
                change = changeList.take();
                data = change.data;
                purpose = change.purpose;
                switch (purpose) {
                    case CREATE:
                        file.seek(file.length());
                        location = file.getFilePointer();
                        file.writeBytes(data.toString());
                        file.seek(location + 40960);
                        file.writeBytes("\n");
                        locationMap.put(data.uri, location);
                        break;
                    case SAVE:
                        if (locationMap.containsKey(data.uri)) {
                            location = locationMap.get(data.uri);
                            file.seek(location);
                            file.writeBytes(data.toString());
                        }
                        break;
                    case DELETE:
                        location = locationMap.get(data.uri);
                        locationMap.remove(data.uri);
                        byte[] buffer = new byte[40961];
                        file.seek(location + 40961);
                        for (int i = 0; file.read(buffer) > -1; i++) {
                            file.seek(location + i * 40961);
                            file.write(buffer);
                            file.seek(location + (i + 2) * 40961);
                        }
                        file.setLength(file.length() - 40961);
                        break;
                }
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(StateManagement.class.getName()).log(Level.SEVERE, null, ex);
            }
        }).start();
    }

    public void changeState(StateData data, StateActivity purpose) {
        try {
            changeList.put(new dataChange(data, purpose));
        } catch (InterruptedException ex) {
            Logger.getLogger(StateManagement.class.getName()).log(Level.SEVERE, null, ex);
        }
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
