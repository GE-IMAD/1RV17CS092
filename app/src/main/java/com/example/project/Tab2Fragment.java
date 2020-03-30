package com.example.project;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Tab2Fragment extends Fragment {

    private String str_textpath;
    private int mode;
    private Context context;
    private View view;

    public Tab2Fragment(String str_textpath, int mode, Context context)
    {
        this.str_textpath = str_textpath;
        this.mode = mode;
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        view =  inflater.inflate(R.layout.fragment2, container, false);
        TextView text = view.findViewById(R.id.display_text);
        String t;
        if(mode==Constants.LOCAL_MODE)
        {
            t = readFileLocal(str_textpath);
            if(t.equals(""))
                text.setText(getText(R.string.file_open_fail));
            else
                text.setText(t);
        }
        else
            readFileCloud(str_textpath);
        return view;
    }

    private String readFileLocal(String textpath)
    {
        File root = new File(Environment.getExternalStorageDirectory(), "OCRFiles");
        if (!root.exists()) {
            return "";
        }
        File file = new File(root, textpath);

        StringBuilder text = new StringBuilder();

        try
        {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append(' ');
            }
            br.close();
            return text.toString();
        }
        catch (IOException e) {
            Log.d("FILE DEBUG", e.getMessage());
            return "";
        }
    }

    private void readFileCloud(String textpath)
    {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl(str_textpath);

        final long ONE_MEGABYTE = 1024 * 1024;
        storageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>()
        {
            @Override
            public void onSuccess(byte[] bytes)
            {
                TextView text = view.findViewById(R.id.display_text);
                try
                {
                    String ftext = new String(bytes, "UTF-8");
                    text.setText(ftext);
                } catch (IOException e)
                {
                    text.setText(getText(R.string.file_open_fail));
                    Log.d("FILE READ", "Error: " + e.getMessage());
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {

            }
        });
    }
}
