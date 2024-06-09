package com.Jambox.party;

import android.app.Application;

import com.jambox.monetisation.AdjustHelper;

public class GlobalApplication extends Application
{
    @Override
    public void onCreate() {
        super.onCreate();
        AdjustHelper.Initialize(this, "uw277yqlu1hc");
    }
}
