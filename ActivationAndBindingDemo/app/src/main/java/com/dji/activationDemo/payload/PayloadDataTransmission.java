package com.dji.activationDemo.payload;

import android.util.Log;

import com.dji.activationDemo.DemoApplication;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.ViewHelper;


import dji.sdk.payload.Payload;

public class PayloadDataTransmission {
    private static final String TAG = "PayloadSendGetData";
    private Payload payload = null;
    public PayloadDataTransmission(){
        initListener();
    }
    private void initListener() {
        if (ModuleVerificationUtil.isPayloadAvailable()) {
            payload = DemoApplication.getAircraftInstance().getPayload();
        }
    }

    /**
     * @param isGripperOpen The boolean value to control the gripper open or close.
     */
    public void gripperControl(boolean isGripperOpen){
        if (isGripperOpen) {
            sendDataToPayload("y");
        } else {
            sendDataToPayload("n");
        }
    }

    /**
     * @param sendingDataStr The data to be sent to the payload device.
     */
    public void sendDataToPayload(String sendingDataStr) {
        Log.e(TAG, "sending:" + sendingDataStr);
        final byte[] data = ViewHelper.getBytes(sendingDataStr);
        if(ModuleVerificationUtil.isPayloadAvailable() && null != payload) {
            payload.sendDataToPayload(data, djiError -> Log.i(TAG, djiError == null ? "Send data successfully" : djiError.getDescription()));
        }
    }
}
