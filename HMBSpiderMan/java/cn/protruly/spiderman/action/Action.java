package cn.protruly.spiderman.action;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import cn.protruly.spiderman.collectservice.SystemPropertiesProxy;

import static android.content.Context.TELEPHONY_SERVICE;

/**
 * Created by lijia on 17-5-13.
 */

public abstract class Action {

    private final String TAG = "SpiderMan";
    private Context mContext;
    String targetPath; // log存储路径
    private String dn;
    protected String mSpiderManLogStoragePath;
    private long time;

    String ANRFilePath = "/data/anr/traces.txt";
    String LogdFilePath = "/data/misc/logd/";

    public Action(Context context) {
        this.mContext = context;
        getSpiderManLogStoragePath();
        getSystemTime();
    }

    public abstract void collectLogFile(String tag, String pg, String es, String filePath);

    protected void getSpiderManLogStoragePath() {
        mSpiderManLogStoragePath = "/data/data/" + mContext.getPackageName() + "/spiderman/";
    }

    int copyTraceAndTombstoneFile(String sourceFilePath, String filePath, String destFilePath) {

        targetPath = destFilePath + time + "-" + getIMEINumber();
        File targetDir = new File(targetPath);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        File anrDirFile = new File(sourceFilePath);
        if (filePath != null) {
            do_exec("chmod 777 /data/tombstones/*");
            do_exec("cp -rf /data/tombstones/ " + targetPath);
        }

        if (!anrDirFile.exists()) {
            return -1;
        }

        copyFile(anrDirFile.getPath(), targetPath + "/" + anrDirFile.getName());

        anrDirFile.delete();
        return 0;
    }

    String do_exec(String cmd) {
        /*String s = "/n";*/
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            /*BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                s += line + "/n";
            }*/
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        /*Log.v(TAG, s);*/
        return cmd;
    }

    int copyLogdFile(String logdFilePath) {

        File logdDirFile = new File(logdFilePath);
        if (!logdDirFile.exists()) {
            return -1;
        }

        File targetDir = new File(targetPath + "/logcat/");
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        File[] fl = logdDirFile.listFiles();
        for (int i = 0; i < fl.length; i++) {
            copyFile(fl[i].getPath(), targetDir.getAbsolutePath() + "/" + fl[i].getName());
        }
        return 0;
    }

    public int copyFile(String fromFile, String toFile) {

        try {
            InputStream sourceFile = new FileInputStream(fromFile);
            OutputStream destFile = new FileOutputStream(toFile);
            byte bt[] = new byte[1024];
            int c;
            while ((c = sourceFile.read(bt)) > 0) {
                destFile.write(bt, 0, c);
            }
            sourceFile.close();
            destFile.close();

            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

    }

    private String getIMEINumber() {
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(TELEPHONY_SERVICE);
        dn = tm.getDeviceId();
        return dn;
    }

    private void getSystemTime() {
        time = System.currentTimeMillis();
    }

    protected void collectErrorInfo(String infoFilePath, String tag, String pg, String es) {

        String md = SystemPropertiesProxy.get(mContext, "ro.product.model");
        String rv = SystemPropertiesProxy.get(mContext, "ro.sw.version");
        String pf = SystemPropertiesProxy.get(mContext, "ro.board.platform");

        PackageManager pm = mContext.getPackageManager();
        String clientVersion = null;
        String versionName = null;
        int versionCode = 0;

        try {
            PackageInfo clientInfo = pm.getPackageInfo(mContext.getPackageName(), 0);
            clientVersion = clientInfo.versionName;
            PackageInfo packageInfo = pm.getPackageInfo(pg, 0);
            versionName = packageInfo.versionName;

            packageInfo = pm.getPackageInfo(pg, 0);
            versionCode = packageInfo.versionCode;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            if (tag.equalsIgnoreCase("TOMBSTONE")) {
                pg = "tombstone";
            }
        }

        JSONObject clientKey = new JSONObject();

        try {
            clientKey.put("dn", dn);
            clientKey.put("md", md);
            clientKey.put("rv", rv);
            clientKey.put("dt", "" + time);
            clientKey.put("pg", pg);
            clientKey.put("pvc", versionCode);
            clientKey.put("pvn", versionName);

            clientKey.put("et", tag);
            clientKey.put("cv", clientVersion);
            clientKey.put("pf", pf);
            clientKey.put("es", es);

            String content = String.valueOf(clientKey);

            File infoFile = new File(infoFilePath + "/info.txt");
            if (!infoFile.exists()) {
                File dir = new File(infoFile.getParent());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                infoFile.createNewFile();
            }

            FileOutputStream out = new FileOutputStream(infoFile);
            out.write(content.getBytes());
            out.close();

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    protected boolean zipFileDirectory(File sourceDir) {
        if (!sourceDir.exists()) {
            return false;
        }
        ZipOutputStream out = null;
        BufferedOutputStream bos = null;
        try {
            out = new ZipOutputStream(new FileOutputStream(new File(sourceDir.getAbsolutePath() + ".zip")));
            bos = new BufferedOutputStream(out);
            compressFile(sourceDir, sourceDir.getName(), bos, out);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace(); // 压缩失败
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
                if (out != null) {
                    out.close();
                }
                deleteAllFiles(sourceDir);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return false;
    }

    private void compressFile(File sourceFile, String baseDir, BufferedOutputStream bos,
                              ZipOutputStream out) throws Exception {
        if (sourceFile.isDirectory()) {
            File[] fl = sourceFile.listFiles();
            if (fl.length == 0) {
                out.putNextEntry(new ZipEntry(baseDir + "/")); // 创建zip压缩进入点base
            }
            for (int i = 0; i < fl.length; i++) {
                if (fl[i].getName().equals("info.txt")) {
                    compressFile(fl[i], fl[i].getName(), bos, out); // info.txt压缩至根目录
                } else {
                    compressFile(fl[i], baseDir + "/" + fl[i].getName(), bos, out); // 递归遍历子文件夹

                }
            }
        } else {
            out.putNextEntry(new ZipEntry(baseDir)); // 创建zip压缩进入点base
            FileInputStream in = new FileInputStream(sourceFile);
            BufferedInputStream bi = new BufferedInputStream(in);
            int b;
            while ((b = bi.read()) != -1) {
                bos.write(b); // 将字节流写入当前zip目录
            }
            bos.flush();
            bi.close();
            in.close(); // 输入流关闭
        }
    }

    protected void deleteAllFiles(File root) {
        File files[] = root.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) { // 判断是否为文件夹
                    deleteAllFiles(f);
                    try {
                        f.delete();
                    } catch (Exception e) {
                    }
                } else {
                    if (f.exists()) { // 判断是否存在
                        deleteAllFiles(f);
                        try {
                            f.delete();
                        } catch (Exception e) {
                        }
                    }
                }
            }
            root.delete();
        }
    }

}
