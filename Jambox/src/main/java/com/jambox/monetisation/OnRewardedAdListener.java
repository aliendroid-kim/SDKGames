package com.jambox.monetisation;

public interface OnRewardedAdListener
{
    void OnAdDisplayFailed();
    void OnAdDisplayed();
    void OnAdCompleted();
    void OnAdHidden();
}
