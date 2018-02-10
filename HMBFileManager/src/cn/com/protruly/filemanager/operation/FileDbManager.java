package cn.com.protruly.filemanager.operation;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteFullException;
import android.mtp.MtpConstants;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.utils.FileEnumerator;
import cn.com.protruly.filemanager.utils.FilePathUtil;
import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.MediaFileType;
import cn.com.protruly.filemanager.utils.MediaFileUtil;
import cn.com.protruly.filemanager.utils.Util;
import cn.com.protruly.filemanager.R;

/**
 * Created by sqf on 17-5-25.
 */

public class FileDbManager {

    private Context mContext;
    private ContentResolver mContentResolver;

    private static final String TAG = "FileDbManager";

    private static final int SCAN_THRESHOLD = 500;

    // values below are determined by Media Database.
    // folders or directory
    private static final int FILE_FORMAT_DIRECTORY = MtpConstants.FORMAT_ASSOCIATION;

    private HashMap<Uri, ArrayList<String>> mDeleteOperations = new HashMap<>();
    private HashMap<Uri, ArrayList<ContentValues>> mInsertOperations = new HashMap<>();

    //private ArrayList<ContentProviderOperation> mDeleteOperations = new ArrayList<>();
    //private ArrayList<ContentProviderOperation> mInsertOperations = new ArrayList<>();

    public FileDbManager(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
    }

    private static final int OPERATION_THRESHOLD = 30;

    /**
     * batch delete files whose path starts with prefix, used only in "rename"
     * @param context
     * @param pathPrefix
     */
    public void batchDeleteFileStartWithPathPrefix(Context context, String pathPrefix) {
        try {
            String where = MediaStore.Files.FileColumns.DATA + " LIKE ?";
            String[] selectionArgs = {
                    pathPrefix + "%",
            };
            int rowDeleted = mContentResolver.delete(GlobalConstants.FILES_URI, where, selectionArgs);
        /*
        if(rowDeleted > 0) {
            LogUtil.i(TAG, "Delete Success: rowDeleted:" + rowDeleted + " delete path with path prefix: " + pathPrefix);
        }
        */
            mContentResolver.notifyChange(GlobalConstants.FILES_URI, null);
        } catch (SQLiteFullException e) {
            Util.showToast(context, R.string.failed_due_to_sqlite_full);
        } catch (Exception e) {
            if(e != null) LogUtil.e(TAG, "batchDeleteFileStartWithPathPrefix --> " + e.getMessage());
        }
    }

    /**
     * If fileInfo is a directory, scanFileInfo will scan files under it.
     * @param fileInfo
     */
    public void scanFileInfo(FileInfo fileInfo) {
        if (fileInfo == null || fileInfo.getFile() == null) return;
        final ArrayList<String> paths = new ArrayList<String>();
        int totalNum = FilePathUtil.getAllFileAndDirectoryNum(fileInfo.getFile());
        //final FileDbManager fileDbManager = new FileDbManager(mContext);
        FileEnumerator enumerator = new FileEnumerator(totalNum) {
            @Override
            public void onFileEnumerated(File file) {
                //LogUtil.i("Scan", "onFileEnumerated file:" + file.getPath());
                super.onFileEnumerated(file);
                paths.add(file.getPath());
                //LogUtil.i("Scan", "String path added:" + file.getPath());
                boolean reachThreshold = paths.size() >= SCAN_THRESHOLD;
                boolean finished = isEnumeationFinished();
                //fileDbManager.addToInsertList(file.getPath(), file.isDirectory());
                addToInsertList(file.getPath(), file.isDirectory());
                if(reachThreshold || finished) {
                    //fileDbManager.insertFromContentValueList();
                    insertFromContentValueList();
                }
            }
        };
        FilePathUtil.enumerateFile(fileInfo.getFile(), enumerator);
    }

    ////// Delete //////

    private void putToDeleteMap(Uri uri, String filePath) {
        ArrayList<String> filePathList = mDeleteOperations.get(uri);
        if(filePathList == null) {
            filePathList = new ArrayList<>();
            mDeleteOperations.put(uri, filePathList);
        }
        filePathList.add(filePath);
    }

    public void addToDeleteList(String filePath) {
        try {
            //boolean exists = new File(filePath).exists();
            ContentProviderOperation operation = null;
            MediaFileType mediaFileType = MediaFileUtil.getFileType(filePath);

            String [] selectionArgs = new String[] {filePath};
            if (mediaFileType == null) {
                //LogUtil.i(TAG, "addToDeleteList 000: mimeType is null");
                /*
                operation = ContentProviderOperation.newDelete(GlobalConstants.FILES_URI)
                        .withSelection(MediaStore.Files.FileColumns.DATA + " = ? ",  selectionArgs)
                        .withYieldAllowed(true)
                        .build(); */
                //putToDeleteMap(GlobalConstants.FILES_URI, operation);
                putToDeleteMap(GlobalConstants.FILES_URI, filePath);
                deleteIfThresholdReached();
                return;
            }
            int fileType = mediaFileType.mFileType;
            if (MediaFileUtil.isImageFileType(fileType)) {
                //LogUtil.i(TAG, "addToDeleteList 111: image");
                /*
                operation = ContentProviderOperation.newDelete(GlobalConstants.PICTURE_URI)
                        .withSelection(MediaStore.Images.ImageColumns.DATA + " = ? ", selectionArgs )
                        .withYieldAllowed(true)
                        .build();*/
                //putToDeleteMap(GlobalConstants.PICTURE_URI, operation);
                putToDeleteMap(GlobalConstants.PICTURE_URI, filePath);
            } else if (MediaFileUtil.isAudioFileType(fileType)) {
                //LogUtil.i(TAG, "addToDeleteList 222: music");
                /*
                operation = ContentProviderOperation.newDelete(GlobalConstants.MUSIC_URI)
                        .withSelection(MediaStore.Audio.AudioColumns.DATA + " = ? ", selectionArgs )
                        .withYieldAllowed(true)
                        .build();*/
                putToDeleteMap(GlobalConstants.MUSIC_URI, filePath);
            } else if (MediaFileUtil.isVideoFileType(fileType)) {
                //LogUtil.i(TAG, "addToDeleteList 333: video");
                /*
                operation = ContentProviderOperation.newDelete(GlobalConstants.VIDEO_URI)
                        .withSelection(MediaStore.Video.VideoColumns.DATA + " = ? ", selectionArgs )
                        .withYieldAllowed(true)
                        .build();*/
                putToDeleteMap(GlobalConstants.VIDEO_URI, filePath);

                //holy shit...3gpp may be a music file...
                if("3gpp".equals(FilePathUtil.getFileExtension(filePath))) {
                    putToDeleteMap(GlobalConstants.MUSIC_URI, filePath);
                }
            } else {
                //LogUtil.i(TAG, "addToDeleteList 000: mimeType is null");
                /*
                operation = ContentProviderOperation.newDelete(GlobalConstants.FILES_URI)
                        .withSelection(MediaStore.Files.FileColumns.DATA + " = ? ", selectionArgs )
                        .withYieldAllowed(true)
                        .build();*/
                putToDeleteMap(GlobalConstants.FILES_URI, filePath);
            }
            deleteIfThresholdReached();
        } catch (Exception e) {
            if(e != null) LogUtil.e(TAG, "addToDeleteList:" + e.getMessage());
        }
    }

    /**
     * if operation count more than OPERATION_THRESHOLD, delete here
     */
    public void deleteIfThresholdReached() {
        Set<Uri> uris = mDeleteOperations.keySet();
        Iterator<Uri> it = uris.iterator();
        int totalOperationCount = 0;
        while(it.hasNext()) {
            Uri uri = it.next();
            ArrayList<String> operationList = mDeleteOperations.get(uri);
            if(operationList != null && ! operationList.isEmpty()) {
                totalOperationCount += operationList.size();
            }
        }
        if(totalOperationCount >= OPERATION_THRESHOLD) {
            deleteFromDeleteList();
        }
    }

    public void deleteFromDeleteList() {
        try {
            Set<Uri> uris = mDeleteOperations.keySet();
            Iterator<Uri> it = uris.iterator();
            while(it.hasNext()) {
                Uri uri = it.next();
                //LogUtil.i(TAG, "deleteFromDeleteList-->uri:" + uri);
                ArrayList<String> filePathList = mDeleteOperations.get(uri);
                if(filePathList != null && !filePathList.isEmpty()) {
                    StringBuilder whereStringBuilder = new StringBuilder();
                    for(int i = 0; i < filePathList.size(); i++) {
                        if(i>0) {
                            whereStringBuilder.append(" OR ");
                        }
                        whereStringBuilder.append(MediaStore.Files.FileColumns.DATA + " = ? ");
                    }
                    String where = whereStringBuilder.toString();
                    String [] whereArgs = new String[filePathList.size()];
                    filePathList.toArray(whereArgs);
                    //LogUtil.i(TAG, "deleteFromDeleteList where: " + where);
                    /*
                    for(int i = 0; i < whereArgs.length; i++) {
                        LogUtil.i(TAG, "deleteFromDeleteList whereArgs:" + whereArgs[i]);
                    }
                    */
                    mContentResolver.delete(uri, whereStringBuilder.toString(), whereArgs);
                    filePathList.clear();
                }
            }
        } catch(Exception e) {
            LogUtil.e(TAG, "deleteFromDeleteList:" + e.getMessage());
        }

        /*catch (RemoteException e) {
            e.printStackTrace();
            LogUtil.e(TAG, "" + e.getMessage());
        } catch (OperationApplicationException e) {
            e.printStackTrace();
            LogUtil.e(TAG, "" + e.getMessage());
        }*/

    }


    /*
    //if whether a file is file or directory is uncertain, we use this to delete file
    public void delete(Context context, String filePath) {
        try {
            MediaFileType mediaFileType = MediaFileUtil.getFileType(filePath);
            if(mediaFileType == null) {
                deleteFromFileDb(mContext, filePath);
                return;
            }
            int fileType = mediaFileType.mFileType;
            if(MediaFileUtil.isImageFileType(fileType)) {
                deleteImageFromDb(context, filePath);
            } else if(MediaFileUtil.isAudioFileType(fileType)) {
                deleteAudioFromDb(context, filePath);
            } else if(MediaFileUtil.isVideoFileType(fileType)) {
                deleteVideoFromDb(context, filePath);
            } else {
                deleteFromFileDb(context, filePath);
            }
        } catch (SQLiteFullException e) {
            Util.showToast(context, R.string.failed_due_to_sqlite_full);
        } catch (Exception e) {
            if(e != null) LogUtil.e(TAG, "delete 111 --> " + e.getMessage());
        }
    }

    public void delete(Context context, String filePath, boolean isDirectory) {
        try {
            if(isDirectory) {
                deleteFromFileDb(context, filePath);
                return;
            }
            MediaFileType mediaFileType = MediaFileUtil.getFileType(filePath);
            if(mediaFileType == null) {
                deleteFromFileDb(mContext, filePath);
                return;
            }
            int fileType = mediaFileType.mFileType;
            if(MediaFileUtil.isImageFileType(fileType)) {
                deleteImageFromDb(context, filePath);
            } else if(MediaFileUtil.isAudioFileType(fileType)) {
                deleteAudioFromDb(context, filePath);
            } else if(MediaFileUtil.isVideoFileType(fileType)) {
                deleteVideoFromDb(context, filePath);
            } else {
                deleteFromFileDb(context, filePath);
            }
        } catch (SQLiteFullException e) {
            Util.showToast(context, R.string.failed_due_to_sqlite_full);
        } catch (Exception e) {
            if(e != null) LogUtil.e(TAG, "delete 222 --> " + e.getMessage());
        }
    }
    */

    /*
    private void deleteFromFileDb(Context context, String filePath) {

        String where = MediaStore.Files.FileColumns.DATA + " = ? ";
        String[] selectionArgs = {
                filePath
        };
        int rowDeleted = mContentResolver.delete(GlobalConstants.FILES_URI, where, selectionArgs);
            if(rowDeleted > 0) {
                LogUtil.i(TAG, "Delete Success: rowDeleddted:" + rowDeleted + " delete: " + filePath);
            }
        mContentResolver.notifyChange(GlobalConstants.FILES_URI, null);
    }

    private void deleteImageFromDb(Context context, String filePath) {
        String where = MediaStore.Images.ImageColumns.DATA + " = ? ";
        String[] selectionArgs = {
                filePath
        };
        int rowDeleted = mContentResolver.delete(GlobalConstants.PICTURE_URI, where, selectionArgs);
        mContentResolver.notifyChange(GlobalConstants.PICTURE_URI, null);
    }

    private void deleteAudioFromDb(Context context, String filePath) {
        String where = MediaStore.Audio.AudioColumns.DATA + " = ? ";
        String[] selectionArgs = {
                filePath
        };
        int rowDeleted = mContentResolver.delete(GlobalConstants.MUSIC_URI, where, selectionArgs);
        mContentResolver.notifyChange(GlobalConstants.MUSIC_URI, null);
    }

    private void deleteVideoFromDb(Context context, String filePath) {
        String where = MediaStore.Video.VideoColumns.DATA + " = ? ";
        String[] selectionArgs = {
                filePath
        };
        int rowDeleted = mContentResolver.delete(GlobalConstants.VIDEO_URI, where, selectionArgs);
        mContentResolver.notifyChange(GlobalConstants.VIDEO_URI, null);
    }
    */



    ////// Insert //////

    /*
    public void addToInsertList() {
        ContentProviderOperation operation = null;
        MediaFileType mediaFileType = MediaFileUtil.getFileType(filePath);
        if (mediaFileType == null) {

        }
    }
    */

    private void insertIfThresholdReached() {

        Set<Uri> uris = mInsertOperations.keySet();
        Iterator<Uri> it = uris.iterator();
        int totalOperationCount = 0;
        while(it.hasNext()) {
            Uri uri = it.next();
            ArrayList<ContentValues> contentValueList = mInsertOperations.get(uri);
            if(contentValueList != null && ! contentValueList.isEmpty()) {
                totalOperationCount += contentValueList.size();
            }
        }
        if(totalOperationCount >= OPERATION_THRESHOLD) {
            insertFromContentValueList();
        }
    }

    public void insertFromContentValueList() {
        try {
            //LogUtil.i2(TAG, "insertFromContentValueList---> ");
            Set<Uri> uris = mInsertOperations.keySet();
            Iterator<Uri> it = uris.iterator();
            while(it.hasNext()) {
                Uri uri = it.next();
                ArrayList<ContentValues> contentValuesList = mInsertOperations.get(uri);
                if(contentValuesList != null) {
                    //mContentResolver.applyBatch(uri.getAuthority(), contentValuesList);
                    //LogUtil.i(TAG, "bulkInsert---> uri:" + uri + " ");

                    // DEBUG BEGIN
                    /*
                    for(int i=0; i<contentValuesList.size(); i++) {
                        LogUtil.i(TAG, "bulkInsert content values: " + contentValuesList.get(i));
                    }
                    */
                    // DEBUG END

                    ContentValues [] cvs = new ContentValues[contentValuesList.size()];
                    mContentResolver.bulkInsert(uri, (ContentValues[]) contentValuesList.toArray(cvs));
                    mContentResolver.notifyChange(uri, null);
                    contentValuesList.clear();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.e(TAG, "insertFromContentValueList:" + e.getMessage());
        }
    }

    public void addToInsertList(String newFilePath, boolean isDirectory) {
        try {
            //LogUtil.i(TAG, "addToInsertList 111 isDirectory:" + isDirectory + " newFilePath:"+ newFilePath);
            MediaFileType mediaFileType = MediaFileUtil.getFileType(newFilePath);
            if (isDirectory || mediaFileType == null) {
                addInsertContentValuesForFileDb(newFilePath, isDirectory);
                return;
            }
            int fileType = mediaFileType.mFileType;
            if (MediaFileUtil.isImageFileType(fileType)) {
                addInsertContentValuesForImageDb(newFilePath);
            } else if (MediaFileUtil.isAudioFileType(fileType)) {
                addInsertContentValuesForAudioDb(newFilePath);
            } else if (MediaFileUtil.isVideoFileType(fileType)) {
                addInsertContentValuesForVideoDb(newFilePath);
            } else {
                //LogUtil.i(TAG, "inset insertIntoFileDb");
                addInsertContentValuesForFileDb(newFilePath, isDirectory);
            }
            insertIfThresholdReached();
        } catch (Exception e) {
            LogUtil.e(TAG, "addIntoInsertList:" + e.getMessage());
        }
    }

    /*
    public void insert(String newFilePath, boolean isDirectory) {
        try {
            MediaFileType mediaFileType = MediaFileUtil.getFileType(newFilePath);
            if (isDirectory || mediaFileType == null) {
                insertIntoFileDb(newFilePath, new File(newFilePath).isDirectory());
                return;
            }
            int fileType = mediaFileType.mFileType;
            if (MediaFileUtil.isImageFileType(fileType)) {
                insertImageIntoDb(newFilePath);
            } else if (MediaFileUtil.isAudioFileType(fileType)) {
                insertAudioIntoDb(newFilePath);
            } else if (MediaFileUtil.isVideoFileType(fileType)) {
                insertVideoIntoDb(newFilePath);
            } else {
                //LogUtil.i(TAG, "inset insertIntoFileDb");
                insertIntoFileDb(newFilePath, isDirectory);
            }
        } catch (SQLiteFullException e) {
            Util.showToast(mContext, R.string.failed_due_to_sqlite_full);
        } catch (Exception e) {
            if(e != null) LogUtil.i(TAG, "insert 111" + e.getMessage());
        }
    }
    */


    public void addToInsertList(String originalFilePath, String newFilePath, boolean isDirectory) {
        try {
            //LogUtil.i(TAG, "addToInsertList 222 isDirectory:" + isDirectory + " originalFilePath:" + originalFilePath + " newFilePath:"+ newFilePath);
            MediaFileType mediaFileType = MediaFileUtil.getFileType(originalFilePath);
            if (isDirectory || mediaFileType == null) {
                //insertIntoFileDb(newFilePath, new File(newFilePath).isDirectory());
                addInsertContentValuesForFileDb(newFilePath, isDirectory);
                return;
            }
            int fileType = mediaFileType.mFileType;
            if (MediaFileUtil.isImageFileType(fileType)) {
                addInsertContentValuesForImageDb(originalFilePath, newFilePath);
            } else if (MediaFileUtil.isAudioFileType(fileType)) {
                addInsertContentValuesForAudioDb(originalFilePath, newFilePath);
            } else if (MediaFileUtil.isVideoFileType(fileType)) {
                addInsertContentValuesForVideoDb(originalFilePath, newFilePath);
            } else {
                //insertIntoFileDb(newFilePath, new File(newFilePath).isDirectory());
                addInsertContentValuesForFileDb(newFilePath, isDirectory);
            }
            insertIfThresholdReached();
        } catch (SQLiteFullException e) {
            Util.showToast(mContext, R.string.failed_due_to_sqlite_full);
        } catch (Exception e) {
            if(e != null) LogUtil.i(TAG, "insert 222" + e.getMessage());
        }
    }


    /*
    public void insert(String originalFilePath, String newFilePath, boolean isDirectory) {
        try {
            MediaFileType mediaFileType = MediaFileUtil.getFileType(originalFilePath);
            if (isDirectory || mediaFileType == null) {
                insertIntoFileDb(newFilePath, new File(newFilePath).isDirectory());
                return;
            }
            int fileType = mediaFileType.mFileType;
            if (MediaFileUtil.isImageFileType(fileType)) {
                insertImageIntoDb(originalFilePath, newFilePath);
            } else if (MediaFileUtil.isAudioFileType(fileType)) {
                insertAudioIntoDb(originalFilePath, newFilePath);
            } else if (MediaFileUtil.isVideoFileType(fileType)) {
                insertVideoIntoDb(originalFilePath, newFilePath);
            } else {
                insertIntoFileDb(newFilePath, new File(newFilePath).isDirectory());
            }
        } catch (SQLiteFullException e) {
            Util.showToast(mContext, R.string.failed_due_to_sqlite_full);
        } catch (Exception e) {
            if(e != null) LogUtil.i(TAG, "insert 222" + e.getMessage());
        }
    }
    */

    /*
    private void insertIntoFileDb(String filePath, boolean isDirectory) {
        ContentValues cv = new ContentValues();
        String mimeType = MediaFileUtil.getMimeTypeForFile(filePath);
        if(TextUtils.isEmpty(mimeType)) {
            mimeType = "";
        }
        cv.put(MediaStore.Files.FileColumns.DATA, filePath);
        cv.put(MediaStore.Files.FileColumns.MIME_TYPE, mimeType);
        if(isDirectory) {
            cv.put(MediaStore.Files.FileColumns.FORMAT, FILE_FORMAT_DIRECTORY);
        }
        mContentResolver.insert(GlobalConstants.FILES_URI, cv);
        //LogUtil.i(TAG, "insertIntoFileDb....filePath:" + filePath + " mimeType:" + mimeType);
    }
    */

    private void addContentValuesToInsertList(Uri uri, ContentValues cv) {
        ArrayList<ContentValues> valuesList = mInsertOperations.get(uri);
        if(valuesList == null) {
            valuesList = new ArrayList<>();
            mInsertOperations.put(uri, valuesList);
        }
        valuesList.add(cv);
    }

    private void addInsertContentValuesForFileDb(String filePath, boolean isDirectory) {
        ContentValues cv = new ContentValues();
        String mimeType = MediaFileUtil.getMimeTypeForFile(filePath);
        if(TextUtils.isEmpty(mimeType)) {
            mimeType = "";
        }
        cv.put(MediaStore.Files.FileColumns.DATA, filePath);
        cv.put(MediaStore.Files.FileColumns.MIME_TYPE, mimeType);
        if(isDirectory) {
            cv.put(MediaStore.Files.FileColumns.FORMAT, FILE_FORMAT_DIRECTORY);
        }
        //LogUtil.i(TAG, "addInsertContentValuesForFileDb 111 :" + filePath + " isDirectory:" + isDirectory + " mimeType:" + mimeType);
        addContentValuesToInsertList(GlobalConstants.FILES_URI, cv);
        //LogUtil.i(TAG, "addContentValuesToInsertList....filePath:" + filePath + " mimeType:" + mimeType);
    }

    /*
    private void insertImageIntoDb(String newFilePath) {
        Uri uri = GlobalConstants.PICTURE_URI;
        long time = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.ImageColumns.DATA, newFilePath);
        cv.put(MediaStore.Images.ImageColumns.MIME_TYPE, MediaFileUtil.getMimeTypeForFile(newFilePath));
        cv.put(MediaStore.Images.ImageColumns.SIZE, new File(newFilePath).length());
        cv.put(MediaStore.Images.ImageColumns.DATE_ADDED, time / 1000);
        cv.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, time / 1000);
        cv.put(MediaStore.Images.ImageColumns.DATE_TAKEN, time);
        // This is a workaround to trigger the MediaProvider to re-generate the thumbnail.
        cv.put(MediaStore.Images.Media.MINI_THUMB_MAGIC, 0);
        mContentResolver.insert(uri, cv);
        mContentResolver.notifyChange(uri, null);
    }
    */

    private void addInsertContentValuesForImageDb(String newFilePath) {
        Uri uri = GlobalConstants.PICTURE_URI;
        long time = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.ImageColumns.DATA, newFilePath);
        cv.put(MediaStore.Images.ImageColumns.MIME_TYPE, MediaFileUtil.getMimeTypeForFile(newFilePath));
        cv.put(MediaStore.Images.ImageColumns.SIZE, new File(newFilePath).length());
        cv.put(MediaStore.Images.ImageColumns.DATE_ADDED, time / 1000);
        cv.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, time / 1000);
        cv.put(MediaStore.Images.ImageColumns.DATE_TAKEN, time);
        // This is a workaround to trigger the MediaProvider to re-generate the thumbnail.
        cv.put(MediaStore.Images.Media.MINI_THUMB_MAGIC, 0);
        //mContentResolver.insert(uri, cv);
        //mContentResolver.notifyChange(uri, null);
        //LogUtil.i(TAG, "addInsertContentValuesForImageDb: 111 " + cv);
        addContentValuesToInsertList(GlobalConstants.PICTURE_URI, cv);
    }

    /*
    private void insertImageIntoDb(String originalFilePath, String newFilePath) {
        Uri uri = GlobalConstants.PICTURE_URI;
        String [] projections = {
                MediaStore.Images.ImageColumns.MIME_TYPE,
                MediaStore.Images.ImageColumns.SIZE,
                MediaStore.Images.ImageColumns.DATE_ADDED,
                MediaStore.Images.ImageColumns.DATE_MODIFIED,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.ORIENTATION,
                MediaStore.Images.ImageColumns.LONGITUDE,
                MediaStore.Images.ImageColumns.LATITUDE,
        };
        String selection = MediaStore.MediaColumns.DATA  + " = ? ";
        String selectionArgs [] = { originalFilePath };

        long time = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(uri, projections, selection, selectionArgs, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                //mime
                int columnIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.MIME_TYPE);
                String mimeType = cursor.getString(columnIndex);
                //size
                columnIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.SIZE);
                long size = cursor.getLong(columnIndex);
                //date added
                columnIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_ADDED);
                long dateAdded = cursor.getLong(columnIndex);
                //date modified
                columnIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_MODIFIED);
                long dateModified = cursor.getLong(columnIndex);
                //date taken
                columnIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN);
                long dateTaken = cursor.getLong(columnIndex);
                //orientation
                columnIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.ORIENTATION);
                int orientation = cursor.getInt(columnIndex);
                //longtitude
                columnIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.LONGITUDE);
                double longitude = cursor.getDouble(columnIndex);
                //latitude
                columnIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.LATITUDE);
                double latitude = cursor.getDouble(columnIndex);
                cursor.close();
                cv.put(MediaStore.Images.ImageColumns.DATA, newFilePath);
                cv.put(MediaStore.Images.ImageColumns.MIME_TYPE, mimeType);
                cv.put(MediaStore.Images.ImageColumns.SIZE, size);
                cv.put(MediaStore.Images.ImageColumns.DATE_ADDED, time / 1000);
                cv.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, dateModified);
                cv.put(MediaStore.Images.ImageColumns.DATE_TAKEN, dateTaken);
                cv.put(MediaStore.Images.ImageColumns.ORIENTATION, orientation);
                cv.put(MediaStore.Images.ImageColumns.LONGITUDE, longitude);
                cv.put(MediaStore.Images.ImageColumns.LATITUDE, latitude);
            } else {
                cv.put(MediaStore.Images.ImageColumns.DATA, newFilePath);
                cv.put(MediaStore.Images.ImageColumns.MIME_TYPE, MediaFileUtil.getMimeTypeForFile(originalFilePath));
                cv.put(MediaStore.Images.ImageColumns.SIZE, new File(originalFilePath).length());
                cv.put(MediaStore.Images.ImageColumns.DATE_ADDED, time / 1000);
                cv.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, time / 1000);
                cv.put(MediaStore.Images.ImageColumns.DATE_TAKEN, time);
            }
            mContentResolver.insert(uri, cv);
            mContentResolver.notifyChange(uri, null);
        } catch (Exception e) {
            if(e != null) {
                LogUtil.e(TAG, e.getMessage());
            }
        } finally {
            closeSilently(cursor);
        }
    }
    */

    private void addInsertContentValuesForImageDb(String originalFilePath, String newFilePath) {
        //LogUtil.i(TAG, "addInsertContentValuesForImageDb originalFilePath:" + originalFilePath + " newFilePath:" + newFilePath);
        Uri uri = GlobalConstants.PICTURE_URI;
        String [] projections = {
                MediaStore.Images.ImageColumns.MIME_TYPE,
                MediaStore.Images.ImageColumns.SIZE,
                MediaStore.Images.ImageColumns.DATE_ADDED,
                MediaStore.Images.ImageColumns.DATE_MODIFIED,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.ORIENTATION,
                MediaStore.Images.ImageColumns.LONGITUDE,
                MediaStore.Images.ImageColumns.LATITUDE,
        };
        String selection = MediaStore.MediaColumns.DATA  + " = ? ";
        String selectionArgs [] = { originalFilePath };

        long time = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(uri, projections, selection, selectionArgs, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                //mime
                int columnIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.MIME_TYPE);
                String mimeType = cursor.getString(columnIndex);
                //size
                columnIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.SIZE);
                long size = cursor.getLong(columnIndex);
                //date added
                columnIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_ADDED);
                long dateAdded = cursor.getLong(columnIndex);
                //date modified
                columnIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_MODIFIED);
                long dateModified = cursor.getLong(columnIndex);
                //date taken
                columnIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN);
                long dateTaken = cursor.getLong(columnIndex);
                //orientation
                columnIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.ORIENTATION);
                int orientation = cursor.getInt(columnIndex);
                //longtitude
                columnIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.LONGITUDE);
                double longitude = cursor.getDouble(columnIndex);
                //latitude
                columnIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.LATITUDE);
                double latitude = cursor.getDouble(columnIndex);
                cursor.close();
                cv.put(MediaStore.Images.ImageColumns.DATA, newFilePath);
                cv.put(MediaStore.Images.ImageColumns.MIME_TYPE, mimeType);
                cv.put(MediaStore.Images.ImageColumns.SIZE, size);
                cv.put(MediaStore.Images.ImageColumns.DATE_ADDED, time / 1000);
                cv.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, dateModified);
                cv.put(MediaStore.Images.ImageColumns.DATE_TAKEN, dateTaken);
                cv.put(MediaStore.Images.ImageColumns.ORIENTATION, orientation);
                cv.put(MediaStore.Images.ImageColumns.LONGITUDE, longitude);
                cv.put(MediaStore.Images.ImageColumns.LATITUDE, latitude);
            } else {
                cv.put(MediaStore.Images.ImageColumns.DATA, newFilePath);
                cv.put(MediaStore.Images.ImageColumns.MIME_TYPE, MediaFileUtil.getMimeTypeForFile(originalFilePath));
                cv.put(MediaStore.Images.ImageColumns.SIZE, new File(originalFilePath).length());
                cv.put(MediaStore.Images.ImageColumns.DATE_ADDED, time / 1000);
                cv.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, time / 1000);
                cv.put(MediaStore.Images.ImageColumns.DATE_TAKEN, time);
            }
            //mContentResolver.insert(uri, cv);
            //mContentResolver.notifyChange(uri, null);
            addContentValuesToInsertList(uri, cv);
            //LogUtil.i(TAG, "addInsertContentValuesForImageDb: 222 " + cv);

        } catch (Exception e) {
            if(e != null) {
                LogUtil.e(TAG, "addInsertContentValuesForImageDb 222: " + e.getMessage());
            }
        } finally {
            closeSilently(cursor);
        }
    }

    /*
    private void insertAudioIntoDb(String newFilePath) {
        Uri uri = GlobalConstants.MUSIC_URI;
        long time = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        long size = new File(newFilePath).length();
        cv.put(MediaStore.Audio.AudioColumns.DATA, newFilePath);
        cv.put(MediaStore.Audio.AudioColumns.MIME_TYPE, MediaFileUtil.getMimeTypeForFile(newFilePath));
        cv.put(MediaStore.Audio.AudioColumns.SIZE, size);
        cv.put(MediaStore.Audio.AudioColumns.DATE_ADDED, time / 1000);
        cv.put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, time / 1000);
        mContentResolver.insert(uri, cv);
        mContentResolver.notifyChange(uri, null);
    }
    */

    private void addInsertContentValuesForAudioDb(String newFilePath) {
        Uri uri = GlobalConstants.MUSIC_URI;
        long time = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        long size = new File(newFilePath).length();
        cv.put(MediaStore.Audio.AudioColumns.DATA, newFilePath);
        cv.put(MediaStore.Audio.AudioColumns.MIME_TYPE, MediaFileUtil.getMimeTypeForFile(newFilePath));
        cv.put(MediaStore.Audio.AudioColumns.SIZE, size);
        cv.put(MediaStore.Audio.AudioColumns.DATE_ADDED, time / 1000);
        cv.put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, time / 1000);
        //mContentResolver.insert(uri, cv);
        //mContentResolver.notifyChange(uri, null);
        //LogUtil.i(TAG, "addInsertContentValuesForAudioDb 111 :" + cv);
        addContentValuesToInsertList(GlobalConstants.MUSIC_URI, cv);

    }

    /*
    private void insertAudioIntoDb(String originalFilePath, String newFilePath) {
        Uri uri = GlobalConstants.MUSIC_URI;
        String [] projections = {
                MediaStore.Audio.AudioColumns.MIME_TYPE,
                MediaStore.Audio.AudioColumns.SIZE,
                MediaStore.Audio.AudioColumns.DURATION,
                MediaStore.Audio.AudioColumns.DATE_ADDED,
                MediaStore.Audio.AudioColumns.DATE_MODIFIED,
        };
        String selection = MediaStore.MediaColumns.DATA  + " = ? ";
        String selectionArgs [] = { originalFilePath };

        long time = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        Cursor cursor = null;
        try {
            mContentResolver.query(uri, projections, selection, selectionArgs, null);

            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                //mime
                int columnIndex = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.MIME_TYPE);
                String mimeType = cursor.getString(columnIndex);
                //size
                columnIndex = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.SIZE);
                long size = cursor.getLong(columnIndex);
                //duration
                columnIndex = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION);
                long duration = cursor.getLong(columnIndex);
                //date added
                columnIndex = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATE_ADDED);
                long dateAdded = cursor.getLong(columnIndex);
                //date modified
                columnIndex = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATE_MODIFIED);
                long dateModified = cursor.getLong(columnIndex);

                cursor.close();
                cv.put(MediaStore.Audio.AudioColumns.DATA, newFilePath);
                cv.put(MediaStore.Audio.AudioColumns.MIME_TYPE, mimeType);
                cv.put(MediaStore.Audio.AudioColumns.SIZE, size);
                cv.put(MediaStore.Audio.AudioColumns.DURATION, duration);
                cv.put(MediaStore.Audio.AudioColumns.DATE_ADDED, time / 1000);
                cv.put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, dateModified);

            } else {
                long size = new File(originalFilePath).length();
                cv.put(MediaStore.Audio.AudioColumns.DATA, newFilePath);
                cv.put(MediaStore.Audio.AudioColumns.MIME_TYPE, MediaFileUtil.getMimeTypeForFile(originalFilePath));
                cv.put(MediaStore.Audio.AudioColumns.SIZE, size);
                cv.put(MediaStore.Audio.AudioColumns.DATE_ADDED, time / 1000);
                cv.put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, time / 1000);
            }
            mContentResolver.insert(uri, cv);
            mContentResolver.notifyChange(uri, null);
        } catch (Exception e) {
            if(e != null) {
                LogUtil.e(TAG, e.getMessage());
            }
        } finally {
            closeSilently(cursor);
        }
    }
    */

    private void addInsertContentValuesForAudioDb(String originalFilePath, String newFilePath) {
        Uri uri = GlobalConstants.MUSIC_URI;
        String [] projections = {
                MediaStore.Audio.AudioColumns.MIME_TYPE,
                MediaStore.Audio.AudioColumns.SIZE,
                MediaStore.Audio.AudioColumns.DURATION,
                MediaStore.Audio.AudioColumns.DATE_ADDED,
                MediaStore.Audio.AudioColumns.DATE_MODIFIED,
        };
        String selection = MediaStore.MediaColumns.DATA  + " = ? ";
        String selectionArgs [] = { originalFilePath };

        long time = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        Cursor cursor = null;
        try {
            mContentResolver.query(uri, projections, selection, selectionArgs, null);

            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                //mime
                int columnIndex = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.MIME_TYPE);
                String mimeType = cursor.getString(columnIndex);
                //size
                columnIndex = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.SIZE);
                long size = cursor.getLong(columnIndex);
                //duration
                columnIndex = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION);
                long duration = cursor.getLong(columnIndex);
                //date added
                columnIndex = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATE_ADDED);
                long dateAdded = cursor.getLong(columnIndex);
                //date modified
                columnIndex = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATE_MODIFIED);
                long dateModified = cursor.getLong(columnIndex);

                cursor.close();
                cv.put(MediaStore.Audio.AudioColumns.DATA, newFilePath);
                cv.put(MediaStore.Audio.AudioColumns.MIME_TYPE, mimeType);
                cv.put(MediaStore.Audio.AudioColumns.SIZE, size);
                cv.put(MediaStore.Audio.AudioColumns.DURATION, duration);
                cv.put(MediaStore.Audio.AudioColumns.DATE_ADDED, time / 1000);
                cv.put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, dateModified);
            } else {
                long size = new File(newFilePath).length();
                cv.put(MediaStore.Audio.AudioColumns.DATA, newFilePath);
                cv.put(MediaStore.Audio.AudioColumns.MIME_TYPE, MediaFileUtil.getMimeTypeForFile(originalFilePath));
                cv.put(MediaStore.Audio.AudioColumns.SIZE, size);
                cv.put(MediaStore.Audio.AudioColumns.DATE_ADDED, time / 1000);
                cv.put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, time / 1000);
                /*
                LogUtil.i(TAG, "insertAudioIntoDb 222: newFilePath:" + newFilePath + " mimeType:" + MediaFileUtil.getMimeTypeForFile(originalFilePath) +
                        " size:" + size + " DATE_ADDED:" + (time / 1000)); */
            }
            cv.put(MediaStore.Audio.AudioColumns.TITLE, FilePathUtil.getFileNameAndExtension(newFilePath));
            cv.put(MediaStore.Audio.AudioColumns.IS_MUSIC, 1);
            mContentResolver.insert(uri, cv);
            mContentResolver.notifyChange(uri, null);
            //LogUtil.i(TAG, "addInsertContentValuesForAudioDb: 222 " + cv);
        } catch (Exception e) {
            if(e != null) {
                LogUtil.e(TAG, "addInsertContentValuesForAudioDb 222: " + e.getMessage());
            }
        } finally {
            closeSilently(cursor);
        }
    }

    /*
    private void insertVideoIntoDb(String newFilePath) {
        Uri uri = GlobalConstants.VIDEO_URI;
        long time = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        long size = new File(newFilePath).length();
        cv.put(MediaStore.Video.VideoColumns.DATA, newFilePath);
        cv.put(MediaStore.Video.VideoColumns.MIME_TYPE, MediaFileUtil.getMimeTypeForFile(newFilePath));
        cv.put(MediaStore.Video.VideoColumns.SIZE, new File(newFilePath).length());
        cv.put(MediaStore.Video.VideoColumns.DATE_ADDED, time / 1000);
        cv.put(MediaStore.Video.VideoColumns.DATE_MODIFIED, time / 1000);
        cv.put(MediaStore.Video.VideoColumns.DATE_TAKEN, time);
        mContentResolver.insert(uri, cv);
        mContentResolver.notifyChange(uri, null);
    }
    */

    private void addInsertContentValuesForVideoDb(String newFilePath) {
        Uri uri = GlobalConstants.VIDEO_URI;
        long time = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        long size = new File(newFilePath).length();
        cv.put(MediaStore.Video.VideoColumns.DATA, newFilePath);
        cv.put(MediaStore.Video.VideoColumns.MIME_TYPE, MediaFileUtil.getMimeTypeForFile(newFilePath));
        cv.put(MediaStore.Video.VideoColumns.SIZE, new File(newFilePath).length());
        cv.put(MediaStore.Video.VideoColumns.DATE_ADDED, time / 1000);
        cv.put(MediaStore.Video.VideoColumns.DATE_MODIFIED, time / 1000);
        cv.put(MediaStore.Video.VideoColumns.DATE_TAKEN, time);
        //mContentResolver.insert(uri, cv);
        //mContentResolver.notifyChange(uri, null);
        //LogUtil.i(TAG, "addInsertContentValuesForVideoDb: 111 " + cv);
        addContentValuesToInsertList(GlobalConstants.VIDEO_URI, cv);
    }

    /*
    private void insertVideoIntoDb(String originalFilePath, String newFilePath) {
        Uri uri = GlobalConstants.VIDEO_URI;
        String [] projections = {
                MediaStore.Video.VideoColumns.MIME_TYPE,
                MediaStore.Video.VideoColumns.SIZE,
                MediaStore.Video.VideoColumns.DURATION,
                MediaStore.Video.VideoColumns.DATE_ADDED,
                MediaStore.Video.VideoColumns.DATE_MODIFIED,
                MediaStore.Video.VideoColumns.DATE_TAKEN,
        };
        String selection = MediaStore.MediaColumns.DATA  + " = ? ";
        String selectionArgs [] = { originalFilePath };
        long time = System.currentTimeMillis();
        ContentValues cv = new ContentValues();


        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(uri, projections, selection, selectionArgs, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                //mime
                int columnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.MIME_TYPE);
                String mimeType = cursor.getString(columnIndex);
                //size
                columnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.SIZE);
                long size = cursor.getLong(columnIndex);
                //duration
                columnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION);
                long duration = cursor.getLong(columnIndex);
                //date added
                columnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATE_ADDED);
                long dateAdded = cursor.getLong(columnIndex);
                //date modified
                columnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATE_MODIFIED);
                long dateModified = cursor.getLong(columnIndex);
                //date taken
                columnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATE_TAKEN);
                long dateTaken = cursor.getLong(columnIndex);
                //longtitude
                columnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.LONGITUDE);
                double longitude = cursor.getDouble(columnIndex);
                //latitude
                columnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.LATITUDE);
                double latitude = cursor.getDouble(columnIndex);
                cursor.close();
                cv.put(MediaStore.Video.VideoColumns.DATA, newFilePath);
                cv.put(MediaStore.Video.VideoColumns.MIME_TYPE, mimeType);
                cv.put(MediaStore.Video.VideoColumns.SIZE, size);
                cv.put(MediaStore.Video.VideoColumns.DURATION, duration);
                cv.put(MediaStore.Video.VideoColumns.DATE_ADDED, time / 1000);
                cv.put(MediaStore.Video.VideoColumns.DATE_MODIFIED, time / 1000);
                cv.put(MediaStore.Video.VideoColumns.DATE_TAKEN, dateTaken);
                cv.put(MediaStore.Video.VideoColumns.LONGITUDE, longitude);
                cv.put(MediaStore.Video.VideoColumns.LATITUDE, latitude);
            } else {
                cv.put(MediaStore.Video.VideoColumns.DATA, newFilePath);
                cv.put(MediaStore.Video.VideoColumns.MIME_TYPE, MediaFileUtil.getMimeTypeForFile(originalFilePath));
                cv.put(MediaStore.Video.VideoColumns.SIZE, new File(originalFilePath).length());
                cv.put(MediaStore.Video.VideoColumns.DATE_ADDED, time / 1000);
                cv.put(MediaStore.Video.VideoColumns.DATE_MODIFIED, time / 1000);
                cv.put(MediaStore.Video.VideoColumns.DATE_TAKEN, time);
            }
            mContentResolver.insert(uri, cv);
            mContentResolver.notifyChange(uri, null);
        } catch (Exception e) {
            if(e != null) {
                LogUtil.e(TAG, e.getMessage());
            }
        } finally {
            closeSilently(cursor);
        }
    }
    */

    private void addInsertContentValuesForVideoDb(String originalFilePath, String newFilePath) {
        Uri uri = GlobalConstants.VIDEO_URI;
        String [] projections = {
                MediaStore.Video.VideoColumns.MIME_TYPE,
                MediaStore.Video.VideoColumns.SIZE,
                MediaStore.Video.VideoColumns.DURATION,
                MediaStore.Video.VideoColumns.DATE_ADDED,
                MediaStore.Video.VideoColumns.DATE_MODIFIED,
                MediaStore.Video.VideoColumns.DATE_TAKEN,
        };
        String selection = MediaStore.MediaColumns.DATA  + " = ? ";
        String selectionArgs [] = { originalFilePath };
        long time = System.currentTimeMillis();
        ContentValues cv = new ContentValues();

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(uri, projections, selection, selectionArgs, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();

                //mime
                int columnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.MIME_TYPE);
                //LogUtil.i(TAG, "mime columnIndex:" + columnIndex);
                String mimeType = columnIndex == -1 ? "" : cursor.getString(columnIndex);

                //size
                columnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.SIZE);
                //LogUtil.i(TAG, "size columnIndex:" + columnIndex);
                long size = columnIndex == -1 ? 0 : cursor.getLong(columnIndex);

                //duration
                columnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION);
                //LogUtil.i(TAG, "duration columnIndex:" + columnIndex);
                long duration = columnIndex == -1 ? 0 : cursor.getLong(columnIndex);

                //date added
                columnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATE_ADDED);
                //LogUtil.i(TAG, "dateAdded columnIndex:" + columnIndex);
                long dateAdded = columnIndex == -1 ?  time : cursor.getLong(columnIndex);

                //date modified
                columnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATE_MODIFIED);
                //LogUtil.i(TAG, "dateModified columnIndex:" + columnIndex);
                long dateModified = columnIndex == -1 ?  time : cursor.getLong(columnIndex);

                //date taken
                columnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATE_TAKEN);
                //LogUtil.i(TAG, "dateTaken columnIndex:" + columnIndex);
                long dateTaken = columnIndex == -1 ?  time : cursor.getLong(columnIndex);

                //longtitude
                columnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.LONGITUDE);
                //LogUtil.i(TAG, "longitude columnIndex:" + columnIndex);
                double longitude = columnIndex == -1 ? 0 : cursor.getDouble(columnIndex);

                //latitude
                columnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.LATITUDE);
                //LogUtil.i(TAG, "latitude columnIndex:" + columnIndex);
                double latitude = columnIndex == -1 ? 0 : cursor.getDouble(columnIndex);
                cursor.close();
                cv.put(MediaStore.Video.VideoColumns.DATA, newFilePath);
                cv.put(MediaStore.Video.VideoColumns.MIME_TYPE, mimeType);
                cv.put(MediaStore.Video.VideoColumns.SIZE, size);
                cv.put(MediaStore.Video.VideoColumns.DURATION, duration);
                cv.put(MediaStore.Video.VideoColumns.DATE_ADDED, time / 1000);
                cv.put(MediaStore.Video.VideoColumns.DATE_MODIFIED, time / 1000);
                cv.put(MediaStore.Video.VideoColumns.DATE_TAKEN, dateTaken);
                cv.put(MediaStore.Video.VideoColumns.LONGITUDE, longitude);
                cv.put(MediaStore.Video.VideoColumns.LATITUDE, latitude);
            } else {
                cv.put(MediaStore.Video.VideoColumns.DATA, newFilePath);
                cv.put(MediaStore.Video.VideoColumns.MIME_TYPE, MediaFileUtil.getMimeTypeForFile(originalFilePath));
                cv.put(MediaStore.Video.VideoColumns.SIZE, new File(originalFilePath).length());
                cv.put(MediaStore.Video.VideoColumns.DATE_ADDED, time / 1000);
                cv.put(MediaStore.Video.VideoColumns.DATE_MODIFIED, time / 1000);
                cv.put(MediaStore.Video.VideoColumns.DATE_TAKEN, time);
            }
            //mContentResolver.insert(uri, cv);
            //mContentResolver.notifyChange(uri, null);
            //LogUtil.i(TAG, "addInsertContentValuesForVideoDb: 222 " + cv);
            addContentValuesToInsertList(uri, cv);
        } catch (Exception e) {
            if(e != null) {
                LogUtil.e(TAG, "addInsertContentValuesForVideoDb 222: " + e.getMessage());
            }
        } finally {
            closeSilently(cursor);
        }
    }

    private void closeSilently(Cursor cursor) {
        if(null == cursor) return;
        cursor.close();
    }
}
