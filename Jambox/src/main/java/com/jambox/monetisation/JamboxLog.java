package com.jambox.monetisation;

import android.util.Log;

public class JamboxLog
{
    public static void Info(String msg)
    {
        Log.i("JamboxSDK", msg);
    }

    public static void Error(String msg)
    {
        Log.e("JamboxSDK", msg);
    }

    public static void Warn(String msg)
    {
        Log.w("JamboxSDK", msg);
    }
}
