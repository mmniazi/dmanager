/*
 * To change this license header; choose License Headers in Project Properties.
 * To change this template file; choose Tools | Templates
 * and open the template in the editor.
 */
package States;

import Util.State;

import java.net.URI;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * @author muhammad
 */
public class StateData {

    public String downloadDirectory;
    public URI uri;
    public String fileName;
    public AtomicLongArray initialState;
    public AtomicLongArray finalState;
    public AtomicLong bytesDone;
    public int segments;
    public long sizeOfFile;
    public State state;
    public boolean initialized;

    public StateData(String downloadDirectory, URI uri, String fileName,
            int segments) {
        this.downloadDirectory = downloadDirectory;
        this.uri = uri;
        this.fileName = fileName;
        initialState = null;
        finalState = null;
        bytesDone = new AtomicLong(0);
        this.segments = segments;
        sizeOfFile = 0;
        state = State.ACTIVE;
        initialized = false;
    }

    public StateData(String downloadDirectory, URI uri, String fileName,
            AtomicLongArray initialState, AtomicLongArray finalState,
            AtomicLong bytesDone, int segments, long sizeOfFile,
            State state, boolean initialized) {
        this.downloadDirectory = downloadDirectory;
        this.uri = uri;
        this.fileName = fileName;
        this.initialState = initialState;
        this.finalState = finalState;
        this.bytesDone = bytesDone;
        this.segments = segments;
        this.state = state;
        this.sizeOfFile = sizeOfFile;
        this.initialized = initialized;
    }

    @Override
    public String toString() {

        return downloadDirectory + "::" + uri + "::" + fileName
                + "::" + initialState + "::" + finalState + "::" + bytesDone
                + "::" + segments + "::" + sizeOfFile + "::" + state + "::"
                + initialized + "::";
    }
}
