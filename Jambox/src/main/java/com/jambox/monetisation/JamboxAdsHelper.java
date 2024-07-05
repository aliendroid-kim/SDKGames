package com.jambox.monetisation;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.MaxRewardedAdListener;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.mediation.ads.MaxAppOpenAd;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.applovin.mediation.ads.MaxRewardedAd;
import com.applovin.mediation.nativeAds.MaxNativeAdListener;
import com.applovin.mediation.nativeAds.MaxNativeAdLoader;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinPrivacySettings;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkSettings;
import com.applovin.sdk.AppLovinSdkUtils;
import com.google.android.gms.appset.AppSet;
import com.google.android.gms.appset.AppSetIdClient;
import com.google.android.gms.appset.AppSetIdInfo;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class JamboxAdsHelper
{

    public static boolean IsInitialized;
    private static boolean IsInitializeCalled;
    private static Context context;

    public static String jamboxKey = "T7PPns0K6JV00uGv0ZAEKsTWrpwA-N4Hchi_KKecaqTa_U5zQcyyoI_pTcC5TM1OgfrLz5dWGdASKWgK6l5Sks";
    private static String applovinKey = "";
    public static int statusBarPadding;
    public static int navigationBarPadding;

    private static OnJamboxAdInitializeListener listener;

    //region INITIALIZE
    public static void InitializeAds(Context context, String interstitialId, String rewardedId, String bannerId)
    {
        InitializeAds(context, interstitialId, rewardedId, bannerId, null);
    }

    public static void InitializeAds(Context context, String interstitialId, String rewardedId,
                                     String bannerId, OnJamboxAdInitializeListener listener)
    {
        InitializeAds(context, interstitialId, rewardedId, bannerId, listener, false);
    }

    public static void InitializeAds(Context context, String interstitialId, String rewardedId,
                                     String bannerId, OnJamboxAdInitializeListener listener, boolean testMode)
    {
        if (IsInitializeCalled)
            return;

        JamboxLog.Info("Initializing Ads...");
        //Getting the status and navigation bar paddings
        View emptyView = new View(context);
        ViewCompat.setOnApplyWindowInsetsListener(emptyView, (v, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            statusBarPadding = statusBars.top;
            navigationBarPadding = navigationBars.bottom;

            ViewGroup parent =  (ViewGroup) v.getParent();
            parent.removeView(v);
            return insets;
        });
        ((Activity)context).addContentView(emptyView, new FrameLayout.LayoutParams(0, 0));

        ApplicationInfo applicationInfo = null;
        try
        {
            applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            JamboxLog.Error("Ads Initialization failed : " + e);
            JamboxLog.Error("Please make sure that the Applovin SDK Key is properly set");
            return;
        }
        applovinKey = applicationInfo.metaData.getString("applovin.sdk.key");

        JamboxAdsHelper.context = context;
        if (!IsSdkKeyValid())
            return;

        JamboxAdsHelper.interstitialId = interstitialId;
        JamboxAdsHelper.rewardedId = rewardedId;
        JamboxAdsHelper.bannerId = bannerId;
        JamboxAdsHelper.listener = listener;
        IsInitializeCalled = true;

        if (!testMode)
        {
            InitApplovin(false, "");
            return;
        }

        //In test mode, we fetch the gaid and then init applovin
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                AppSetIdClient client = AppSet.getClient(context);
                Task<AppSetIdInfo> info = client.getAppSetIdInfo();
                try
                {
                    Tasks.await(info);
                    InitApplovin(true, info.getResult().getId());
                }
                catch (ExecutionException | InterruptedException e)
                {
                    throw new RuntimeException(e);
                }
                return info.getResult().getId();
            }
        };
        task.execute();
    }

    static void InitApplovin(boolean testMode, String appSetID)
    {
        AppLovinSdk sdk;
        if (testMode)
        {
            JamboxLog.Info("Applovin Init Test Device AAID : " + appSetID);
            AppLovinSdkSettings settings = new AppLovinSdkSettings( context );
            settings.setTestDeviceAdvertisingIds(Arrays.asList(appSetID));
            sdk = AppLovinSdk.getInstance(settings, context);
        }
        else
        {
            sdk = AppLovinSdk.getInstance(context);
        }

        sdk.setMediationProvider("max");
        sdk.initializeSdk(new AppLovinSdk.SdkInitializationListener() {
            @Override
            public void onSdkInitialized(AppLovinSdkConfiguration appLovinSdkConfiguration) {
                //SDK Initialized
                JamboxLog.Info("Ads Initialized");
                JamboxLog.Info("Loading Ads...");
                IsInitialized = true;
                InitializeInterstitial(context);
                InitializeRewarded(context);
                if (listener != null)
                {
                    listener.OnJamboxAdsInitialized();
                }
            }
        });
    }
    //endregion

    private static boolean IsSdkKeyValid()
    {
        if (applovinKey.equals(jamboxKey))
        {
            return true;
        }
        else
        {
            if (context != null)
                Toast.makeText(context, "Please use the applovin SDK key provided by Jambox Games", Toast.LENGTH_SHORT).show();
            JamboxLog.Error("Ads Initialization failed : Please use the applovin SDK key provided by Jambox Games");
            return false;
        }
    }

    //region INTERSTITIAL
    private static String interstitialId;
    private static MaxInterstitialAd interstitialAd;
    private static OnInterstitialAdListener interstitialAdListener;
    private static boolean IsInvalidInterstitialId = false;
    private static int interstitialRetryAttempt = 0;
    private static void InitializeInterstitial(Context context)
    {
        interstitialAd = new MaxInterstitialAd( interstitialId, (Activity) context );
        interstitialAd.setListener(new MaxAdListener()
        {
            @Override
            public void onAdLoaded(@NonNull MaxAd maxAd)
            {
                // Interstitial ad is ready to be shown. interstitialAd.isReady() will now return 'true'
                // Reset retry attempt
                interstitialRetryAttempt = 0;
            }

            @Override
            public void onAdDisplayed(@NonNull MaxAd maxAd)
            {
                if (interstitialAdListener != null)
                {
                    interstitialAdListener.OnAdDisplayed();
                }
            }

            @Override
            public void onAdHidden(@NonNull MaxAd maxAd)
            {
                // Interstitial ad is hidden. Pre-load the next ad
                interstitialAd.loadAd();
                if (interstitialAdListener != null)
                {
                    interstitialAdListener.OnAdHidden();
                }
            }

            @Override
            public void onAdClicked(@NonNull MaxAd maxAd) { }

            @Override
            public void onAdLoadFailed(@NonNull String s, @NonNull MaxError maxError)
            {
                JamboxLog.Error("Interstitial Load Failed: " + maxError.getMessage());
                if (maxError.getCode() == -5603)
                {
                    IsInvalidInterstitialId = true;
                    ShowInvalidInterstitialIDAlert();
                    return;
                }

                // Interstitial ad failed to load
                // AppLovin recommends that you retry with exponentially higher delays up to a maximum delay (in this case 64 seconds)
                interstitialRetryAttempt++;
                long delayMillis = TimeUnit.SECONDS.toMillis( (long) Math.pow( 2, Math.min( 6, interstitialRetryAttempt ) ) );
                new Handler().postDelayed( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        interstitialAd.loadAd();
                    }
                }, delayMillis );
            }

            @Override
            public void onAdDisplayFailed(@NonNull MaxAd maxAd, @NonNull MaxError maxError)
            {
                JamboxLog.Error("Interstitial Display Failed: " + maxError.getMessage());
                // Interstitial ad failed to display. AppLovin recommends that you load the next ad.
                interstitialAd.loadAd();
                if (interstitialAdListener != null)
                {
                    interstitialAdListener.OnAdDisplayFailed();
                }
            }
        });

        interstitialAd.loadAd();
    }

    public static void ShowInterstitial(OnInterstitialAdListener _interstitialAdListener)
    {
        if (!IsSdkKeyValid()) return;
        if (!IsInitialized)
        {
            JamboxLog.Warn("Make sure that the SDK is initialized before trying to show ads...");
            return;
        }

        //Checking for invalid placement Id
        if (IsInvalidInterstitialId)
        {
            ShowInvalidInterstitialIDAlert();
            return;
        }

        interstitialAdListener = null;
        if (interstitialAd.isReady())
        {
            interstitialAdListener = _interstitialAdListener;
            interstitialAd.showAd();
        }
        else
        {
            JamboxLog.Error("Interstitial Ad is not ready");
        }
    }

    static void ShowInvalidInterstitialIDAlert()
    {
        if (context != null)
            Toast.makeText(context, "Please double-check the placement Id for Interstitial Ads", Toast.LENGTH_SHORT).show();
        JamboxLog.Error("Please double-check the placement Id for Interstitial Ads");
    }
    //endregion

    //region REWARDED
    private static String rewardedId;
    private static MaxRewardedAd rewardedAd;
    private static OnRewardedAdListener rewardedAdListener;
    private static boolean IsInvalidRewardedId = false;
    private static int rewardedRetryAttempt = 0;
    private static void InitializeRewarded(Context context)
    {
        rewardedAd = MaxRewardedAd.getInstance( rewardedId, (Activity) context );
        rewardedAd.setListener(new MaxRewardedAdListener()
        {
            @Override
            public void onUserRewarded(@NonNull MaxAd maxAd, @NonNull MaxReward maxReward)
            {
                // Rewarded ad was displayed and user should receive the reward
                if (rewardedAdListener != null)
                {
                    rewardedAdListener.OnAdCompleted();
                }
            }

            @Override
            public void onAdLoaded(@NonNull MaxAd maxAd)
            {
                // Rewarded ad is ready to be shown. rewardedAd.isReady() will now return 'true'
                // Reset retry attempt
                rewardedRetryAttempt = 0;
            }

            @Override
            public void onAdDisplayed(@NonNull MaxAd maxAd) { }

            @Override
            public void onAdHidden(@NonNull MaxAd maxAd)
            {
                // rewarded ad is hidden. Pre-load the next ad
                rewardedAd.loadAd();
                if (rewardedAdListener != null)
                {
                    rewardedAdListener.OnAdHidden();
                }
            }

            @Override
            public void onAdClicked(@NonNull MaxAd maxAd) { }

            @Override
            public void onAdLoadFailed(@NonNull String s, @NonNull MaxError maxError)
            {
                JamboxLog.Error("Rewarded Load Failed: " + maxError.getMessage());
                if (maxError.getCode() == -5603)
                {
                    IsInvalidRewardedId = true;
                    ShowInvalidRewardedIDAlert();
                    return;
                }

                // Rewarded ad failed to load
                // AppLovin recommends that you retry with exponentially higher delays up to a maximum delay (in this case 64 seconds)
                rewardedRetryAttempt++;
                long delayMillis = TimeUnit.SECONDS.toMillis( (long) Math.pow( 2, Math.min( 6, rewardedRetryAttempt ) ) );
                new Handler().postDelayed( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        rewardedAd.loadAd();
                    }
                }, delayMillis );
            }

            @Override
            public void onAdDisplayFailed(@NonNull MaxAd maxAd, @NonNull MaxError maxError)
            {
                JamboxLog.Error("Rewarded Display Failed: " + maxError.getMessage());
                // Rewarded ad failed to display. AppLovin recommends that you load the next ad.
                rewardedAd.loadAd();
                if (rewardedAdListener != null)
                {
                    rewardedAdListener.OnAdDisplayFailed();
                }
            }
        });
        rewardedAd.loadAd();
    }

    public static void ShowRewarded(OnRewardedAdListener _rewardedAdListener)
    {
        if (!IsSdkKeyValid()) return;

        if (!IsInitialized) {
            JamboxLog.Warn("Make sure that the SDK is initialized before trying to show ads...");
            if(_rewardedAdListener != null) _rewardedAdListener.OnAdDisplayFailed();
            return;
        };

        //Checking for invalid placement Id
        if (IsInvalidRewardedId)
        {
            ShowInvalidRewardedIDAlert();
            return;
        }

        rewardedAdListener = null;
        if (rewardedAd.isReady())
        {
            rewardedAdListener = _rewardedAdListener;
            rewardedAd.showAd();
        }
        else
        {
            JamboxLog.Error("Rewarded Ad is not ready");
            if(_rewardedAdListener != null) _rewardedAdListener.OnAdDisplayFailed();
        }
    }

    static void ShowInvalidRewardedIDAlert()
    {
        if (context != null)
            Toast.makeText(context, "Please double-check the placement Id for Rewarded Ads", Toast.LENGTH_SHORT).show();
        JamboxLog.Error("Please double-check the placement Id for Rewarded Ads");
    }
    //endregion

    //region BANNER
    private static String bannerId;
    private static MaxAdView bannerAdView;
    private static boolean IsInvalidBannerId = false;
    public static void ShowBannerAd(BannerPosition position)
    {
        if (!IsSdkKeyValid()) return;
        if (!IsInitialized)
        {
            JamboxLog.Warn("Make sure that the SDK is initialized before trying to show ads...");
            return;
        }

        if (bannerAdView != null && bannerAdView.isShown())
        {
            JamboxLog.Warn("Banner Ad is already being shown");
            return;
        }

        if (IsInvalidBannerId)
        {
            ShowInvalidBannerIDAlert();
            return;
        }

        bannerAdView = new MaxAdView(bannerId, context);
        bannerAdView.setListener(new MaxAdViewAdListener()
        {
            @Override
            public void onAdExpanded(@NonNull MaxAd maxAd) { }
            @Override
            public void onAdCollapsed(@NonNull MaxAd maxAd) { }
            @Override
            public void onAdLoaded(@NonNull MaxAd maxAd) { }
            @Override
            public void onAdDisplayed(@NonNull MaxAd maxAd) { }
            @Override
            public void onAdHidden(@NonNull MaxAd maxAd) { }
            @Override
            public void onAdClicked(@NonNull MaxAd maxAd) { }
            @Override
            public void onAdLoadFailed(@NonNull String s, @NonNull MaxError maxError) {
                HideBannerAd();
                JamboxLog.Error("Banner Load Failed: " + maxError.getMessage());
                if (maxError.getCode() == -5603)
                {
                    IsInvalidBannerId = true;
                    ShowInvalidBannerIDAlert();
                }
            }
            @Override
            public void onAdDisplayFailed(@NonNull MaxAd maxAd, @NonNull MaxError maxError) {
                HideBannerAd();
                JamboxLog.Error("Banner Display Failed: " + maxError.getMessage());
            }
        });

        // Stretch to the width of the screen for banners to be fully functional
        int width = ViewGroup.LayoutParams.MATCH_PARENT;

        // Get the adaptive banner height.
        int heightDp = MaxAdFormat.BANNER.getAdaptiveSize( (Activity) context ).getHeight();
        int heightPx = AppLovinSdkUtils.dpToPx( context, heightDp );

        if (position == BannerPosition.TOP)
        {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams( width, heightPx);
            params.topMargin += statusBarPadding;
            params.gravity = Gravity.TOP;
            bannerAdView.setLayoutParams(params);
        }
        else
        {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams( width, heightPx);
            params.bottomMargin += navigationBarPadding;
            params.gravity = Gravity.BOTTOM;
            bannerAdView.setLayoutParams(params);

            //bannerAdView.setLayoutParams( new FrameLayout.LayoutParams( width, heightPx, Gravity.BOTTOM) );
        }
        bannerAdView.setExtraParameter( "adaptive_banner", "true" );
        bannerAdView.setLocalExtraParameter( "adaptive_banner_width", 400 );
        bannerAdView.getAdFormat().getAdaptiveSize( 400, context ).getHeight(); // Set your ad height to this value

        // Set background or background color for banners to be fully functional
        bannerAdView.setBackgroundColor(0);

        ViewGroup rootView = ((Activity) context).findViewById( android.R.id.content );
        rootView.addView( bannerAdView );

        // Load the ad
        bannerAdView.loadAd();

    }

    public static void ShowBannerAd(RelativeLayout layoutAds)
    {
        if (!IsSdkKeyValid()) return;
        if (!IsInitialized) return;

        if (bannerAdView != null && bannerAdView.isShown())
            return;

        bannerAdView = new MaxAdView(bannerId, context);
        bannerAdView.setListener(new MaxAdViewAdListener()
        {
            @Override
            public void onAdExpanded(@NonNull MaxAd maxAd) { }
            @Override
            public void onAdCollapsed(@NonNull MaxAd maxAd) { }
            @Override
            public void onAdLoaded(@NonNull MaxAd maxAd) { }
            @Override
            public void onAdDisplayed(@NonNull MaxAd maxAd) {

            }
            @Override
            public void onAdHidden(@NonNull MaxAd maxAd) { }
            @Override
            public void onAdClicked(@NonNull MaxAd maxAd) { }
            @Override
            public void onAdLoadFailed(@NonNull String s, @NonNull MaxError maxError) {
                layoutAds.setVisibility(View.GONE);
            }
            @Override
            public void onAdDisplayFailed(@NonNull MaxAd maxAd, @NonNull MaxError maxError) { }
        });

        bannerAdView.setBackgroundColor(0);
        layoutAds.addView( bannerAdView );

        // Load the ad
        bannerAdView.loadAd();

    }

    public static void HideBannerAd()
    {
        if (bannerAdView == null)
            return;

        AppLovinSdkUtils.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                bannerAdView.destroy();
                bannerAdView = null;
            }
        });
    }

    public static boolean IsShowingBanner()
    {
        return bannerAdView != null && bannerAdView.isShown();
    }

    static void ShowInvalidBannerIDAlert()
    {
        if (context != null)
            Toast.makeText(context, "Please double-check the placement Id for Banner Ads", Toast.LENGTH_SHORT).show();
        JamboxLog.Error("Please double-check the placement Id for Banner Ads");
    }

    public static int GetBannerHeightInPx()
    {
        int dp = MaxAdFormat.BANNER.getAdaptiveSize( (Activity) context ).getHeight();
        return AppLovinSdkUtils.dpToPx(context, dp);
    }
    //endregion

    //region NATIVE
    private static String nativeId;
    private static MaxNativeAdLoader nativeAdLoader;
    private static MaxAd nativeAd;
    private static boolean IsInvalidNativeId = false;

    public static void InitializeNativeAd(String nativeId)
    {
        if (!IsSdkKeyValid()) return;
        JamboxAdsHelper.nativeId = nativeId;
    }


    public static void ShowNativeAd(FrameLayout frameLayout, NativeAdTemplate template)
    {
        if (!IsSdkKeyValid()) return;
        if (!IsInitialized)
        {
            JamboxLog.Warn("Make sure that the SDK is initialized before trying to show ads...");
            return;
        }

        if (nativeId == null || nativeId.isEmpty())
        {
            JamboxLog.Warn("Native ID should not be empty");
            return;
        }

        if (IsInvalidNativeId)
        {
            ShowInvalidNativeIDAlert();
            return;
        }

        nativeAdLoader = new MaxNativeAdLoader( nativeId, context );
        nativeAdLoader.setNativeAdListener( new MaxNativeAdListener()
        {
            @Override
            public void onNativeAdLoaded(final MaxNativeAdView nativeAdView, final MaxAd ad)
            {
                // Clean up any pre-existing native ad to prevent memory leaks.
                if ( nativeAd != null )
                {
                    nativeAdLoader.destroy( nativeAd );
                }

                // Save ad for cleanup.
                nativeAd = ad;

                // Add ad view to view.
                frameLayout.removeAllViews();
                frameLayout.addView( nativeAdView );
            }

            @Override
            public void onNativeAdLoadFailed(final String adUnitId, final MaxError error)
            {
                JamboxLog.Error("Native Load Failed: " + error.getMessage());
                if (error.getCode() == -5603)
                {
                    IsInvalidNativeId = true;
                    ShowInvalidNativeIDAlert();
                }

                // We recommend retrying with exponentially higher delays up to a maximum delay
            }

            @Override
            public void onNativeAdClicked(final MaxAd ad)
            {
                // Optional click callback
            }
        } );

        nativeAdLoader.loadAd(new MaxNativeAdView( (template == NativeAdTemplate.SMALL) ?
                "small_template_1" : "medium_template_1", context));
    }

    public static void HideNativeAd()
    {
        if ( nativeAd != null )
        {
            nativeAdLoader.destroy( nativeAd );
        }
    }

    static void ShowInvalidNativeIDAlert()
    {
        if (context != null)
            Toast.makeText(context, "Please double-check the placement Id for Native Ads", Toast.LENGTH_SHORT).show();
        JamboxLog.Error("Please double-check the placement Id for Native Ads");
    }
    //endregion

    //region APP OPEN
    private static MaxAppOpenAd appOpenAd;
    private static String appOpenId;
    private static boolean IsInvalidAppOpenId = false;
    private static boolean isAppOpenLoading;
    private static boolean showAppOpenOnLoad;

    public static void InitializeAppOpenAds(String appOpenAId)
    {
        if (!IsSdkKeyValid()) return;
        JamboxAdsHelper.appOpenId = appOpenAId;
        appOpenAd = new MaxAppOpenAd( appOpenAId, context);
        appOpenAd.setListener(new MaxAdListener()
        {
            @Override
            public void onAdLoaded(@NonNull MaxAd maxAd)
            {
                isAppOpenLoading = false;
                if (showAppOpenOnLoad)
                {
                    ShowAppOpenAd();
                    showAppOpenOnLoad = false;
                }
            }
            @Override
            public void onAdDisplayed(@NonNull MaxAd maxAd) { }
            @Override
            public void onAdHidden(@NonNull MaxAd maxAd)
            {
                isAppOpenLoading = true;
                appOpenAd.loadAd();
            }
            @Override
            public void onAdClicked(@NonNull MaxAd maxAd) { }
            @Override
            public void onAdLoadFailed(@NonNull String s, @NonNull MaxError maxError)
            {
                JamboxLog.Error("AppOpenAd Load Failed: " + maxError.getMessage());
                if (maxError.getCode() == -5603)
                {
                    IsInvalidAppOpenId = true;
                    ShowInvalidAppOpenIDAlert();
                }

                isAppOpenLoading = false;
                showAppOpenOnLoad = false;
            }
            @Override
            public void onAdDisplayFailed(@NonNull MaxAd maxAd, @NonNull MaxError maxError)
            {
                JamboxLog.Error("AppOpenAd Display Failed: " + maxError.getMessage());
                isAppOpenLoading = true;
                appOpenAd.loadAd();
            }
        });

        isAppOpenLoading = true;
        appOpenAd.loadAd();
    }

    public static void ShowAppOpenAd()
    {
        if (!IsSdkKeyValid()) return;
        if (appOpenAd == null || !IsInitialized)
        {
            JamboxLog.Warn("Make sure that the SDK and AppOpen Ad is initialized before trying to show ads...");
            return;
        }

        //Checking for invalid placement Id
        if (IsInvalidAppOpenId)
        {
            ShowInvalidAppOpenIDAlert();
            return;
        }

        if (appOpenAd.isReady())
        {
            appOpenAd.showAd(appOpenId);
        }
        else
        {
            if (isAppOpenLoading)
            {
                showAppOpenOnLoad = true;
            }
            else
            {
                isAppOpenLoading = true;
                appOpenAd.loadAd();
            }
        }
    }

    static void ShowInvalidAppOpenIDAlert()
    {
        if (context != null)
            Toast.makeText(context, "Please double-check the placement Id for AppOpen Ads", Toast.LENGTH_SHORT).show();
        JamboxLog.Error("Please double-check the placement Id for AppOpen Ads");
    }

    public enum NativeAdTemplate
    {
        SMALL,
        MEDIUM
    }
    //endregion

    public static void ShowMediationDebugger()
    {
        if (!IsSdkKeyValid()) return;
        AppLovinSdk.getInstance(context).showMediationDebugger();
    }

    public static class AdsCode
    {
        public static final int BEFORE_ADS_SHOWN = 100;
        public static final int ADS_DISMISSED = 110;
        public static final int ADS_WATCH_SUCCESS = 200;
        public static final int ADS_VIEWED_OR_DISMISSED = 201;
        public static final int ADS_NOT_SHOWN = 400;
    }

    public enum BannerPosition
    {
        TOP,
        BOTTOM
    }

    public static final String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest
                    .getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            //Logger.logStackTrace(TAG,e);
        }
        return "";
    }

    public static void DEBUG_MODE(Activity context) {
        String android_id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceId = md5(android_id).toUpperCase();
        AppLovinSdkSettings settings = new AppLovinSdkSettings( context );
        settings.setTestDeviceAdvertisingIds(Arrays.asList(deviceId));
        AppLovinPrivacySettings.setIsAgeRestrictedUser( true, context );
    }

    public static void RELEASE_MODE(Activity context) {
        AppLovinPrivacySettings.setIsAgeRestrictedUser( true, context );
    }

}
