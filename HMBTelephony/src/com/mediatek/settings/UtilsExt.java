/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 */

package com.mediatek.settings;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.storage.IMountService;
import android.os.RemoteException;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;


import java.net.Inet4Address;
import java.net.Inet6Address;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;


import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import com.android.internal.widget.LockPatternUtils;
import com.mediatek.common.MPlugin;
import com.mediatek.storage.StorageManagerEx;
import com.mediatek.settings.ext.*;
import com.android.phone.R;


public class UtilsExt {
    private static final String TAG = "UtilsExt";

    ///M: DHCPV6 change feature
    private static final String INTERFACE_NAME = "wlan0";
    private static final int BEGIN_INDEX = 0;
    private static final int SEPARATOR_LENGTH = 2;
    // for HetComm feature
    public static final String PKG_NAME_HETCOMM = "com.mediatek.hetcomm";

    // disable apps list file location
    private static final String FILE_DISABLE_APPS_LIST = "/system/etc/disableapplist.txt";
    // read the file to get the need special disable app list
    public static ArrayList<String> disableAppList = readFile(FILE_DISABLE_APPS_LIST);


    /* M: create settigns plugin object
     * @param context Context
     * @return ISettingsMiscExt
     */
    public static ISettingsMiscExt getMiscPlugin(Context context) {
        ISettingsMiscExt ext;
        ext = (ISettingsMiscExt) MPlugin.createInstance(
                     ISettingsMiscExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultSettingsMiscExt(context);
        }
        return ext;
    }

    public static IRCSSettings getRcsSettingsPlugin(Context context) {
        IRCSSettings ext = null;
        ext = (IRCSSettings) MPlugin.createInstance(
                     IRCSSettings.class.getName(), context);
        if (ext == null) {
            ext = new DefaultRCSSettings();
        }
        return ext;
    }

    /**
     * For sim management update preference
     * @param context Context
     * @return ISimManagementExt
     */
    public static ISimManagementExt getSimManagmentExtPlugin(Context context) {
        ISimManagementExt ext;
        ext = (ISimManagementExt) MPlugin.createInstance(
                     ISimManagementExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultSimManagementExt();
        }
        return ext;
    }

    /**
     * do not show SIM Activity Dialog for auto sanity.
     * 1.FeatureOption.MTK_AUTOSANITY is true
     * 2.FeatureOption.MTK_BUILD_TYPE is ENG
     * @return true disable SIM Dialog
     */
    public static boolean shouldDisableForAutoSanity() {
        boolean autoSanity = SystemProperties.get("ro.mtk.autosanity").equals("1");
        String buildType = SystemProperties.get("ro.build.type", "");
        Log.d(TAG, "autoSanity: " + autoSanity + " buildType: " + buildType);
        if (autoSanity && (!TextUtils.isEmpty(buildType)) && buildType.endsWith("eng")) {
            Log.d(TAG, "ShouldDisableForAutoSanity()...");
            return true;
        }
        return false;
    }
   
    /**
     * read the file by line
     * @param path path
     * @return ArrayList
     */
    public static ArrayList<String> readFile(String path) {
         ArrayList<String> appsList = new ArrayList<String>();
         appsList.clear();
         File file = new File(path);
          FileReader fr = null;
          BufferedReader br = null;
         try {
               if (file.exists()) {
                   fr = new FileReader(file);
              } else {
                  Log.d(TAG, "file in " + path + " does not exist!");
                  return null;
             }
               br = new BufferedReader(fr);
               String line;
               while ((line = br.readLine()) != null) {
                     Log.d(TAG, " read line " + line);
                     appsList.add(line);
               }
               return appsList;
         } catch (IOException io) {
                Log.d(TAG, "IOException");
                 io.printStackTrace();
         } finally {
                   try {
                      if (br != null) {
                          br.close();
                         }
                      if (fr != null) {
                         fr.close();
                         }
                      } catch (IOException io) {
                         io.printStackTrace();
                      }
         }
         return null;
     }
}
