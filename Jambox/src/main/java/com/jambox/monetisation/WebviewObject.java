package com.jambox.monetisation;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.google.android.gms.common.util.Strings;

import org.json.JSONObject;

public class WebviewObject {

    private WebView webview;
    private Context context;
    private String ClientId;
    private boolean isDirectGame;
    private boolean isWebviewDestroyed = false;
    private ViewGroup.LayoutParams webviewLayout;
    private boolean isBannerOpenedByWebview = false;

    public WebviewObject(Context context, String ClientId)
    {
        this.context = context;
        this.ClientId = ClientId;
        this.webviewLayout = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    }

    public WebviewObject(Context context, String ClientId, ViewGroup.LayoutParams webviewLayout)
    {
        this.context = context;
        this.ClientId = ClientId;
        this.webviewLayout = webviewLayout;
    }

    public void StartWebview()
    {
        InternalStartWebview("");
    }

    public void StartWebviewGame(JamboxGameKeys gameId)
    {
        InternalStartWebview(gameId.GetGameId());
    }

    private void InternalStartWebview(String gameId)
    {
        if (!JamboxAdsHelper.IsInitialized)
        {
            Log.e("JamboxWebview", "ERROR: Please make sure JamboxAdsHelper is initialized before starting H5 Games");
            return;
        }

        if (webview == null)
        {
            webview = new WebView(context);
            Activity activity = (Activity) context;
            //activity.setContentView(webview);

            FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams)webviewLayout;
            layout.topMargin = JamboxAdsHelper.statusBarPadding;
            layout.bottomMargin = JamboxAdsHelper.GetBannerHeightInPx() + 20 + JamboxAdsHelper.navigationBarPadding;

            activity.addContentView(webview, layout);
        }

        WebSettings webSettings = webview.getSettings();
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);

        isDirectGame = !Strings.isEmptyOrWhitespace(gameId);
        String loadUrl = "";
        if (isDirectGame)
        {
            loadUrl = "https://jamgame.jambox.games/?channel_id=" + ClientId + "&game_id=" + gameId;
            //loadUrl = "http://10.0.2.2/?channel_id=" + ClientId + "&game_id=" + gameId;
        }
        else
        {
            loadUrl = "https://jamgame.jambox.games/?channel_id=" + ClientId;
            //loadUrl = "http://10.0.2.2/?channel_id=" + ClientId;
        }

        //webview.clearCache(true);
        JamboxWebviewClient webviewClient = new JamboxWebviewClient();
        webviewClient.Initialize(loadUrl, isDirectGame, this);
        webview.setWebViewClient(webviewClient);
        webview.addJavascriptInterface(new WebAppInterface(this), "Unity");
        webview.loadUrl(loadUrl);
        webview.setVisibility(View.VISIBLE);

        if (!JamboxAdsHelper.IsShowingBanner())
        {
            isBannerOpenedByWebview = true;
            JamboxAdsHelper.ShowBannerAd(JamboxAdsHelper.BannerPosition.BOTTOM);
        }
    }

    public void BackWebview()
    {
        if (isDirectGame)
        {
            CloseWebview();
        }
        else
        {
            if (CanGoBack())
                webview.goBack();
            else
                CloseWebview();
        }
    }

    public boolean CanGoBack()
    {
        return webview.canGoBack();
    }

    public void CloseWebview()
    {
        if (isBannerOpenedByWebview)
        {
            isBannerOpenedByWebview = false;
            JamboxAdsHelper.HideBannerAd();
        }

        ((ViewGroup) webview.getParent()).removeView(webview);
        webview.destroy();
        isWebviewDestroyed = true;
    }

    public boolean IsWebviewDestroyed()
    {
        return isWebviewDestroyed;
    }

    public void WebviewCallback(String msg)
    {
        switch (msg){
            case "RW":
                JamboxAdsHelper.ShowRewarded(new OnRewardedAdListener()
                {
                    @Override
                    public void OnAdDisplayFailed()
                    {
                        System.out.println("RW display Failed!!");
                        String script = "handleMaxRewardedCallback(" + GetCallbackJson(JamboxAdsHelper.AdsCode.ADS_NOT_SHOWN) + ")";
                        webview.evaluateJavascript(script, null);
                    }

                    @Override
                    public void OnAdDisplayed()
                    {
                        System.out.println("RW Displayed!!");
                        String script = "handleMaxRewardedCallback(" + GetCallbackJson(JamboxAdsHelper.AdsCode.BEFORE_ADS_SHOWN) + ")";
                        webview.evaluateJavascript(script, null);
                    }

                    @Override
                    public void OnAdCompleted()
                    {
                        System.out.println("RW completed!");
                        String script = "handleMaxRewardedCallback(" + GetCallbackJson(JamboxAdsHelper.AdsCode.ADS_WATCH_SUCCESS) + ")";
                        webview.evaluateJavascript(script, null);
                    }

                    @Override
                    public void OnAdHidden()
                    {
                        System.out.println("RW Hidden!!");
                        String script = "handleMaxRewardedCallback(" + GetCallbackJson(JamboxAdsHelper.AdsCode.ADS_VIEWED_OR_DISMISSED) + ")";
                        webview.evaluateJavascript(script, null);
                    }
                });
                break;
            case "IS":
                JamboxAdsHelper.ShowInterstitial(null);
                break;
            case "banner_enable":
                JamboxAdsHelper.ShowBannerAd(JamboxAdsHelper.BannerPosition.BOTTOM);
                break;
            case "banner_disable":
                JamboxAdsHelper.HideBannerAd();
                break;
            default:
                System.out.println("No Match Found");
                break;
        }
        System.out.println(msg);
    }

    private String GetCallbackJson(int code)
    {
        try
        {
            JSONObject json = new JSONObject();
            json.put("code", code);
            return json.toString();
        }
        catch (Exception e)
        {
            System.out.println("Error Json: " + e);
            return "";
        }
    }

}

class JamboxWebviewClient extends WebViewClient {

    private String originUrl;
    private boolean directGame;
    private WebviewObject webviewObject;

    public void Initialize(String originUrl, boolean directGame, WebviewObject webviewObject)
    {
        this.originUrl = originUrl;
        this.directGame = directGame;
        this.webviewObject = webviewObject;
    }

    private int originCount = 0;
    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload)
    {
        if (url.equals(originUrl) && directGame)
        {
            if (originCount <= 0)
                originCount++;
            else
                webviewObject.CloseWebview();
        }
        super.doUpdateVisitedHistory(view, url, isReload);
    }

    @Override
    public void onPageFinished(WebView view, String url)
    {
        if (url.equals(originUrl) && !directGame)
        {
            //clearing history, so that goBack() works properly
            view.clearHistory();
        }
        super.onPageFinished(view, url);
    }

}
