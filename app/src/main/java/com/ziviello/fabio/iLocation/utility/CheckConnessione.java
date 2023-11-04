package com.ziviello.fabio.iLocation.utility;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Fabio on 14/01/2016.
 */
public class CheckConnessione {

    public static NetworkInfo getNetworkInfo(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo();
    }
    public static boolean isConnected(Context context){
        NetworkInfo info = CheckConnessione.getNetworkInfo(context);
        return (info != null && info.isConnected());
    }

}