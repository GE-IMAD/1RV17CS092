package com.example.project;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.material.tabs.TabLayout;


public class DisplayActivity extends AppCompatActivity
{
    private TabAdapter adapter;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private int[] tabIcons = {
            R.drawable.ic_camera_white_small,
            R.drawable.ic_subject_white_small,
            R.drawable.ic_summary_white_small
    };
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            window.setStatusBarColor(ContextCompat.getColor(this,R.color.blue1));
        }

        Intent intent = getIntent();
        String str_imagename = intent.getStringExtra("imagename");
        String str_taglist = intent.getStringExtra("taglist");
        String str_imagepath = intent.getStringExtra("imagepath");
        String str_textpath = intent.getStringExtra("textpath");
        int mode = intent.getIntExtra("mode", Constants.LOCAL_MODE);

        if(str_imagename!=null)
            getSupportActionBar().setTitle(Html.fromHtml("<font color=\"#FFFFFF\">" + str_imagename + "</font>"));
        else
            getSupportActionBar().setTitle(Html.fromHtml("<font color=\"#FFFFFF\">" + "Unknown Image" + "</font>"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        adapter = new TabAdapter(getSupportFragmentManager(), this);
        adapter.addFragment(new Tab1Fragment(str_imagepath, str_taglist, mode,this), "Image", tabIcons[0]);
        adapter.addFragment(new Tab2Fragment(str_textpath, mode, this), "Text", tabIcons[1]);
        adapter.addFragment(new Tab3Fragment(str_textpath, mode), "Summary", tabIcons[2]);

        viewPager.setAdapter(adapter);
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            assert tab != null;
            tab.setCustomView(null);
            tab.setCustomView(adapter.getTabView(i));
        }

        tabLayout.setupWithViewPager(viewPager);
        highLightCurrentTab(0);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            { }
            @Override
            public void onPageSelected(int position)
            {
                highLightCurrentTab(position);
            }
            @Override
            public void onPageScrollStateChanged(int state) { }
        });
    }

    private void highLightCurrentTab(int position)
    {
        for (int i = 0; i < tabLayout.getTabCount(); i++)
        {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            assert tab != null;
            tab.setCustomView(null);
            tab.setCustomView(adapter.getTabView(i));
        }
        TabLayout.Tab tab = tabLayout.getTabAt(position);
        assert tab != null;
        tab.setCustomView(null);
        tab.setCustomView(adapter.getSelectedTabView(position));
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        onBackPressed();
        return true;
    }
}
