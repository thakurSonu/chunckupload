package com.sonu.cupload;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;


public class FileReadTask implements Callable<String>{
    private File file;


    private List<Integer> indexes;
    private static String TAG = "ReadTask";
    private ApacheUploader uploader = new ApacheUploader();
    private ChunchUpload_notify listner;

    // obj instance will be one over all transaction
    public FileReadTask(File file, List<Integer> indexes, ChunchUpload_notify listner) {
        this.listner = listner;
        this.file = file;
        this.indexes = indexes;

    }

    // called only once
    public String call() throws Exception {
        FileInputStream fis = null;

        Random rnd = new Random();
        int randomNum = 100000 + rnd.nextInt(900000);

        String fileName = "";
        if(ChunckConfig.fileType.equalsIgnoreCase(ChunckConfig.videoType))
            fileName = ChunckConfig.user_name + "_" + new SimpleDateFormat("yyyyMMddhhmm").format(new Date()) + String.valueOf(randomNum)+ ".mp4";
        else  if(ChunckConfig.fileType.equalsIgnoreCase(ChunckConfig.documentType))
            fileName = ChunckConfig.user_name+"_"+new SimpleDateFormat("yyyyMMddhhmm").format(new Date())+ String.valueOf(randomNum)+".pdf";
        else
            fileName = ChunckConfig.user_name+"_"+new SimpleDateFormat("yyyyMMddhhmm").format(new Date())+ String.valueOf(randomNum)+".png";

        int partSize = ChunckConfig.PART_SIZE;
        try {
            fis = new FileInputStream(this.file);
            FileChannel fc = fis.getChannel();
            int i = 0;

            while(true) {
                Status status = this.getReadStatus(i, this.indexes);
                if(status == Status.stop) {
                    break;
                }

                if(status == Status.skip) {
                    fc.position(fc.position() + (long)partSize);
                } else if(status == Status.read) {
                    ByteBuffer bb = ByteBuffer.allocate(partSize);
                    int bytesRead = fc.read(bb);
                    if(bytesRead == -1) {
                        break;
                    }

                    byte[] bytes = bb.array();
                    if(bytesRead != partSize) {
                        bytes = Arrays.copyOf(bytes, bytesRead);
                    }
                  //  DocquityLog.e(TAG, "bytes "+bytes.length);
                    ChunckConfig.parts.add(new ChunckUpload.Part(fileName, bytes, i));
                    //uploader.post(new UploadFileService.Part(fileName, bytes, i), "0");
                }

                ++i;
            }


        }catch(Exception ex){
            Log.e(TAG, "error .call.. "+ex.getMessage());
            listner.ChunckUploadfailed();
        } finally {
            if(fis != null) {
                try {
                    fis.close();
                } catch (Exception var15) {

                }
            }

           // this.parts.add(ChunckUpload.Part.NULL);
            performUploadTask();
        }

        return "read";
    }



    public void performUploadTask(){

     //   DocquityLog.e(TAG, "... "+ ChunckConfig.isIntrrupt);
        try {
            if(!ChunckConfig.isIntrrupt){
                if (ChunckConfig.parts.size() > ChunckConfig.read_index && ChunckConfig.retry_count < ChunckConfig.parts.size()) {

                    if (ChunckConfig.parts.size() - 1 == ChunckConfig.read_index)
                        uploader.upload(ChunckConfig.parts.get(ChunckConfig.read_index), "1", this);
                    else
                        uploader.upload(ChunckConfig.parts.get(ChunckConfig.read_index), "0", this);

                    if(ChunckConfig.read_index>0) {
                        int size = ChunckConfig.parts.size();
                        int progressSize = ChunckConfig.read_index;
                        int progressInterval = 100 / size;
                        listner.notifyPercentage(progressSize * progressInterval);
                    }else if(ChunckConfig.parts.size() == 1){
                        listner.notifyPercentage(20);
                    }else if(ChunckConfig.read_index == 0)
                        listner.notifyPercentage(20);

                } else if (ChunckConfig.retry_count >= ChunckConfig.parts.size()) {
                    Log.e(TAG, "failed .....");
                    listner.ChunckUploadfailed();
                } else
                    // done finally
                    listner.ChunckUploaddone(ChunckConfig.final_url, ChunckConfig.absolute_path);

            }
        }catch(Exception ex){
            Log.e(TAG, "error performUploadTask "+ex.getMessage());
            listner.ChunckUploadfailed();
        }

    }

    private Status getReadStatus(int i, List<Integer> indexes) {
        return indexes != null && !indexes.contains(Integer.valueOf(i))?(i > indexes.get(indexes.size() - 1).intValue()? Status.stop: Status.skip): Status.read;
    }

    private enum Status {
        stop,
        skip,
        read;
        Status() {
        }
    }





}
