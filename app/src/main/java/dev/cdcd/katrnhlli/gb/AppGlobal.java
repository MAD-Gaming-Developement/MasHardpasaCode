package dev.cdcd.katrnhlli.gb;

import android.app.Application;
import android.util.Log;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import dev.cdcd.katrnhlli.R;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AppGlobal extends Application
{
    public static final String appCode = "W11";
    public static final String AF_ID = "";
    public static String GAME_URL = "";
    public static final String MAIN_URL = "https://backend.madgamingdev.com/api/gameid";

    @Override
    public void onCreate() {
        super.onCreate();
        setupURL();
    }

    public static class MCrypt {
        public static String decrypt(String encryptedData, String secretKey) throws Exception {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] iv = new byte[16];
            System.arraycopy(encryptedBytes, 0, iv, 0, 16);
            byte[] ciphertext = new byte[encryptedBytes.length - 16];
            System.arraycopy(encryptedBytes, 16, ciphertext, 0, ciphertext.length);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(2, secretKeySpec, new IvParameterSpec(iv));
            byte[] decryptedBytes = cipher.doFinal(ciphertext);
            return (new String(decryptedBytes, StandardCharsets.UTF_8)).trim();
        }
    }

    private void setupURL() {
        VolleyCall.init(this);
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("appid", appCode);
            requestBody.put("package", this.getPackageName());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String endPoint = MAIN_URL + "?appid=" + appCode + "&package=" + this.getPackageName();
        Log.d("urlResult", endPoint);

        JsonObjectRequest myReq = new JsonObjectRequest(0, endPoint, requestBody, (response) -> {
            Log.e("MGD-DevTools", "JSON:Response - " + response.toString());

            try {
                String decryptedText = MCrypt.decrypt(response.getString("data"), "21913618CE86B5D53C7B84A75B3774CD");
                Log.e("MGD-DevTools", "Decrypted: " + decryptedText);
                JSONObject jsonData = new JSONObject(decryptedText);
                GAME_URL = jsonData.getString("gameURL");
            } catch (Exception var6) {
                var6.printStackTrace();
            }
        }, error ->{
            // Handle the error here
            Log.e("VolleyError", "Error: " + error.getMessage());
        });
        requestQueue.add(myReq);
    }


//    public String getAF_ID()
//    {
//        AF_ID = getString(R.string.AppsFlyerID);
//        return AF_ID;
//    }
//
//    public String getGAME_URL()
//    {
//        return GAME_URL;
//    }
//
//    public String getMAIN_URL()
//    {
//        return MAIN_URL;
//    }
}
