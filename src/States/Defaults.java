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
 *
 * @author muhammad
 */
public class Defaults {
// TODO: a hell lot remaining here
    private RandomAccessFile file;
    int segments;
    String downloadLocation;

    public Defaults() {
        try {
            file = new RandomAccessFile(System.getProperty("user.home") + "/defaults", "rw");
            segments = Integer.valueOf(file.readLine());
            downloadLocation = file.readLine();
        } catch (IOException ex) {
            Logger.getLogger(Defaults.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getDownloadLocation() {
        return downloadLocation;
    }

    public int getSegments() {
        return segments;
    }
    

}
