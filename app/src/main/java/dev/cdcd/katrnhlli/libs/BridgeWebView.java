package dev.cdcd.katrnhlli.libs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.collection.ArrayMap;
import com.google.gson.Gson;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class BridgeWebView extends WebView implements WebViewJavascriptBridge, BridgeWebViewClient.OnLoadJSListener {
    private static final int URL_MAX_CHARACTER_NUM=2097152;
    private Map<String, OnBridgeCallback> mCallbacks = new ArrayMap<>();

    private List<Object> mMessages = new ArrayList<>();

    private BridgeWebViewClient mClient;

    private long mUniqueId = 0;

    private boolean mJSLoaded = false;

    private Gson mGson;

    public BridgeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BridgeWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public BridgeWebView(Context context) {
        super(context);
        init();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        clearCache(true);
        getSettings().setBuiltInZoomControls(true);
        getSettings().setDisplayZoomControls(false);
        getSettings().setLoadsImagesAutomatically(true);
        getSettings().setSupportMultipleWindows(true);
        getSettings().setDomStorageEnabled(true);
        getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        getSettings().setJavaScriptEnabled(true);
        getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        WebView.setWebContentsDebuggingEnabled(true);
        mClient = new BridgeWebViewClient(this);
        super.setWebViewClient(mClient);
    }

    public void setGson(Gson gson) {
        mGson = gson;
    }

    public boolean isJSLoaded() {
        return mJSLoaded;
    }

    public Map<String, OnBridgeCallback> getCallbacks() {
        return mCallbacks;
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        mClient.setWebViewClient(client);
    }

    @Override
    public void onLoadStart() {
        mJSLoaded = false;
    }

    @Override
    public void onLoadFinished() {
        mJSLoaded = true;
        if (mMessages != null) {
            for (Object message : mMessages) {
                dispatchMessage(message);
            }
            mMessages = null;
        }
    }

    @Override
    public void sendToWeb(String data) {
        sendToWeb(data, (OnBridgeCallback) null);
    }

    @Override
    public void sendToWeb(String data, OnBridgeCallback responseCallback) {
        doSend(null, data, responseCallback);
    }

    /**
     * call javascript registered handler
     * 调用javascript处理程序注册
     *
     * @param handlerName handlerName
     * @param data        data
     * @param callBack    OnBridgeCallback
     */
    public void callHandler(String handlerName, String data, OnBridgeCallback callBack) {
        doSend(handlerName, data, callBack);
    }


    @Override
    public void sendToWeb(String function, Object... values) {
        // 必须要找主线程才会将数据传递出去 --- 划重点
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            String jsCommand = String.format(function, values);
            jsCommand = String.format(BridgeUtil.JAVASCRIPT_STR, jsCommand);
            loadUrl(jsCommand);
        }
    }

    /**
     * Save message to message queue
     *
     * @param handlerName      handlerName
     * @param data             data
     * @param responseCallback OnBridgeCallback
     */
    private void doSend(String handlerName, Object data, OnBridgeCallback responseCallback) {
        if (!(data instanceof String) && mGson == null){
            return;
        }
        JSRequest request = new JSRequest();
        if (data != null) {
            request.data = data instanceof String ? (String) data : mGson.toJson(data);
        }
        if (responseCallback != null) {
            String callbackId = String.format(BridgeUtil.CALLBACK_ID_FORMAT, (++mUniqueId) + (BridgeUtil.UNDERLINE_STR + SystemClock.currentThreadTimeMillis()));
            mCallbacks.put(callbackId, responseCallback);
            request.callbackId = callbackId;
        }
        if (!TextUtils.isEmpty(handlerName)) {
            request.handlerName = handlerName;
        }
        queueMessage(request);
    }

    /**
     * Add to message collection otherwise distribute message
     *
     * list<message> != null
     *
     * @param message Message
     */
    private void queueMessage(Object message) {
        if (mMessages != null) {
            mMessages.add(message);
        } else {
            dispatchMessage(message);
        }
    }

    /**
     * The message must be distributed successfully in the main thread.
     *
     * @param message Message
     *
     * The system's native API does Json escaping. There is no need to replace it with regular expressions yourself, and the replacement may not be complete.
     */
    private void dispatchMessage(Object message) {
        if (mGson == null){
            return;
        }
        String messageJson = mGson.toJson(message);
        messageJson = JSONObject.quote(messageJson);
        String javascriptCommand = String.format(BridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA, messageJson);
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            if (javascriptCommand.length()>=URL_MAX_CHARACTER_NUM) {
                this.evaluateJavascript(javascriptCommand,null);
            }else {
                this.loadUrl(javascriptCommand);
            }
        }
    }

    public void sendResponse(Object data, String callbackId) {
        if (!(data instanceof String) && mGson == null){
            return;
        }
        if (!TextUtils.isEmpty(callbackId)) {
            final JSResponse response = new JSResponse();
            response.responseId = callbackId;
            response.responseData = data instanceof String ? (String) data : mGson.toJson(data);
            if (Thread.currentThread() == Looper.getMainLooper().getThread()){
                dispatchMessage(response);
            }else {
                post(() -> dispatchMessage(response));
            }

        }
    }

    @Override
    public void destroy() {
        super.destroy();
        mCallbacks.clear();
    }

    public abstract static class BaseJavascriptInterface {

        private Map<String, OnBridgeCallback> mCallbacks;

        protected BaseJavascriptInterface(Map<String, OnBridgeCallback> callbacks) {
            mCallbacks = callbacks;
        }

        @JavascriptInterface
        public String send(String data, String callbackId) {
            Log.d("BaseJavascriptInterface", data + ", callbackId: " + callbackId + " " + Thread.currentThread().getName());
            return send(data);
        }

        @JavascriptInterface
        public void response(String data, String responseId) {
            Log.d("BaseJavascriptInterface", data + ", responseId: " + responseId + " " + Thread.currentThread().getName());
            if (!TextUtils.isEmpty(responseId)) {
                OnBridgeCallback function = mCallbacks.remove(responseId);
                if (function != null) {
                    function.onCallBack(data);
                }
            }
        }

        public abstract String send(String data);
    }

}
