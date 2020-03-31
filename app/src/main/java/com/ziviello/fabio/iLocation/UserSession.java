package com.ziviello.fabio.iLocation;

import android.content.Context;
import org.json.JSONObject;

/**
 * Created by fabio on 13/09/2017.
 */

public class UserSession {

    private static UserSession instance;
    private JSONObject utente_log;
    private Context ctx;

    public UserSession(Context context) {
        this.ctx = context;
        this.utente_log = getUtente_log();
    }

    public JSONObject getUtente_log() {
        return utente_log;
    }

    public static UserSession get(Context context){
        if(instance == null){
            instance = getSync(context);
        }
        instance.ctx = context;
        return instance;
    }

    private static synchronized UserSession getSync(Context context) {
        if(instance == null){
            instance = new UserSession(context);
        }
        return instance;
    }

    public void setUtente_log(JSONObject tmpUser) {
        this.utente_log = tmpUser;
    }

}