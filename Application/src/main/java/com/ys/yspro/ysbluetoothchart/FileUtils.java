package com.ys.yspro.ysbluetoothchart;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import java.io.File;
import java.net.URISyntaxException;

public class FileUtils {


    /**
     * 由外部 Activity 回傳的資料取得檔案路徑
     * @param context   Activity
     * @param uri       Uri
     * @return
     *  檔案路徑
     * @throws URISyntaxException
     */
    public static String getPath(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Do nothing
            }
            finally {
                if( cursor != null ) cursor.close();
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }



}
