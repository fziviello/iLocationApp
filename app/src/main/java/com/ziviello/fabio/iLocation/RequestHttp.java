package com.ziviello.fabio.iLocation;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RequestHttp extends AsyncTask<String, String, JSONObject> {

    JSONObject objResultHttp = new JSONObject();

    @Override
    protected JSONObject doInBackground(String... params) {
        URL url;
        HttpURLConnection urlConnection = null;
        JSONObject parametri;
        int responseCode;

        try {

            if(params[2].equals("GET"))
            {

                parametri = new JSONObject(params[1]);
                String customUrl="";

                for(int i = 0; i<parametri.names().length(); i++)
                {
                    customUrl = customUrl+"&"+parametri.names().getString(i)+"="+parametri.get(parametri.names().getString(i));
                }
                customUrl=customUrl.substring(1,customUrl.length());
                url = new URL(params[0]+"?"+customUrl);

                urlConnection = (HttpURLConnection) url.openConnection();

                if(!params[3].isEmpty())
                {
                    urlConnection.setRequestProperty("Authorization", "Bearer "+params[3]); //TOKEN
                }

                responseCode = urlConnection.getResponseCode();

                if(responseCode == HttpURLConnection.HTTP_OK)
                {
                    objResultHttp.put("code",urlConnection.getResponseCode());
                    objResultHttp.put("response",readStream(urlConnection.getInputStream()));
                }
                else
                {
                    objResultHttp.put("code",urlConnection.getResponseCode());
                    objResultHttp.put("response",readStream(urlConnection.getErrorStream()));
                }
            }
            else
            {
                url = new URL(params[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setReadTimeout( 30000 /*milliseconds*/ );
                urlConnection.setConnectTimeout( 30000 /* milliseconds */ );
                urlConnection.setRequestMethod(params[2]);
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                urlConnection.setFixedLengthStreamingMode(params[1].getBytes().length);
                urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                //urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                if(!params[3].isEmpty())
                {
                    urlConnection.setRequestProperty("Authorization", "Bearer "+params[3]); //TOKEN
                }

                OutputStream os = new BufferedOutputStream(urlConnection.getOutputStream());
                os.write(params[1].getBytes());
                os.flush();

                responseCode = urlConnection.getResponseCode();

                if(responseCode == HttpURLConnection.HTTP_OK)
                {
                    objResultHttp.put("code",urlConnection.getResponseCode());
                    objResultHttp.put("response",readStream(urlConnection.getInputStream()));
                }
                else
                {
                    objResultHttp.put("code",urlConnection.getResponseCode());
                    objResultHttp.put("response",readStream(urlConnection.getErrorStream()));
                }
            }

        } catch (Exception ex) {
            Log.e("Err RequestHttp", ex.toString());
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();
        }
        return objResultHttp;
    }


    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException ex) {
            Log.e("Errore ReadStream", ex.toString());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    Log.e("Errore ReadStream", ex.toString());
                }
            }
        }
        return response.toString();
    }
}