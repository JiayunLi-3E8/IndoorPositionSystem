package com.android.ipositionsystem;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.customview.widget.Openable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.android.ipositionsystem.ui.MainViewModel;
import com.fengmap.android.FMMapSDK;
import com.google.android.material.navigation.NavigationView;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        IpsDbHelper.DB_NAME = getExternalFilesDir("databases").getAbsolutePath() + File.separator + IpsDbHelper.DB_NAME + ".db"; //将数据库放到sdcard

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Openable openable = findViewById(R.id.open_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_location, R.id.nav_collect)
                .setOpenableLayout(openable)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        FMMapSDK.init(getApplication(), getExternalCacheDir().getPath()); //地图类初始化，将缓存放到sdcard

        MainViewModel viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        SensorManager service = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        IpsDbHelper ipsDbHelper = new IpsDbHelper(this);
        viewModel.setSensorManager(service);
        viewModel.setOpenHelper(ipsDbHelper);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}