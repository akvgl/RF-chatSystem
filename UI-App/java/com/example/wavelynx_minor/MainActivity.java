package com.example.wavelynx_minor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
VideoView vw;
Button b1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.first_page);
        vw=findViewById(R.id.videoView);
        b1=findViewById(R.id.startchatting);
        vw.setVideoPath("android.resource://"+getPackageName()+"/"+R.raw.wavelynx_video);
        vw.start();
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this, DeviceListActivity.class);
                startActivity(intent);
            }
        });


    }
}