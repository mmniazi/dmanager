/*
 * To change dataObject license header, choose License Headers in Project Properties.
 * To change dataObject template file, choose Tools | Templates
 * and open the template in the editor.
 */
package States;

import Util.State;
import Util.Utilities;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.JSONValue;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
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
    private Path filePath;
    private JSONObject json;

    protected StateManagement() {
        json = new JSONObject();
        changeList = new LinkedBlockingQueue<>();
        try {
            filePath = Paths.get(System.getProperty("user.home") + "/download.json");
            file = new RandomAccessFile(filePath.toFile(), "rwd");
        } catch (IOException ex) {
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

    public void changeState(StateData data, StateActivity purpose) {
        try {
            changeList.put(new dataChange(data, purpose));
        } catch (InterruptedException ex) {
            Logger.getLogger(StateManagement.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void start() {
        new Thread(() -> {
            while (true) try {
                dataChange change = changeList.take();
                StateData data = change.data;
                StateActivity purpose = change.purpose;
                switch (purpose) {
                    case CREATE:
                        json.put(data.uri.toString(), data.toString());
                        writeChanges();
                        break;
                    case SAVE:
                        json.replace(data.uri.toString(), data.toString());
                        writeChanges();
                        break;
                    case DELETE:
                        json.remove(data.uri.toString());
                        writeChanges();
                        break;
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(StateManagement.class.getName()).log(Level.SEVERE, null, ex);
            }
        }).start();
    }

    private void writeChanges() {
        try {
            file.setLength(0);
            file.seek(0);
            file.writeChars(JSONValue.toJSONString(json, JSONStyle.MAX_COMPRESS));
        } catch (IOException ex) {
            Logger.getLogger(StateManagement.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ArrayList<StateData> readFromFile() {

        ArrayList<StateData> downloadsList = new ArrayList<>();
        try {
            if (file.length() != 0) {
                String string = new String(Files.readAllBytes(filePath)).replace("\u0000", "");
                json = (JSONObject) JSONValue.parse(string);
                json.forEach((o, o2) -> downloadsList.add(convert(o2.toString())));
            }
        } catch (IOException ex) {
            Logger.getLogger(StateManagement.class.getName()).log(Level.SEVERE, null, ex);
        }
        return downloadsList;
    }

    private StateData convert(String stateString) {
        String stateVariables[] = stateString.split("::");
        return new StateData(
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
        );
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
