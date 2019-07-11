package com.example.myfirmwarewriting;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import cn.wch.ch34xuartdriver.CH34xUARTDriver;
import me.rosuh.filepicker.config.FilePickerManager;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";
    private CH34xUARTDriver MyCH340;
    private UsbSerialDevice Serial;

    private Hex2Bin Mybinfile;
    private int BANDRATE = 115200;
    private String commandstr = "";

    private TextView Info_text_view;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //////////////////实例化 标题字体/////////////////////////////////////////////////////////////
        TextView Title = findViewById(R.id.textView2);
        AssetManager mgr = getAssets();
        Typeface tf = Typeface.createFromAsset(mgr, "CooperHewitt-Bold.ttf");
        Title.setTypeface(tf);

        //////////////////实例化 文件选择按钮/////////////////////////////////////////////////////////
        Button ButtonFile = findViewById(R.id.button);
        mgr = getAssets();
        tf = Typeface.createFromAsset(mgr, "CooperHewitt-Bold.ttf");
        ButtonFile.setTypeface(tf);

        //////////////////实例化 烧录按钮/////////////////////////////////////////////////////////
        Button ButtonWrite = findViewById(R.id.button2);
        mgr = getAssets();
        tf = Typeface.createFromAsset(mgr, "CooperHewitt-Bold.ttf");
        ButtonWrite.setTypeface(tf);

        ////////////////////实例化 信息窗口//////////////////////////////////////////////////////////
        Info_text_view = findViewById(R.id.contentTextViewId);
        TextView Info_text_view = findViewById(R.id.contentTextViewId);
        mgr = getAssets();
        tf = Typeface.createFromAsset(mgr, "apple_SC_mid.ttf");
        ButtonWrite.setTypeface(tf);
        Info_text_view.setMovementMethod(ScrollingMovementMethod.getInstance());

        FilePickerManager.INSTANCE
                        .from(this)
                        .forResult(FilePickerManager.REQUEST_CODE);
        //////////////////文件选择按钮事件////////////////////////////////////////////////////////////
        ButtonFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                FilePickerManager.INSTANCE
//                        .from(this)
//                        .forResult(FilePickerManager.REQUEST_CODE);
            }
        });

        //////////////////烧录按钮事件////////////////////////////////////////////////////////////////
        ButtonWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowInfo("NMSL");
            }
        });


        //////////////////////APP与MCU 数据发送与接收线程/////////////////////////////////////////////
//        Thread myThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    byte[] by = new byte[64];
//                    String s = "";
//                    if (MyCH340.ReadData(by, 64) > 0) {
//                        s = new String(by);
//                        if (commandstr.contains("?") && s.contains("Synchronized\r\nOK\r\n")) {
//                            s = WriteData("12000\r\n");
//                            if (s.contains("12000\r\nOK\r\n")) {
//                                s = WriteData("A 0\r\n");
//                                if (s.contains("A 0\r\n0\r\n")) {
//                                    s = WriteData("U 23130\r\n");
//                                    if (s.contains("0")) {
//                                        s = WriteData("P 0 15\r\n");
//                                        if (s.contains("0")) {
//                                            s = WriteData("E 0 15\r\n");
//                                            if (s.contains("0")) {
//                                                // 成功 继续 无需在此添加代码
//                                            } else {
//                                                commandstr = "0";
//                                                continue;
//                                            }
//                                        } else {
//                                            commandstr = "0";
//                                            continue;
//                                        }
//                                    } else {
//                                        commandstr = "0";
//                                        continue;
//                                    }
//                                } else {
//                                    commandstr = "0";
//                                    continue;
//                                }
//                            } else {
//                                commandstr = "0";
//                                continue;
//                            }
//                            s = WriteData("U 23130\r\n");
//                            if (s.contains("0\r\n")) {
//                                firmwarecount = 0;
//                                Address = 0;
//                                /////////////////////此处开始写入固件/////////////////////////////////
//                                while (firmwarecount < Firmware_Bin.length) {
//                                    /////////////////////准备扇区////////////////////////////////////////
//                                    s = WriteData("P 0 15\r\n");
//                                    if (s.contains("0\r\n")) {
//                                        s = WriteData("W 268436224 512\r\n");
//                                        /////////////////////写 512 字节到 RAM////////////////////////////////////////
//                                        if (s.contains("0\r\n")) {
//                                            if (MyCH340[TARGET_PANEL].WriteData(Firmware_Bin[firmwarecount], Firmware_Bin[firmwarecount].length) != Firmware_Bin[firmwarecount].length) {
//                                                ShowInfo("写数据到RAM失败\n(line 306)");
//                                                break;
//                                            }
//                                        } else {
//                                            break;
//                                        }
//                                    } else {
//                                        break;
//                                    }
//                                    firmwarecount += 1;
//
//                                    s = WriteData("W 268436736 512\r\n");
//                                    if (s.contains("0\r\n")) {
//                                        /////////////////////再写 512 字节到 RAM////////////////////////////////////////
//                                        if (MyCH340[TARGET_PANEL].WriteData(Firmware_Bin[firmwarecount], Firmware_Bin[firmwarecount].length) != Firmware_Bin[firmwarecount].length) {
//                                            ShowInfo("Send .bin failed\n(line 323)");
//                                            break;
//                                        }
//                                    } else {
//                                        ShowInfo("ERROR:" + s +
//                                                "\n(line 128)");
//                                        break;
//                                    }
//
//                                    firmwarecount += 1;
////                                    /////////////////////准备扇区////////////////////////////////////////
//                                    s = WriteData("P 0 15\r\n");
//                                    if (s.contains("0\r\n")) {
//                                        /////////////////////从 RAM 复制1024字节到 Flash////////////////////////////////////////
//                                        s = WriteData(String.format("C %d 268436224 1024\r\n", Address));
//                                        if (s.contains("0")) {
//                                            Address += 1024;
//                                        }
//                                    } else {
//                                        ShowInfo("ERROR:" + s +
//                                                "\n(line 345)");
//                                        break;
//                                    }
//                                    /////////////////开始校验固件////////////////////////////////
//                                    if (firmwarecount == Firmware_Bin.length) {
//                                        int Read_address = 0;
//                                        for (int i = Read_address; i < Firmware_Bin.length; i++) {
//                                            Firmware_check_list = ReadFlash(String.format("R %d %d\r\n", Read_address, RAMWRITE_SIZE), RAMWRITE_SIZE);
//                                            if (new String(Firmware_check_list).equals("ERROR")) {
//                                                return;
//                                            }
//                                            for (int j = 0; j < RAMWRITE_SIZE; j++) {
//                                                if (Firmware_Bin[i][j] != Firmware_check_list[j + 3]) {
//                                                    ShowInfo("Check firmware failed\n(line 358)");
//                                                    START = 0;
//                                                    return;
//                                                }
//                                            }
//                                            Read_address += 512;
//                                        }
//                                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
//                                        ShowInfo("Check firmware succeed\n" + simpleDateFormat.format(mydate) + "\n" + simpleDateFormat.format(writedate));
//                                        boolean TF = MyCH340[TARGET_PANEL].SetConfig(1000000, (byte) 8, (byte) 0, (byte) 0, (byte) 0);
//                                        if (TF == false) {
//                                            ShowInfo("Set baudrate fail\n(line 740)");
//                                        }
//                                        Out_ISPMod();
//                                        commandstr = "0";
//                                    }
//                                    ///////////////////////////////////////////////////////////
//                                }
//                                commandstr = "0";
//                            } else {
//                                ShowInfo("ERROR:" + s + "\n(line 358)");
//                                commandstr = "0";
//                                continue;
//                            }
//                        } else if (s.contains("Synchronized\r\nOK\r\n")) {
//                            ShowInfo(s);
//                        } else if (s.contains("Synchronized\r\n")) {
//                            String string = "Synchronized\r\n";
//                            byte[] bytt = string.getBytes();
//                            if (MyCH340.WriteData(bytt, bytt.length) != bytt.length) {
//                                ShowInfo("Send byte to port failed");
//                            } else
//                                ShowInfo("已经发送 + " + string);
//                        }
//                    }
//                }
//            }
//        });
//        myThread.start();

    }

//    private void ReadHexfile(String filename){
//        AssetManager assetManager = getAssets();
//        InputStream input;
//        try {
//            input = assetManager.open("pixel_all_fw_4.5_hw_0.1.3.bin");
//            int size = input.available();
//            byte[] Buffer = new byte[size];
//            input.read(Buffer);
//            input.close();
//
//            Firmware_Bin = (Buffer.length%RAMWRITE_SIZE == 0) ? new byte[Buffer.length/RAMWRITE_SIZE][] : new byte[(Buffer.length / RAMWRITE_SIZE) + 1][];
//            int i, j;
//            for (i = 0; i < Buffer.length / RAMWRITE_SIZE; i++) {
//                Firmware_Bin[i] = new byte[RAMWRITE_SIZE];
//                for (j = i * RAMWRITE_SIZE; j < i * RAMWRITE_SIZE + RAMWRITE_SIZE; j++) {
//                    Firmware_Bin[i][j - (i*RAMWRITE_SIZE)] = Buffer[j];
//                }
//            }
//            if (Buffer.length%RAMWRITE_SIZE != 0) {
//                Firmware_Bin[i] = new byte[RAMWRITE_SIZE];
//                for (j = Buffer.length - (Buffer.length % RAMWRITE_SIZE); j < Buffer.length; j++) {
//                    Firmware_Bin[i][j - (Buffer.length - (Buffer.length % RAMWRITE_SIZE))] = Buffer[j];
//                }
//                for (j = Buffer.length % RAMWRITE_SIZE; j < RAMWRITE_SIZE; j++){
//                    Firmware_Bin[i][j] = (byte)0xFF;
//                }
//            }
//            Firmware_check_list = new byte[RAMWRITE_SIZE + 3];
//
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }
    private void ShowInfo(String str) {
        Info_text_view.append("  " + str + "\n");
        int offset = Info_text_view.getLineCount() * Info_text_view.getLineHeight();
        if (offset > (Info_text_view.getHeight() - Info_text_view.getLineHeight() - 20)) {
            Info_text_view.scrollTo(0, offset - Info_text_view.getHeight() + Info_text_view.getLineHeight() + 20);
        }
    }


    ///////////////////////////////Init serial port and USB/////////////////////////////////////////
    private int initDriver() {
        MyCH340 = (new CH34xUARTDriver(
                (UsbManager) getSystemService(Context.USB_SERVICE), this,
                ACTION_USB_PERMISSION));
        if (!MyCH340.UsbFeatureSupported()) {
            Toast.makeText(this, "Your Phone does't support OTG", Toast.LENGTH_SHORT).show();
            return 1;
        }
        if (MyCH340.ResumeUsbPermission() == -2) {
            Toast.makeText(this, "No Permission", Toast.LENGTH_SHORT).show();
            return 1;
        }
        if ((MyCH340.EnumerateDevice()) == null) {
            Toast.makeText(this, "No CH340 Device", Toast.LENGTH_SHORT).show();
            return 1;
        }
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        String usbdeviceinfo = "";
        UsbDevice device = deviceIterator.next();
        UsbDeviceConnection fortestusbConnection = manager.openDevice(device);

        Serial = UsbSerialDevice.createUsbSerialDevice(device, fortestusbConnection);

        MyCH340.OpenDevice(device);
        if (MyCH340.UartInit() == false) {
            ShowInfo("Init Serial failed\n(line 218)");
            return 1;
        }

        boolean TF = MyCH340.SetConfig(BANDRATE, (byte) 8, (byte) 0, (byte) 0, (byte) 0);
        if (TF == false) {
            ShowInfo("Set Serial failed\n(line 224)");
            return 1;
        }

        usbdeviceinfo += "DeviceID: " + device.getDeviceId() + "\n";

        return 0;
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