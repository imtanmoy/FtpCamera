package com.example.tanmoybanik.FtpCamera;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.Surface;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {


    private Camera mCamera;
    private CameraPreview mPreview;
    static File pictureFile=null;
    final static String TAG=null;
    FrameLayout preview;
    Activity context;

    private int mInterval = 30000;
    private Handler mHandler;

    static Context ctx;
    static boolean hasCam;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);*/
        setContentView(R.layout.activity_main);


        ////Initialize Camera onCreate/////
        ctx=getApplicationContext();
        context=this;
        hasCam=hasCamera(ctx);


        mCamera=getCameraInstance();
        mPreview = new CameraPreview(this, mCamera);
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        mPreview.setKeepScreenOn(true);


        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }



        if(isNetworkAvailable())
        {
            showAlertDialog("Title","Internet Connected",true);
            mHandler=new Handler();
            mHandler.post(runnableCode);
        }
        else
        {
            showAlertDialog("error","Internet is not Connected",false);
        }


    }


    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            // Do something here on the main thread
            Log.i(TAG, "Thred Called");
            if (mPreview.safeToTakePicture)
            {
                mCamera.startPreview();
                mCamera.takePicture(null, null, mPicture);
                mPreview.safeToTakePicture=false;
            }

            // Repeat this runnable code again every 2 seconds
            mHandler.postDelayed(runnableCode, mInterval);
        }
    };
// Kick off the first runnable task right away





    /////// Creating a camera instance which will open the camera//////

    public Camera getCameraInstance(){

        Camera c = null;
        if (!hasCam)
        {
            Toast.makeText(ctx, "Your Mobile doesn't have camera!!!", Toast.LENGTH_LONG).show();
        }else
        {
            try{
                c=openBackFacingCamera();
            }
            catch (Exception e){
                Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
            }
        }

        return c;
    }

    /////Checking if the device has camera////////////////

    public boolean hasCamera(Context context){
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        }else {
            return false;
        }
    }

    private void releaseCamera(){
        mPreview.setCamera(null);
        if (mCamera!=null){
           // mCamera.lock();
            mCamera.release();
            mCamera=null;
        }
    }

    /////////////////////front facing camera open//////////////

    private Camera openBackFacingCamera()
    {

        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for ( int camIdx = 0; camIdx < cameraCount; camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo );
            if ( cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK  ) {
                try {
                    //releaseCamera();
                    cam = Camera.open( camIdx );
                } catch (RuntimeException e) {
                    Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }

        return cam;
    }


    @Override
    protected void onPause() {
        super.onPause();
        //mPreview.setCamera(null)
        mCamera.stopPreview();
        releaseCamera();
        mHandler.removeCallbacks(runnableCode);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // mCamera.stopPreview();
        releaseCamera();
        //  mCamera=null;
        mHandler.removeCallbacks(runnableCode);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mCamera == null) {
            mCamera = getCameraInstance();
            mPreview = new CameraPreview(this, mCamera);
            mPreview.setCamera(mCamera);
            //FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mPreview);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mCamera != null) {
            if (Build.VERSION.SDK_INT >= 14)
                setCameraDisplayOrientation(context,
                        Camera.CameraInfo.CAMERA_FACING_BACK, mCamera);
            mPreview.setCamera(mCamera);
        }

    }


    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }



    /////////////////////////////////////////////Saving Media Files creating uri and files//////////////////////////////////



    private static Uri getoutputMediaFileUri(){

        return Uri.fromFile(getoutputMediaFile());
    }

    private static File getoutputMediaFile() {

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCameraApp");
        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                Log.d("MycameraApp", "Failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile= new File(mediaStorageDir.getPath()+File.separator+"IMG_"+timeStamp+".jpg");
        return mediaFile;
    }

    byte[] resizeImage(byte[] input) {
        Bitmap original = BitmapFactory.decodeByteArray(input , 0, input.length);
        Bitmap resized = Bitmap.createScaledBitmap(original, 720, 1280, true);

        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 100, blob);

        return blob.toByteArray();
    }


    private Camera.PictureCallback mPicture= new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            byte[] resized=resizeImage(data);
            pictureFile = getoutputMediaFile();

            mCamera.startPreview();
            if (pictureFile == null) {
                //no path to picture, return
                mPreview.safeToTakePicture = true;
                return;
            }

            if (pictureFile==null)
            {
                return;
            }
            try{
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.flush();
                fos.close();
                refreshFallery(pictureFile);
                //imgUpload();

                //String filepath = pictureFile.getAbsolutePath();
                AsyncCallWS task=new AsyncCallWS();
                task.execute();


            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e(TAG, "Camera failed to take picture: " + e.getLocalizedMessage());
            }catch (IOException e){
                e.printStackTrace();
                Log.e(TAG, "Camera failed to take picture: " + e.getLocalizedMessage());
            }
            mPreview.safeToTakePicture = true;
        }
    };



    ///Refreshing the gallery too show the image///////////////s

    private void refreshFallery(File file){

        Intent mediaScanintent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanintent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanintent);
    }



    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void showAlertDialog(String title, String message, boolean status) {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();

        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setCancelable(false);
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        if (status)
        {
            alertDialog.setIcon(R.drawable.ok);
        }
        else
        {
            alertDialog.setIcon(R.drawable.error);
        }

        alertDialog.show();
    }



    public static void imgUpload() {
        FTPClient ftpclient = new FTPClient();
        FileInputStream fis = null;
        boolean result;
        String ftpServerAddress = "31.170.167.137";
        String userName = "u585976816";
        String password = "abcde12345";

        try{
            ftpclient.connect(ftpServerAddress);
            result = ftpclient.login(userName, password);

            if (result == true) {
                //System.out.println("Logged in Successfully !");
                Log.i(TAG, "Logged in Successfully !");
            } else {
                //System.out.println("Login Fail!");
                Log.i(TAG, "Login Fail!");
                return;
            }
            ftpclient.setFileType(FTP.BINARY_FILE_TYPE);

            ftpclient.changeWorkingDirectory("/FtpCam/");
           // String filepath = "/sdcard/file.jpg";

            String filepath = pictureFile.getAbsolutePath();


            File file = new File(filepath);
            String testName = file.getName();
            fis = new FileInputStream(file);

            // Upload file to the ftp server
            result = ftpclient.storeFile(testName, fis);

            if (result == true) {
                //System.out.println("File is uploaded successfully");
                Log.i(TAG, "File is uploaded successfully");
            } else {
               // System.out.println("File uploading failed");
                Log.i(TAG, "File uploading failed");
            }
            ftpclient.logout();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                ftpclient.disconnect();
            } catch (FTPConnectionClosedException e) {
                System.out.println(e);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}

class AsyncCallWS extends AsyncTask<Void, Void, Void> {
    private static final String TAG="";



    @Override
    protected Void doInBackground(Void... params) {
        Log.i(TAG, "doInBackground");
        MainActivity.imgUpload();
         return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        Log.i(TAG, "onPostExecute");
    }


    @Override
    protected void onPreExecute() {
        Log.i(TAG, "onPreExecute");
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        Log.i(TAG, "onProgressUpdate");
    }

}
