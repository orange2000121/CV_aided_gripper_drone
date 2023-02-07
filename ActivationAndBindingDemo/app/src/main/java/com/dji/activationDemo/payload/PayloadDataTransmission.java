package com.dji.activationDemo.payload;

import android.util.Log;

import com.dji.sdk.sample.internal.controller.DJISampleApplication;
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
            payload = DJISampleApplication.getAircraftInstance().getPayload();
        }
    }

    public void gripperControl(boolean isGripperOpen){
        switch (isGripperOpen){
            case true:
                sendDataToPayload("y");
                break;
            case false:
                sendDataToPayload("n");
                break;
        }
    }

    public void sendDataToPayload(String sendingDataStr) {
        Log.e(TAG, "sending:" + sendingDataStr);
        final byte[] data = ViewHelper.getBytes(sendingDataStr);
        if(ModuleVerificationUtil.isPayloadAvailable() && null != payload) {
            payload.sendDataToPayload(data, djiError -> Log.i(TAG, djiError == null ? "Send data successfully" : djiError.getDescription()));
        }
    }
}
