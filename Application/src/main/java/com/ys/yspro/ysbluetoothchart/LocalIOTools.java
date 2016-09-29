package com.ys.yspro.ysbluetoothchart;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;

/**
 * 本地IO操作类
 * @author JerryLi(hzjerry@gmail.com)
 * @version 1.0
 * @date 2010-06-25
 */
public class LocalIOTools {

    public static boolean coverByte2File(String sPath, String sFile, byte[] bData){
        try	{
			/*檢查目錄是否存在*/
            File fhd = new File(sPath);
            if (!fhd.exists())
                if (!fhd.mkdirs())//目錄不存在，創建
                    return false; //目錄創建失敗，退出

			/*檢查文件是否存在*/
            fhd = new File(sPath +"/"+ sFile);
            if (fhd.exists())
                fhd.delete(); //文件存在，刪除

            //追加方式寫入文件
            FileOutputStream fso = new FileOutputStream(fhd);
            fso.write(bData);
            fso.close();
            return true;
        }catch (Exception d){
            System.out.println(d.getMessage());
            return false;
        }
    }
}

