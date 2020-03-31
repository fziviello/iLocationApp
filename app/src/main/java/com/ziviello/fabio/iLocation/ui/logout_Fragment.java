package com.ziviello.fabio.iLocation.ui;

/**
 * Created by fabio on 13/09/2017.
 */

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ziviello.fabio.iLocation.R;
import com.ziviello.fabio.iLocation.SocketSingleton;
import com.ziviello.fabio.iLocation.UserSession;
import com.ziviello.fabio.iLocation.request.RequestHttps;
import com.ziviello.fabio.iLocation.utility.CheckConnessione;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.concurrent.ExecutionException;
import static com.google.android.gms.internal.zzir.runOnUiThread;

public class logout_Fragment extends Fragment {
    View rootview;
    private boolean statoRequest=false;
    private JSONObject jBUser;
    private JSONObject user;
    private JSONObject parametri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootview = inflater.inflate(R.layout.logout_layout, container, false);
        getActivity().setTitle("Logout");

        jBUser = UserSession.get(getActivity()).getUtente_log();

        if(jBUser==null)
        {

            FragmentManager fragmentManager = getFragmentManager();
            Fragment fragment= new login_Fragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }
        else
        {
            try {
                user = new JSONObject();

                user = new JSONObject();
                parametri = new JSONObject();

                parametri.put("id",jBUser.optString("id").toString());
                user.put("logout",parametri);

                CheckConnessione statoRete = new CheckConnessione();

                if (statoRete.isConnected(getActivity())) {

                    final ProgressDialog ProgressLoading = new ProgressDialog(getActivity());

                    ProgressLoading.setTitle("");
                    ProgressLoading.setCancelable(false);
                    ProgressLoading.setMessage("Caricamento in corso...");
                    ProgressLoading.show();

                    Thread thread = new Thread(new Runnable() {
                        public void run() {

                            RequestHttps request_login = new RequestHttps();
                            int codiceRisposta=500;
                            JSONObject rispostaJson=null;

                            try {
                                try {
                                    rispostaJson = request_login.execute("https://192.168.1.24:3000/api/v1/logout", user.toString(), "POST",jBUser.optString("token").toString()).get();
                                    codiceRisposta=Integer.parseInt(rispostaJson.getString("code"));

                                } catch (ExecutionException e) {
                                    Log.w("err", "Exception: " + e.toString());
                                } catch (JSONException e) {
                                    Log.w("err", "Exception: " + e.toString());
                                }

                                if(codiceRisposta<400)
                                {
                                    statoRequest = true;
                                }


                            } catch (InterruptedException e) {
                                Log.w("err", "Exception: " + e.toString());
                            }

                            runOnUiThread(new Runnable() {
                                public void run() {
                                    ProgressLoading.cancel();
                                    String MsgResult="";

                                    if (statoRequest == true) {

                                        JSONObject objSend = new JSONObject();

                                        try {
                                            objSend.put("idClient", jBUser.optString("id").toString());
                                            objSend.put("nome", jBUser.optString("nome").toString());
                                            objSend.put("cognome", jBUser.optString("cognome").toString());
                                            objSend.put("room", jBUser.optString("room").toString());
                                            objSend.put("status", "0");

                                            SocketSingleton.get(getActivity()).getSocket().emit("unsubscribe", objSend);
                                            UserSession.get(getActivity()).setUtente_log(null); //pulisco

                                        } catch (JSONException e) {
                                            Log.w("err", "Exception: " + e.toString());
                                        }

                                        MsgResult = "Disconnesso ";

                                        FragmentManager fragmentManager = getFragmentManager();
                                        Fragment fragment= new login_Fragment();
                                        fragmentManager.beginTransaction()
                                                .replace(R.id.container, fragment)
                                                .commit();

                                    } else {
                                        MsgResult = "Errore Logout";
                                    }

                                    Toast toast = Toast.makeText(getActivity(), MsgResult, Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                            });
                        }
                    });
                    thread.start();

                } else {
                    Toast toast = Toast.makeText(getActivity(), "Assenza di Connessione", Toast.LENGTH_SHORT);
                    toast.show();
                }

            } catch (Exception ex) {
                Log.e("Errore", ex.toString());
            }
        }

        return rootview;
    }
}
