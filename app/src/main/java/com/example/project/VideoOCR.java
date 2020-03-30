package com.example.project;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

public class VideoOCR extends AppCompatActivity
{
    SurfaceView mCameraView;
    TextView mTextView;
    CameraSource mCameraSource;

    private static final String TAG1 = "OCR";
    private static final String TAG2 = "TTS";
    private static final int requestPermissionID = 101;

    private TextToSpeech tts;

    Summarizer s = new Summarizer();

    @Override
    public void onBackPressed() {
        finish();
    }

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_ocr);

        mCameraView = findViewById(R.id.surfaceView);
        mTextView = findViewById(R.id.text_view);

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
                        Log.v(TAG2,"onInit succeeded");
                        speak("Launching OCR", TextToSpeech.QUEUE_FLUSH);
                    }
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Initialization failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        startCameraSource();
    }

    void speak(String s, int option)
    {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            Log.v(TAG2, "Speak new API");
            Bundle bundle = new Bundle();
            bundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
            tts.speak(s, option, bundle, null);
        }
        else
        {
            Log.v(TAG2, "Speak old API");
            HashMap<String, String> param = new HashMap<>();
            param.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
            tts.speak(s, option, param);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode != requestPermissionID)
        {
            Log.d(TAG1, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            try
            {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                {
                    return;
                }
                mCameraSource.start(mCameraView.getHolder());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void startCameraSource()
    {
        //Create the TextRecognizer
        final TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (!textRecognizer.isOperational())
        {
            Log.w(TAG1, "Detector dependencies not loaded yet");
        }
        else
        {
            mCameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(2.0f)
                    .build();

            mCameraView.getHolder().addCallback(new SurfaceHolder.Callback()
            {
                @Override
                public void surfaceCreated(SurfaceHolder holder)
                {
                    try
                    {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                        {

                            ActivityCompat.requestPermissions(VideoOCR.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    requestPermissionID);
                            return;
                        }
                        mCameraSource.start(mCameraView.getHolder());
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder)
                {
                    mCameraSource.stop();
                }
            });

            textRecognizer.setProcessor(new Detector.Processor<TextBlock>()
            {
                @Override
                public void release() { }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections)
                {
                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if (items.size() != 0 )
                    {
                        mTextView.post(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                StringBuilder stringBuilder = new StringBuilder();
                                for(int i=0;i<items.size();i++)
                                {
                                    TextBlock item = items.valueAt(i);
                                    stringBuilder.append(item.getValue());
                                    stringBuilder.append("\n");
                                }
                                //mTextView.setText(stringBuilder.toString());
                                //speak(stringBuilder.toString(), TextToSpeech.QUEUE_ADD);
                                String summary = s.Summarize(stringBuilder.toString(), 5);
                                mTextView.setText(summary);
                            }
                        });
                    }
                }
            });
        }
    }
}
