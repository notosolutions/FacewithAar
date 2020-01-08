package com.app.facerecogn;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Typeface;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;

import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.app.facerecogn.common.Camerasource;
import com.app.facerecogn.common.CameraSourcePreview;
import com.app.facerecogn.common.GraphicOverlay;
import com.app.facerecogn.facedetection.FaceContourDetectorProcessor;
import com.app.facerecogn.interfaces.adapteritemclick;
import com.app.facerecogn.utils.Conversions;
import com.app.facerecogn.utils.progressdialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.example.mylibrary.*;
import java.util.concurrent.Semaphore;

import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    //private ProgressDialog dialog=null, dialog1=null;
    ImageView image_view, clickcamera;
    GifImageView gifguider;
    private static final String TAG = "AndroidCameraApi";
    private LinearLayout takePictureButton;
    //private TextureView textureView;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    int type=0;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private Semaphore cameraopencloselock = new Semaphore(1);
    //Toast t=null;

    TextView text_type;
    byte[] bytes;
    File file = null;
    ImageView imageeee;
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;

    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private Bitmap screenshotBitmap;
    FrameLayout frame_view;
    Size smallerPreviewSize;
    private Camerasource cameraSource = null;
    private static final String FACE_CONTOUR = "Face Contour";
    private String selectedModel = FACE_CONTOUR;
    private static final int PERMISSION_REQUESTS = 1;
    FaceContourDetectorProcessor FaceContourDetectoObj;

    SensorManager mSensorManager;
    Handler handler;
    TextView txt_guider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

       // appTestJni();
        String strresult = Conversions.getInstance().SkintoneCRJni("");
        Log.e("strresult=",""+strresult);

       /* String strresult1 = SkintoneCRJni();
        double strresult2 = PoresCRJni();*/

        setContentView(R.layout.activity_main);
        preview = findViewById(R.id.firePreview);
        if (preview == null) {
            Log.d(TAG, "Preview is null");
        }
        graphicOverlay = findViewById(R.id.fireFaceOverlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }
        image_view = (ImageView) findViewById(R.id.image_view);
        gifguider = (GifImageView) findViewById(R.id.gifguider);
        text_type= (TextView) findViewById(R.id.text_type);
        frame_view=(FrameLayout) findViewById(R.id.frame_view);
        txt_guider = (TextView) findViewById(R.id.guider);
        clickcamera = (ImageView) findViewById(R.id.clickcamera);

        image_view.setImageResource(R.drawable.front_1);
        if (allPermissionsGranted()) {
            createCameraSource(selectedModel);
        } else {
            getRuntimePermissions();
        }
        takePictureButton = (LinearLayout) findViewById(R.id.btn_takepicture);
        assert takePictureButton != null;

        FaceContourDetectoObj = new FaceContourDetectorProcessor();

        Typeface font = Typeface.createFromAsset(getAssets(), "EUROSTIB.ttf");
        txt_guider.setTypeface(font);

        frame_view.setOnClickListener(new DoubleClickListener() {

            @Override
            public void onSingleClick(View v) {

            }

            @Override
            public void onDoubleClick(View v) {
                ClickPicture();
            }
        });

        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClickPicture();
            }
        });
        takePictureButton.setClickable(false);

        String d=getDeviceName();
        Log.e("device"," "+d);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        handler = new Handler();
    }

    //public native double appTestJni(String str1, String str2);

    private void createCameraSource(String model) {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = new Camerasource(this, graphicOverlay);
            cameraSource.setFacing(Camerasource.CAMERA_FACING_FRONT);
        }

        try {
            Log.i(TAG, "Using Face Contour Detector Processor");
            cameraSource.setMachineLearningFrameProcessor(new FaceContourDetectorProcessor(new adapteritemclick() {
                @Override
                public void onItemClicked(float value) {
                    if (value>1600) {
                        takePictureButton.setClickable(true);
                        clickcamera.setVisibility(View.VISIBLE);
                        //Toast.makeText(MainActivity.this, ""+value, Toast.LENGTH_SHORT).show();
                    }else {
                        takePictureButton.setClickable(false);
                        clickcamera.setVisibility(View.GONE);
                    }
                }
            }));
        } catch (Exception e) {
            Log.e(TAG, "Can not create image processor: " + model, e);
            Toast.makeText(getApplicationContext(), "Can not create image processor: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null");
                }
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    /*TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };*/

    /*private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraopencloselock.release();
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraopencloselock.release();
            camera.close();
            cameraDevice = null;
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraopencloselock.release();
            camera.close();
            cameraDevice = null;
        }
    };*/

    /*final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };*/

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;

        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }
        return phrase.toString();
    }

    protected void ClickPicture(){
        if (cameraSource !=null){
            type++;
            progressdialog.showwaitingdialog(MainActivity.this);
            //bytes = cameraSource.getData();
            //Camerasource.takeByteArrayofPicture();

            cameraSource.takeByteArrayofPicture(null, new Camerasource.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data) {
                    bytes=data;
                }
            });

            File image_dir = new File("/sdcard/SelfieApp/");
            image_dir.mkdirs();
            Random rand = new Random();
            int n = rand.nextInt(2000);

            if(type==1) {
                text_type.setText("Left");
                file = new File(Environment.getExternalStorageDirectory() + "/SelfieApp/front_"+n+".jpg");
            }else if(type==2) {
                text_type.setText("Right");
                file= new File(Environment.getExternalStorageDirectory()+"/SelfieApp/left_"+n+".jpg");
            }else if(type==3) {
                file= new File(Environment.getExternalStorageDirectory()+"/SelfieApp/right_"+n+".jpg");
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);

                        progressdialog.dismisswaitdialog();
                        switch(type) {
                            case 1: {

                                //image_view.setImageResource(R.drawable.left_1);
                                gifguider.setVisibility(View.VISIBLE);
                                gifguider.setImageResource(R.drawable.image_guide_gif_left);
                                txt_guider.setVisibility(View.VISIBLE);
                                break;
                            }
                            case 2:{
                                //image_view.setImageResource(R.drawable.right_1);
                                gifguider.setVisibility(View.VISIBLE);
                                gifguider.setImageResource(R.drawable.image_guide_gif_right);
                                txt_guider.setVisibility(View.VISIBLE);
                                break;
                            }
                        }
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                if (type == 3) {
                                    finish();
                                    Intent in = new Intent(MainActivity.this, HomeActivity.class);
                                    startActivity(in);
                                }
                            }
                        });
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (null != output) {
                            try {
                                output.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }, 1000);
        }
    }

    private Camerasource.PictureCallback mPicture = new Camerasource.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data) {

        }
    };

    protected void takePicture() {

        if(null == cameraDevice){
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        type++;

        progressdialog.showwaitingdialog(MainActivity.this);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            switch(type) {
                case 1: {

                    image_view.setImageResource(R.drawable.left_1);
                    gifguider.setVisibility(View.VISIBLE);
                    gifguider.setImageResource(R.drawable.image_guide_gif_left);
                    txt_guider.setVisibility(View.VISIBLE);
                    break;
                }
                case 2:{
                    image_view.setImageResource(R.drawable.right_1);
                    gifguider.setVisibility(View.VISIBLE);
                    gifguider.setImageResource(R.drawable.image_guide_gif_right);
                    txt_guider.setVisibility(View.VISIBLE);
                    break;
                }
            }

            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 3);
            final List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            //outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,getJpegOrientation(characteristics,rotation));

            File image_dir = new File("/sdcard/SelfieApp/");
            image_dir.mkdirs();
            Random rand = new Random();
            int n = rand.nextInt(2000);

            if(type==1) {
                text_type.setText("Left");
                file = new File(Environment.getExternalStorageDirectory() + "/SelfieApp/front_"+n+".jpg");
            }else if(type==2) {
                text_type.setText("Right");
                file= new File(Environment.getExternalStorageDirectory()+"/SelfieApp/left_"+n+".jpg");
            }else if(type==3) {
                file= new File(Environment.getExternalStorageDirectory()+"/SelfieApp/right_"+n+".jpg");
            }

            final ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {

                    Image image = null;
                    try {
                        image = reader.acquireNextImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }

                private void save(final byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);

                        progressdialog.dismisswaitdialog();
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                if(type==3) {
                                    finish();
                                    Intent in = new Intent(MainActivity.this, HomeActivity.class);
                                    startActivity(in);
                                }
                            }
                        });
                    }finally{
                        if(null != output) {
                            output.close();
                        }
                    }
                }
            };
            /*reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);

            *//*new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {*//*

                    final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                            super.onCaptureCompleted(session, request, result);
                            createCameraPreview();
                        }
                    };
                    try {
                        cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                try {
                                    session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }
                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {
                            }
                        }, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                *//*}
            }, 1000);*/

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }

    private int getJpegOrientation(CameraCharacteristics c, int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return 0;
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        deviceOrientation = (deviceOrientation + 45) / 90 * 90;
        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;

        return jpegOrientation;
    }

    /*protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());

            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }*/
/*    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[1];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            smallerPreviewSize = chooseVideoSize(map.getOutputSizes(SurfaceTexture.class));
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    protected Size chooseVideoSize(Size[] choices) {
        List<Size> smallEnough = new ArrayList<>();

        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                smallEnough.add(size);
            }
        }
        if (smallEnough.size() > 0) {
            return Collections.max(smallEnough, new CompareSizeByArea());
        }

        return choices[choices.length - 1];
    }

    public class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (allPermissionsGranted()) {
            createCameraSource(selectedModel);
        }else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startCameraSource();

        /*Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                Float contourheight =  FaceContourDetectoObj.getContourHeight();
                Toast.makeText(MainActivity.this, Float.toString(contourheight), Toast.LENGTH_SHORT).show();
                handler.postDelayed(this, 5000);
            }
        };
        handler.postDelayed(myRunnable, 5000);*/
        /*startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }*/
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();
        progressdialog.dismisswaitdialog();
        preview.stop();
        //stopBackgroundThread();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public abstract class DoubleClickListener implements View.OnClickListener {

        private static final long DOUBLE_CLICK_TIME_DELTA = 300;//milliseconds

        long lastClickTime = 0;

        @Override
        public void onClick(View v) {
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA){
                onDoubleClick(v);
                lastClickTime = 0;
            } else {
                onSingleClick(v);
            }
            lastClickTime = clickTime;
        }

        public abstract void onSingleClick(View v);
        public abstract void onDoubleClick(View v);
    }
}