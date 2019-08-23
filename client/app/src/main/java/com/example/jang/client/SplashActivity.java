package com.example.jang.client;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.jang.client.Singleton.NetworkConnector;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class SplashActivity extends AppCompatActivity {

    private static final int REQ_CODE_PERM_CAMERA = 1001;
    private static final int REQ_CODE_PERM_STORAGE = 1002;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        handler = new Handler();

        if (Build.VERSION.SDK_INT >= 23) {
            checkCameraPermission();
        } else {
//            goToMainActivity();
//            Intent intent = new Intent(getApplicationContext(), PortActivity.class);
//            startActivity(intent);
//            finish();
            requestUserPort();
        }
    }

    public void checkCameraPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.CAMERA);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            checkStoragePermission();
        } else {
            ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.CAMERA}, REQ_CODE_PERM_CAMERA);
        }
    }

    public void checkStoragePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
//            Intent intent = new Intent(getApplicationContext(), PortActivity.class);
//            startActivity(intent);
//            finish();
            requestUserPort();
        } else {
            ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_CODE_PERM_STORAGE);
        }
    }

//    public void goToMainActivity() {
//        boolean bool = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
//        if (bool) {
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(intent);
//                    finish();
//                }
//            }, 500);
//        } else {
//            finish();
//        }
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQ_CODE_PERM_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkStoragePermission();
            } else {
                finish();
            }
        } else if (requestCode == REQ_CODE_PERM_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                goToMainActivity();
//                Intent intent = new Intent(getApplicationContext(), PortActivity.class);
//                startActivity(intent);
                requestUserPort();
            } else {
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void requestUserPort() {
        RequestPort requestPort = new RequestPort();
        requestPort.execute();
    }

    private class RequestPort extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            String urlString = NetworkConnector.getInstance().getUrl() + ":9990?type=connect";
            Log.d("urlTestLog", urlString);
//            String result = NetworkConnector.getInstance().get(url);
////            Log.d("result",result);
//            return result;

            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("user_id", "androidTest");
                jsonObject.accumulate("name", "yang");

                HttpURLConnection con = null;
                BufferedReader reader = null;

                try {
                    //URL url = new URL("http://192.168.25.16:3000/users");
                    URL url = new URL(urlString);
                    con = (HttpURLConnection) url.openConnection();
                    con.connect();

                    InputStream stream = con.getInputStream();

                    reader = new BufferedReader(new InputStreamReader(stream));

                    StringBuffer buffer = new StringBuffer();

                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }

                    return buffer.toString();

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                    try {
                        if (reader != null) {
                            reader.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
//            Log.d("ressu",s);

            if (s == null) {
                Log.d("error", "null res");
            } else if (s.contains("error")) {
                Log.d("error", s);
            } else {
                Log.d("result", s);
//                Intent intent = new Intent(getApplicationContext(), PortActivity.class);
//                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//                intent.putExtra("port", s);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);
//                finish();
                goToMainActivity(s);
            }
        }
    }

    public void goToMainActivity(final String port) {
        boolean bool = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        if (bool) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.putExtra("port", port);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }, 500);
        } else {
            finish();
        }
    }

}
