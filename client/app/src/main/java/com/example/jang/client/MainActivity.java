package com.example.jang.client;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jang.client.Singleton.NetworkConnector;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * edited by heegi96
 * 2019.06.04 ~
 */


public class MainActivity extends AppCompatActivity {

    public static final String TAG = "태그";
    public static final String IP = "106.10.38.88";
//        public static final int PORT = 6123;
    public int PORT = 7000;

    public int tTop;
    public int tBottom;
    public int tRight;
    public int tLeft;

    public boolean isDetecting;
    public int clickCount = 0;

    Rect uRect = new Rect();
    public List<Rect> faceRects = new ArrayList<>();

    int tempColor[] = {Color.BLACK, Color.BLUE, Color.CYAN, Color.DKGRAY, Color.GRAY, Color.GRAY, Color.GREEN, Color.LTGRAY, Color.MAGENTA, Color.RED, Color.WHITE, Color.YELLOW};
    int tempColor1[] = {Color.parseColor("#FFFFFF"), Color.parseColor("#FF0000"), Color.parseColor("#FF4500"), Color.parseColor("#FF7F00"), Color.parseColor("#F89B00"), Color.parseColor("#FFD400")};
    int tempColor2[] = {Color.parseColor("#000000"), Color.parseColor("#005666"), Color.parseColor("#0080FF"), Color.parseColor("#0099FF"), Color.parseColor("#003153"), Color.parseColor("#080B54")};

    public List<String> faceTextName = new ArrayList<>();
    public List<String> beforeFaceTextInfo = new ArrayList<>();
    public List<String> afterFaceTextInfo = new ArrayList<>();
    public List<Integer> faceIndex = new ArrayList<>();

    private Camera camera;
    private Camera.CameraInfo cameraInfo;

    private Handler handler;
    private boolean isStarting;

    private FrameLayout fl_preview;
    private SurfaceView surfaceView;

    private PredictionsAdapter predictionsAdapter;
    private InformationAdapter informationAdapter;
    private ArrayList<Predictions> predictionsArrayList;
    private ArrayList<Information> informationArrayList;

    private Socket socket; // 연결
    private BufferedReader bufferedReader; // 문자열 수신
    private PrintWriter printWriter; // 문자열 발신
    private BufferedOutputStream bufferedOutputStream; // 바이트 발신

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Intent intent = getIntent();
        if(intent!=null) {
            PORT = Integer.valueOf(intent.getStringExtra("port"));
        }
            Log.d("main port",PORT+"");

        handler = new Handler(getMainLooper());

        isStarting = false;

        fl_preview = findViewById(R.id.fl_preview);

        fl_preview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickCount++;
                Log.i("태그", "클릭 확인 유무");
            }
        });

        fl_preview.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String webPort = String.valueOf(10000+PORT);
                Toast.makeText(tThis, "웹서비스 URL : "+NetworkConnector.getInstance().getUrl()+":"+webPort, Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        setListView();

        getCameraInstance();
    }


    public void setListView() {
        predictionsArrayList = new ArrayList<>();
        informationArrayList = new ArrayList<>();
        predictionsAdapter = new PredictionsAdapter(getApplicationContext(), R.layout.list_predictions);
        informationAdapter = new InformationAdapter(getApplicationContext(), R.layout.list_information);
    }

    public void getCameraInstance() {
        cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                try {
                    camera = Camera.open(i);
                    createSurface();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        RequestDisconnect requestDisconnect = new RequestDisconnect();
        requestDisconnect.execute(String.valueOf(PORT));
        finish();
    }

    public void createSurface() {
        CameraPreview cameraPreview = new CameraPreview(getApplicationContext());
        fl_preview.addView(cameraPreview);
        Box box = new Box(tThis);
        fl_preview.addView(box);
    }

    public class CameraPreview extends SurfaceView {
        private SurfaceHolder surfaceHolder;

        public CameraPreview(Context context) {
            super(context);
            surfaceHolder = getHolder();
            surfaceHolder.addCallback(surfaceHolderCallback);
        }
    }

    SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            setCameraPreview(holder);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (holder.getSurface() == null) {
                return;
            }
            try {
                camera.stopPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
            setCameraPreview(holder);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };

    public void setCameraPreview(SurfaceHolder surfaceHolder) {
        try {
            Camera.Parameters parameters = camera.getParameters();
            List<Camera.Size> list1 = parameters.getSupportedPreviewSizes();//보여주는화면
            List<Camera.Size> list2 = parameters.getSupportedPictureSizes();//찍엇을떄화면
            List<String> list3 = parameters.getSupportedFocusModes();

            StringBuilder supportedPreviewSizes = new StringBuilder("Supported Preview Sizes : ");
            for (int i = 0; i < list1.size(); i++) {
                String str = "(" + list1.get(i).width + "," + list1.get(i).height + ")" + " ";
                supportedPreviewSizes.append(str);
            }
            Log.i(TAG, supportedPreviewSizes.toString());

            StringBuilder supportedPictureSizes = new StringBuilder("Supported Picture Sizes : ");
            for (int i = 0; i < list2.size(); i++) {
                String str = "(" + list2.get(i).width + "," + list2.get(i).height + ")" + " ";
                supportedPictureSizes.append(str);
            }
            Log.i(TAG, supportedPictureSizes.toString());

            StringBuilder supportedFocusModes = new StringBuilder("Supported Focus Modes : ");
            for (int i = 0; i < list3.size(); i++) {
                String str = "(" + list3.get(i) + ")" + " ";
                supportedFocusModes.append(str);
            }
            Log.i(TAG, supportedFocusModes.toString());

            // parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            parameters.setPictureSize(640, 480);
            parameters.setPreviewSize(1920, 1080);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            // parameters.setRotation(90);
            camera.setParameters(parameters);

            camera.setPreviewCallback(previewCallback);
            camera.setDisplayOrientation(0);
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();

            MyFaceDetectionListener fDListener = new MyFaceDetectionListener();
            camera.setFaceDetectionListener(fDListener);
            camera.startFaceDetection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Context tThis = this;

    private class MyFaceDetectionListener implements Camera.FaceDetectionListener {
        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            uRect.setEmpty();
            faceRects.clear();
            isDetecting = false;

            if (faces.length == 0) {
                isDetecting = false;
            } else {
                isDetecting = true;
                for (int i = 0; i < faces.length; i++) {
                    tTop = faces[i].rect.top;
                    tBottom = faces[i].rect.bottom;
                    tRight = faces[i].rect.right;
                    tLeft = faces[i].rect.left;

                    int tempTop = (1000 + tTop) * 1440 / 2000;
                    int tempBottom = (1000 + tBottom) * 1440 / 2000;
                    int tempRight = (1000 + tRight) * 2768 / 2000;
                    int tempLeft = (1000 + tLeft) * 2768 / 2000;
                    uRect = new Rect(tempLeft, tempTop, tempRight, tempBottom);
                    //Log.i(TAG,tempTop + " " + tempBottom + " " + tempLeft + " " + tempRight );
                    faceRects.add(uRect);
                }
                // add function to draw rects on view/surface/canvas
            }

        }
    }

    class Box extends View {

        private Paint rectPaint;
        private Paint textPaint;
        private Paint textStrokePaint;

        Box(Context context) {
            super(context);

            rectPaint = new Paint();
            rectPaint.setAntiAlias(true);
            rectPaint.setColor(Color.YELLOW);
            rectPaint.setStrokeWidth(5);
            rectPaint.setStyle(Paint.Style.STROKE);

            textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setColor(Color.WHITE);
            textPaint.setTypeface(Typeface.create((String) null, Typeface.BOLD));
            textPaint.setTextSize(60);

            textStrokePaint = new Paint();
            textStrokePaint.setAntiAlias(true);
            textStrokePaint.setColor(Color.BLACK);
            textStrokePaint.setTypeface(Typeface.create((String) null, Typeface.BOLD));
            textStrokePaint.setTextSize(60);
            textStrokePaint.setStyle(Paint.Style.STROKE);
            textStrokePaint.setStrokeWidth(2);
        }

        @Override
        protected void onDraw(Canvas canvas) { // Override the onDraw() Method.

            invalidate();

            if (isDetecting && clickCount % 2 == 0) {
                if (!faceRects.isEmpty()) {
                    for (int i = 0; i < faceRects.size(); i++) {
                        Rect rect = faceRects.get(i);
                        canvas.drawRoundRect(rect.left, rect.top, rect.right, rect.bottom + 50, 50, 50, rectPaint);
                    }
                    for (int i = 0; i < faceIndex.size(); i++) {
                        try {
                            if (faceRects.size() > faceIndex.get(i) && faceIndex.size() == faceTextName.size() && faceIndex.size() == beforeFaceTextInfo.size() && faceIndex.size() == afterFaceTextInfo.size()) {
                                String name = faceTextName.get(i);
                                String beforeInfo = beforeFaceTextInfo.get(i);
                                String afterInfo = afterFaceTextInfo.get(i);
                                Rect rect = faceRects.get(faceIndex.get(i));
                                canvas.drawText(name, rect.right + 15, rect.top + 60, textPaint);
                                canvas.drawText(name, rect.right + 15, rect.top + 60, textStrokePaint);
                                canvas.drawText(beforeInfo, rect.right + 15, rect.top + 140, textPaint);
                                canvas.drawText(beforeInfo, rect.right + 15, rect.top + 140, textStrokePaint);
                                canvas.drawText(afterInfo, rect.right + 15, rect.top + 220, textPaint);
                                canvas.drawText(afterInfo, rect.right + 15, rect.top + 220, textStrokePaint);
                            }
                        } catch (IndexOutOfBoundsException e) {
                            Log.e("태그", e.toString());
                        }
                    }
                }
            } else {
                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            }
        }
    }

    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (socket == null) {
                connect();
            }
        }
    };

    public void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    public void takePicture() {
        camera.takePicture(null, null, pictureCallback);
    }

    Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File file = getOutputFile();
            if (file != null) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    // bitmap = rotate(bitmap, 0);
                    bitmap = Bitmap.createScaledBitmap(bitmap, 480, 360, false);

                    FileOutputStream fileOutputStream = new FileOutputStream(file);//이게 기존에 사진을 bitmap과정을 거쳐 나온 사진으로 덮는다는 건가??
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bufferedOutputStream);
                    bufferedOutputStream.flush();
                    bufferedOutputStream.close();

                    Uri uri = Uri.parse("file://" + file.getPath());
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
                    sendBroadcast(intent);

                    SendThread sendThread = new SendThread(file);
                    sendThread.start();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public Bitmap rotate(Bitmap bitmap, float angle) {
        float centerX = bitmap.getWidth() / 2f;
        float centerY = bitmap.getHeight() / 2f;
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        matrix.postScale(-1, 1, centerX, centerY);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public File getOutputFile() {
        File directory = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + getString(R.string.app_name));
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                return null;
            }
        }
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = simpleDateFormat.format(date);
        return new File(directory.getPath() + File.separator + fileName + ".jpg");
    }

    public void connect() {
        ConnectThread connectThread = new ConnectThread();
        connectThread.start();
    }

    private class ConnectThread extends Thread {
        @Override
        public void run() {
            try {
                socket = new Socket();
                SocketAddress socketAddress = new InetSocketAddress(IP, PORT);
                socket.connect(socketAddress, 3000);

                InputStream inputStream = socket.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                bufferedReader = new BufferedReader(inputStreamReader);

                OutputStream outputStream = socket.getOutputStream();
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                printWriter = new PrintWriter(outputStreamWriter);

                bufferedOutputStream = new BufferedOutputStream(outputStream);

                takePicture();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class SendThread extends Thread {

        private File file;

        SendThread(File file) {
            this.file = file;
        }

        @Override
        public void run() {
            try {
                //file에서 얼굴 감지해서 감지된 사진만 전송되게 하기
                String fileName = file.getName();
                long fileSize = file.length();

                //-----발신------
                JSONObject json = new JSONObject();
                json.put("fileName", fileName);
                json.put("fileSize", fileSize);
                printWriter.print(json.toString());
                printWriter.flush();
                Log.i(TAG, "test0. 발신 테스트");

                //------수신-------
                String message = bufferedReader.readLine();
                JSONObject object = new JSONObject(message);
                String _fileName = object.getString("fileName");
                String _fileSize = object.getString("fileSize");
                Log.i(TAG, "test1. 파일 이름 : " + _fileName + " / 파일 크기 : " + _fileSize);

                FileInputStream fileInputStream = new FileInputStream(file);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream); // 바이트 수신

                byte[] buffer = new byte[65536];
                long check = 0;
                while (check != fileSize) {
                    int length = bufferedInputStream.read(buffer);
                    bufferedOutputStream.write(buffer, 0, length);
                    check += length;
                    Log.i("체크", "test2 " + length);
                }
                //음 뒤에 수신을 안써서 바로 넘어가서 그러는건가..흠..
                bufferedOutputStream.flush();
                bufferedInputStream.close();

                // --------------- 수신 ---------------
                message = bufferedReader.readLine();
                object = new JSONObject(message);

                String _currentDate = object.getString("currentDate");
                String _currentStat = object.getString("currentStat");
                //Log.i(TAG, "현재 시각 : " + _currentDate + " / 현재 상태 : " + _currentStat);
                faceTextName.clear();
                beforeFaceTextInfo.clear();
                afterFaceTextInfo.clear();
                faceIndex.clear();
                int middleH = 0;
                int middleW = 0;


                /*String engName[] = {"Jongkwon Jang", "Woosung Jung", "Bora Kim", "Byoungho Yu", "Dokyoung Lee", "Jaeyeop Jeong", "Jisoo Lee", "Kyoungpil Won", "Joohyeon Lee", "Prof Choo"};
                String beforeKorInfo[] = {"10/15 IoT 학회 참석 완료", "09/02 SDN 학회 참석 완료", "11/26 논문 발표 예정", "11/15 미팅 참석", "11/10 세미나 참석", "12/15 서울 학회 참석", "10/23 해외 학회 참석", "10/13 국내 학회 참석", "02/15 논문 제출 완료", "네트워킹 연구실"};
                String afterKorInfo[] = {"11/25 결혼 예정", "12/02 학회 참석 예정", "11/26 논문 발표 예정", "12/15 미팅 예정", "11/10 세미나 참석", "12/15 서울 학회 참석", "12/23 해외 학회 참석", "12/13 국내 학회 참석", "12/15 해외 학회 참석 예정", "네트워킹 연구실"};
                String korName[] = {"장종권", "정우성", "김보라", "유병호", "이도경", "정재엽", "이지수", "원경필", "이주현", "추 교수님"};*/

                switch (_currentStat) {
                    case "Recognition Success":
                        JSONArray _predictions = object.getJSONArray("predictions");
                        JSONArray _information = object.getJSONArray("information");
                        JSONArray _faceLocation = object.getJSONArray("faceLocation");

                        Log.i(TAG, "Test now, _infomation length : " + _information.length() + " _locationg Length : " + _faceLocation.length());

                        for (int i = 0; i < _faceLocation.length(); i += 4) {
                            middleH = (_faceLocation.getInt(i) + (_faceLocation.getInt(i + 1))) / 2;
                            middleW = (_faceLocation.getInt(i + 2) + (_faceLocation.getInt(i + 3))) / 2;
                            for (int j = 0; j < faceRects.size(); j++) {
                                try {
                                    if (middleH >= faceRects.get(j).top && middleH <= faceRects.get(j).bottom && middleW >= faceRects.get(j).left && middleW <= faceRects.get(j).right) {
                                        Log.i(TAG, "여기들어가나? " + i);
                                        Log.i(TAG, " name : " + _information.getJSONObject(i / 4).getString("korName") + " facelocation L : " + _faceLocation.length() + " info L : " + _information.length());
                                        //faceTextName.add(_predictions.getJSONObject(i / 4).getString("className"));
                                        faceTextName.add(_information.getJSONObject(i / 4).getString("korName"));
                                        beforeFaceTextInfo.add(_information.getJSONObject(i / 4).getString("beforeInfo"));
                                        afterFaceTextInfo.add(_information.getJSONObject(i / 4).getString("afterInfo"));
                                        /*
                                        for (int k = 0; k < korName.length; k++) {
                                            Log.i(TAG, "여기는? " + k + " " + _predictions.getJSONObject(i / 4).getString("className"));
                                            if (_predictions.getJSONObject(i / 4).getString("className").equals(engName[k])) {
                                                faceTextName.add(korName[k]);
                                                beforeFaceTextInfo.add(beforeKorInfo[k]);
                                                afterFaceTextInfo.add(afterKorInfo[k]);
                                                Log.i(TAG, "여기들어가나?/ " + korName[k]);
                                            }
                                        }*/
                                        //faceTextName.add(korName[i/4]);
                                        //Log.i(TAG,"i/4 = " + i/4 + " name : " + korName[i/4] + " _information : "+ _information.getJSONObject(i / 4).getString("name"));
                                        faceIndex.add(j);
                                    }
                                } catch (IndexOutOfBoundsException e) {
                                    Log.e("태그", e.toString());
                                }
                            }
                        }
                        Log.i(TAG, "인식 성공 " + "현재 시각 : " + _currentDate + " / 현재 상태 : " + _currentStat + " / " + _predictions + " " + _information);
                        //printResults(_currentDate, _currentStat, _predictions, _information);
                        break;
                    case "Recognition Failure":
                        Log.i(TAG, "인식 실패 " + "현재 시각 : " + _currentDate + " / 현재 상태 : " + _currentStat);
                        break;
                    case "Detection Failure":
                        Log.i(TAG, "검출 실패 " + "현재 시각 : " + _currentDate + " / 현재 상태 : " + _currentStat);
                        printResults();
                        break;
                    default:
                        Log.i(TAG, "낫 띵");
                        break;
                }

                takePicture();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void printResults() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    predictionsArrayList.clear();
                    informationArrayList.clear();
                    predictionsAdapter.notifyDataSetChanged();
                    informationAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    int informationCount = 0;
    int informationColorCount = 0;
    int informationStandardCount = 0;

    public void printResults(final String currentDate, final String currentStat, final JSONArray predictions, final JSONArray information) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                //Iterator<String> keys[] = null;
                try {
                    String text = "[" + currentDate + "] " + currentStat;
                    predictionsArrayList.clear();
                    Log.i("확인", "prediction length : " + predictions.length() + "");
                    for (int i = 0; i < predictions.length(); i++) {
                        String className = predictions.getJSONObject(i).getString("className");
                        String distance = predictions.getJSONObject(i).getString("distance");
                        predictionsArrayList.add(new Predictions(className, distance));
                    }
                    informationArrayList.clear();
                    informationCount = 0;
                    for (int i = 0; i < information.length(); i++) {
                        Log.i(TAG, "information length : " + information.length() + "   information : " + information);
                        informationCount = information.length();
                        Iterator<String> keys = information.getJSONObject(i).keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            String value = information.getJSONObject(i).getString(key);
                            informationArrayList.add(new Information(key, value));
                        }
                        Log.i(TAG, "iformationArrayList size : " + informationArrayList.size());
                    }
                    predictionsAdapter.notifyDataSetChanged();
                    informationAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    class PredictionsAdapter extends ArrayAdapter {
        private Context context;

        PredictionsAdapter(@NonNull Context context, int resource) {
            super(context, resource);
            this.context = context;

        }

        @Override
        public int getCount() {
            return predictionsArrayList.size();
        }

        @Nullable
        @Override
        public Object getItem(int position) {
            return predictionsArrayList.get(position);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            PredictionsList predictionsList = new PredictionsList(context);
            predictionsList.setClassName(predictionsArrayList.get(position).className);
            predictionsList.setDistance(predictionsArrayList.get(position).distance);
            return predictionsList;
        }
    }

    class InformationAdapter extends ArrayAdapter {
        private Context context;

        InformationAdapter(@NonNull Context context, int resource) {
            super(context, resource);
            this.context = context;
        }

        @Override
        public int getCount() {
            return informationArrayList.size();
        }

        @Nullable
        @Override
        public Object getItem(int position) {
            return informationArrayList.get(position);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            InformationList informationList = new InformationList(context);
            informationList.setKey(informationArrayList.get(position).key);
            informationList.setValue(informationArrayList.get(position).value);
            /*if (position % 3 == 0) {
                informationList.showBlank();
            }*/
            return informationList;
        }
    }

    class PredictionsList extends LinearLayout {
        private TextView tv_className;
        private TextView tv_distance;

        public PredictionsList(Context context) {
            super(context);
            LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            if (layoutInflater != null) {
                layoutInflater.inflate(R.layout.list_predictions, this, true);
            }
            tv_className = findViewById(R.id.tv_className);
            tv_distance = findViewById(R.id.tv_distance);
        }

        public void setClassName(String className) {
            tv_className.setText(className);
        }

        public void setDistance(String distance) {
            tv_distance.setText(distance);
        }
    }

    class InformationList extends LinearLayout {

        private TextView tv_key;
        private TextView tv_value;

        public InformationList(Context context) {
            super(context);

            //tv_key.setTextColor(Color.TRANSPARENT);
            LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            if (layoutInflater != null) {
                layoutInflater.inflate(R.layout.list_information, this, true);
            }
            tv_key = findViewById(R.id.tv_key);
            tv_value = findViewById(R.id.tv_value);
        }

        public void setKey(String key) {
            tv_key.setText(key);
            /*Log.i(TAG, "Key test : " + key + " informationColorCount : " + informationColorCount + " informationStand Count : " + informationStandardCount + " information Count : " + informationCount);
            if (informationColorCount != informationCount) {
                tv_key.setTextColor(tempColor2[informationColorCount]);
            }*/
        }

        public void setValue(String value) {
            tv_value.setText(value);
            /*Log.i(TAG, "Value test : " + value + " informationColorCount : " + informationColorCount + " informationStand Count : " + informationStandardCount + " information Count : " + informationCount);
            if (informationColorCount != informationCount) {
                tv_value.setTextColor(tempColor2[informationColorCount]);
                if (informationStandardCount % 2 == 0 && informationStandardCount != 0) {
                    informationColorCount++;
                    informationStandardCount = 0;

                    if (informationColorCount == informationCount) {
                        informationColorCount = 0;
                    }
                } else {
                    informationStandardCount++;
                }
            } else if (informationColorCount == informationCount && informationStandardCount != 0) {
                informationColorCount = 0;
                informationStandardCount = 0;
            }*/
        }
    }

    class Predictions {
        String className;
        String distance;

        Predictions(String className, String distance) {
            this.className = className;
            this.distance = distance;
        }
    }

    class Information {
        String key;
        String value;

        Information(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bufferedOutputStream != null) {
                bufferedOutputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (printWriter != null) {
                printWriter.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private class RequestDisconnect extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
                    String urlString = NetworkConnector.getInstance().getUrl() + ":9990?type=disconnect&port="+strings[0];
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
            Log.d("ressu",s);

//            if (s == null) {
//                Log.d("error", "null res");
//            } else if (s.contains("error")) {
//                Log.d("error", s);
//            } else {
//                Log.d("result", s);
////                Intent intent = new Intent(getApplicationContext(), PortActivity.class);
//                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//                intent.putExtra("port", s);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);
//                finish();
//            }
        }
    }
}