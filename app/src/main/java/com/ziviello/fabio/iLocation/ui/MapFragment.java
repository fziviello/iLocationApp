package com.ziviello.fabio.iLocation.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.ziviello.fabio.iLocation.R;
import com.ziviello.fabio.iLocation.SocketSingleton;
import com.ziviello.fabio.iLocation.UserSession;
import com.ziviello.fabio.iLocation.geo.UtilityLocation;
import com.ziviello.fabio.iLocation.utility.RequestHttp;
import com.ziviello.fabio.iLocation.utility.CheckConnessione;
import com.ziviello.fabio.iLocation.utility.CustomKeyboard;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import io.socket.emitter.Emitter;
import static com.google.android.gms.internal.zzir.runOnUiThread;
import static com.ziviello.fabio.iLocation.utility.Config.PATH_AUTOCOMPLETE_MAPS;
import static com.ziviello.fabio.iLocation.utility.Config.PATH_MAPS_DIRECTIONS;
import androidx.core.app.ActivityCompat;

public class MapFragment extends Fragment implements AdapterView.OnItemClickListener {

    TextToSpeech objTextToSpeech;
    MapView mapView;
    GoogleMap map = null;
    View rootview;
    AutoCompleteTextView autoCompView;
    List<LatLng> polyz;
    private boolean stato_traffico = false, primo_zoom = false;
    ArrayList<HashMap> MapListaCoordinate = new ArrayList<HashMap>();
    HashMap<String, Object> MapPoint = new HashMap<String, Object>();
    Polyline LineaDirezioni;
    private Double Current_latitude;
    private Double Current_longitude;
    private String destinazione = "Pescara";
    private String waypoints = "";
    private Boolean stato_destinazione = true;
    private LatLng indirizzo_destinazione;
    private JSONObject jBUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootview = inflater.inflate(R.layout.mappa_layout, container, false);
        getActivity().setTitle("Navigatore");

        jBUser = UserSession.get(getActivity()).getUtente_log();

        autoCompView =  rootview.findViewById(R.id.txtSearchMap);
        autoCompView.setAdapter(new GooglePlacesAutocompleteAdapter(getActivity(), R.layout.list_item));
        autoCompView.setOnItemClickListener(this);

        objTextToSpeech = new TextToSpeech(getActivity(), status -> {
        if (status != TextToSpeech.ERROR) {
            objTextToSpeech.setLanguage(Locale.ITALIAN);
        }
        });

        mapView = rootview.findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        map = mapView.getMap();
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setRotateGesturesEnabled(true);
        map.getUiSettings().setAllGesturesEnabled(true);
        map.getUiSettings().setIndoorLevelPickerEnabled(true);
        map.setMapType(1);


        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }

        map.setOnMarkerClickListener(
                marker -> {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

                    alertDialogBuilder.setTitle(marker.getTitle());
                    alertDialogBuilder.setMessage("Vuoi eliminare questo Punto?");
                    alertDialogBuilder.setPositiveButton("Si", (dialog, id) -> {
                        UtilityLocation test = new UtilityLocation();
                        DeleteMarker(map, marker);
                    });
                    alertDialogBuilder.setNegativeButton("No", (dialog, id) -> dialog.cancel());
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                    return false;
                }
        );


        MapsInitializer.initialize(this.getActivity());
        map.setOnMyLocationChangeListener(myLocationChangeListener);

        Button BtnCerca =  rootview.findViewById(R.id.btnUpdateMap);
        Button btnTraffico =  rootview.findViewById(R.id.btnTraffico);
        Button BtnPulisci =  rootview.findViewById(R.id.btnPulisci);
        Button BtnPercorso =  rootview.findViewById(R.id.btnPercoso);
        ImageButton btnParla = rootview.findViewById(R.id.imgBtnParla);

        if(jBUser==null)
        {
            String MsgResult = "Accesso Negato";
            Toast toast = Toast.makeText(getActivity(), MsgResult, Toast.LENGTH_SHORT);
            toast.show();

            FragmentManager fragmentManager = getFragmentManager();
            Fragment fragment= new LoginFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }
        else {

            JSONObject objSend = new JSONObject();

            try {
                objSend.put("idClient", jBUser.optString("id"));
                objSend.put("nome", jBUser.optString("nome"));
                objSend.put("cognome", jBUser.optString("cognome"));
                objSend.put("room", jBUser.optString("room"));
                objSend.put("status", "1");

                SocketSingleton.get(getActivity()).getSocket().emit("subscribe", objSend);
                SocketSingleton.get(getActivity()).getSocket().on("posizione", emitPosizione);

            } catch (JSONException e) {
                Log.w("err", "Exception: " + e);
            }

            BtnCerca.setOnClickListener(
                    v -> {
                        CustomKeyboard keyboard = new CustomKeyboard();
                        keyboard.close(getActivity().getApplicationContext(), autoCompView.getWindowToken());

                        String ValoreRicerca = autoCompView.getText().toString();

                        if (ValoreRicerca.matches("")) {
                            Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Inserisci un indirizzo", Toast.LENGTH_SHORT);
                            toast.show();
                        } else {
                            setMarker(ValoreRicerca);
                            autoCompView.setText(null);
                        }
                    }
            );

            BtnPercorso.setOnClickListener(
                    v -> getDirections()
            );

            BtnPulisci.setOnClickListener(
                    v -> {
                        map.clear();
                        MapListaCoordinate.removeAll(MapListaCoordinate);
                        waypoints = "";
                        stato_destinazione = true;
                    }
            );

            btnTraffico.setOnClickListener(
                    v -> {
                        if (!stato_traffico) {
                            map.setTrafficEnabled(true);
                            stato_traffico = true;
                            btnTraffico.setText("Traffico ON");
                            objTextToSpeech.speak("Traffico Attivato", TextToSpeech.QUEUE_FLUSH, null);

                            Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Traffico Attivato", Toast.LENGTH_SHORT);
                            toast.show();

                        } else {
                            map.setTrafficEnabled(false);
                            stato_traffico = false;
                            btnTraffico.setText("Traffico OFF");
                            objTextToSpeech.speak("Traffico Disattivato", TextToSpeech.QUEUE_FLUSH, null);

                            Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Traffico Disattivato", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
            );

            btnParla.setOnClickListener(
                    v -> promptSpeechInput()
            );
        }

            return rootview;
    }

    //click autocomplete
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        String str = (String) adapterView.getItemAtPosition(position);
        Toast.makeText(getActivity(), str, Toast.LENGTH_SHORT).show();
    }

    public ArrayList<String> autocomplete(String input) throws UnsupportedEncodingException {
        ArrayList<String> resultList = null;
        RequestHttp request_autocomplete = new RequestHttp();
        JSONObject rispostaJson = null;

        try {
            JSONObject jboUrl = new JSONObject();

            jboUrl.put("language", "it");
            jboUrl.put("types", "address");
            jboUrl.put("input", String.valueOf(URLEncoder.encode(input,"utf8")));
            jboUrl.put("key", getString(R.string.google_app_id));

            rispostaJson = request_autocomplete.execute(PATH_AUTOCOMPLETE_MAPS, String.valueOf(jboUrl), "GET", "").get();

            JSONObject risultatoJson=new JSONObject(rispostaJson.getString("response"));
            JSONArray predsJsonArray = risultatoJson.getJSONArray("predictions");

            resultList = new ArrayList<String>(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
            }

        } catch (Exception e) {
            Log.e("errore:", "Error connecting to Places API", e);
            return resultList;
        } finally {

            if (!CheckConnessione.isConnected(getActivity())) {
                Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Assenza di connessione Dati", Toast.LENGTH_SHORT);
                toast.show();
            }
        }

        return resultList;
    }

    public void DeleteMarker(GoogleMap map, Marker marker) {
        for (int i = 0; i < MapListaCoordinate.size(); i++) {
            if (marker.getPosition().equals(MapListaCoordinate.get(i).get("latlng"))) {
                MapListaCoordinate.remove(i);
            }
        }
        marker.remove();
        map.clear();
        waypoints = "";
        for (int i = 0; i < MapListaCoordinate.size(); i++) {

            String lat_tmp = String.valueOf(((LatLng) Objects.requireNonNull(MapListaCoordinate.get(i).get("latlng"))).latitude);
            String long_tmp = String.valueOf(((LatLng) Objects.requireNonNull(MapListaCoordinate.get(i).get("latlng"))).longitude);

            waypoints = "|" + lat_tmp + "," + long_tmp + waypoints;
        }
        getDirections();
    }

    private void getDirections() {

        if (CheckConnessione.isConnected(getActivity()) && Current_longitude != null && Current_latitude != null) {
            if (MapListaCoordinate.size() > 0) {

                if (stato_destinazione) {
                    UtilityLocation test = new UtilityLocation();
                    indirizzo_destinazione = test.getLatLng(getActivity().getApplicationContext(), destinazione);
                    destinazione = Current_latitude + "," + Current_longitude;
                    stato_destinazione = false;
                }

                MapPoint = new HashMap<>();


                MapPoint.put("titolo", destinazione);
                MapPoint.put("latlng", indirizzo_destinazione);
                MapListaCoordinate.add(MapPoint);

                final ProgressDialog ProgressLoading = new ProgressDialog(getActivity());

                ProgressLoading.setTitle("");
                ProgressLoading.setCancelable(false);
                ProgressLoading.setMessage("Caricamento in corso...");
                ProgressLoading.show();

                objTextToSpeech.speak("Calcolo itinerario in corso...", TextToSpeech.QUEUE_FLUSH, null);

                Thread thread = new Thread(() -> {

                    RequestHttp request_directions = new RequestHttp();
                    JSONObject rispostaJson = null;
                    JSONObject jboUrl = new JSONObject();

                    try {

                        jboUrl.put("language", "it");
                        jboUrl.put("sensor", "false");
                        jboUrl.put("origin", Current_latitude + "," + Current_longitude);
                        jboUrl.put("destination", destinazione);
                        jboUrl.put("waypoints", "optimize:true"+waypoints);
                        jboUrl.put("key", getString(R.string.google_app_id));

                        rispostaJson = request_directions.execute(PATH_MAPS_DIRECTIONS, jboUrl.toString(), "GET", "").get();
                        JSONObject risultatoJson=new JSONObject(rispostaJson.getString("response"));
                        JSONArray routesArray = risultatoJson.getJSONArray("routes");
                        JSONObject route = routesArray.getJSONObject(0);
                        JSONObject poly = route.getJSONObject("overview_polyline");
                        String polyline = poly.getString("points");
                        polyz = decodePoly(polyline);
                        JSONArray steps = route.getJSONArray("legs").getJSONObject(0).getJSONArray("steps");
                        ArrayList<HashMap<String, String>> info_route = new ArrayList<>();

                        for (int i = 0; i < steps.length(); i++) {
                            HashMap<String, String> tmp_hash = new HashMap<>();
                            tmp_hash.put("distanze", steps.getJSONObject(i).getJSONObject("distance").getString("text"));
                            tmp_hash.put("durate", steps.getJSONObject(i).getJSONObject("duration").getString("text"));
                            tmp_hash.put("istruzioni", String.valueOf(Html.fromHtml(steps.getJSONObject(i).optString("html_instructions"))));
                            info_route.add(tmp_hash);
                        }


                        JSONArray legs_array = route.getJSONArray("legs");

                        MapListaCoordinate.removeAll(MapListaCoordinate);

                        for (int i = 0; i < legs_array.length(); i++) {
                            Log.e("size", String.valueOf(legs_array.length()));
                            JSONObject legs = legs_array.getJSONObject(i);
                            Log.e("titolo", legs.getString("end_address"));
                            Log.e("lat", legs_array.getJSONObject(i).getJSONObject("end_location").getString("lat"));
                            Log.e("lng", legs_array.getJSONObject(i).getJSONObject("end_location").getString("lng"));
                            LatLng latlng_tmp = new LatLng(Double.valueOf(legs_array.getJSONObject(i).getJSONObject("end_location").getString("lat")), Double.valueOf(legs_array.getJSONObject(i).getJSONObject("end_location").getString("lng")));

                            MapPoint = new HashMap<>();
                            MapPoint.put("titolo", legs.getString("end_address"));
                            MapPoint.put("latlng", latlng_tmp);
                            MapListaCoordinate.add(MapPoint);
                        }

                    } catch (Exception e) {
                        Log.w("err", "Exception: " + e.toString());
                    }

                    runOnUiThread(() -> {

                        map.clear();
                        addAllMarkers();
                        if (LineaDirezioni != null) {
                            LineaDirezioni.remove();
                        }

                        if (MapListaCoordinate.size() > 1) {
                            for (int i = 0; i < polyz.size() - 1; i++) {
                                LatLng src = polyz.get(i);
                                LatLng dest = polyz.get(i + 1);
                                LineaDirezioni = map.addPolyline(new PolylineOptions()
                                        .add(new LatLng(src.latitude, src.longitude),
                                                new LatLng(dest.latitude, dest.longitude))
                                        .width(10).color(Color.BLUE).geodesic(true));
                            }
                        }
                        ProgressLoading.cancel();
                    });
                });

                thread.start();
            } else {

                objTextToSpeech.speak("Impossibile calcolare un itinerario, Inserisci un indirizzo valido.", TextToSpeech.QUEUE_FLUSH, null);

                Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Impossibile calcolare un itinerario, Inserisci un indirizzo valido.", Toast.LENGTH_SHORT);
                toast.show();
            }
        } else {
            objTextToSpeech.speak("Assenza di segnale", TextToSpeech.QUEUE_FLUSH, null);

            Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Assenza di segnale Internet o GPS", Toast.LENGTH_SHORT);
            toast.show();
        }

    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    private String LatLongToAddress(Double lat, Double lng) throws IOException {
        Geocoder geocoder;
        List<android.location.Address> addresses;
        geocoder = new Geocoder(getActivity(), Locale.getDefault());

        addresses = geocoder.getFromLocation(lat, lng, 1);

        String address = addresses.get(0).getAddressLine(0);
        String city = addresses.get(0).getLocality();
        String state = addresses.get(0).getAdminArea();
        String country = addresses.get(0).getCountryName();
        String postalCode = addresses.get(0).getPostalCode();
        String knownName = addresses.get(0).getFeatureName();

        return address;

    }

    private GoogleMap.OnMyLocationChangeListener myLocationChangeListener = new GoogleMap.OnMyLocationChangeListener() {
        @Override
        public void onMyLocationChange(Location location) {
            if(Current_latitude== null || Current_longitude == null){
                Current_latitude = location.getLatitude();
                Current_longitude = location.getLongitude();
            }
            if(Current_latitude != location.getLatitude() || Current_longitude != location.getLongitude()){
                Current_latitude = location.getLatitude();
                Current_longitude = location.getLongitude();
                String IndirizzoAttuale = "";

                if (!primo_zoom) {
                    primo_zoom = true;
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(Current_latitude, Current_longitude), 8);
                    map.animateCamera(cameraUpdate);
                } else {
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(Current_latitude, Current_longitude), map.getCameraPosition().zoom);
                    map.animateCamera(cameraUpdate);
                }

                try {
                    IndirizzoAttuale = LatLongToAddress(Current_latitude, Current_longitude);
                } catch (IOException e) {
                    Log.w("err", "Exception: " + e);
                }

                JSONObject objSend = new JSONObject();

                try {
                    Date currentTime = Calendar.getInstance().getTime();

                    objSend.put("id",  jBUser.optString("id"));
                    objSend.put("lat", location.getLatitude());
                    objSend.put("lng", location.getLongitude());
                    objSend.put("title", jBUser.optString("cognome")+" "+ jBUser.optString("nome"));
                    objSend.put("desc", "Posizione inviata dall'app - " + currentTime);
                    objSend.put("address", IndirizzoAttuale);
                    objSend.put("room",  jBUser.optString("room"));
                    objSend.put("colorMarker",  jBUser.optString("colorMarker"));

                } catch (JSONException e) {
                    Log.w("err", "Exception: " + e);
                }

                SocketSingleton.get(getActivity()).getSocket().emit("send-position", objSend);

                Log.i("MyLocation: ", "Send: Latitude: " + location.getLatitude() + " - Longitude: " + location.getLongitude());

            }else{
                Log.i("MyLocation: ", "Latitude: " + location.getLatitude() + " - Longitude: " + location.getLongitude());
            }
        }

    };

    private void addAllMarkers() {
        for (int i = 0; i < MapListaCoordinate.size(); i++) {

            if (i == MapListaCoordinate.size() - 1) {
                map.addMarker(new MarkerOptions().position((LatLng) MapListaCoordinate.get(i).get("latlng")).title((String) MapListaCoordinate.get(i).get("titolo")).icon(BitmapDescriptorFactory.fromBitmap(writeTextOnDrawable(R.drawable.marker_fine, ""))));
            } else {
                map.addMarker(new MarkerOptions().position((LatLng) MapListaCoordinate.get(i).get("latlng")).title((String) MapListaCoordinate.get(i).get("titolo")).icon(BitmapDescriptorFactory.fromBitmap(writeTextOnDrawable(R.drawable.marker, String.valueOf(i + 1)))));
            }
        }
    }

    private void setMarker(String indirizzo) {
        UtilityLocation test = new UtilityLocation();
        LatLng address = test.getLatLng(getActivity().getApplicationContext(), indirizzo);
        if (address != null) {
            map.addMarker(new MarkerOptions().position(address).title(indirizzo).icon(BitmapDescriptorFactory.fromBitmap(writeTextOnDrawable(R.drawable.marker, ""))));

            map.moveCamera(CameraUpdateFactory.newLatLng(address));

            MapPoint = new HashMap<>();

            MapPoint.put("titolo", indirizzo);
            MapPoint.put("latlng", address);
            MapListaCoordinate.add(MapPoint);

            JSONObject objSend = new JSONObject();

            try {
                objSend.put("id",  jBUser.optString("id"));
                objSend.put("lat", address.latitude);
                objSend.put("lng", address.longitude);
                objSend.put("title", jBUser.optString("cognome") +" "+ jBUser.optString("nome"));
                objSend.put("desc", "Posizione inviata dall'app");
                objSend.put("address", indirizzo);
                objSend.put("room",  jBUser.optString("room"));
                objSend.put("colorMarker",  jBUser.optString("colorMarker"));
            } catch (JSONException e) {
                Log.w("err", "Exception: " + e);
            }

            SocketSingleton.get(getActivity()).getSocket().emit("send-position", objSend);

        } else {
            objTextToSpeech.speak("Indirizzo non valido.", TextToSpeech.QUEUE_FLUSH, null);
            Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Indirizzo non valido", Toast.LENGTH_SHORT);
            toast.show();
        }

        waypoints = "";
        for (int i = 0; i < MapListaCoordinate.size(); i++) {

            String lat_tmp = String.valueOf(((LatLng) Objects.requireNonNull(MapListaCoordinate.get(i).get("latlng"))).latitude);
            String long_tmp = String.valueOf(((LatLng) Objects.requireNonNull(MapListaCoordinate.get(i).get("latlng"))).longitude);

            waypoints = "|" + lat_tmp + "," + long_tmp + waypoints;
        }
    }

    private void promptSpeechInput() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "");

        try {
            startActivityForResult(intent, 100);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getActivity(), "Funzione Non disponibile.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100) {
            if (resultCode == getActivity().RESULT_OK && null != data) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                final String indirizzoVocale = result.get(0);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

                alertDialogBuilder.setTitle("Informazione");
                alertDialogBuilder.setMessage("Vuoi aggiungere " + indirizzoVocale + " ?");
                alertDialogBuilder.setPositiveButton("Si", (dialog, id) -> setMarker(indirizzoVocale));
                alertDialogBuilder.setNegativeButton("No", (dialog, id) -> dialog.cancel());
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        }
    }
    private Bitmap writeTextOnDrawable(int drawableId, String text) {

        Bitmap bm = BitmapFactory.decodeResource(getResources(), drawableId).copy(Bitmap.Config.ARGB_8888, true);

        Typeface tf = Typeface.create("Helvetica", Typeface.BOLD);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTypeface(tf);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(convertToPixels(getActivity(), 8));

        Rect textRect = new Rect();
        paint.getTextBounds(text, 0, text.length(), textRect);

        Canvas canvas = new Canvas(bm);

        if (textRect.width() >= (canvas.getWidth() - 4)) {
            paint.setTextSize(convertToPixels(getActivity(), 4));
        }

        int xPos = (canvas.getWidth() / 2);
        int yPos = (int) ((int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2)) - getDipFromPixels(13));
        canvas.drawText(text, xPos, yPos, paint);

        return bm;
    }

    public static int convertToPixels(Context context, int nDP) {
        final float conversionScale = context.getResources().getDisplayMetrics().density;
        return (int) ((nDP * conversionScale) + 0.5f);
    }
    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }
    @Override
    public void onPause() {
        if (objTextToSpeech != null) {
            objTextToSpeech.stop();
            objTextToSpeech.shutdown();
        }
        mapView.onPause();
        super.onPause();
    }
    @Override
    public void onDestroy() {

        if (objTextToSpeech != null) {
            objTextToSpeech.stop();
            objTextToSpeech.shutdown();
        }
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
    }

    public static float getDipFromPixels(float px) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_PX,
                px,
                Resources.getSystem().getDisplayMetrics()
        );
    }

    class GooglePlacesAutocompleteAdapter extends ArrayAdapter<String> implements Filterable {
        private ArrayList<String> resultList;
        public GooglePlacesAutocompleteAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int index) {
            return resultList.get(index);
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        try {
                            resultList = autocomplete(constraint.toString());
                        } catch (UnsupportedEncodingException e) {
                            Log.e("err", "Exception: " + e);
                        }

                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
            return filter;
        }
    }

    Emitter.Listener emitPosizione= args -> runOnUiThread(() -> {

        if (args[0] instanceof JSONObject) {

            final JSONObject data = (JSONObject) args[0];

            Log.i("Socket Listener:", String.valueOf(args[0]));

            try {
                if (!(data.getString("id").equals("1"))) {

                    if(data.getString("address").isEmpty()){
                        setMarker(data.getString("address"));
                    }
                }

            } catch (JSONException e) {
                Log.e("err", "Exception: " + e);
            }
        }
    });
}