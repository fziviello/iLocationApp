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
import com.ziviello.fabio.iLocation.utility.RequestHttp;
import com.ziviello.fabio.iLocation.utility.CheckConnessione;

import org.json.JSONException;
import org.json.JSONObject;

import static com.google.android.gms.internal.zzir.runOnUiThread;
import static com.ziviello.fabio.iLocation.utility.Config.BASE_PATH;
import static com.ziviello.fabio.iLocation.utility.Config.PATH_LOGOUT;

public class LogoutFragment extends Fragment {
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
            Fragment fragment= new LoginFragment();
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

                parametri.put("id", jBUser.optString("id"));
                user.put("logout",parametri);

                if (CheckConnessione.isConnected(getActivity())) {

                    final ProgressDialog ProgressLoading = new ProgressDialog(getActivity());

                    ProgressLoading.setTitle("");
                    ProgressLoading.setCancelable(false);
                    ProgressLoading.setMessage("Caricamento in corso...");
                    ProgressLoading.show();

                    Thread thread = new Thread(new Runnable() {
                        public void run() {

                            RequestHttp request_login = new RequestHttp();
                            int codiceRisposta=500;
                            JSONObject rispostaJson=null;

                            try {

                                rispostaJson = request_login.execute(BASE_PATH + "/" + PATH_LOGOUT, user.toString(), "POST", jBUser.optString("token").toString()).get();
                                codiceRisposta=Integer.parseInt(rispostaJson.getString("code"));

                                if(codiceRisposta<400) { statoRequest = true; }

                            } catch (Exception e) {
                                Log.w("err", "Exception: " + e);
                            }

                            runOnUiThread(new Runnable() {
                                public void run() {
                                    ProgressLoading.cancel();
                                    String MsgResult="";

                                    if (statoRequest) {

                                        JSONObject objSend = new JSONObject();

                                        try {
                                            objSend.put("idClient", jBUser.optString("id"));
                                            objSend.put("nome", jBUser.optString("nome"));
                                            objSend.put("cognome", jBUser.optString("cognome"));
                                            objSend.put("room", jBUser.optString("room"));
                                            objSend.put("status", "0");

                                            SocketSingleton.get(getActivity()).getSocket().emit("unsubscribe", objSend);
                                            UserSession.get(getActivity()).setUtente_log(null);

                                        } catch (JSONException e) {
                                            Log.w("err", "Exception: " + e);
                                        }

                                        MsgResult = "Disconnesso ";

                                        FragmentManager fragmentManager = getFragmentManager();
                                        Fragment fragment= new LoginFragment();
                                        fragmentManager.beginTransaction()
                                                .replace(R.id.container, fragment)
                                                .commit();

                                        SocketSingleton.get(getContext()).getSocket().disconnect();

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
