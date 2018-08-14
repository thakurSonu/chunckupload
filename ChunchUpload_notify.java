package com.sonu.cupload;

/**
 * Created by sonu on 13/5/16.
 */
public interface ChunchUpload_notify {

    void ChunckUploaddone(String url, String absolutePath);

    void ChunckUploadfailed();


    void notifyPercentage(int percentage);

}
