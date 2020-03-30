package com.example.project;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity
{

    private static final int REQ_CODE = 100; //Request code to select image from gallery
    private FirebaseAuth auth;
    private Menu optionsmenu;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            window.setStatusBarColor(ContextCompat.getColor(this,R.color.blue1));
        }

        getSupportActionBar().setTitle(Html.fromHtml("<font color=\"#FFFFFF\">" + getString(R.string.app_name) + "</font>"));

        auth = FirebaseAuth.getInstance();
    }

    public boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public boolean onOptionsItemSelected(MenuItem item)
    { switch(item.getItemId())
    {
        case R.id.backup:
            //TODO BACKUP
            return true;
        case R.id.signin:
        {
            if (auth.getCurrentUser() == null)
            {
                if(isInternetAvailable())
                {
                    Intent login = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(login);
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Internet connection not available.", Toast.LENGTH_SHORT).show();
                }
            }
            else
            {
                auth.signOut();
                item.setTitle(R.string.sign_in);
                optionsmenu.findItem(R.id.backup).setEnabled(false);
                optionsmenu.findItem(R.id.backup).getIcon().setAlpha(100);
            }
            return true;
        }
    }
        return(super.onOptionsItemSelected(item));
    }

    protected void onResume()
    {
        super.onResume();
        invalidateOptionsMenu();
    }

    public void takeImage(View view)
    {
        Intent launchImageOCR = new Intent(MainActivity.this, ImageOCRActivity.class);
        launchImageOCR.putExtra("camera","yes");
        startActivity(launchImageOCR);
    }

    public void openGallery(View view)
    {
        Intent galleryPicker = new Intent();
        galleryPicker.setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        galleryPicker.setAction(Intent.ACTION_PICK);
        startActivityForResult(Intent.createChooser(galleryPicker, "Select Image"), REQ_CODE);
    }

    public void savedImages(View view)
    {
        Intent showSavedData = new Intent(MainActivity.this, DisplayDataActivity.class);
        startActivity(showSavedData);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE && resultCode == RESULT_OK)
        {

            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            cursor.moveToFirst();
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            Intent launchImageOCR = new Intent(MainActivity.this, ImageOCRActivity.class);
            launchImageOCR.putExtra("camera","no");
            launchImageOCR.putExtra("path", picturePath);
            startActivity(launchImageOCR);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        optionsmenu = menu;
        if(menu instanceof MenuBuilder)
        {
            ((MenuBuilder) menu).setOptionalIconsVisible(true);
        }
        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (auth.getCurrentUser() != null) {
            menu.findItem(R.id.signin).setTitle(R.string.sign_out);
            menu.findItem(R.id.backup).setEnabled(true);
            menu.findItem(R.id.backup).getIcon().setAlpha(255);
        }
        else
        {
            menu.findItem(R.id.signin).setTitle(R.string.sign_in);
            menu.findItem(R.id.backup).setEnabled(false);
            menu.findItem(R.id.backup).getIcon().setAlpha(100);
        }
        return true;
    }
}
