package com.marcinmoskala.arcseekbar.sample;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.marcinmoskala.arcseekbar.ArcSeekBar;
import com.marcinmoskala.arcseekbar.ProgressListener;

public class MainActivity extends AppCompatActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ArcSeekBar arcSeekBar = findViewById(R.id.seekArc);

        arcSeekBar.setMaxProgress(200);
        ProgressListener progressListener = progress -> Log.i("SeekBar", "Value is " + progress);
        progressListener.invoke(0);
        arcSeekBar.setOnProgressChangedListener(progressListener);

        int[] intArray = getResources().getIntArray(R.array.progressGradientColors);
        arcSeekBar.setProgressGradient(intArray);
    }
}
