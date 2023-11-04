package com.ziviello.fabio.iLocation.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.ziviello.fabio.iLocation.R;
import com.ziviello.fabio.iLocation.SocketSingleton;
import com.ziviello.fabio.iLocation.UserSession;
import com.ziviello.fabio.iLocation.utility.RequestHttp;
import com.ziviello.fabio.iLocation.utility.CheckConnessione;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import static com.google.android.gms.internal.zzir.runOnUiThread;
import static com.ziviello.fabio.iLocation.utility.Config.BASE_PATH;
import static com.ziviello.fabio.iLocation.utility.Config.PATH_LOGIN;

public class LoginFragment extends Fragment {
    View rootview;
    private boolean statoLogin=false;
    private JSONObject utente_log;
    private JSONObject user;
    private JSONObject parametri;
    private JSONObject jBUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootview = inflater.inflate(R.layout.login_layout, container, false);
        getActivity().setTitle("Login");
        Button btnReset= rootview.findViewById(R.id.btnReset);
        Button BtnLogin=rootview.findViewById(R.id.BtnLogin);
        final EditText txtUser=rootview.findViewById(R.id.txtUser);
        final EditText txtPassword=rootview.findViewById(R.id.txtPassword);
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtUser.setText("");
                txtPassword.setText("");
            }
        });

        jBUser = UserSession.get(getActivity()).getUtente_log();

        if(jBUser==null) {
            BtnLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {


                    if (txtUser.length() <= 0 && txtPassword.length() <= 0) {
                        txtUser.setError("Inserisci la tua email");
                        txtPassword.setError("Inserisci una password");
                    } else {

                        if (txtPassword.length() <= 0 && txtUser.length() > 0) {
                            txtPassword.setError("Inserisci una password");
                        }
                        if (txtUser.length() <= 0 && txtPassword.length() > 0) {
                            txtUser.setError("Inserisci la tua email");
                        }
                        if (txtUser.length() > 0 && txtPassword.length() > 0) {

                            try {

                                user = new JSONObject();
                                parametri = new JSONObject();

                                parametri.put("email", txtUser.getText().toString());
                                parametri.put("password", txtPassword.getText().toString());
                                user.put("login", parametri);

                                if (CheckConnessione.isConnected(getActivity())) {

                                    final ProgressDialog ProgressLoading = new ProgressDialog(getActivity());

                                    ProgressLoading.setTitle("");
                                    ProgressLoading.setCancelable(false);
                                    ProgressLoading.setMessage("Caricamento in corso...");
                                    ProgressLoading.show();

                                    Thread thread = new Thread(new Runnable() {
                                        public void run() {

                                            RequestHttp request_login = new RequestHttp();
                                            int codiceRisposta = 500;
                                            JSONObject rispostaJson = null;
                                            JSONObject risultatoJson = null;

                                            try {

                                                    rispostaJson = request_login.execute(BASE_PATH + "/" + PATH_LOGIN, user.toString(), "POST", "").get();
                                                    codiceRisposta = Integer.parseInt(rispostaJson.getString("code"));
                                                    risultatoJson = new JSONObject(rispostaJson.getString("response"));

                                                if (codiceRisposta < 400) {
                                                    statoLogin = true;

                                                    JSONArray jsonNodoRisultato = risultatoJson.optJSONArray("result");

                                                    Log.i("Risposta HTTP:", String.valueOf(jsonNodoRisultato));

                                                    JSONObject jsonChildNode = null;

                                                    jsonChildNode = jsonNodoRisultato.getJSONObject(0);

                                                    utente_log = new JSONObject();

                                                    utente_log.put("id", jsonChildNode.optString("id"));
                                                    utente_log.put("token", jsonChildNode.optString("token"));
                                                    utente_log.put("nome", jsonChildNode.optString("nome"));
                                                    utente_log.put("cognome", jsonChildNode.optString("cognome"));
                                                    utente_log.put("email", jsonChildNode.optString("email"));
                                                    utente_log.put("room", jsonChildNode.optString("room"));
                                                    utente_log.put("colorMarker", jsonChildNode.optString("colorMarker"));
                                                }

                                            } catch (Exception e) {
                                                Log.w("err", "Exception: " + e);
                                            }

                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    ProgressLoading.cancel();
                                                    String MsgResult = "";

                                                    if (statoLogin) {

                                                        SocketSingleton.get(getContext()).getSocket().connect();

                                                        try {
                                                            MsgResult = "Benvenuto " + utente_log.getString("nome");
                                                            FragmentManager fragmentManager = getFragmentManager();
                                                            Fragment fragment = new MapFragment();
                                                            fragmentManager.beginTransaction()
                                                                    .replace(R.id.container, fragment)
                                                                    .commit();

                                                            //rendo globale l utente connesso
                                                            UserSession.get(getActivity()).setUtente_log(utente_log);

                                                        } catch (JSONException e) {
                                                            Log.w("err", "Exception: " + e.toString());
                                                        }

                                                    } else {
                                                        MsgResult = "Errore Login";
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
                    }
                }
            });
        }
        else
        {
            FragmentManager fragmentManager = getFragmentManager();
            Fragment fragment = new MapFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }
        return rootview;
    }
}