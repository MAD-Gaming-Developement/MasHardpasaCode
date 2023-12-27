package dev.cdcd.katrnhlli.gb;

import android.content.Context;
import com.android.volley.RequestQueue;

public class VolleyCall {
    private static RequestQueue mRequestQueue;
    private VolleyCall() {}
    public static void init(Context context) {
        mRequestQueue = com.android.volley.toolbox.Volley.newRequestQueue(context);
    }
    public static RequestQueue getRequestQueue() {
        if (mRequestQueue != null) {
            return mRequestQueue;
        } else {
            throw new IllegalStateException("RequestQueue not initialized");
        }
    }
}
