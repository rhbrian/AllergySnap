package com.example.brhee.allergysnap;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import android.Manifest;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiDetector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Camera2 extends AppCompatActivity {

    private SurfaceView sv;
    private TextView tv;
    private BarcodeDetector barcodeDetector;
    private TextRecognizer textRecognizer;
    private MultiDetector multiDetector;
    private CameraSource cameraSource;

    private static final int requestPermissionID = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera2);
        sv = (SurfaceView)findViewById(R.id.sv_barcode);
        tv = (TextView) findViewById(R.id.tv_barcode);
        tv.setMovementMethod(new ScrollingMovementMethod());

        startCameraSource();
    }

    public void TakePicture(View v) {
        cameraSource.stop();
        String source = tv.getText().toString();
        String ret = "";
        source = source.replaceAll("-", " ");
        source = source.replaceAll(",", "");
        source = source.replaceAll("\\.", "");
        source = source.replaceAll("[(]", "");
        source = source.replaceAll("[)]", "");
        source = source.replaceAll("\\[", "");
        source = source.replaceAll("\\]", "");
        source = source.replaceAll("\\:", "");
        source = source.replaceAll("\\s", " ");
        source = source.replaceAll("INGREDIENTS", "");
        source = source.replaceAll("Ingredients", "");
        source = source.replaceAll("ingredients", "");
//        List<String> list = new ArrayList<>();
//        StringTokenizer st = new StringTokenizer(source, " ");
//        String tok;
//        while (st.hasMoreTokens()) {
//            tok =  st.nextToken();
//            if (tok.length() > 1) {
//                if (!tok.equals("INGREDIENTS") && !tok.equals("ingredients") && !tok.equals("Ingredients")) {
//                    if (wordcheck(tok)) {
//                        list.add(tok);
//                    }
//                }
//            }
//        }
//        if (!list.isEmpty()) {
//            for (int x = 0; x < list.size(); x++) {
//                ret+=list.get(x);
//                ret+=", ";
//            }
//        }
        Bundle bundle = new Bundle();
        Intent i = new Intent(this, ResultActivity.class);
        bundle.putString("picture_value", source);
        i.putExtras(bundle);
        startActivity(i);
    }

    public boolean wordcheck(String word) {
        File external = new File("../words.txt");
        try {
            InputStream is = getApplicationContext().getAssets().open("words.txt");
//            FileInputStream fis = new FileInputStream(external);
//            DataInputStream din = new DataInputStream(fis);
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String str;
            //Log.e("string", "found words.txt");
            while ((str = in.readLine()) != null) {
                if (str.indexOf(word) != -1) {
                    return true;
                }
            }
            in.close();
        } catch (IOException e) {
            Log.e("string", "could not find word.txt");
        }

        return false;
    }

    private void startCameraSource() {


        // Create the BarcodeDetector
        barcodeDetector = new BarcodeDetector.Builder(this).build();

        // Create the TextRecognizer
        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        // Create MultiDetector
        multiDetector = new MultiDetector.Builder().add(barcodeDetector).add(textRecognizer).build();

        // Create CameraSource
        cameraSource = new CameraSource.Builder(this, multiDetector).setAutoFocusEnabled(true).setRequestedPreviewSize(1600, 1024).build();;

        if (!multiDetector.isOperational()) {
            Log.d("cam2activity","Detector dependencies not loaded yet");
        } else {

            /**
             * Add call back to SurfaceView and check if camera permission is granted.
             * If permission is granted we can start our cameraSource and pass it to surfaceView
             */

            sv.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    try {

                        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(Camera2.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    requestPermissionID);
                            return;
                        }
                        cameraSource.start(sv.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }

                /**
                 * Release resources for cameraSource
                 */

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    cameraSource.stop();
                }
            });

            //Set the TextRecognizer's Processor.
            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {
                }

                /**
                 * Detect all the text from camera using TextBlock and the values into a stringBuilder
                 * which will then be set to the textView.
                 * */

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {
                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if (items.size() != 0 ){

                        tv.post(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder stringBuilder = new StringBuilder();
                                for(int i=0;i<items.size();i++) {
                                    TextBlock item = items.valueAt(i);
                                    stringBuilder.append(item.getValue());
                                    stringBuilder.append("-");
                                }
                                tv.setText(stringBuilder.toString());
                            }
                        });
                    }
                }
            });
        }

        /**
          * Callback for Barcode Scanner
          */
        sv.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                if (ActivityCompat.checkSelfPermission(Camera2.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                try {
                    cameraSource.start(sv.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                cameraSource.stop();
            }
        });


        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();

                if (barcodes.size() > 0) {
                    Intent intent = new Intent();
                    intent.putExtra("barcode", barcodes.valueAt(0)); // Gets the latest barcode from the array
                    setResult(CommonStatusCodes.SUCCESS, intent);
                    finish();
                }
            }
        });
    }

}
