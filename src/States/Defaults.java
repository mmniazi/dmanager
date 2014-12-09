/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package States;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author muhammad
 */
public class Defaults {
    int segments;
    String downloadLocation;

    public Defaults() {
        try {
            RandomAccessFile file = new RandomAccessFile(System.getProperty("user.home") + "/defaults", "rw");
            if (file.length() > 0) {
                segments = Integer.valueOf(file.readLine());
                downloadLocation = file.readLine();
            } else {
                segments = 10;
                downloadLocation = System.getProperty("user.home");
                file.write(segments);
                file.writeBytes("\n");
                file.writeChars(downloadLocation);
            }
        } catch (IOException ex) {
            Logger.getLogger(Defaults.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getDownloadLocation() {
        return downloadLocation;
    }

    public void setDownloadLocation(String downloadLocation) {
        this.downloadLocation = downloadLocation;
    }

    public int getSegments() {
        return segments;
    }

    public void setSegments(int segments) {
        this.segments = segments;
    }
}
