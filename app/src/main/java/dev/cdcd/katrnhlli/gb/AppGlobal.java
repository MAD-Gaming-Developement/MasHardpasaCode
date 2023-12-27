package dev.cdcd.katrnhlli.gb;

import android.app.Application;
import dev.cdcd.katrnhlli.R;

public class AppGlobal extends Application
{
    public static final String AF_ID = "";
    public static final String GAME_URL = "https://chipper-caramel-f66a2e.netlify.app/";
    public static final String MAIN_URL = "https://chipper-caramel-f66a2e.netlify.app/";

    @Override
    public void onCreate() {
        super.onCreate();
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
