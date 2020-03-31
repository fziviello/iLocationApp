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
import com.ziviello.fabio.iLocation.UserSession;
import com.ziviello.fabio.iLocation.request.RequestHttps;
import com.ziviello.fabio.iLocation.utility.CheckConnessione;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import static com.google.android.gms.internal.zzir.runOnUiThread;

public class login_Fragment extends Fragment {
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
        Button btnReset=(Button) rootview.findViewById(R.id.btnReset);
        Button BtnLogin=(Button) rootview.findViewById(R.id.BtnLogin);
        final EditText txtUser=(EditText) rootview.findViewById(R.id.txtUser);
        final EditText txtPassword=(EditText) rootview.findViewById(R.id.txtPassword);
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
                        txtUser.setError("Inserire la tua email");
                        txtPassword.setError("Inserire una password");
                    } else {
                        if (txtPassword.length() <= 0 && txtUser.length() > 0) {
                            txtPassword.setError("Inserire una password");
                        }
                        if (txtUser.length() <= 0 && txtPassword.length() > 0) {
                            txtUser.setError("Inserire la tua email");
                        }
                        if (txtUser.length() > 0 && txtPassword.length() > 0) {

                            try {

                                user = new JSONObject();
                                parametri = new JSONObject();

                                parametri.put("email", txtUser.getText().toString());
                                parametri.put("password", txtPassword.getText().toString());
                                user.put("login", parametri);

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
                                            int codiceRisposta = 500;
                                            JSONObject rispostaJson = null;
                                            JSONObject risultatoJson = null;

                                            try {
                                                try {
                                                    rispostaJson = request_login.execute("https://192.168.1.24:3000/api/v1/login", user.toString(), "POST", "").get();
                                                    codiceRisposta = Integer.parseInt(rispostaJson.getString("code"));
                                                    risultatoJson = new JSONObject(rispostaJson.getString("response"));

                                                } catch (ExecutionException e) {
                                                    Log.w("err", "Exception: " + e.toString());
                                                } catch (JSONException e) {
                                                    Log.w("err", "Exception: " + e.toString());
                                                }

                                                if (codiceRisposta < 400) {
                                                    statoLogin = true;

                                                    JSONArray jsonNodoRisultato = risultatoJson.optJSONArray("result");

                                                    Log.e("Risposta HTTP:", String.valueOf(jsonNodoRisultato));

                                                    ArrayList<HashMap<String, String>> Lista_risultato = new ArrayList<HashMap<String, String>>();
                                                    JSONObject jsonChildNode = null;

                                                    try {
                                                        jsonChildNode = jsonNodoRisultato.getJSONObject(0);
                                                    } catch (JSONException e) {
                                                        Log.w("err", "Exception: " + e.toString());
                                                    }

                                                    utente_log = new JSONObject();

                                                    try {
                                                        utente_log.put("id", jsonChildNode.optString("id").toString());
                                                        utente_log.put("token", jsonChildNode.optString("token").toString());
                                                        utente_log.put("nome", jsonChildNode.optString("nome").toString());
                                                        utente_log.put("cognome", jsonChildNode.optString("cognome").toString());
                                                        utente_log.put("email", jsonChildNode.optString("email").toString());
                                                        utente_log.put("room", jsonChildNode.optString("room").toString());
                                                        utente_log.put("colorMarker", jsonChildNode.optString("colorMarker").toString());


                                                    } catch (JSONException e) {
                                                        Log.w("err", "Exception: " + e.toString());
                                                    }
                                                }

                                            } catch (InterruptedException e) {
                                                Log.w("err", "Exception: " + e.toString());
                                            }

                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    ProgressLoading.cancel();
                                                    String MsgResult = "";

                                                    if (statoLogin == true) {
                                                        try {

                                                            MsgResult = "Benvenuto " + utente_log.getString("nome");
                                                            FragmentManager fragmentManager = getFragmentManager();
                                                            Fragment fragment = new mappa_Fragment();
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
            Fragment fragment = new mappa_Fragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }
        return rootview;
    }
}