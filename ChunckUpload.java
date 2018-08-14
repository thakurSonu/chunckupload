package com.sonu.cupload;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.iceteck.silicompressorr.SiliCompressor;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ChunckUpload implements ChunchUpload_notify {
    private static String TAG = "ChunckUpload";
    Context _cotext;
    private File file;
    private ExecutorService executor;
    private ChunchUpload_notify listner;
    private String file_url;

    public ChunckUpload(String file_url, Context context, ChunchUpload_notify listner, String type,
                        String custom_id) {
        this._cotext = context;
        this.listner = listner;
        ChunckConfig.fileType = type;
        ChunckConfig.user_name = custom_id;
        fileWork(file_url);

    }

    private void fileWork(String file_url){
        this.file_url = file_url;
        this.file = new File(file_url);

        if(!this.file.exists() || !this.file.isFile()) {
            throw new RuntimeException("File:" + this.file + " isn\'t correct!");
        }

    }

    public void upload() {
        try {
            if(ChunckConfig.fileType.equalsIgnoreCase(ChunckConfig.videoType)) {

                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(file_url);
                String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                int originalwidth=0;
                int originalheight=0;

                if(!Validation.isEmptyString(width) && !Validation.isEmptyString(height)){
                    originalwidth=Integer.parseInt(width);
                    originalheight=Integer.parseInt(height);

                }

                if(originalheight>360 && originalwidth>640)
                    new MediaCompressor().execute();
                else {
                    if (setChunckSize())
                        this.doUpload(null);
                }
            }else if(ChunckConfig.fileType.equalsIgnoreCase(ChunckConfig.imageType)){
                new MediaCompressor().execute();

            } else{
                if(setChunckSize())
                    this.doUpload(null);
            }

        } finally {
            this.stop();
        }

    }

    public void forceStop(){
        ChunckConfig.isIntrrupt = true;
        this.stop();
    }

    public void stop() {

        if(this.executor != null) {
            this.executor.shutdown();
        }

    }
    private void doUpload(List<Integer> indexes) {
        //new FileReadTask(this.file, indexes, this);
        ExecutorCompletionService cs = new ExecutorCompletionService(this.executor);
        cs.submit(new FileReadTask(this.file, indexes, this));
        // read process now time to upload in sequence.
    }

    @Override
    public void ChunckUploaddone(String url, String absolutePath) {
        listner.ChunckUploaddone(url, absolutePath);
    }

    @Override
    public void ChunckUploadfailed() {
        listner.ChunckUploadfailed();
    }

    @Override
    public void notifyPercentage(int percentage) {
        listner.notifyPercentage(percentage);
    }


    public static class Part {
        private byte[] content;
        private String fileName;
        public static final Part NULL =
                new Part();
        public int index;
       // public int upload_status = 0;

        public Part() {
            this(null, null, 1);
        }

        public Part(String fileName, byte[] content, int in) {
            this.content = content;
            this.fileName = fileName;
            this.index = ++in;
       //     this.upload_status = upload_status;
        }

     /*   public void setUpload_status(int upload_status){
            this.upload_status = upload_status;
        }
*/
        public void setIndex(int index){
            this.index = index;
        }

        public int getIndex(){
            return index;
        }
        public byte[] getContent() {
            return this.content;
        }

        public String getFileName() {
            return this.fileName;
        }
    }

    private Boolean setChunckSize(){
        executor = Executors.newFixedThreadPool(10000);
        ChunckConfig.final_url = "";
        ChunckConfig.absolute_path = "";
        ChunckConfig.read_index = 0;
        if(ChunckConfig.parts.size()>0)
            ChunckConfig.parts.clear();

        ChunckConfig.retry_count = 0;
        ChunckConfig.isIntrrupt = false;

        if(Connectivity_chk_speed.isConnectedFast(_cotext) == 0){
            ChunckConfig.PART_SIZE = ChunckConfig.Original_size/6;
        }else  if(Connectivity_chk_speed.isConnectedFast(_cotext) == 1){
            ChunckConfig.PART_SIZE = ChunckConfig.Original_size/4;
        }
        else if(Connectivity_chk_speed.isConnectedFast(_cotext) == 2)
            ChunckConfig.PART_SIZE = ChunckConfig.Original_size/2;
        else if(Connectivity_chk_speed.isConnectedFast(_cotext) == 3)
            ChunckConfig.PART_SIZE = ChunckConfig.Original_size;
        else {
            listner.ChunckUploadfailed();
            return false;
        }
        return true;
    }



    class MediaCompressor extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG,"Start video compression");
        }

        @Override
        protected String doInBackground(Void... voids) {

            try {

                String filePath = null;
                if(ChunckConfig.fileType.equalsIgnoreCase(ChunckConfig.imageType)) {
                    File cacheFile = new ImageProcess(DocApplication.getContext()).getOutputMediaFile("");
                    filePath = SiliCompressor.with(_cotext).compress(Uri.fromFile(file).toString(), cacheFile);
                } else {
                   // cacheFile = new ImageProcess(DocApplication.getContext()).getOutputMediaFile(".mp4");
                    filePath = MediaController.getInstance().convertVideo(file_url);
                }

                // MediaController.getInstance().convertVideo(file_url);
                return filePath;
            }catch (Exception ex){
                return null;
            }
        }

        @Override
        protected void onPostExecute(String compress_file_path) {
            super.onPostExecute(compress_file_path);
            if(!Validation.isEmptyString(compress_file_path)){

                Log.e(TAG,"Compression success ... !"+compress_file_path);
                fileWork(compress_file_path);
            }

            if(setChunckSize())
                doUpload(null);


        }
    }

}
