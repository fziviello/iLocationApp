package com.ziviello.fabio.iLocation;

/**
 * Created by fabio on 11/09/2017.
 */

import android.content.Context;
import java.net.URISyntaxException;
import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketSingleton {
    private static SocketSingleton instance;
    private String SERVER_ADDRESS;
    private Socket mSocket;
    private Context ctx;

    public SocketSingleton(Context context) {
        this.ctx = context;
        SERVER_ADDRESS="http://192.168.1.24:4200";
        this.mSocket = getServerSocket();
    }

    public static SocketSingleton get(Context context){
        if(instance == null){
            instance = getSync(context);
        }
        instance.ctx = context;
        return instance;
    }

    private static synchronized SocketSingleton getSync(Context context) {
        if(instance == null){
            instance = new SocketSingleton(context);
        }
        return instance;
    }

    public Socket getSocket(){
        return this.mSocket;
    }

    public Socket getServerSocket() {
        try {
            IO.Options opts = new IO.Options();
            opts.forceNew = true;
            opts.reconnection = true;
            //opts.transports = new String[]{WebSocket.NAME, Polling.NAME};
            mSocket = IO.socket(SERVER_ADDRESS, opts);
            return mSocket;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}