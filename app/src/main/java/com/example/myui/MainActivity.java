package com.example.myui;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.hardware.usb.*;
import android.content.Intent;
import android.content.Context;
import java.util.HashMap;
import java.util.Iterator;
import android.content.BroadcastReceiver;
import android.app.PendingIntent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity{
    ServerSocket serverSocket;
    Thread Thread1 = null;
    TextView tvIP, tvPort;
    TextView tvMessages;
    public static String SERVER_IP = "";
    public static final int SERVER_PORT = 8080;
    Intent intent = new Intent();
    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
                        }
                    } else {
                        //Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvIP = findViewById(R.id.tvIP);
        tvPort = findViewById(R.id.tvPort);
        tvMessages = findViewById(R.id.tvMessages);
        try {
            SERVER_IP = getLocalIpAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        Thread1 = new Thread(new Thread1());
        Thread1.start();

    }
    private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }
    public void usbTransfer(String data){
        byte[] bytes = {Byte.parseByte(data)};
        int TIMEOUT = 0;
        boolean forceClaim = true;


        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while(deviceIterator.hasNext()){
            device = deviceIterator.next();
        }

        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);
        usbManager.requestPermission(device, permissionIntent);



        UsbInterface intf = device.getInterface(0);//can use iterator on device to find interface index
        UsbEndpoint endpoint = intf.getEndpoint(1);//can use iterator in interface to find endpoint index
        UsbDeviceConnection connection = usbManager.openDevice(device);
        connection.claimInterface(intf, forceClaim);
        connection.bulkTransfer(endpoint, bytes, bytes.length, TIMEOUT);
    }
    private BufferedReader input;
    class Thread1 implements Runnable {
        String message="";
        @Override
        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvMessages.setText("Commands: ");
                        tvIP.setText("IP: " + SERVER_IP);
                        tvPort.setText("Port: " + String.valueOf(SERVER_PORT));
                    }
                });
                while(!message.equalsIgnoreCase("over")) {
                    try {
                        socket = serverSocket.accept();
                        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        message = input.readLine();
                        usbTransfer(message);
                        if (message != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tvMessages.append(message + ", ");
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}