package com.example.jang.client;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

public class PortActivity extends AppCompatActivity {
    private TextInputEditText textInputEditText;
    private ListView listView;

    private Button submit;
    private String port;

    private Handler handler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_port);

        textInputEditText = findViewById(R.id.port_input);
        submit = findViewById(R.id.port_enter);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!textInputEditText.getText().toString().equals("")) {
                    port = textInputEditText.getText().toString();
                    textInputEditText.setText("");
//                    Toast.makeText(PortActivity.this, port ,Toast.LENGTH_SHORT).show();
                    goToMainActivity();
                }
            }
        });

        handler = new Handler();

    }

    public void goToMainActivity() {
        boolean bool = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        if (bool) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.putExtra("port", port);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
//                    finish();
                }
            }, 500);
        } else {
            finish();
        }
    }
}
