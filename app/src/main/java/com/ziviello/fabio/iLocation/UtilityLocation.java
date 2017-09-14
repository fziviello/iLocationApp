package com.ziviello.fabio.iLocation;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import com.google.android.gms.maps.model.LatLng;
import java.util.List;

/**
 * Created by Fabio on 22/01/2016.
 */
public class UtilityLocation {

    public LatLng getLatLng(Context context, String strAddress)
    {
        Geocoder coder= new Geocoder(context);
        List<Address> address;
        LatLng coordinate = null;

        try
        {
            address = coder.getFromLocationName(strAddress, 5);
            if(address==null || address.size() <= 0)
            {
                return null;
            }

            Address location = address.get(0);
            location.getLatitude();
            location.getLongitude();

            coordinate = new LatLng(location.getLatitude(), location.getLongitude());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return coordinate;
    }
}
