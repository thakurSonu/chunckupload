package com.sonu.cupload;

public interface ChunchUpload_notify {

    void ChunckUploaddone(String url, String absolutePath);

    void ChunckUploadfailed();


    void notifyPercentage(int percentage);

}
