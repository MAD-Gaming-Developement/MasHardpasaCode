package dev.cdcd.katrnhlli.libs;

import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BridgeHelper implements WebViewJavascriptBridge {
    private static final String TAG = "BridgeHelper";

    private static final String BRIDGE_JS = "WebViewJavascriptBridge.js";
    private final Map<String, OnBridgeCallback> responseCallbacks = new HashMap<>();
    private final Map<String, BridgeHandler> messageHandlers = new HashMap<>();
    private BridgeHandler defaultHandler = new DefaultHandler();

    private List<Message> startupMessage = new ArrayList<>();

    private List<Message> getStartupMessage() {
        return startupMessage;
    }

    private void setStartupMessage(List<Message> startupMessage) {
        this.startupMessage = startupMessage;
    }

    private long uniqueId = 0;

    private final IWebView webView;

    public BridgeHelper(IWebView webView) {
        this.webView = webView;
    }

    /**
     * @param handler default handler,handle messages send by js without assigned handler name,
     *                if js message has handler name, it will be handled by named handlers registered by native
     */
    public void setDefaultHandler(BridgeHandler handler) {
        this.defaultHandler = handler;
    }

    /**
     * 获取到CallBackFunction data执行调用并且从数据集移除
     *
     * @param url
     */
    private void handlerReturnData(String url) {
        String functionName = BridgeUtil.getFunctionFromReturnUrl(url);
        OnBridgeCallback f = responseCallbacks.get(functionName);
        String data = BridgeUtil.getDataFromReturnUrl(url);
        if (f != null) {
            f.onCallBack(data);
            responseCallbacks.remove(functionName);
        }
    }

    /**
     * 保存message到消息队列
     *
     * @param handlerName      handlerName
     * @param data             data
     * @param responseCallback CallBackFunction
     */
    private void doSend(String handlerName, String data, OnBridgeCallback responseCallback) {
        Message m = new Message();
        if (!TextUtils.isEmpty(data)) {
            m.setData(data);
        }
        if (responseCallback != null) {
            String callbackStr = String.format(BridgeUtil.CALLBACK_ID_FORMAT, ++uniqueId + (BridgeUtil.UNDERLINE_STR + SystemClock.currentThreadTimeMillis()));
            responseCallbacks.put(callbackStr, responseCallback);
            m.setCallbackId(callbackStr);
        }
        if (!TextUtils.isEmpty(handlerName)) {
            m.setHandlerName(handlerName);
        }
        queueMessage(m);
    }

    /**
     * list<message> != null 添加到消息集合否则分发消息
     *
     * @param m Message
     */
    private void queueMessage(Message m) {
        if (startupMessage != null) {
            startupMessage.add(m);
        } else {
            dispatchMessage(m);
        }
    }

    /**
     * 分发message 必须在主线程才分发成功
     *
     * @param m Message
     */
    private void dispatchMessage(Message m) {
        String messageJson = m.toJson();
        //escape special characters for json string  为json字符串转义特殊字符
        messageJson = messageJson.replace("(\\\\)([^utrn])", "\\\\\\\\$1$2");
        messageJson = messageJson.replace("(?<=[^\\\\])(\")", "\\\\\"");
        messageJson = messageJson.replace("(?<=[^\\\\])(\')", "\\\\\'");
        messageJson = messageJson.replace("%7B", URLEncoder.encode("%7B"));
        messageJson = messageJson.replace("%7D", URLEncoder.encode("%7D"));
        messageJson = messageJson.replace("%22", URLEncoder.encode("%22"));
        String javascriptCommand = String.format(BridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA, messageJson);
        // 必须要找主线程才会将数据传递出去 --- 划重点
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            this.loadUrl(javascriptCommand);
        }
    }

    /**
     * 刷新消息队列
     */
    private void flushMessageQueue() {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            loadUrl(BridgeUtil.JS_FETCH_QUEUE_FROM_JAVA, data -> {
                // deserializeMessage 反序列化消息
                List<Message> list = null;
                try {
                    list = Message.toArrayList(data);
                } catch (Exception e) {
                    Log.w(TAG, e);
                    return;
                }
                if (list == null || list.isEmpty()) {
                    return;
                }
                for (int i = 0; i < list.size(); i++) {
                    Message m = list.get(i);
                    String responseId = m.getResponseId();
                    // 是否是response  CallBackFunction
                    if (!TextUtils.isEmpty(responseId)) {
                        OnBridgeCallback function = responseCallbacks.get(responseId);
                        String responseData = m.getResponseData();
                        function.onCallBack(responseData);
                        responseCallbacks.remove(responseId);
                    } else {
                        OnBridgeCallback responseFunction = null;
                        // if had callbackId 如果有回调Id
                        final String callbackId = m.getCallbackId();
                        if (!TextUtils.isEmpty(callbackId)) {
                            responseFunction = data1 -> {
                                Message responseMsg = new Message();
                                responseMsg.setResponseId(callbackId);
                                responseMsg.setResponseData(data1);
                                queueMessage(responseMsg);
                            };
                        } else {
                            responseFunction = data12 -> {
                                // do nothing
                            };
                        }
                        // BridgeHandler执行
                        BridgeHandler handler;
                        if (!TextUtils.isEmpty(m.getHandlerName())) {
                            handler = messageHandlers.get(m.getHandlerName());
                        } else {
                            handler = defaultHandler;
                        }
                        if (handler != null) {
                            handler.handler(m.getData(), responseFunction);
                        }
                    }
                }
            });
        }
    }

    private void loadUrl(String jsUrl, OnBridgeCallback returnCallback) {
        this.loadUrl(jsUrl);
        // 添加至 Map<String, CallBackFunction>
        responseCallbacks.put(BridgeUtil.parseFunctionName(jsUrl), returnCallback);
    }

    private void loadUrl(String jsUrl) {
        webView.loadUrl(jsUrl);
    }

    /**
     * register handler,so that javascript can call it
     * 注册处理程序,以便javascript调用它
     *
     * @param handlerName handlerName
     * @param handler     BridgeHandler
     */
    public void registerHandler(String handlerName, BridgeHandler handler) {
        if (handler != null) {
            // 添加至 Map<String, BridgeHandler>
            messageHandlers.put(handlerName, handler);
        }
    }

    /**
     * unregister handler
     *
     * @param handlerName
     */
    public void unregisterHandler(String handlerName) {
        if (handlerName != null) {
            messageHandlers.remove(handlerName);
        }
    }

    /**
     * call javascript registered handler
     * 调用javascript处理程序注册
     *
     * @param handlerName handlerName
     * @param data        data
     * @param callBack    CallBackFunction
     */
    public void callHandler(String handlerName, String data, OnBridgeCallback callBack) {
        doSend(handlerName, data, callBack);
    }

    public void onPageFinished() {
        webViewLoadLocalJs();

        if (getStartupMessage() != null) {
            for (Message m : getStartupMessage()) {
                dispatchMessage(m);
            }
            setStartupMessage(null);
        }
    }

    private void webViewLoadLocalJs() {
        String jsContent = BridgeUtil.assetFile2Str(webView.getContext(), BridgeHelper.BRIDGE_JS);
        loadUrl("javascript:" + jsContent);
    }

    public boolean shouldOverrideUrlLoading(String url) {
        try {
            // decode 之前，处理 % 和 +
            String replacedUrl = url.replace("%(?![0-9a-fA-F]{2})", "%25").replace("\\+", "%2B");
            url = URLDecoder.decode(replacedUrl, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, e);
        }

        if (url.startsWith(BridgeUtil.YY_RETURN_DATA)) { // 如果是返回数据
            handlerReturnData(url);
            return true;
        } else if (url.startsWith(BridgeUtil.YY_OVERRIDE_SCHEMA)) { //
            flushMessageQueue();
            return true;
        }
        return false;
    }

    @Override
    public void sendToWeb(String data) {
        sendToWeb(data, (OnBridgeCallback) null);
    }

    @Override
    public void sendToWeb(String data, OnBridgeCallback responseCallback) {
        doSend(null, data, responseCallback);
    }

    @Override
    public void sendToWeb(String function, Object... values) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            String jsCommand = String.format(function, values);
            jsCommand = String.format(BridgeUtil.JAVASCRIPT_STR, jsCommand);
            loadUrl(jsCommand);
        }
    }
}
