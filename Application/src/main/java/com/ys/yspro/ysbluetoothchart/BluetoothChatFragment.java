/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ys.yspro.ysbluetoothchart;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.ys.yspro.common.logger.Log;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.jar.Pack200;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatFragment extends Fragment {

    private static final String TAG = "BluetoothChatFragment";
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int FILE_SELECT_CODE = 4;
    // Layout Views
    byte[] readBuf;
    private String hexS;
    private String at,Tg;
    private double sum=0 ,average=0;
 //   private Button mClearButton;
    private TextView mAverage;
    private EditText mEditData,mTrigger;
    private Button BTOFF;
    private LineChart mLineChart;
    private StringBuffer saveBuf = new StringBuffer();
    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;
    private ArrayList<String> list = new ArrayList<String>();
    private ArrayList<String> listData = new ArrayList<String>();
    /**
     * Array adapter for the conversation thread
     */
    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;
    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;
    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }


    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        /*if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();

        }*/
        if (mChatService == null) {
            setupChat();

        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        BTOFF = (Button)view.findViewById(R.id.BTOff);
        mAverage = (TextView)view.findViewById(R.id.average);
        mEditData = (EditText)view.findViewById(R.id.editData);
        mLineChart = (LineChart) view.findViewById(R.id.spread_line_chart);
        mTrigger = (EditText)view.findViewById(R.id.Trigger);
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        BTOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChatService.stop();
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), mHandler);
        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);

        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    public static String hexToASCII(String hexValue)
    {
        StringBuilder output = new StringBuilder("");
        for (int i = 0; i < hexValue.length(); i += 2)
        {
            String str = hexValue.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        return output.toString();
    }
    public static final String byte2hex(byte b[]) {
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = Integer.toHexString(b[n] & 0x7f);
            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
            } else {
                hs = hs + stmp;
            }
        }
        return hs.toUpperCase();
    }

    public  static float ava2ge(ArrayList value){
        float abc,ab = 0,sum=0;
        int k;
            for (k = 0; k < value.size(); k++) {
                if (!value.get(k).toString().isEmpty()) {
                    ab = Float.parseFloat(String.valueOf(value.get(k)));
                }
                sum += ab;
            }
            abc=sum/(value.size());

        return abc;
    }
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_READ:
                    readBuf = (byte[]) msg.obj;
                    String b2hex;
                    b2hex = byte2hex(readBuf);
                    hexS = hexToASCII(b2hex);
                    Tg=mTrigger.getText().toString();
                    at=mEditData.getText().toString();
                    int numb;
                    int trigger;
                    float hes = 0;
                    if( at.equals("")){  //editText沒有輸入值
                        numb= 50;
                        //Toast.makeText(getActivity(), "如不輸入預設為100筆!", Toast.LENGTH_SHORT).show();
                    } else {
                        numb = Integer.parseInt(at);  //轉成要的值
                    }
                    if ( Tg.equals("")){
                        trigger = 0;
                    }
                    else {
                        trigger = Integer.parseInt(Tg);
                    }
                    try {
                        hexS = hexS.replaceAll("[^[-?(0|[0-9]\\d*)(\\.\\d+)?]]", "");

                        if (!hexS.toString().isEmpty()) {
                            hes = Float.parseFloat(hexS);
                        }
                        if (hes > 0 && trigger > 0) {
                            if (hes >= trigger) {
                                list.add(hexS);
                                saveBuf.append(hexS + "\n");
                            }
                        } else if (hes < 0 && trigger < 0) {
                            if (hes <= trigger) {
                                list.add(hexS);
                                saveBuf.append(hexS + "\n");
                            }
                        } else if (trigger == 0) {
                            list.add(hexS);
                            saveBuf.append(hexS + "\n");
                        }
                    }catch (Exception e){

                    }
                    LineData mLineData = getLineData();
                    showChart(mLineChart, mLineData, Color.rgb(114, 188, 223));

                    NumberFormat nf = NumberFormat.getInstance();
                    nf.setMaximumFractionDigits(3);
                    mAverage.setText(nf.format(ava2ge(list)));

                    if (list.size() == numb) {
                        mChatService.stop();
                        //藍牙斷開
                    }

                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "連結至 "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
            case FILE_SELECT_CODE:
                // 請求確認返回
                if (resultCode ==Activity.RESULT_OK) {
                    // 取得檔案路徑 Uri
                    Uri uri = data.getData();
                    // 取得路徑
                    String path = null;
                    try {
                        path = FileUtils.getPath(getActivity(), uri);
                    } catch (URISyntaxException e) {
                        Toast.makeText(getActivity(), "檔案不符合!", Toast.LENGTH_SHORT).show();

                        return;
                    }

                    String read;
                    String mRead;
                    BufferedReader bufread;
                    try	{
                        File fhd = new File(path);
                        bufread = new BufferedReader(new FileReader(fhd));
                        while ((read = bufread.readLine()) != null) {
                            mRead = String.valueOf(read.replaceAll("[^[-?(0|[0-9]\\d*)(\\.\\d+)?]]", ""));
                            listData.add(mRead);
                        }
                        bufread.close();
                        LineData mReadData= readDataFile();
                        showChart(mLineChart,mReadData, Color.rgb(114, 188, 223));
                        float ab;

                        for (int k = 0; k < listData.size(); k++) {
                                ab =Float.parseFloat(listData.get(k));
                                sum += ab;
                            }
                        average=sum/(listData.size());
                        NumberFormat nf = NumberFormat.getInstance();
                        nf.setMaximumFractionDigits(3);
                        mAverage.setText(nf.format(average));
                    }catch (Exception d){
                        Toast.makeText(getActivity(), "檔案格式錯誤", Toast.LENGTH_SHORT).show();
                    }
                }

                break;
         }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private void saveData2File(){
        if (this.saveBuf.length()> 0)
            this.save2SD(this.saveBuf.toString());
    }
    protected void save2SD(String sData){
        String sRoot = null;
        String sFileName = null;
        String sPath = null;
        //判斷SD卡是否存在,取出根目錄(尾端不帶'/')

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            sRoot = Environment.getExternalStorageDirectory().toString();//獲取根目錄
        else
            return;
        //生成文件名
        sFileName = (new SimpleDateFormat("MM:dd:HH:mm:ss", Locale.getDefault())).format(new Date()) + ".txt";
        //生成最終保存路徑
        sPath = sRoot.concat("/").concat(this.getString(R.string.app_name));
        if (LocalIOTools.coverByte2File(sPath, sFileName, sData.getBytes())){
            String sMsg = ("Save to:").concat(sPath).concat("/").concat(sFileName);
            Toast.makeText(getActivity(), sMsg, Toast.LENGTH_LONG).show();//提示 保存成功

        }else{
            Toast.makeText(getActivity(), getString(R.string.msg_save_file_fail),
                    Toast.LENGTH_SHORT).show();
        }
    }
    private void readData2File() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult( Intent.createChooser(intent, "選擇檔案"), FILE_SELECT_CODE );
        } catch (android.content.ActivityNotFoundException ex) {
            // 若使用者沒有安裝檔案瀏覽器的 App 則顯示提示訊息
            Toast.makeText(getActivity(), "沒有檔案瀏覽器", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * Establish connection with other divice
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                list.clear();
                listData.clear();
                saveBuf.delete(0, saveBuf.length());
                return true;
            }
            case R.id.save:{
                saveData2File();
                return true;
            }
            case R.id.read:{
                readData2File();
                listData.clear();
                sum=0;
                return true;
            }
        }
        return false;
    }


    private void showChart(LineChart lineChart, LineData lineData, int color) {
        lineChart.setDrawBorders(false);
        // no description text
        lineChart.setDescription("");
        lineChart.setNoDataTextDescription("You need to provide data for the chart.");

        // enable / disable grid background
        lineChart.setDrawGridBackground(false);
        lineChart.setGridBackgroundColor(Color.WHITE & 0x70FFFFFF);

        // enable touch gestures
        lineChart.setTouchEnabled(true);

        // enable scaling and dragging
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);

        // if disabled, scaling can be done on x- and y-axis separately
        lineChart.setPinchZoom(false);//

        lineChart.setBackgroundColor(color);

        // add data
        lineChart.setData(lineData);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getAxisRight().setEnabled(false);
        Legend mLegend = lineChart.getLegend();
        mLegend.setForm(Legend.LegendForm.CIRCLE);
        mLegend.setFormSize(6f);
        mLegend.setTextColor(Color.WHITE);


        lineChart.animateX(2500);
    }


    private LineData getLineData() {

        ArrayList<String> xValues = new ArrayList<String>();
        ArrayList<Entry> yValues = new ArrayList<Entry>();
        for (int i=0 ; i<list.size();i++) {
            xValues.add(" " + i);
            if (!list.get(i).toString().isEmpty()) {
                yValues.add(new Entry(Float.parseFloat(list.get(i)), i));
            }

        }
        LineDataSet lineDataSet = new LineDataSet(yValues, "Y軸");
        lineDataSet.setLineWidth(1.75f); // 線寬
        lineDataSet.setCircleSize(3f);// 顯示的圓形大小
        lineDataSet.setColor(Color.WHITE);// 顯示顏色
        lineDataSet.setCircleColor(Color.WHITE);// 圓形的顏色
        lineDataSet.setHighLightColor(Color.WHITE); // 線
        lineDataSet.setValueFormatter(new MyValueFormatter());

        ArrayList<ILineDataSet> lineDataSets = new ArrayList<ILineDataSet>();
        lineDataSets.add(lineDataSet); // add the datasets
        // create a data object with the datasets
        LineData lineData = new LineData(xValues,lineDataSets);//藍牙Data
        return lineData;
    }
    private LineData readDataFile(){
        ArrayList<String> mxValues = new ArrayList<String>();
        ArrayList<Entry> myValues = new ArrayList<Entry>();
        for (int j = 0; j < listData.size(); j++) {
            mxValues.add("" + j);
            myValues.add(new Entry(Float.parseFloat(listData.get(j)),j));
        }




        LineDataSet mlineDataSet = new LineDataSet(myValues, "Y軸");
        mlineDataSet.setLineWidth(1.75f); // 線寬
        mlineDataSet.setCircleSize(3f);// 顯示的圓形大小
        mlineDataSet.setColor(Color.WHITE);// 顯示顏色
        mlineDataSet.setCircleColor(Color.WHITE);// 圓形的顏色
        mlineDataSet.setHighLightColor(Color.WHITE); // 線
        mlineDataSet.setValueFormatter(new MyValueFormatter());

        ArrayList<ILineDataSet> mlineDataSets = new ArrayList<ILineDataSet>();
        mlineDataSets.add(mlineDataSet); // add the datasets
        // create a data object with the datasets
        LineData lineData2 = new LineData(mxValues,mlineDataSets);//txtData

        return lineData2;
    }
    public class MyValueFormatter implements ValueFormatter {
        private DecimalFormat mFormat;

        public MyValueFormatter() {
            mFormat = new DecimalFormat("###,###,##0.000"); // Data顯示格式
        }

        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
            return mFormat.format(value);
        }
    }
}
