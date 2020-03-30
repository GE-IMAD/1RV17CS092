package com.example.project;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SearchView;

import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class DisplayDataActivity extends AppCompatActivity implements ImageAdapter.ImageAdapterListener
{
    private DBHelper db;
    private static final String TAG = MainActivity.class.getSimpleName();
    private RecyclerView recyclerView;
    private List<ImageData> imageList;
    private ImageAdapter mAdapter;
    private SearchView searchView;
    private ArrayList<ImageData> data_local;
    private ArrayList<ImageData> data_cloud;
    private Toolbar toolbar;
    private FirebaseUser user;

    @Override
    public void onImageDataSelected(ImageData imageData)
    {
        Intent displayIntent = new Intent(DisplayDataActivity.this, DisplayActivity.class);
        displayIntent.putExtra("imagename",imageData.getImagename());
        displayIntent.putExtra("imagepath",imageData.getImagepath());
        displayIntent.putExtra("textpath",imageData.getTextpath());
        displayIntent.putExtra("taglist",imageData.getTaglist());
        displayIntent.putExtra("mode",imageData.getMode());
        startActivity(displayIntent);
    }

    class Load extends AsyncTask<String, String, String>
    {

        private ProgressDialog progDialog;
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            progDialog = new ProgressDialog(DisplayDataActivity.this, R.style.MyAlertDialogStyle);
            progDialog.setMessage("Loading saved data...");
            progDialog.setIndeterminate(false);
            progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progDialog.setCancelable(true);
            progDialog.show();
        }

        @Override
        protected String doInBackground(String... params)
        {
            data_local = db.getData(Constants.LOCAL_MODE,"");
            imageList.clear();
            imageList.addAll(data_local);

            user = FirebaseAuth.getInstance().getCurrentUser();

            if(user!=null)
            {
                data_cloud = db.getData(Constants.CLOUD_MODE, user.getUid());
                imageList.addAll(data_cloud);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            mAdapter.notifyDataSetChanged();
            super.onPostExecute(result);
            progDialog.dismiss();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_data);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            window.setStatusBarColor(ContextCompat.getColor(this,R.color.blue1));
        }

        getSupportActionBar().setTitle(Html.fromHtml("<font color=\"#FFFFFF\">" + "Saved Images" + "</font>"));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.data_list);
        imageList = new ArrayList<>();
        mAdapter = new ImageAdapter(this, imageList, this);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST, 36));
        recyclerView.setAdapter(mAdapter);
        db = new DBHelper(this);
        new Load().execute();
    }

    /*
    @Override
    protected void onPause() {
        super.onPause();
        Log.d("LIFECYCLE","onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("LIFECYCLE","onResume");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("LIFECYCLE","onRestart");
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        Log.d("LIFECYCLE", "onDestroy");
    }
    */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setQueryHint(getText(R.string.search_hint));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                mAdapter.getFilter().filter(query);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void onBackPressed() {
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
        } else {
            super.onBackPressed();
        }
    }
}
