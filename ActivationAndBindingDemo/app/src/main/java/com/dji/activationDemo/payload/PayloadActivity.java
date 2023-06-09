package com.dji.activationDemo.payload;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.dji.activationDemo.R;
import com.dji.activationDemo.DemoApplication;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.utils.ViewHelper;

import dji.sdk.payload.Payload;

public class PayloadActivity extends AppCompatActivity implements View.OnClickListener{
    private final String TAG = "PayloadActivity";
    private TextView pushUARTTextView;
    private TextView payloadNameView;
    private TextView pushUDPTextView;
    private Payload payload = null;
    private String payloadName = "";
    private PayloadDataTransmission dataTransmission = null;
    private boolean usePayload = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_payload_testing);
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        payloadNameView = (TextView) findViewById(R.id.payload_name);
        pushUARTTextView = (TextView) findViewById(R.id.push_info_text_UART);
        pushUDPTextView = (TextView) findViewById(R.id.push_info_text_UDP);
        pushUARTTextView.setMovementMethod(new ScrollingMovementMethod());

        dataTransmission = new PayloadDataTransmission(this);
        initListener();
    }

    private void initListener() {
        findViewById(R.id.sent_data).setOnClickListener(this);
        findViewById(R.id.btn_trigger_status).setOnClickListener(this);
        findViewById(R.id.btn_location).setOnClickListener(this);
        findViewById(R.id.btn_open_gripper).setOnClickListener(this);
        findViewById(R.id.btn_close_gripper).setOnClickListener(this);
        findViewById(R.id.btn_get_circle).setOnClickListener(this);
        findViewById(R.id.btn_stop_circle).setOnClickListener(this);
        findViewById(R.id.btn_grip).setOnClickListener(this);
        if (ModuleVerificationUtil.isPayloadAvailable()) {
            if(usePayload){
                payload = DemoApplication.getAircraftInstance().getPayload();
            }

            /**
             *  Gets the product name defined by the manufacturer of the payload device.
             */
            if(usePayload){
                payloadName = payload.getPayloadProductName();
            }
            payloadNameView.setText("Payload Name:" + (TextUtils.isEmpty(payloadName) ? "N/A" : payloadName));
            payloadNameView.invalidate();

            /**
             *  Set the command data callback, the command data typically sent by payload in UART/CAN channel, the max bandwidth of this channel is 3KBytes/s on M200.
             */
            if(usePayload){
                payload.setCommandDataCallback(new Payload.CommandDataCallback() {
                    @Override
                    public void onGetCommandData(byte[] bytes) {
                        String str = ViewHelper.getStringUTF8(bytes, 0, bytes.length);
                        Log.i("PayloadActivity", "onGetCommandData: " + str);
                        updateUARTPushData(str);
                    }
                });
            }


            /**
             *  Set the UDP data callback, this callback is for receiving the Non-Video data in UDP channel, the max bandwidth of this channel is 8Mbps in M200, 4Mbps in M210
             */
            if(usePayload) {
                payload.setStreamDataCallback(new Payload.StreamDataCallback() {
                    @Override
                    public void onGetStreamData(byte[] bytes, int i) {
                        String str = ViewHelper.getStringUTF8(bytes, 0, i);
                        updateUDPPushData(str);
                    }
                });
            }
        }
    }

    private void updateUARTPushData(final String str) {
        if (pushUARTTextView != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pushUARTTextView.setText(str + "\n");
                    pushUARTTextView.invalidate();
                }
            });
        }
    }

    private void updateUDPPushData(final String str) {
        if (pushUDPTextView != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pushUDPTextView.setText(str + "\n");
                    pushUDPTextView.invalidate();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (ModuleVerificationUtil.isPayloadAvailable()) {
            if (null != payload) {
                payload.setCommandDataCallback(null);
                payload.setStreamDataCallback(null);
            }
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sent_data:
                Intent intent = new Intent(this, PayloadSendGetDataActivity.class);
                this.startActivity(intent);
                finish();
                break;
            case R.id.btn_location:
                if(dataTransmission.payload!=null){
                    if(!dataTransmission.payload.isFeatureOpened()){
                        Log.i(TAG, "payload feature is not opened");
                        break;
                    }else {
                        Log.i(TAG, "payload feature is opened");
                    }
                }
                Float[] location = dataTransmission.getBottomLocation();
                if(location != null) {
                    ToastUtils.showToast(location[6] + ": " + location[0] + ", " + location[1] + ", " + location[2]);
                }else{
                    ToastUtils.showToast("location is null");
                }
                break;
            case R.id.btn_open_gripper:
                dataTransmission.gripperControl(true);
                break;
            case R.id.btn_close_gripper:
                dataTransmission.gripperControl(false);
                break;
            case R.id.btn_get_circle:
                Float[] circle = dataTransmission.getCircleLocation();
                if(circle != null) {
                    ToastUtils.showToast("circle: " + circle[0] + ", " + circle[1]);
                }else{
                    ToastUtils.showToast("circle is null");
                }
                break;
            case R.id.btn_stop_circle:
                dataTransmission.stopFindCircleLocation();
                break;
            case R.id.btn_grip:
                dataTransmission.startGripBall();
                break;
            case R.id.btn_trigger_status:
                boolean status = dataTransmission.getGripStatus();
                ToastUtils.showToast("grip status: " + status);
            default:
        }
    }
}
