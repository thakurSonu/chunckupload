package com.sonu.cupload;

import java.util.ArrayList;

public class ChunckConfig {

    public static final int Original_size = 2097152;
    public static int PART_SIZE = 2097152;
    public static int retry_count = 0;
    public static int read_index = 0;
    public static ArrayList<ChunckUpload.Part> parts = new ArrayList<>();
    public static String final_url = "";
    public static String absolute_path = "";
    public static String user_name = "";
    public static Boolean isIntrrupt = false;
    // document, video
    public static String fileType = "";
    public static String documentType = "document";
    public static String videoType = "video";
    public static String imageType = "image";
    public static String product_type = "1";

}
