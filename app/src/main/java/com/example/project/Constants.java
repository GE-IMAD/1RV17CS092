package com.example.project;

public class Constants
{
    public static final int LOCAL_MODE = 0;
    public static final int CLOUD_MODE = 1;
    public static String FIREBASE_URL = "gs://project-11622.appspot.com";
    public static int tagsLoaded = 0;

    public static void setTagsLoaded(int val)
    {
        tagsLoaded = val;
    }
}
