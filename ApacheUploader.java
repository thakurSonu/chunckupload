package com.sonu.cupload;

import android.util.Log;

import com.virinchi.api.ApiManager;
import com.virinchi.core.DocApplication;
import com.virinchi.core.GlobalSetting;
import com.virinchi.util.LogEx;
import com.virinchi.util.UtilsUserInfo;
import com.virinchi.util.Validation;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;


public class ApacheUploader{
    private static String TAG = "ApacheHCUploader";
    FileReadTask service;
    SharePrefObj userInfo;
    public ApacheUploader() {
    }

    public void upload(ChunckUpload.Part part, String status, final FileReadTask service) {
        this.service = service;
        userInfo = new SharePrefObj(getContext());
        RequestBody fbody;

        if(ChunckConfig.fileType.equalsIgnoreCase(ChunckConfig.videoType))
            fbody = RequestBody.create(MediaType.parse("video/*"), part.getContent());
        else if(ChunckConfig.fileType.equalsIgnoreCase(ChunckConfig.documentType))
            fbody = RequestBody.create(MediaType.parse("application/*"), part.getContent());
        else
            fbody = RequestBody.create(MediaType.parse("image/*"), part.getContent());

        RequestBody filename = RequestBody.create(MediaType.parse("text/plain"), part.getFileName());
        RequestBody chunk_status = RequestBody.create(MediaType.parse("text/plain"), status);
        RequestBody num = RequestBody.create(MediaType.parse("text/plain"), ""+part.getIndex());
        RequestBody num_chunks = RequestBody.create(MediaType.parse("text/plain"), ""+ ChunckConfig.parts.size());
        RequestBody fileType = RequestBody.create(MediaType.parse("text/plain"), ChunckConfig.fileType);
        RequestBody product_type = RequestBody.create(MediaType.parse("text/plain"), ChunckConfig.product_type); // 1 = feed,

        Map<String, RequestBody> map = new HashMap<>();
        map.put("file\"; filename=\""+part.getFileName()+"\" ", fbody);
        map.put("name", filename);
        map.put("num_chunks", num_chunks);
        map.put("num", num);
        map.put("chunk_status", chunk_status);
        map.put("file_type", fileType);
        map.put("product_type", product_type);

     /// api call
      
        success {

            try {
                        if (!Validation.isEmptyString(searchResponse.getStatus()) &&
                                searchResponse.getStatus().equalsIgnoreCase("1")) {
                            ChunckConfig.final_url = searchResponse.getData().getFileurl();
                            ChunckConfig.absolute_path = searchResponse.getData().getAbsolute_path();
                            ChunckConfig.read_index = ChunckConfig.read_index + 1;

                            service.performUploadTask();
                        } else {
                          //  ChunckConfig.parts.get(ChunckConfig.read_index).setUpload_status(1);
                            ChunckConfig.retry_count = ChunckConfig.retry_count + 1;
                            service.performUploadTask();
                        }
                    }catch (Exception ex){
                        if(ex != null)
                            Log.e(TAG, "upload: "+ex.getMessage() );
                    }

      }


     Fail {

      try {
                        LogEx.displayRetrofitError(TAG, throwable);
                        ChunckConfig.retry_count = ChunckConfig.retry_count + 1;
                        service.performUploadTask();
                    }catch (Exception ex){

                        if(ex != null)
                            Log.e(TAG, "upload: "+ex.getMessage() );

                    }

}

           

}
