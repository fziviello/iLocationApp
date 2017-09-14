package com.ziviello.fabio.iLocation;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.List;

/**
 * Created by Fabio on 20/01/2016.
 */
public class AddressSolver extends AsyncTask<Location, Void, String> {

    private Geocoder geo = null;

    public AddressSolver(Geocoder geo )
    {
        this.geo=geo;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(Location... params) {
        Location pos = params[0];
        double latitude = pos.getLatitude();
        double longitude = pos.getLongitude();

        List<Address> addresses = null;

        try {
            addresses = geo.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {

        }
        if (addresses != null) {
            if (addresses.isEmpty()) {

                return "-";
            }
            else
            {
                if (addresses.size() > 0)
                {
                    StringBuffer address = new StringBuffer();
                    Address tmp = addresses.get(0);

                    for (int y = 0; y < tmp.getMaxAddressLineIndex(); y++)
                    {
                        address.append(tmp.getAddressLine(y) + "\n");
                    }

                    return address.toString();
                }
            }
        }
        return "-";
    }

    @Override
    protected void onPostExecute(String result)
    {
       //Log.e("geo: ", result);
    }
}
