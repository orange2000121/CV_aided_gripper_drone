package com.dji.activationDemo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientSocket extends Activity {
    String TAG = "ClientSocket";

    private Thread thread;
    private Socket clientSocket;//客戶端的socket
    private BufferedWriter bw;  //取得網路輸出串流
    private BufferedReader br;  //取得網路輸入串流
    private String tmp;         //做為接收時的緩存
    private JSONObject jsonWrite, jsonRead; //從java伺服器傳遞與接收資料的json
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payload_testing);
//        thread=new Thread(Connection);
//        thread.start();
    }

    private Runnable connection = new Runnable() {
        @Override
        public void run() {
            try {
                Log.i(TAG, "連線中...");
                // 輸入Server端的IP
                String serverIpAddress = "192.168.1.127";
                // 自訂所使用的Port(1024 ~ 65535)
                int serverPort = 1024;

                // 建立連線
                InetAddress serverAddress = InetAddress.getByName(serverIpAddress);
                clientSocket = new Socket(serverIpAddress, serverPort);
                Log.i(TAG, "已建立連線");

                // 取得網路輸出串流
                bw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                // 取得網路輸入串流
                br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // 傳送訊息給Server端
                sendMessageToServer("Hello, Server!");

                // 接收來自Server端的回應
                String response = br.readLine();
                Log.i(TAG, "收到回應: " + response);

                // 確認是否連線成功後，可關閉Socket
                closeConnection();
            }catch (Exception e){
                e.printStackTrace();
                Log.e("text","Socket連線="+e.toString());
            }
        }
    };
    public void sendMessageToServer(String message) throws IOException {
        bw.write(message + "\n");
        bw.flush();
    }
    public void startConnection(){
        thread=new Thread(connection);
        thread.start();
    }
    public void closeConnection() {
        try {
            if (br != null) br.close();
            if (bw != null) bw.close();
            if (clientSocket != null) clientSocket.close();
            Log.i(TAG, "連線已關閉");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            //傳送離線 Action 給 Server 端
            jsonWrite = new JSONObject();
            jsonWrite.put("action","離線");

            //寫入
            bw.write(jsonWrite + "\n");
            //立即發送
            bw.flush();

            //關閉輸出入串流後,關閉Socket
            bw.close();
            br.close();
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}