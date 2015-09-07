package com.example.tanmoybanik.FtpCamera;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;


/**
 * Created by Tanmoy Banik on 8/14/2015.
 */
public class FtpConnection {
    private static final String TAG = null;

    public static void imgUpload(File pictureFile) {
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
