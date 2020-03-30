package com.example.project;

import android.text.TextUtils;

import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class ImageData
{
    String imagename;
    String imagepath;
    String textpath;
    ArrayList<String> tags;
    int mode;

    public ImageData(String imagename, String imagepath, String textpath, ArrayList<String> tags, int mode)
    {
        this.imagename = imagename;
        this.imagepath = imagepath;
        this.textpath = textpath;
        this.tags = tags;
        this.mode = mode;
    }

    public String getTextpath()
    {
        return textpath;
    }

    public ArrayList<String> getTags()
    {
        return tags;
    }

    public String getImagepath()
    {
        return imagepath;
    }

    public String getImagename()
    {
        return imagename;
    }

    public String getTaglist()
    {
        if(tags==null)
            return ("");
        return TextUtils.join(", ",tags);
    }

    public int getMode()
    {
        return mode;
    }

    public void setTags(ArrayList<String> tags)
    {
        this.tags = tags;
    }
}
