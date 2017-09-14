package com.ziviello.fabio.iLocation;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class RequestHttps extends AsyncTask<String, String, JSONObject> implements X509TrustManager {

    private JSONObject objResultHttp = new JSONObject();
    private static TrustManager[] trustManagers;
    private static final X509Certificate[] _AcceptedIssuers = new X509Certificate[]{};

    @Override
    protected JSONObject doInBackground(String... params) {
        URL url;
        HttpsURLConnection urlConnection = null;
        JSONObject parametri;
        int responseCode;



        try {

            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }

            });

            SSLContext context = null;
            if (trustManagers == null) {
                trustManagers = new TrustManager[]{new RequestHttps()};
            }

            try {
                context = SSLContext.getInstance("SSL");
                context.init(null, trustManagers, new SecureRandom());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {

            }

            HttpsURLConnection.setDefaultSSLSocketFactory(context != null ? context.getSocketFactory() : null);

            switch (params[2]) {
                case "GET": {

                    String customUrl = "";

                    if(!params[1].isEmpty()){

                        parametri = new JSONObject(params[1]);

                        for (int i = 0; i < parametri.names().length(); i++) {
                            customUrl = customUrl + "/"+parametri.get(parametri.names().getString(i));
                        }

                    }

                    url = new URL(params[0] + customUrl);

                    urlConnection = (HttpsURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");

                    if (!params[3].isEmpty()) {
                        urlConnection.setRequestProperty("Authorization", "Bearer " + params[3]); //TOKEN
                    }

                    responseCode = urlConnection.getResponseCode();

                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        objResultHttp.put("code", urlConnection.getResponseCode());
                        objResultHttp.put("response", readStream(urlConnection.getInputStream()));
                    } else {
                        objResultHttp.put("code", urlConnection.getResponseCode());
                        objResultHttp.put("response", readStream(urlConnection.getErrorStream()));
                    }
                    break;
                }
                case "GET?": {

                    parametri = new JSONObject(params[1]);
                    String customUrl = "";

                    for (int i = 0; i < parametri.names().length(); i++) {
                        customUrl = customUrl + "&" + parametri.names().getString(i) + "=" + parametri.get(parametri.names().getString(i));
                    }
                    customUrl = customUrl.substring(1, customUrl.length());

                    url = new URL(params[0] + customUrl);

                    urlConnection = (HttpsURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");

                    if (!params[3].isEmpty()) {
                        urlConnection.setRequestProperty("Authorization", "Bearer " + params[3]); //TOKEN
                    }

                    responseCode = urlConnection.getResponseCode();

                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        objResultHttp.put("code", urlConnection.getResponseCode());
                        objResultHttp.put("response", readStream(urlConnection.getInputStream()));
                    } else {
                        objResultHttp.put("code", urlConnection.getResponseCode());
                        objResultHttp.put("response", readStream(urlConnection.getErrorStream()));
                    }
                    break;
                }
                case "QUERYSTRING": {
                    //params[0]=url
                    //params[1]=Json
                    //params[3]=token

                    //castomizzo la url
                    parametri = new JSONObject(params[1]);
                    String customUrl = "";

                    for (int i = 0; i < parametri.names().length(); i++) {
                        customUrl = customUrl + "&" + parametri.names().getString(i) + "=" + parametri.get(parametri.names().getString(i));
                    }

                    customUrl = params[0] + "?" + customUrl.substring(1, customUrl.length());

                    HttpClient httpclient = null;

                    httpclient = getNewHttpClient();


                    HttpPost httppost = new HttpPost(customUrl);

                    if (!params[3].isEmpty()) {
                        httppost.setHeader("Authorization", "Bearer " + params[3]); //TOKEN
                    }

                    AndroidMultiPartEntity entity = new AndroidMultiPartEntity(
                            new AndroidMultiPartEntity.ProgressListener() {

                                @Override
                                public void transferred(long num) {

                                }
                            });

                    httppost.setEntity(entity);

                    // Making server call
                    HttpResponse response = httpclient.execute(httppost);
                    HttpEntity r_entity = response.getEntity();

                    int statusCode = response.getStatusLine().getStatusCode();

                    objResultHttp.put("code", statusCode);
                    objResultHttp.put("response", EntityUtils.toString(r_entity));
                    break;
                }
                case "POST": {
                    url = new URL(params[0]);
                    urlConnection = (HttpsURLConnection) url.openConnection();

                    urlConnection.setReadTimeout(30000 /*milliseconds*/);
                    urlConnection.setConnectTimeout(30000 /* milliseconds */);
                    urlConnection.setRequestMethod(params[2]);
                    urlConnection.setDoInput(true);
                    urlConnection.setDoOutput(true);
                    urlConnection.setFixedLengthStreamingMode(params[1].getBytes().length);
                    urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                    //urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    if (!params[3].isEmpty()) {
                        urlConnection.setRequestProperty("Authorization", "Bearer " + params[3]); //TOKEN
                    }

                    OutputStream os = new BufferedOutputStream(urlConnection.getOutputStream());
                    os.write(params[1].getBytes());
                    os.flush();

                    responseCode = urlConnection.getResponseCode();

                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        objResultHttp.put("code", responseCode);
                        objResultHttp.put("response", readStream(urlConnection.getInputStream()));
                    } else {
                        objResultHttp.put("code", responseCode);
                        objResultHttp.put("response", readStream(urlConnection.getErrorStream()));
                    }
                    break;
                }
                case "PUT": {
                    url = new URL(params[0]);
                    urlConnection = (HttpsURLConnection) url.openConnection();

                    urlConnection.setReadTimeout(30000 /*milliseconds*/);
                    urlConnection.setConnectTimeout(30000 /* milliseconds */);
                    urlConnection.setRequestMethod(params[2]);
                    urlConnection.setDoInput(true);
                    urlConnection.setDoOutput(true);
                    urlConnection.setFixedLengthStreamingMode(params[1].getBytes().length);
                    urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                    //urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    if (!params[3].isEmpty()) {
                        urlConnection.setRequestProperty("Authorization", "Bearer " + params[3]); //TOKEN
                    }

                    OutputStream os = new BufferedOutputStream(urlConnection.getOutputStream());
                    os.write(params[1].getBytes());
                    os.flush();

                    responseCode = urlConnection.getResponseCode();

                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        objResultHttp.put("code", responseCode);
                        objResultHttp.put("response", readStream(urlConnection.getInputStream()));
                    } else {
                        objResultHttp.put("code", responseCode);
                        objResultHttp.put("response", readStream(urlConnection.getErrorStream()));
                    }
                    break;
                }
            }

        } catch (Exception ex) {
            Log.e("Err RequestHttps", ex.toString());
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
            //Log.e("Errore ReadStream", ex.toString());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    //Log.e("Errore ReadStream", ex.toString());
                }
            }
        }
        return response.toString();
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

    }

    public boolean isClientTrusted(X509Certificate[] chain) {
        return true;
    }

    public boolean isServerTrusted(X509Certificate[] chain) {
        return true;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return _AcceptedIssuers;
    }

    //classe che aggira i certificati
    class MySSLSocketFactory extends SSLSocketFactory {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
            super(truststore);

            TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(javax.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                }

                public void checkServerTrusted(javax.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            sslContext.init(null, new TrustManager[] { tm }, null);
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
            return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        }

        @Override
        public Socket createSocket() throws IOException {
            return sslContext.getSocketFactory().createSocket();
        }
    }

    private HttpClient getNewHttpClient() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            MySSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }
}

//public class RequestHttps extends AsyncTask<String, String, JSONObject> {
//
//    JSONObject objResultHttp = new JSONObject();
//
//    @Override
//    protected JSONObject doInBackground(String... params) {
//        URL url;
//        HttpURLConnection urlConnection = null;
//        JSONObject parametri;
//        int responseCode;
//
//        try {
//
//            switch (params[2]) {
//                case "GET?": {
//
//                    parametri = new JSONObject(params[1]);
//                    String customUrl = "";
//
//                    for (int i = 0; i < parametri.names().length(); i++) {
//                        customUrl = customUrl + "&" + parametri.names().getString(i) + "=" + parametri.get(parametri.names().getString(i));
//                    }
//                    customUrl = customUrl.substring(1, customUrl.length());
//
//                    url = new URL(params[0] + customUrl);
//
//                    urlConnection = (HttpURLConnection) url.openConnection();
//                    urlConnection.setRequestMethod("GET");
//
//                    if (!params[3].isEmpty()) {
//                        urlConnection.setRequestProperty("Authorization", "Bearer " + params[3]); //TOKEN
//                    }
//
//                    responseCode = urlConnection.getResponseCode();
//
//                    if (responseCode == HttpsURLConnection.HTTP_OK) {
//                        objResultHttp.put("code", urlConnection.getResponseCode());
//                        objResultHttp.put("response", readStream(urlConnection.getInputStream()));
//                    } else {
//                        objResultHttp.put("code", urlConnection.getResponseCode());
//                        objResultHttp.put("response", readStream(urlConnection.getErrorStream()));
//                    }
//                    break;
//                }
//                case "GET": {
//
//                    String customUrl = "";
//
//                    if(!params[1].isEmpty()){
//
//                        parametri = new JSONObject(params[1]);
//
//                        for (int i = 0; i < parametri.names().length(); i++) {
//                            customUrl = customUrl + "/"+parametri.get(parametri.names().getString(i));
//                        }
//
//                    }
//
//                    url = new URL(params[0] + customUrl);
//
//                    urlConnection = (HttpURLConnection) url.openConnection();
//
//                    if (!params[3].isEmpty()) {
//                        urlConnection.setRequestProperty("Authorization", "Bearer " + params[3]); //TOKEN
//                    }
//
//                    responseCode = urlConnection.getResponseCode();
//
//                    if (responseCode == HttpURLConnection.HTTP_OK) {
//                        objResultHttp.put("code", urlConnection.getResponseCode());
//                        objResultHttp.put("response", readStream(urlConnection.getInputStream()));
//                    } else {
//                        objResultHttp.put("code", urlConnection.getResponseCode());
//                        objResultHttp.put("response", readStream(urlConnection.getErrorStream()));
//                    }
//                    break;
//                }
//                case "QUERYSTRING": {
//                    //params[0]=url
//                    //params[1]=Json
//                    //params[3]=token
//
//                    //castomizzo la url
//                    parametri = new JSONObject(params[1]);
//                    String customUrl = "";
//
//                    for (int i = 0; i < parametri.names().length(); i++) {
//                        customUrl = customUrl + "&" + parametri.names().getString(i) + "=" + parametri.get(parametri.names().getString(i));
//                    }
//
//                    customUrl = params[0] + "?" + customUrl.substring(1, customUrl.length());
//
//                    HttpClient httpclient = null;
//
//                    httpclient = new DefaultHttpClient();
//
//
//                    HttpPost httppost = new HttpPost(customUrl);
//
//                    if (!params[3].isEmpty()) {
//                        httppost.setHeader("Authorization", "Bearer " + params[3]); //TOKEN
//                    }
//
//                    AndroidMultiPartEntity entity = new AndroidMultiPartEntity(
//                            new AndroidMultiPartEntity.ProgressListener() {
//
//                                @Override
//                                public void transferred(long num) {
//
//                                }
//                            });
//
//                    httppost.setEntity(entity);
//
//                    // Making server call
//                    HttpResponse response = httpclient.execute(httppost);
//                    HttpEntity r_entity = response.getEntity();
//
//                    int statusCode = response.getStatusLine().getStatusCode();
//
//                    objResultHttp.put("code", statusCode);
//                    objResultHttp.put("response", EntityUtils.toString(r_entity));
//                    break;
//                }
//                case "POST": {
//                    url = new URL(params[0]);
//                    urlConnection = (HttpURLConnection) url.openConnection();
//                    urlConnection.setReadTimeout(30000 /*milliseconds*/);
//                    urlConnection.setConnectTimeout(30000 /* milliseconds */);
//                    urlConnection.setRequestMethod(params[2]);
//                    urlConnection.setDoInput(true);
//                    urlConnection.setDoOutput(true);
//                    urlConnection.setFixedLengthStreamingMode(params[1].getBytes().length);
//                    urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
//                    //urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//
//                    if (!params[3].isEmpty()) {
//                        urlConnection.setRequestProperty("Authorization", "Bearer " + params[3]); //TOKEN
//                    }
//
//                    OutputStream os = new BufferedOutputStream(urlConnection.getOutputStream());
//                    os.write(params[1].getBytes());
//                    os.flush();
//
//                    responseCode = urlConnection.getResponseCode();
//
//                    if (responseCode == HttpURLConnection.HTTP_OK) {
//                        objResultHttp.put("code", urlConnection.getResponseCode());
//                        objResultHttp.put("response", readStream(urlConnection.getInputStream()));
//                    } else {
//                        objResultHttp.put("code", urlConnection.getResponseCode());
//                        objResultHttp.put("response", readStream(urlConnection.getErrorStream()));
//                    }
//                    break;
//                }
//                case "PUT": {
//                    url = new URL(params[0]);
//                    urlConnection = (HttpURLConnection) url.openConnection();
//                    urlConnection.setReadTimeout(30000 /*milliseconds*/);
//                    urlConnection.setConnectTimeout(30000 /* milliseconds */);
//                    urlConnection.setRequestMethod(params[2]);
//                    urlConnection.setDoInput(true);
//                    urlConnection.setDoOutput(true);
//                    urlConnection.setFixedLengthStreamingMode(params[1].getBytes().length);
//                    urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
//                    //urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//
//                    if (!params[3].isEmpty()) {
//                        urlConnection.setRequestProperty("Authorization", "Bearer " + params[3]); //TOKEN
//                    }
//
//                    OutputStream os = new BufferedOutputStream(urlConnection.getOutputStream());
//                    os.write(params[1].getBytes());
//                    os.flush();
//
//                    responseCode = urlConnection.getResponseCode();
//
//                    if (responseCode == HttpURLConnection.HTTP_OK) {
//                        objResultHttp.put("code", urlConnection.getResponseCode());
//                        objResultHttp.put("response", readStream(urlConnection.getInputStream()));
//                    } else {
//                        objResultHttp.put("code", urlConnection.getResponseCode());
//                        objResultHttp.put("response", readStream(urlConnection.getErrorStream()));
//                    }
//                    break;
//                }
//            }
//
//        } catch (Exception ex) {
//            //Log.e("Err RequestHttp", ex.toString());
//        } finally {
//            if (urlConnection != null)
//                urlConnection.disconnect();
//        }
//        return objResultHttp;
//    }
//
//    private String readStream(InputStream in) {
//        BufferedReader reader = null;
//        StringBuffer response = new StringBuffer();
//        try {
//            reader = new BufferedReader(new InputStreamReader(in));
//            String line = "";
//            while ((line = reader.readLine()) != null) {
//                response.append(line);
//            }
//        } catch (IOException ex) {
//            //Log.e("Errore ReadStream", ex.toString());
//        } finally {
//            if (reader != null) {
//                try {
//                    reader.close();
//                } catch (IOException ex) {
//                    //Log.e("Errore ReadStream", ex.toString());
//                }
//            }
//        }
//        return response.toString();
//    }
//}