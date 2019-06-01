package com.example.jz.aprotect;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.support.annotation.RequiresApi;
import android.util.ArrayMap;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dalvik.system.DexClassLoader;

public class MyApplication extends Application {

    private static Context mContext;
    private String mApkPath;
    private String mOutdexPath;
    private String mLibPath;
    static {
        if (!Debug.isDebuggerConnected()) {
            //没有Debug调试状态
            System.loadLibrary("protect1");
        }
        //Debug调试状态
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            //创建两个私有的、可写的文件夹
            File odex = this.getDir("source_odex", MODE_PRIVATE);
            File libs = this.getDir("source_lib", MODE_PRIVATE);
            mOutdexPath = odex.getAbsolutePath();
            mLibPath = libs.getAbsolutePath();
            mApkPath = mOutdexPath + "/source.apk";

            File apkFile = new File(mApkPath);
            Log.d("AProtect","apk path :" + mApkPath);
            Log.d("AProtect", "apk size :" + apkFile.length());
            if (!apkFile.exists()) {
                Log.d("AProtect","create apk");
                apkFile.createNewFile(); //创建源Apk
                byte[] dexData = this.getDexFromApk(); //获得脱壳Apk的dex
                this.splitApkFromDex(dexData); //从dex中获得加密后的Apk文件,并写入apkFile中
            }
            Log.d("AProtect","apk size after write:" + apkFile.length());

            //获取主线程对象
            Object currentActivityThread = RefInvoke.invokeStaticMethod(
                    "android.app.ActivityThread", "currentActivityThread",
                    new Class[]{} , new Object[]{}
            );

            //当前apk的包名
            String packageName = this.getPackageName();

            Log.i("AProtect","packageName:" + packageName);
            ArrayMap mPackages = (ArrayMap) RefInvoke.getFieldObject(
                    "android.app.ActivityThread", currentActivityThread,
                    "mPackages"
            );

            WeakReference wr = (WeakReference) mPackages.get(packageName);

            //创建被加壳apk的DexClassLoader对象，加载apk内的类和Native代码
            DexClassLoader dLoader = new DexClassLoader(mApkPath, mOutdexPath, mLibPath, (ClassLoader) RefInvoke.getFieldObject(
                    "android.app.LoadedApk", wr.get(), "mClassLoader"
            ));

            //把被加壳的apk的DexClassLoader设置为当前进程的DexClassLoader
            RefInvoke.setFieldObject("android.app.LoadedApk", "mClassLoader", wr.get(), dLoader);

            Log.d("AProtect", "classLoader:" + dLoader);
            Log.d("AProtect", "set classLoader over");

            try {
                //load 被加壳Apk的MainActivity
                Object mainActObj = dLoader.loadClass("com.example.jz.testapk.MainActivity"); //这个地方不能写死
                Log.d("AProtect", "mainActivityObject:" + mainActObj);
            } catch (Exception e) {
                Log.e("AProtect", "activity:" + Log.getStackTraceString(e));
                e.printStackTrace();
            }
        } catch (Exception e) {
            Log.e("AProtect", "error:" + Log.getStackTraceString(e));
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("AProtect", "MyApplication onCreate");

        //如果源apk有application时，运行源apk的application
        String appClassName = null;
        try {
            ApplicationInfo ai = this.getPackageManager().getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            if (bundle != null && bundle.containsKey("APPLICATION_CLASS_NAME")) {
                appClassName = bundle.getString("APPLICATION_CLASS_NAME");
                Log.i("AProtect","Source Apk Name:" + appClassName);
            } else {
                Log.i("AProtect","Source apk do not have stub application");
                return;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Object currentActivityThread = RefInvoke.invokeStaticMethod("android.app.ActivityThread", "currentActivityThread", new Class[]{}, new Object[]{});
        Object mBoundApplication = RefInvoke.getFieldObject("android.app.ActivityThread", currentActivityThread, "mBoundApplication");
        Object loadedApkInfo = RefInvoke.getFieldObject("android.app.ActivityThread$AppBindData", mBoundApplication, "info");
        RefInvoke.setFieldObject("android.app.LoadedApk", "mApplication", loadedApkInfo, null);
        Object oldApplication = RefInvoke.getFieldObject("android.app.ActivityThread", currentActivityThread, "mInitialApplication");
        ArrayList<Application> mAllApplications = (ArrayList<Application>) RefInvoke.getFieldObject("android.app.ActivityThread", currentActivityThread, "mAllApplications");
        mAllApplications.remove(oldApplication);

        ApplicationInfo appinfo_In_LoadedApk = (ApplicationInfo) RefInvoke.getFieldObject("android.app.LoadedApk", loadedApkInfo, "mApplicationInfo");
        ApplicationInfo appinfo_In_AppBindData = (ApplicationInfo) RefInvoke.getFieldObject("android.app.ActivityThread$AppBindData", mBoundApplication, "appInfo");
        appinfo_In_LoadedApk.className = appClassName;
        appinfo_In_AppBindData.className = appClassName;
        Application app = (Application) RefInvoke.invokeMethod("android.app.LoadedApk", "makeApplication", loadedApkInfo, new Class[]{ boolean.class, Instrumentation.class }, new Object[]{ false, null });
        RefInvoke.setFieldObject("android.app.ActivityThread", "mInitialApplication", currentActivityThread, app);
        ArrayMap mProviderMap = (ArrayMap) RefInvoke.getFieldObject("android.app.ActivityThread", currentActivityThread, "mProviderMap");
        Iterator it = mProviderMap.values().iterator();
        while (it.hasNext()) {
            Object providerClientRecord = it.next();
            Object localProvider = RefInvoke.getFieldObject("android.app.ActivityThread$ProviderClientRecord", providerClientRecord, "mLocalProvider");
            RefInvoke.setFieldObject("android.content.ContentProvider", "mContext", localProvider, app);
        }
        app.onCreate();
    }

    private byte[] getDexFromApk() {
        try{
            //获得当前apk的zip流
            ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(this.getApplicationInfo().sourceDir)));

            //定义输出的二进制流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            while(true){
                ZipEntry zipEntry = zipInputStream.getNextEntry();
                if(zipEntry == null){
                    zipInputStream.close();
                    break;
                }
                if((zipEntry.getName().equals("classes.dex"))){
                    //找到dex文件,并将dex输出
                    byte[] arrayOfByte = new byte[1024];
                    while (true) {
                        int i = zipInputStream.read(arrayOfByte);
                        if (i == -1)
                            break;
                        outputStream.write(arrayOfByte, 0, i);
                    }
                    zipInputStream.close();
                    return outputStream.toByteArray();
                }
            }
            zipInputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    private void splitApkFromDex(byte[] dexData){
        //从dex文件中获得被加壳的apk，并将apk中的lib放入source_lib目录下
        int fullLen = dexData.length;

        Log.d("AProtect","dexLen:" + String.valueOf(fullLen));
        //获得apk的大小
        byte[] apkSize = new byte[4];
        System.arraycopy(dexData,fullLen - 4,apkSize,0,4);
        Log.d("AProtect","apkSize bytes:" + apkSize);
        ByteArrayInputStream bais = new ByteArrayInputStream(apkSize);
        DataInputStream in = new DataInputStream(bais);
        try{
            //读出apk并写入mApkPath路径的apk文件中
            int readInt = in.readInt();
            byte[] apkFile = new byte[readInt];
            System.arraycopy(dexData,fullLen - 4 - readInt,apkFile,0,readInt);
            Log.d("AProtect","apk pos:" + Integer.toHexString(fullLen - 4 - readInt));
            Log.d("AProtect","Apk Size:" + Integer.toHexString(readInt));
            for(int i = 0;i < readInt;i++){
                apkFile[i] = (byte)(0xff ^ apkFile[i]);
            }
            File apk = new File(mApkPath);
            FileOutputStream outputStream = new FileOutputStream(apk);
            outputStream.write(apkFile);
            outputStream.close();
            Log.d("AProtect","Finish write to apk");

            //分析被加壳的apk文件
            ZipInputStream inputStream = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(apk)));
            while (true) {
                ZipEntry zipEntry = inputStream.getNextEntry();
                if (zipEntry == null) {
                    inputStream.close();
                    break;
                }
                //取出被加壳apk用到的so文件，放到 libPath中（data/data/包名/payload_lib)
                String name = zipEntry.getName();
                Log.d("AProtect","apk file name:" + zipEntry.getName());
                if (name.startsWith("lib/") && name.endsWith(".so")) {
                    //找到so文件，解密so
                    File storeFile = new File(mLibPath + "/"
                            + name.substring(name.lastIndexOf('/')));

                    Log.i("AProtect","So File Path:" + storeFile.getAbsolutePath());
                    storeFile.createNewFile();
                    FileOutputStream fos = new FileOutputStream(storeFile);
                    byte[] arrayOfByte = new byte[1024];
                    while (true) {
                        int i = inputStream.read(arrayOfByte);
                        if (i == -1)
                            break;
                        fos.write(arrayOfByte, 0, i);
                    }
                    fos.flush();
                    fos.close();
                }
                inputStream.closeEntry();
            }
            inputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] decryptDex(byte[] dexData){
        for(int i = 0;i < dexData.length;i++){
            dexData[i] = (byte)(0xff ^ dexData[i]);
        }
        return dexData;
    }
    //以下是加载资源
    protected AssetManager mAssetManager;//资源管理器
    protected Resources mResources;//资源
    protected Resources.Theme mTheme;//主题

    protected void loadResources(String dexPath) {
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
            addAssetPath.invoke(assetManager, dexPath);
            mAssetManager = assetManager;
        } catch (Exception e) {
            Log.i("inject", "loadResource error:"+Log.getStackTraceString(e));
            e.printStackTrace();
        }
        Resources superRes = super.getResources();
        superRes.getDisplayMetrics();
        superRes.getConfiguration();
        mResources = new Resources(mAssetManager, superRes.getDisplayMetrics(),superRes.getConfiguration());
        mTheme = mResources.newTheme();
        mTheme.setTo(super.getTheme());
    }

    @Override
    public AssetManager getAssets() {
        return mAssetManager == null ? super.getAssets() : mAssetManager;
    }

    @Override
    public Resources getResources() {
        return mResources == null ? super.getResources() : mResources;
    }

    @Override
    public Resources.Theme getTheme() {
        return mTheme == null ? super.getTheme() : mTheme;
    }

}
