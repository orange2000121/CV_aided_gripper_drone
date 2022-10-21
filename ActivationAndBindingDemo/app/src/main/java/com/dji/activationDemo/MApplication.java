package com.dji.activationDemo;

import android.app.Application;
import android.content.Context;

import com.secneo.sdk.Helper;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.BluetoothProductConnector;

public class MApplication extends Application {


    private DemoApplication demoApplication;

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);
        if (demoApplication == null) {
            demoApplication = new DemoApplication();
            demoApplication.setContext(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        demoApplication.onCreate();
    }


}
