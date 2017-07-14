package cn.bertsir.controlservice;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.Utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private TextView tv_ip;
    private BufferedReader in;
    private ServerSocket server;
    private Socket socket;
    private int port = 5210;

    private String ex;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    //todo 发送图片
                    Log.e(TAG, "handleMessage: 发送图片");
                    try {
                        sendData();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "handleMessage: 发送图片出错" + e.toString());
                    }
                    break;
                case 2:
                    execShell("screencap -p /sdcard/1.png");
                    break;
            }
        }
    };
    private DataOutputStream dos;
    private FileInputStream fis;
    private TextView tv_client;
    private EditText et_log;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.init(this);
        initView();

        new Thread(new Runnable() {
            @Override
            public void run() {
                startSocket();
            }
        }).start();
        mHandler.sendEmptyMessageDelayed(2, 10000);
    }


    private void startSocket() {
        try {
            server = null;
            try {
                server = new ServerSocket(port);
                System.out.println("服务器启动成功");
            } catch (Exception e) {
                System.out.println("没有启动监听：" + e.toString());
            }
            socket = null;
            try {
                socket = server.accept();
                final String hostAddress = socket.getInetAddress().getHostAddress();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_client.setText(tv_client.getText().toString() + hostAddress + "已连接");
                    }
                });
            } catch (Exception e) {
                System.out.println("Error." + e);
            }

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            while (true) {
                ex = in.readLine();
                System.out.println("client1:" + ex);
                if (ex != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            execShell(ex);
                            ex = null;
                        }
                    });
                }
            }

        } catch (Exception e) {//出错，打印出错信息
            System.out.println("Error." + e);
        }
    }

    private void stopSocket() throws IOException {
        dos.close();
        fis.close();
        in.close(); //关闭Socket输入流
        socket.close(); //关闭Socket
        server.close(); //关闭ServerSocket
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            stopSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initView() {
        tv_ip = (TextView) findViewById(R.id.tv_ip);
        tv_ip.setText(tv_ip.getText().toString() + NetworkUtils.getIPAddress(true) + ":" + port);
        tv_client = (TextView) findViewById(R.id.tv_client);

        et_log = (EditText) findViewById(R.id.et_log);

    }


    private void execShell(String exec) {
        Log.e(TAG, "execShell: 执行：" + stampToDate(String.valueOf(System.currentTimeMillis()))+":"+exec);
        if(!exec.contains("screencap")){
            String log = et_log.getText().toString();
            et_log.setText(log+"\n"+stampToDate(String.valueOf(System.currentTimeMillis()))+":"+exec);
        }
        try {
            // 申请获取root权限，这一步很重要，不然会没有作用
            Process process = Runtime.getRuntime().exec("su");
            // 获取输出流
            OutputStream outputStream = process.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(
                    outputStream);
            if (exec.contains("screencap -p /sdcard/1.png")) {
                mHandler.sendEmptyMessageDelayed(1, 5000);
            }
            dataOutputStream.writeBytes(exec);
            dataOutputStream.flush();
            dataOutputStream.close();
            Log.e(TAG, "execShell: 执行完成");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "execShell: " + e.toString());
        }
    }

    private void sendData() throws IOException {
        if (socket != null) {
            dos = new DataOutputStream(socket.getOutputStream());
            fis = new FileInputStream("/sdcard/1.png");
            int size = fis.available();
            System.out.println("size = " + size);
            if (size > 100) {
                byte[] data = new byte[size];
                fis.read(data);
                dos.writeInt(size);
                dos.write(data);
                dos.flush();
            }
            mHandler.sendEmptyMessageDelayed(2, 1000);
        }
    }


    public  String stampToDate(String s){
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        long lt = new Long(s);
        Date date = new Date(lt);
        res = simpleDateFormat.format(date);
        return res;
    }


}
