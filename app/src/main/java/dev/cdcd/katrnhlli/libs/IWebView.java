package dev.cdcd.katrnhlli.libs;

import android.content.Context;

public interface IWebView {
    Context getContext();

    void loadUrl(String url);
}
