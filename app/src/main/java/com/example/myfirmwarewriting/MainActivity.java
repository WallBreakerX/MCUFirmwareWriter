package com.example.myfirmwarewriting;
import cn.wch.ch34xuartdriver.CH34xUARTDriver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.leon.lfilepickerlibrary.LFilePicker;
import com.leon.lfilepickerlibrary.utils.Constant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";
    private CH34xUARTDriver MyCH340;
    private UsbSerialDevice Serial;

    private Hex2Bin MyHexfile = new Hex2Bin();
    private byte[] MyBindate;
    private byte[][] Firmware_Bin;
    private byte[] Firmware_check_list;
    private int RAMWRITE_SIZE = 512;
    private String Hexfilename = null;
    private int BANDRATE = 115200;
    private String commandstr = "";

    private Button ButtonFile, ButtonWrite, ButtonFlash;
    private TextView Info_text_view;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private int REQUESTCODE_FROM_ACTIVITY = 1000;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView Title = findViewById(R.id.textView2);              //实例化 标题字体
        AssetManager mgr = getAssets();
        Typeface tf = Typeface.createFromAsset(mgr, "CooperHewitt-Bold.ttf");
        Title.setTypeface(tf);

        ButtonWrite = findViewById(R.id.button2);            //实例化 烧录按钮
        ButtonWrite.setTypeface(tf);

        ButtonFlash = findViewById(R.id.button3);            //实例化 Flash选择按钮
        ButtonFlash.setTypeface(tf);

        ButtonFile = findViewById(R.id.button);              //实例化 文件选择按钮
        ButtonFile.setTypeface(tf);



        Info_text_view = findViewById(R.id.contentTextViewId);      //实例化 信息输出窗口
        tf = Typeface.createFromAsset(mgr, "apple_SC_BlackBold.ttf");
        Info_text_view.setTypeface(tf);
        Info_text_view.setMovementMethod(ScrollingMovementMethod.getInstance());

        //文件选择按钮事件
        ButtonFile.setOnClickListener(v -> {
            new LFilePicker()
                    .withActivity(MainActivity.this)
                    .withRequestCode(REQUESTCODE_FROM_ACTIVITY)
                    .withStartPath("/storage/emulated/0/")
                    .withMutilyMode(false)//多选禁用
                    .withChooseMode(true)//选文件模式
                    .withTitle("Choose file")
//                    .withBackgroundColor("white")
                    .withIconStyle(Constant.BACKICON_STYLEONE)
                    .start();
        });

        //烧录按钮事件
        ButtonWrite.setOnClickListener(v -> {
            ShowInfo("NMSL");
            if (Hexfilename == null || !Hexfilename.contains(".hex"))
                Toast.makeText(MainActivity.this, "请选择.hex文件", Toast.LENGTH_SHORT).show();
            else {
                ButtonWrite.setText("烧录中...");
                ButtonWrite.setClickable(false);
                Thread HextoBin = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MyBindate = MyHexfile.transform(Hexfilename);   //转换hex 到 bin byte数组
                        BintoWriteBin(MyBindate);                       //分割byte[]
                        ShowInfo(String.valueOf(Firmware_Bin.length));
//                        if (!initDriver()){                             //初始化CH340设备
//                            RecoverButton();
//                            return;
//                        }
//                        Into_ISPMod();

                    }
                });
                HextoBin.start();
            }
        });

        //Flash选择按钮事件
        ButtonFlash.setOnClickListener(v -> {
            showListDialog();
        });

        verifyStoragePermissions(this);


    }

    private boolean MainWriteCore(){
        int firmwarecount = 0;
        int Address = 0;
        String recv;
        /////////////////////此处开始写入固件/////////////////////////////////
        while (firmwarecount < Firmware_Bin.length) {
            /////////////////////准备扇区////////////////////////////////////////
            recv = WriteData("P 0 15\r\n");
            if (recv.contains("0\r\n")) {
                recv = WriteData("W 268436224 512\r\n");
                /////////////////////写 512 字节到 RAM////////////////////////////////////////
                if (recv.contains("0\r\n")) {
                    if (MyCH340.WriteData(Firmware_Bin[firmwarecount], Firmware_Bin[firmwarecount].length) != Firmware_Bin[firmwarecount].length) {
                        ShowInfo("写数据到RAM失败");
                        return false;
                    }
                } else {
                    ShowInfo("接收数据错误");
                    return false;
                }
            } else {
                ShowInfo("接收数据错误");
                return false;
            }
            firmwarecount += 1;

            recv = WriteData("W 268436736 512\r\n");
            if (recv.contains("0\r\n")) {
                /////////////////////再写 512 字节到 RAM////////////////////////////////////////
                if (MyCH340.WriteData(Firmware_Bin[firmwarecount], Firmware_Bin[firmwarecount].length) != Firmware_Bin[firmwarecount].length) {
                    ShowInfo("写数据到RAM失败");
                    return false;
                }
            } else {
                ShowInfo("接收数据错误");
                return false;
            }

            firmwarecount += 1;
//                                    /////////////////////准备扇区////////////////////////////////////////
            recv = WriteData("P 0 15\r\n");
            if (recv.contains("0\r\n")) {
                /////////////////////从 RAM 复制1024字节到 Flash////////////////////////////////////////
                recv = WriteData(String.format("C %d 268436224 1024\r\n", Address));
                if (recv.contains("0")) {
                    Address += 1024;
                }
            } else {
                ShowInfo("接收数据错误");
                return false;
            }
            /////////////////开始校验固件////////////////////////////////
            if (firmwarecount == Firmware_Bin.length) {
                int Read_address = 0;
                for (int i = Read_address; i < Firmware_Bin.length; i++) {
                    Firmware_check_list = ReadFlash(String.format("R %d %d\r\n", Read_address, RAMWRITE_SIZE), RAMWRITE_SIZE);
                    if (new String(Firmware_check_list).equals("ERROR")) {
                        return false;
                    }
                    for (int j = 0; j < RAMWRITE_SIZE; j++) {
                        if (Firmware_Bin[i][j] != Firmware_check_list[j + 3]) {
                            ShowInfo("校验固件失败");
                            return false;
                        }
                    }
                    Read_address += 512;
                }
                Out_ISPMod();
                commandstr = "0";
            }
            ///////////////////////////////////////////////////////////
        }
        commandstr = "0";
        return true;
    }
    private void RecoverButton(){
        ButtonWrite.setText("GO");
        ButtonWrite.setClickable(true);
    }
    private void BintoWriteBin(byte[] Binbuffer){
        if (((Binbuffer.length/RAMWRITE_SIZE)/2 != 0 && Binbuffer.length%RAMWRITE_SIZE != 0) || ((Binbuffer.length/RAMWRITE_SIZE)/2 == 0 && Binbuffer.length%RAMWRITE_SIZE == 0)) {

            Firmware_Bin = (Binbuffer.length % RAMWRITE_SIZE == 0) ? new byte[Binbuffer.length / RAMWRITE_SIZE][] : new byte[(Binbuffer.length / RAMWRITE_SIZE) + 1][];
            int i, j;
            for (i = 0; i < Binbuffer.length / RAMWRITE_SIZE; i++) {
                Firmware_Bin[i] = new byte[RAMWRITE_SIZE];
                for (j = i * RAMWRITE_SIZE; j < i * RAMWRITE_SIZE + RAMWRITE_SIZE; j++) {
                    Firmware_Bin[i][j - (i * RAMWRITE_SIZE)] = Binbuffer[j];
                }
            }
            if (Binbuffer.length % RAMWRITE_SIZE != 0) {
                Firmware_Bin[i] = new byte[RAMWRITE_SIZE];
                for (j = Binbuffer.length - (Binbuffer.length % RAMWRITE_SIZE); j < Binbuffer.length; j++) {
                    Firmware_Bin[i][j - (Binbuffer.length - (Binbuffer.length % RAMWRITE_SIZE))] = Binbuffer[j];
                }
                for (j = Binbuffer.length % RAMWRITE_SIZE; j < RAMWRITE_SIZE; j++) {
                    Firmware_Bin[i][j] = (byte) 0xFF;
                }
            }
            Firmware_check_list = new byte[RAMWRITE_SIZE + 3];
        }
        else if (((Binbuffer.length/RAMWRITE_SIZE)/2 == 0 && Binbuffer.length%RAMWRITE_SIZE != 0) || ((Binbuffer.length/RAMWRITE_SIZE)/2 != 0 && Binbuffer.length%RAMWRITE_SIZE == 0)){
            Firmware_Bin = (Binbuffer.length % RAMWRITE_SIZE == 0) ? new byte[Binbuffer.length / RAMWRITE_SIZE+1][] : new byte[(Binbuffer.length / RAMWRITE_SIZE) + 2][];

            int i, j;
            for (i = 0; i < Binbuffer.length / RAMWRITE_SIZE; i++) {
                Firmware_Bin[i] = new byte[RAMWRITE_SIZE];
                for (j = i * RAMWRITE_SIZE; j < i * RAMWRITE_SIZE + RAMWRITE_SIZE; j++) {
                    Firmware_Bin[i][j - (i * RAMWRITE_SIZE)] = Binbuffer[j];
                }
            }
            if (Binbuffer.length % RAMWRITE_SIZE != 0) {
                Firmware_Bin[i] = new byte[RAMWRITE_SIZE];
                for (j = Binbuffer.length - (Binbuffer.length % RAMWRITE_SIZE); j < Binbuffer.length; j++) {
                    Firmware_Bin[i][j - (Binbuffer.length - (Binbuffer.length % RAMWRITE_SIZE))] = Binbuffer[j];
                }
                for (j = Binbuffer.length % RAMWRITE_SIZE; j < RAMWRITE_SIZE; j++) {
                    Firmware_Bin[i][j] = (byte) 0xFF;
                }
            }
            Firmware_Bin[Firmware_Bin.length-1] = new byte[RAMWRITE_SIZE];
            for (i=0; i<RAMWRITE_SIZE; i++){
                Firmware_Bin[Firmware_Bin.length-1][i] = (byte) 0xFF;
            }
            Firmware_check_list = new byte[RAMWRITE_SIZE + 3];
        }
    }


    public static void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUESTCODE_FROM_ACTIVITY) {
                //如果是文件选择模式，需要获取选择的所有文件的路径集合
                List<String> list = data.getStringArrayListExtra("paths");
                Hexfilename = list.get(0);
                String reverse = "";
                for (int i=Hexfilename.length()-1; i>=0; i--){ //提取出文件名
                    if (Hexfilename.charAt(i) == '/')
                        break;
                    reverse += Hexfilename.charAt(i);
                }
                ButtonFile.setText(new StringBuffer(reverse).reverse().toString());
            }
        }
    }

    private void showListDialog() {
        String[] list_String = {"8KB", "16KB", "24KB", "32KB", "64KB"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setItems(list_String, new DialogInterface.OnClickListener() {//列表对话框；
            @Override
            public void onClick(DialogInterface dialog, int which) {//根据这里which值，即可以指定是点击哪一个Item；
                ButtonFlash.setText(list_String[which]);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void ShowInfo(String str) {
        Info_text_view.append("  " + str + "\n");
        int offset = Info_text_view.getLineCount() * Info_text_view.getLineHeight();
        if (offset > (Info_text_view.getHeight() - Info_text_view.getLineHeight()) + 40) {
            Info_text_view.scrollTo(0, offset - Info_text_view.getHeight() + Info_text_view.getLineHeight() - 40);
        }
    }


    ///////////////////////////////Init serial port and USB/////////////////////////////////////////
    private boolean initDriver() {
        MyCH340 = (new CH34xUARTDriver(
                (UsbManager) getSystemService(Context.USB_SERVICE), this,
                ACTION_USB_PERMISSION));
        if (!MyCH340.UsbFeatureSupported()) {
            Toast.makeText(this, "Your Phone does't support OTG", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (MyCH340.ResumeUsbPermission() == -2) {
            Toast.makeText(this, "No Permission", Toast.LENGTH_SHORT).show();
            return false;
        }
        if ((MyCH340.EnumerateDevice()) == null) {
            Toast.makeText(this, "No CH340 Device", Toast.LENGTH_SHORT).show();
            return false;
        }
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        UsbDevice device = deviceIterator.next();
        UsbDeviceConnection fortestusbConnection = manager.openDevice(device);

        Serial = UsbSerialDevice.createUsbSerialDevice(device, fortestusbConnection);

        MyCH340.OpenDevice(device);
        if (MyCH340.UartInit() == false) {
            Toast.makeText(this, "Init Serial failed", Toast.LENGTH_SHORT).show();
            return false;
        }

        boolean TF = MyCH340.SetConfig(BANDRATE, (byte) 8, (byte) 0, (byte) 0, (byte) 0);
        if (TF == false) {
            Toast.makeText(this, "Set bandrate failed", Toast.LENGTH_SHORT).show();
            return false;
        }

        //////////////////////APP与MCU 数据发送与接收线程/////////////////////////////////////////////
        Thread myThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    byte[] by = new byte[64];
                    String s = "";
                    if (MyCH340.ReadData(by, 64) > 0) {
                        s = new String(by);
                        if (commandstr.contains("?") && s.contains("Synchronized\r\nOK\r\n")) {
                            s = WriteData("12000\r\n");
                            if (s.contains("12000\r\nOK\r\n")) {
                                s = WriteData("A 0\r\n");
                                if (s.contains("A 0\r\n0\r\n")) {
                                    s = WriteData("U 23130\r\n");
                                    if (s.contains("0")) {
                                        s = WriteData("P 0 15\r\n");
                                        if (s.contains("0")) {
                                            s = WriteData("E 0 15\r\n");
                                            if (s.contains("0")) {
                                                // 成功 继续 无需在此添加代码
                                            } else {
                                                commandstr = "0";
                                                continue;
                                            }
                                        } else {
                                            commandstr = "0";
                                            continue;
                                        }
                                    } else {
                                        commandstr = "0";
                                        continue;
                                    }
                                } else {
                                    commandstr = "0";
                                    continue;
                                }
                            } else {
                                commandstr = "0";
                                continue;
                            }
                            s = WriteData("U 23130\r\n");
                            if (s.contains("0\r\n")) {
                                MainWriteCore();
                            } else {
                                ShowInfo("ERROR:" + s + "\n(line 358)");
                                commandstr = "0";
                                continue;
                            }
                        } else if (s.contains("Synchronized\r\nOK\r\n")) {
                            ShowInfo(s);
                        } else if (s.contains("Synchronized\r\n")) {
                            String string = "Synchronized\r\n";
                            byte[] bytt = string.getBytes();
                            if (MyCH340.WriteData(bytt, bytt.length) != bytt.length) {
                                ShowInfo("Send byte to port failed");
                            }
                        }
                    }
                }
            }
        });
        myThread.start();

        return true;
    }

    ///////////////////////////Send date to MCU via UART////////////////////////////////////////////
    private String WriteData(String str) {
        byte[] tmpbyte;
        tmpbyte = str.getBytes();
        if (MyCH340.WriteData(tmpbyte, tmpbyte.length) != tmpbyte.length) {
            ShowInfo("发送命令失败\n(line 128)");
            return "ERROR";
        }
        tmpbyte = new byte[32];
        int count = 0;
        try {
            sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while ((!(MyCH340.ReadData(tmpbyte, 32) > 0))) {
            count++;
            if (count > 4000000) {
                ShowInfo("接收超时\n(line 141)");
                return "ERROR";
            }
        }

        return new String(tmpbyte);
    }

    ///////////////////////////Read date from MCU via UART//////////////////////////////////////////
    private byte[] ReadFlash(String command, int read_size) {
        byte[] tmpbyte = command.getBytes();
        if (MyCH340.WriteData(tmpbyte, tmpbyte.length) != tmpbyte.length) {
            ShowInfo("发送命令失败\n(line 152)");
            return "ERROR".getBytes();
        }
        tmpbyte = new byte[read_size + 3];
        byte[] tmmpp = new byte[32];
        int count = 0;
        int position = 0;
        int recv = 0;
        int circle = 0;
        int circle_limit = ((read_size) / 32 == 0) ? (read_size) / 32 : ((read_size) / 32) + 1;
        while (circle < circle_limit) {
            while ((!((recv = MyCH340.ReadData(tmmpp, 32)) > 0))) {
                count++;
                if (count > 4000000) {
                    ShowInfo("接收超时\n(line 166)");
                    return "ERROR".getBytes();
                }
            }
            for (int i = 0; i < recv; i++) {
                tmpbyte[position] = tmmpp[i];
                position += 1;
            }
            circle += 1;
        }

        return tmpbyte;
    }

    ////////////////////////////Set MCU to ISP mod//////////////////////////////////////////////////
    private void Into_ISPMod() {
        Serial.setRTS(true);
        try {
            Thread.sleep(50);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Serial.setDTR(true);
        try {
            Thread.sleep(50);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Serial.setDTR(false);
        try {
            Thread.sleep(50);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Serial.setRTS(false);
        try {
            Thread.sleep(50);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ////////////////////////////Set MCU out of ISP mod//////////////////////////////////////////////
    private void Out_ISPMod() {
        Serial.setRTS(true);
        try {
            Thread.sleep(50);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Serial.setDTR(true);
        try {
            Thread.sleep(50);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Serial.setRTS(false);
        try {
            Thread.sleep(50);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Serial.setDTR(false);
        try {
            Thread.sleep(50);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}