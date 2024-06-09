package com.Jambox.party;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.jambox.monetisation.AdjustHelper;
import com.jambox.monetisation.JamboxAdsHelper;
import com.jambox.monetisation.JamboxGameKeys;
import com.jambox.monetisation.OnJamboxAdInitializeListener;
import com.jambox.monetisation.OnRewardedAdListener;
import com.jambox.monetisation.WebviewObject;

public class MainActivity extends AppCompatActivity {

    private WebviewObject webview = null;
    private AppOpenManager appOpenManager;
    private Context context;
    private String interstitialId = "0ee55073fd46cb13";
    private String rewardedId = "7d64a59befe5cef9";
    private String bannerId = "ba924c1fc44d29ac";
    private String appOpenId = "fce5b3d0bbba9df0";
    private String nativeId = "8d9bec8b94279ed6";

    private String h5ClientId = "9285717016";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        context = this;
        findViewById(R.id.main).setVisibility(View.GONE);
        //Test app open event
        AdjustHelper.TrackEvent("svtdhn");

        //Ad initialization
        JamboxAdsHelper.InitializeAds(this, interstitialId, rewardedId, bannerId,getString(R.string.sdk_key_applovin), new OnJamboxAdInitializeListener()
                {
                    @Override
                    public void OnJamboxAdsInitialized()
                    {
                        findViewById(R.id.main).setVisibility(View.VISIBLE);

                        //Initializing native
                        JamboxAdsHelper.InitializeNativeAd(nativeId);

                        //Initializing App Open Ad
                        JamboxAdsHelper.InitializeAppOpenAds(appOpenId);
                        appOpenManager = new AppOpenManager(context);
                    }
                });

        SetButtonListeners();

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true)
        {
            @Override
            public void handleOnBackPressed()
            {
                CloseWebview();
            }
        });
    }

    void SetButtonListeners()
    {
        Button startBtn = findViewById(R.id.btn_start);
        startBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                StartWebview();
            }
        });

        Button rw_btn = findViewById(R.id.btn_rw);
        rw_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ShowRW();
            }
        });

        Button is_btn = findViewById(R.id.btn_is);
        is_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ShowIS();
            }
        });

        Button banner_show = findViewById(R.id.btn_banner_show);
        banner_show.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ShowBanner();
            }
        });

        Button banner_hide = findViewById(R.id.btn_banner_hide);
        banner_hide.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                HideBanner();
            }
        });

        Button native_btn_small = findViewById(R.id.btn_native_small);
        native_btn_small.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                findViewById(R.id.native_ad_small).setVisibility(View.VISIBLE);
                JamboxAdsHelper.ShowNativeAd(findViewById(R.id.native_ad_small), JamboxAdsHelper.NativeAdTemplate.SMALL);
            }
        });

        Button native_btn_medium = findViewById(R.id.btn_native_medium);
        native_btn_medium.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                findViewById(R.id.native_ad_small).setVisibility(View.GONE);
                JamboxAdsHelper.ShowNativeAd(findViewById(R.id.native_ad_medium), JamboxAdsHelper.NativeAdTemplate.MEDIUM);
            }
        });

        Button native_btn_hide = findViewById(R.id.btn_native_hide);
        native_btn_hide.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ((FrameLayout)findViewById(R.id.native_ad_small)).removeAllViews();
                ((FrameLayout)findViewById(R.id.native_ad_medium)).removeAllViews();
                JamboxAdsHelper.HideNativeAd();
            }
        });

        Button mediation_btn = findViewById(R.id.btn_mediation);
        mediation_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                JamboxAdsHelper.ShowMediationDebugger();
            }
        });
    }

    void StartWebview()
    {
        if (webview != null)
            return;

        webview = new WebviewObject(this, h5ClientId);
        //FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        //layoutParams.setMargins(0, 300, 0, 0);
        //webview = new WebviewObject(this, h5ClientId, layoutParams);

        //webview.StartWebview();
        webview.StartWebviewGame(JamboxGameKeys.flip_jump);
    }

    void CloseWebview()
    {
        if (webview == null)
            return;

        webview.CloseWebview();
        webview = null;
    }

    void ShowBanner()
    {
        JamboxAdsHelper.ShowBannerAd(JamboxAdsHelper.BannerPosition.TOP);
    }

    void HideBanner()
    {
        JamboxAdsHelper.HideBannerAd();
    }

    void ShowRW()
    {
        JamboxAdsHelper.ShowRewarded(new OnRewardedAdListener()
        {
            @Override
            public void OnAdDisplayFailed() { }
            @Override
            public void OnAdDisplayed() { }
            @Override
            public void OnAdCompleted()
            {
                System.out.println("User Rewarded");
            }
            @Override
            public void OnAdHidden() { }
        });
    }

    void ShowIS()
    {
        JamboxAdsHelper.ShowRewarded(null);
    }

    protected void onResume() {
        super.onResume();
        AdjustHelper.onResume();
    }
    protected void onPause() {
        super.onPause();
        AdjustHelper.onPause();
    }

}