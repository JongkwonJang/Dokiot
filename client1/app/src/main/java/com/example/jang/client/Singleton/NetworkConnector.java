package com.example.jang.client.Singleton;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class NetworkConnector {
    private final String TAG = "NETWORK_CONNECT";
    private String port = "";

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUrl() {
        return "http://106.10.38.88";
    }

    private HttpURLConnection connection;

    private static NetworkConnector instance = new NetworkConnector();

    public static NetworkConnector getInstance() {
        return instance;
    }

    private NetworkConnector() {
    }

    private void connect(String url) {
        try {
            URL url1 = new URL(url);
            this.connection = (HttpURLConnection) url1.openConnection();

            connection.setDefaultUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("content-type", "application/x-www-form-urlencoded");
            connection.setRequestMethod("POST");
        } catch (Exception e) {
            Log.e(TAG, "CONNECT ERROR");
            e.printStackTrace();
        }
    }

    private String write() {
        String result = null;

        try {
            BufferedReader bf = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder string = new StringBuilder();
            String line = null;
            while ((line = bf.readLine()) != null) {
                string.append(line);
            }
            result = string.toString();

        } catch (Exception e) {
            Log.e(TAG, "WRITE ERROR");
            e.printStackTrace();
        }

        return result;
    }

    public String post(String url, String data) {
        connect(url);
        if (!data.contains("data=")) data = "data=" + data;

        try {
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
            pw.write(data);
            pw.flush();
            pw.close();
        } catch (Exception e) {
            Log.e(TAG, "POST ERROR");
            e.printStackTrace();
        }

        return write();
    }

    public String get(String url) {
        connect(url);
        return write();
    }

    public String encodeData(String data) {
        try {
            String string = URLEncoder.encode(data, "utf-8");
            return string;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }
}