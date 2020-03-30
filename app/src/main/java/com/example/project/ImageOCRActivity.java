package com.example.project;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ImageOCRActivity extends AppCompatActivity
{
    private TextView textView;
    private TextView title;
    private TextView imagename;
    private TextInputLayout newtaglayout;
    private Uri file;
    private static final int REQ_CODE = 100; //Request code to check camera permission
    private static final int REQ_CODE2 = 101; //Request code to capture image
    private boolean tts_enabled = false;
    private int textMode = 0;
    private String text;
    private String summary;
    private DBHelper db;
    private TextToSpeech tts;
    private ChipGroup chipGroup;
    private View popupView;
    private TextInputEditText newtag;
    private FloatingActionButton fab;
    private Chip addchip;
    private PopupWindow popupWindow;
    private String imagepath;
    private HashSet<String> tagset;
    private ArrayList<String> tags;
    private ArrayList<String> keywords;
    private static final int MAX_KEYS = 5;
    private FirebaseAuth auth;
    private int save_mode;
    private String text_file_path;
    private FirebaseUser user;
    private ArrayList<String> tags_for_cloud;

    Summarizer s = new Summarizer();

    Thread thread = new Thread(){
        @Override
        public void run() {
            try {
                Thread.sleep(Toast.LENGTH_SHORT);
                ImageOCRActivity.this.finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_ocr);

        user = FirebaseAuth.getInstance().getCurrentUser();

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        popupView = inflater.inflate(R.layout.save_options_popup, null);
        chipGroup = popupView.findViewById(R.id.chipGroup);
        newtag = popupView.findViewById(R.id.newtag);
        newtaglayout = popupView.findViewById(R.id.newtagField);
        fab = popupView.findViewById(R.id.add_done);
        imagename = popupView.findViewById(R.id.imagename);
        disable();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            window.setStatusBarColor(ContextCompat.getColor(this,R.color.blue1));
        }

        getSupportActionBar().setTitle(Html.fromHtml("<font color=\"#FFFFFF\">" + "Recognized Text" + "</font>"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        addchip = new Chip(this);
        addchip.setTextColor(getResources().getColor(R.color.chip_text));
        addchip.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                enable();
            }
        });
        addchip.setChipDrawable(ChipDrawable.createFromResource(this, R.xml.addchip));
        textView = findViewById(R.id.imageText);
        title = findViewById(R.id.title);

        Intent intent = getIntent();
        String check = intent.getStringExtra("camera");

        try
        {
            if(check.equals("yes"))
            {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                {
                    ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQ_CODE);
                }
                takePicture();
            }
            else
            {
                imagepath = intent.getStringExtra("path");
                performOCR();
            }
        }
        catch (NullPointerException e)
        {
            Log.e("INTENT", "Camera check value null.");
        }

        tts=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {
                if (status == TextToSpeech.SUCCESS)
                {
                    int result = tts.setLanguage(Locale.UK);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
                    {
                        Toast.makeText(getApplicationContext(), "This language is not supported!", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Log.d("CAMERA","onInit succeeded");
                        tts_enabled = true;
                        //speak("Take a picture", TextToSpeech.QUEUE_FLUSH);
                    }
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Initialization failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        onBackPressed();
        return true;
    }

    void speak(String s, int option)
    {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            Log.d("SPEECH", "Speak new API");
            Bundle bundle = new Bundle();
            bundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
            tts.speak(s, option, bundle, null);
        }
        else
        {
            Log.d("SPEECH", "Speak old API");
            HashMap<String, String> param = new HashMap<>();
            param.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
            tts.speak(s, option, param);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (requestCode == REQ_CODE)
        {
            if(!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED))
            {
                Toast.makeText(getApplicationContext(), "Camera permission denied.", Toast.LENGTH_SHORT).show();
                thread.start();
            }
        }
    }

    public void takePicture()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(getPackageManager()) != null)
        {
            file = Uri.fromFile(getOutputMediaFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, file);
            try
            {
                startActivityForResult(intent, REQ_CODE2);
            }
            catch(ActivityNotFoundException a)
            {
                Toast.makeText(getApplicationContext(), "Not supported on your device!", Toast.LENGTH_SHORT).show();
                thread.start();
            }
        }
    }

    private static File getOutputMediaFile()
    {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File mediaStorageDir = new File(root + "/saved_images");
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE2 && resultCode == RESULT_OK)
        {
            imagepath = file.getPath();
            performOCR();
        }
        else
            finish();
    }

    public void TTS(View view)
    {
        if(tts_enabled)
        {
            if(textMode==0)
                speak(text, TextToSpeech.QUEUE_FLUSH);
            else
                speak(summary, TextToSpeech.QUEUE_FLUSH);
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Initialization failed!", Toast.LENGTH_SHORT).show();
        }
    }

    public void summarize(View view)
    {
        if(textMode==0)
        {
            FloatingActionButton fab = findViewById(R.id.summarize);
            fab.setImageDrawable(getResources().getDrawable(R.mipmap.fulltext_round));
            getSupportActionBar().setTitle(Html.fromHtml("<font color=\"#FFFFFF\">" + "Summarixed Text" + "</font>"));
            if(text.equals(""))
                textView.setText(R.string.TextEmptyMessage);
            else
            {
                summary = s.Summarize(text, 5);
                textView.setText(summary);
            }
            textMode = 1;
        }
        else
        {
            FloatingActionButton fab = findViewById(R.id.summarize);
            fab.setImageDrawable(getResources().getDrawable(R.mipmap.summarize_round));
            getSupportActionBar().setTitle(Html.fromHtml("<font color=\"#FFFFFF\">" + "Recognized Text" + "</font>"));
            if(text.equals(""))
                textView.setText(R.string.TextEmptyMessage);
            else
                textView.setText(text);
            textMode = 0;
        }
    }

    private void performOCR()
    {
        Log.d("OCR","Starting new thread for processing");
        new Processing().execute();
    }

    class Processing extends AsyncTask<String, String, String>
    {

        private ProgressDialog progDialog;
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            progDialog = new ProgressDialog(ImageOCRActivity.this, R.style.MyAlertDialogStyle);
            progDialog.setMessage("Performing OCR...");
            progDialog.setIndeterminate(false);
            progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progDialog.setCancelable(true);
            progDialog.show();

        }

        @Override
        protected String doInBackground(String... params)
        {
            //PERFORM OCR
            Bitmap bitmap = BitmapFactory.decodeFile(imagepath);
            final TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
            Log.d("OCR","OCR IMAGE PATH:"+imagepath);
            if (!textRecognizer.isOperational())
            {
                Log.w("OCR", "Detector dependencies not loaded yet");
            }
            else
            {
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray items = textRecognizer.detect(frame);
                StringBuilder strBuilder = new StringBuilder();
                for (int i = 0; i < items.size(); i++)
                {
                    TextBlock item = (TextBlock) items.valueAt(i);
                    strBuilder.append(item.getValue());
                    strBuilder.append(" ");
                }
                text = strBuilder.toString();
                text = text.replace("\n", " ").replace("\r", " ");
            }

            //KEYWORD EXTRACTION
            keywords = new ArrayList<>();
            try
            {
                List<Keyword> temp = KeywordExtraction.guessFromString(text);
                for(Keyword k:temp)
                {
                    try
                    {
                        Set<String> temp1 = k.getTerms();
                        keywords.add(temp1.iterator().next());
                    }
                    catch (Exception e)
                    {
                        Log.d("ERROR","Error in adding keywords"+e.getMessage());
                    }
                    if(keywords==null||keywords.size()>MAX_KEYS)
                        break;
                }
                int l = keywords.size();
                if(l>MAX_KEYS) {
                    keywords.subList(MAX_KEYS, l).clear();
                }
            }
            catch (IOException e)
            {
                Log.d("ERROR","Error extracting keywords.");
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);
            if(text.equals(""))
                textView.setText(R.string.TextEmptyMessage);
            else
                textView.setText(text);
            progDialog.dismiss();
        }
    }

    public void save(View view)
    {
        FloatingActionButton save = findViewById(R.id.save);

        auth = FirebaseAuth.getInstance();

        PopupMenu save_options = new PopupMenu(ImageOCRActivity.this, save);
        save_options.getMenuInflater().inflate(R.menu.save_menu, save_options.getMenu());
        if (auth.getCurrentUser() == null)
            save_options.getMenu().getItem(1).setEnabled(false);
        save_options.show();
    }

    public static void dimBehind(PopupWindow popupWindow)
    {
        View container = popupWindow.getContentView().getRootView();
        Context context = popupWindow.getContentView().getContext();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        p.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = 0.8f;
        wm.updateViewLayout(container, p);
    }

    public void saveData(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.save_option_local:
            {
                save_mode = 0;
                break;
            }
            case R.id.save_option_cloud:
            {
                save_mode = 1;
                break;
            }
        }

        tags = new ArrayList<>(keywords);
        tagset = new HashSet<>(keywords);

        View view = findViewById(android.R.id.content).getRootView();
        imagename.setText(imagepath.substring(imagepath.lastIndexOf("/")+1));

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true;

        popupWindow = new PopupWindow(popupView, width, height, focusable);

        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {

            @Override
            public void onDismiss()
            {
                tags.clear();
                tagset.clear();
                disable();
            }
        });

        while(chipGroup.getChildCount()>0)
        {
            Chip temp = (Chip) chipGroup.getChildAt(0);
            chipGroup.removeView(temp);
        }

        for (String tag : tags)
            addChip(tag);

        chipGroup.addView(addchip);

        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
        dimBehind(popupWindow);
    }

    private void addChip(String item)
    {
        final Chip chip = new Chip(this);
        chip.setText(item);
        chip.setTextColor(getResources().getColor(R.color.chip_text));
        chip.setChipDrawable(ChipDrawable.createFromResource(this, R.xml.chip));
        chip.setChipIconVisible(true);
        chip.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        chip.setTextEndPadding(7);
        chip.setOnCloseIconClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String chiptext = chip.getText().toString();
                if(tagset.contains(chiptext))
                {
                    tagset.remove(chiptext);
                    tags.remove(chiptext);
                }
                chipGroup.removeView(chip);
            }
        });
        chipGroup.addView(chip);
    }

    public void addDone(View view)
    {
        String newtext = newtag.getText().toString();
        int i=0;
        boolean flag = false;
        if(tagset.contains(newtext)||newtext.equals(""))
            flag = true;
        if(!flag)
        {
            addChip(newtext);
            tagset.add(newtext);
            tags.add(newtext);
        }
        newtag.getText().clear();
        disable();
        chipGroup.removeView(addchip);
        chipGroup.addView(addchip);
    }


    private void disable()
    {
        newtag.setVisibility(View.GONE);
        newtaglayout.setVisibility(View.GONE);
        fab.setVisibility(View.GONE);
    }

    private void enable()
    {
        newtag.setVisibility(View.VISIBLE);
        newtaglayout.setVisibility(View.VISIBLE);
        fab.setVisibility(View.VISIBLE);
    }

    public void saveOptions(View view)
    {
        if(save_mode==0)
            saveLocal(view);
        if(save_mode==1)
            saveCloud(view);
    }

    public void saveLocal(View view)
    {
        db = new DBHelper(this);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String filename = "Text_"+timeStamp+".txt";
        boolean check = generateFileOnSD(getApplicationContext(),filename ,text);
        if(!check)
        {
            Toast toast = Toast.makeText(getApplicationContext(), "Failed to save text.", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        check = db.insertNew(imagepath,filename,tags, Constants.LOCAL_MODE, "");
        if(!check)
        {
            Toast toast = Toast.makeText(getApplicationContext(), "This image already exists.", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
        popupWindow.dismiss();
    }

    public void saveCloud(View view)
    {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String filename = "Text_"+timeStamp+".txt";
        boolean check = generateFileOnSD(getApplicationContext(),filename ,text);
        if(!check)
        {
            Toast toast = Toast.makeText(getApplicationContext(), "Failed to save text.", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        String uid = user.getUid();
        db = new DBHelper(this);
        String firebase_image_url = Constants.FIREBASE_URL + "/users/"+uid+"/images/"+imagepath.substring(imagepath.lastIndexOf('/') + 1);
        String firebase_text_url = Constants.FIREBASE_URL + "/users/"+uid+"/text/"+filename;
        check = db.insertNew(firebase_image_url, firebase_text_url, tags, Constants.CLOUD_MODE, uid);
        if(!check)
        {
            Toast toast = Toast.makeText(getApplicationContext(), "This image already exists.", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        tags_for_cloud = new ArrayList<>();
        tags_for_cloud.addAll(tags);

        popupWindow.dismiss();

        new TextUpload().execute();
        new ImageUpload().execute();
    }

    public void saveCancel(View view)
    {
        popupWindow.dismiss();
    }

    public boolean generateFileOnSD(Context context, String sFileName, String sBody)
    {
        try
        {
            File root = new File(Environment.getExternalStorageDirectory(), "OCRFiles");
            if (!root.exists())
                root.mkdirs();
            File textfile = new File(root, sFileName);
            text_file_path = textfile.getAbsolutePath();
            FileWriter writer = new FileWriter(textfile);
            writer.append(sBody);
            writer.flush();
            writer.close();
            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    class TextUpload extends AsyncTask<String, String, String>
    {

        private ProgressDialog progressDialog;
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

            progressDialog = new ProgressDialog(ImageOCRActivity.this, R.style.MyAlertDialogStyle);
            progressDialog.setMessage("Uploading text file...");
            progressDialog.setIndeterminate(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setProgressNumberFormat("");
            progressDialog.setCancelable(true);
            progressDialog.show();

        }

        @Override
        protected String doInBackground(String... params)
        {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReferenceFromUrl("gs://project-11622.appspot.com");

            String uid;
            if (user != null)
            {
                uid = user.getUid();
                Uri file = Uri.fromFile(new File(text_file_path));
                StorageReference sRef1 = storageRef.child("users/"+uid+"/text/"+file.getLastPathSegment());
                UploadTask uploadTask = sRef1.putFile(file);

                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast toast = Toast.makeText(getApplicationContext(), "Could not upload files.", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        progressDialog.dismiss();
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        progressDialog.setProgress((int)progress);
                    }
                }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                        progressDialog.setMessage("Upload paused.");
                    }
                });

            }
            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);
        }
    }

    class ImageUpload extends AsyncTask<String, String, String>
    {

        private ProgressDialog progressDialog;
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            progressDialog = new ProgressDialog(ImageOCRActivity.this, R.style.MyAlertDialogStyle);
            progressDialog.setMessage("Uploading image file...");
            progressDialog.setIndeterminate(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setProgressNumberFormat("");
            progressDialog.setCancelable(true);
            progressDialog.show();

        }

        @Override
        protected String doInBackground(String... params)
        {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReferenceFromUrl(Constants.FIREBASE_URL);

            String uid;
            if (user != null)
            {
                uid = user.getUid();
                Uri file = Uri.fromFile(new File(imagepath));
                final StorageReference sRef2 = storageRef.child("users/"+uid+"/images/"+file.getLastPathSegment());
                final StorageMetadata metadata = new StorageMetadata.Builder()
                        .setCustomMetadata("tags", TextUtils.join(", ",tags_for_cloud))
                        .build();

                UploadTask uploadTask = sRef2.putFile(file, metadata);

                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast toast = Toast.makeText(getApplicationContext(), "Could not upload files.", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        sRef2.updateMetadata(metadata)
                                .addOnSuccessListener(new OnSuccessListener<StorageMetadata>()
                                {
                                    @Override
                                    public void onSuccess(StorageMetadata storageMetadata)
                                    {
                                        Log.d("FIREBASE", "Image metadata set.");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener()
                                {
                                    @Override
                                    public void onFailure(@NonNull Exception exception)
                                    {
                                        Log.d("FIREBASE", "Image metadata error: "+ exception.getMessage());
                                    }
                                });
                        progressDialog.dismiss();
                        Toast toast = Toast.makeText(getApplicationContext(), "Saved to cloud.", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        progressDialog.setProgress((int)progress);
                    }
                }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                        progressDialog.setMessage("Upload paused.");
                    }
                });

            }
            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);
        }
    }
}
