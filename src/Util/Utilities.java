/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import States.StateManagement;

import java.util.concurrent.atomic.AtomicLongArray;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author muhammad
 */
public class Utilities {
  // TODO: how to get file name from content?
  public static String getFromURI(String url, String required) {
    String fileName;
    String param;
    int slashIndex = url.lastIndexOf("/");
    int qIndex = url.lastIndexOf("?");
    if (qIndex > slashIndex) { //if it has parameters
      fileName = url.substring(slashIndex + 1, qIndex);
      param = url.substring(qIndex);
    } else {
      fileName = url.substring(slashIndex + 1);
      param = "";
    }
    switch (required) {
      case "param":
        return param;
      case "filename.ext":
        return fileName;
      case "filename":
        if (fileName.contains(".")) return fileName.substring(0, fileName.lastIndexOf("."));
        else return fileName;
      case "ext":
        return fileName.substring(fileName.lastIndexOf(".") + 1);
      default:
        return fileName;
    }
  }

  public static AtomicLongArray getArrayFromString(String string) {
    if (string.equals("null")) return null;
    String[] items = string.replaceAll("\\[", "").replaceAll("\\]", "")
        .replaceAll(" ", "").split(",");

    long[] results = new long[items.length];
    for (int i = 0; i < items.length; i++) {
      try {
        results[i] = Long.parseLong(items[i]);
      } catch (NumberFormatException ex) {
        Logger.getLogger(StateManagement.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    return new AtomicLongArray(results);
  }

  public static String speedConverter(float speed) {
    if (speed < 1024.0)
      return String.format("%.2f", speed) + " B/s";
    else if (speed < 1048576.0)
      return String.format("%.2f", speed / 1024) + " KB/s";
    else
      return String.format("%.2f", speed / 1048576) + " MB/s";
  }

  public static String sizeConverter(long size) {
    if (size < 1024)
      return size + " B";
    else if (size < 1048576)
      return String.format("%.2f", size / 1024.0) + " KB";
    else if (size < 1073741824)
      return String.format("%.2f", size / 1048576.0) + " MB";
    else
      return String.format("%.2f", size / 1073741824.0) + " GB";
  }

  public static String timeConverter(long sizeRemaining, float speed) {
    if (speed == 0) {
      return "∞";
    } else {
      long seconds = (long) (sizeRemaining / speed);
      if (seconds < 60) return seconds + "s";
      else if (seconds < 3600) return seconds / 60 + "m" + seconds % 60 + "s";
      else if (seconds < 86400)
        return seconds / 3600 + "h" + (seconds % 3600) / 60 + "m" + (seconds % 3600) % 60 + "s";
      else return seconds / 2592000 + "months";
    }
  }

  public static String findType(String ext) {

    switch (ext) {
      /*----- Documents -----*/
      case "doc":
      case "docx":
      case "log":
      case "msg":
      case "odt":
      case "pages":
      case "rtf":
      case "tex":
      case "txt":
      case "wpd":
      case "key":
      case "pps":
      case "ppt":
      case "pptx":
      case "ott":
      case "uot":
      case "stw":
      case "sxw":
      case "xls":
      case "ots":
      case "ods":
      case "sxc":
      case "stc":
      case "dif":
      case "dbf":
      case "xlt":
      case "csv":
      case "xml":
      case "uos":
      case "odp":
      case "otp":
      case "sxi":
      case "sti":
      case "pot":
      case "sxd":
      case "uop":
      case "odf":
      case "sxm":
      case "mml":
      case "xlsx":
      case "xlr":
        return "Documents";

      /*----- Audio -----*/
      case "aif":
      case "iff":
      case "m3u":
      case "m4a":
      case "mid":
      case "mp3":
      case "mpa":
      case "ra":
      case "ram":
      case "wav":
      case "wma":
      case "ogg":
      case "oga":
      case "act":
      case "aiff":
      case "aac":
      case "amr":
      case "au":
      case "awb":
      case "dvf":
      case "flac":
      case "gsm":
      case "iklax":
      case "ivs":
      case "mmf":
      case "mpc":
      case "msv":
      case "opus":
      case "tta":
      case "wv":
        return "Audio";

      /*----- Video -----*/
      case "webm":
      case "mkv":
      case "flv":
      case "ogv":
      case "mng":
      case "drc":
      case "avi":
      case "mov":
      case "qt":
      case "wmv":
      case "rm":
      case "rmvb":
      case "asf":
      case "m4p":
      case "m4v":
      case "mp4":
      case "mpg":
      case "mp2":
      case "mpeg":
      case "mpe":
      case "mpv":
      case "m2v":
      case "svi":
      case "3gp":
      case "3g2":
      case "nsv":
        return "Videos";

      /*----- Images -----*/
      case "bmp":
      case "dds":
      case "gif":
      case "jpg":
      case "png":
      case "psd":
      case "pspimage":
      case "tga":
      case "thm":
      case "tif":
      case "tiff":
      case "yuv":
      case "ai":
      case "eps":
      case "ps":
      case "svg":
      case "jpeg":
      case "ico":
      case "ora":
      case "icon":
      case "bitmap":
        return "Images";

      /*----- Programs -----*/
      case "apk":
      case "app":
      case "bat":
      case "cgi":
      case "com":
      case "exe":
      case "gadget":
      case "jar":
      case "pif":
      case "vb":
      case "wsf":
      case "action":
      case "bin":
      case "cmd":
      case "command":
      case "cpl":
      case "csh":
      case "inf":
      case "ins":
      case "inx":
      case "ipa":
      case "isu":
      case "job":
      case "jse":
      case "ksh":
      case "lnk":
      case "msc":
      case "msi":
      case "msp":
      case "mst":
      case "osx":
      case "out":
      case "paf":
      case "ps1":
      case "reg":
      case "rgs":
      case "run":
      case "sct":
      case "shb":
      case "u3p":
      case "vbe":
      case "vbs":
      case "vbscript":
      case "workflow":
      case "deb":
      case "rpm":
        return "Programs";

      /*----- Compressed -----*/
      case "7z":
      case "cbr":
      case "gz":
      case "pkg":
      case "rar":
      case "sitx":
      case "zip":
      case "zipx":
      case "tgz":
      case "Z":
      case "bz2":
      case "tbz2":
      case "lzma":
      case "tlz":
        return "Compressed";
    }

    /*----- Other -----*/
    return "Others";
  }
}
