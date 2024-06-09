package com.jambox.monetisation;

import android.webkit.JavascriptInterface;

public class WebAppInterface {

    private WebviewObject webviewObject;

    public WebAppInterface(WebviewObject webviewObject)
    {
        this.webviewObject = webviewObject;
    }

    @JavascriptInterface
    public void call(String msg)
    {
        webviewObject.WebviewCallback(msg);
    }
}
