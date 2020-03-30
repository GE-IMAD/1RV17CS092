package com.example.project;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;

public class DBHelper extends SQLiteOpenHelper
{
    public static final String DATABASE_NAME = "AppStorage.db";
    public DBHelper(@Nullable Context context)
    {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL("create table images (imagepath text not null, textpath text not null)");
        db.execSQL("create table tags (tag text, imagepath text," +
                "foreign key(imagepath) references images(imagepath) on update cascade on delete cascade)");
        db.execSQL("create table refs (uid text not null, imagepath text not null, textpath text not null)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {

    }

    public boolean checkExists(String imagepath, int mode, String uid)
    {
        SQLiteDatabase db = this.getReadableDatabase();
        switch(mode)
        {
            case Constants.LOCAL_MODE:
            {
                Cursor cursor =  db.rawQuery( "select imagepath from images", null );
                if(cursor.getCount() >0 && cursor.moveToFirst())
                {
                    while(!cursor.isAfterLast())
                    {
                        String path = cursor.getString(cursor.getColumnIndex("imagepath"));
                        if(path.equals(imagepath))
                        {
                            db.close();
                            return true;
                        }
                        cursor.moveToNext();
                    }
                }
                cursor.close();
                break;
            }
            case Constants.CLOUD_MODE:
            {
                Cursor cursor =  db.rawQuery( "select imagepath from refs where uid = ?", new String[] {uid} );
                if(cursor.getCount() >0 && cursor.moveToFirst())
                {
                    while(!cursor.isAfterLast())
                    {
                        String path = cursor.getString(cursor.getColumnIndex("imagepath"));
                        if(path.equals(imagepath))
                            return true;
                        cursor.moveToNext();
                    }
                }
                cursor.close();
                break;
            }
        }
        db.close();
        return false;
    }

    public boolean insertNew(String imagepath, String textpath, ArrayList<String> tags, int mode, String uid)
    {
        if(checkExists(imagepath, mode, uid))
            return false;
        SQLiteDatabase db = this.getWritableDatabase();
        switch(mode)
        {
            case Constants.LOCAL_MODE:
            {
                ContentValues contentValues = new ContentValues();
                contentValues.put("imagepath", imagepath);
                contentValues.put("textpath", textpath);
                long check = db.insert("images", null, contentValues);
                if(check!=-1)
                {
                    if(tags.size()>0)
                    {
                        for(int i=0;i<tags.size();i++)
                        {
                            contentValues.clear();
                            contentValues.put("imagepath", imagepath);
                            contentValues.put("tag", tags.get(i));
                            db.insert("tags",null, contentValues);
                        }
                    }
                    db.close();
                    return true;
                }
                break;
            }
            case Constants.CLOUD_MODE:
            {
                ContentValues contentValues = new ContentValues();

                contentValues.put("imagepath", imagepath);
                contentValues.put("textpath", textpath);
                contentValues.put("uid",uid);
                long check = db.insert("refs", null, contentValues);
                if(check!=-1)
                {
                    db.close();
                    return true;
                }
                break;
            }
        }
        db.close();
        return false;
    }

    public ArrayList<ImageData> getData(int mode, String uid)
    {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<ImageData> data = new ArrayList<>();
        switch(mode)
        {
            case Constants.LOCAL_MODE:
            {
                Cursor cursor =  db.rawQuery( "select * from images", null );
                if(cursor.getCount() >0 && cursor.moveToFirst())
                {
                    while(!cursor.isAfterLast())
                    {
                        String path = cursor.getString(cursor.getColumnIndex("imagepath"));
                        String file = cursor.getString(cursor.getColumnIndex("textpath"));
                        ArrayList<String> taglist = new ArrayList<>();
                        Cursor cursor1 =  db.rawQuery( "select tag from tags where imagepath = ?", new String[] {path} );
                        if(cursor1.getCount()>0 && cursor1.moveToFirst())
                        {
                            while(!cursor1.isAfterLast())
                            {
                                taglist.add(cursor1.getString(cursor1.getColumnIndex("tag")));
                                cursor1.moveToNext();
                            }
                        }
                        cursor1.close();
                        data.add(new ImageData(path.substring(path.lastIndexOf("/")+1), path, file, taglist, Constants.LOCAL_MODE));
                        cursor.moveToNext();
                    }
                }
                cursor.close();
                break;
            }
            case Constants.CLOUD_MODE:
            {
                Cursor cursor =  db.rawQuery( "select imagepath, textpath from refs where uid = ?", new String[] {uid} );
                if(cursor.getCount() >0 && cursor.moveToFirst())
                {
                    while(!cursor.isAfterLast())
                    {
                        String path = cursor.getString(cursor.getColumnIndex("imagepath"));
                        String file = cursor.getString(cursor.getColumnIndex("textpath"));

                        FirebaseStorage storage = FirebaseStorage.getInstance();
                        StorageReference storageRef = storage.getReferenceFromUrl(path);

                        ArrayList<String> taglist;

                        final ImageData temp = new ImageData(path.substring(path.lastIndexOf("/")+1), path, file, null, Constants.CLOUD_MODE);

                        data.add(temp);

                        storageRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>()
                        {
                            @Override
                            public void onSuccess(StorageMetadata storageMetadata)
                            {
                                temp.setTags(new ArrayList<>(Arrays.asList(storageMetadata.getCustomMetadata("tags").split(","))));
                                Log.d("FIREBASE","Loaded metadata.");
                            }
                        }).addOnFailureListener(new OnFailureListener()
                        {
                            @Override
                            public void onFailure(@NonNull Exception exception)
                            {
                                Log.d("FIREBASE","Failed to fetch metadata.");
                            }
                        });

                        cursor.moveToNext();
                    }
                }
                cursor.close();
                break;
            }
        }
        db.close();
        return data;
    }
}
