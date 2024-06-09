package com.Jambox.party;

import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.jambox.monetisation.JamboxAdsHelper;

public class AppOpenManager implements LifecycleObserver
{
    private final Context context;
    private boolean isAppOpened = false;

    public AppOpenManager(final Context context)
    {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        this.context = context;
        //JamboxAdsHelper.ShowAppOpenAd();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart()
    {
        if (isAppOpened)
            JamboxAdsHelper.ShowAppOpenAd();
        else
            isAppOpened = true;
    }
}