package com.example.project;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class Tab1Fragment extends Fragment {

    private String str_taglist;
    private String str_imagepath;
    private Context context;
    private int mode;

    public Tab1Fragment(String str_imagepath, String str_taglist, int mode, Context context)
    {
        this.str_imagepath = str_imagepath;
        this.str_taglist = str_taglist;
        this.context = context;
        this.mode = mode;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment1, container, false);
        TextView taglist = view.findViewById(R.id.display_tag_list);
        ImageView image = view.findViewById(R.id.display_image);

        taglist.setText(str_taglist);
        if(mode==Constants.LOCAL_MODE)
        {
            Bitmap bitmap = BitmapFactory.decodeFile(str_imagepath);
            image.setImageBitmap(bitmap);
        }
        else
        {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReferenceFromUrl(str_imagepath);

            GlideApp.with(context)
                    .load(storageRef)
                    .into(image);
        }

        image.setMaxHeight(1000);
        return view;
    }
}