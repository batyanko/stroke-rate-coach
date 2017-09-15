/*
 * The code below was taken from StackOverflow user ISURU
 * (https://stackoverflow.com/users/3276345/isuru)
 * as an answer to a question
 * (https://stackoverflow.com/questions/15570542/determining-the-speed-of-a-vehicle-using-gps-in-android)
 * by StackOverflow user John Kulova
 * (https://stackoverflow.com/users/1550867/john-kulova)
 * The code is licensed under CC BY-SA 3.0 ( http://creativecommons.org/licenses/by-sa/3.0/ ).
 *
 * Minor modifications: 2017 Yanko Georgiev
 */

package com.batyanko.strokeratecoach.Speed;

import android.location.Location;

public class CLocation extends Location {

    private boolean bUseMetricUnits = false;

    public CLocation(Location location)
    {
        this(location, true);
    }

    public CLocation(Location location, boolean bUseMetricUnits) {
        // TODO Auto-generated constructor stub
        super(location);
        this.bUseMetricUnits = bUseMetricUnits;
    }


    public boolean getUseMetricUnits()
    {
        return this.bUseMetricUnits;
    }

    public void setUseMetricunits(boolean bUseMetricUntis)
    {
        this.bUseMetricUnits = bUseMetricUntis;
    }

    @Override
    public float distanceTo(Location dest) {
        // TODO Auto-generated method stub
        float nDistance = super.distanceTo(dest);
        if(!this.getUseMetricUnits())
        {
            //Convert meters to feet
            nDistance = nDistance * 3.28083989501312f;
        }
        return nDistance;
    }

    @Override
    public float getAccuracy() {
        // TODO Auto-generated method stub
        float nAccuracy = super.getAccuracy();
        if(!this.getUseMetricUnits())
        {
            //Convert meters to feet
            nAccuracy = nAccuracy * 3.28083989501312f;
        }
        return nAccuracy;
    }

    @Override
    public double getAltitude() {
        // TODO Auto-generated method stub
        double nAltitude = super.getAltitude();
        if(!this.getUseMetricUnits())
        {
            //Convert meters to feet
            nAltitude = nAltitude * 3.28083989501312d;
        }
        return nAltitude;
    }

    @Override
    public float getSpeed() {
        // TODO Auto-generated method stub
        float nSpeed = super.getSpeed() * 3.6f;
        if(!this.getUseMetricUnits())
        {
            //Convert meters/second to miles/hour
            nSpeed = nSpeed * 2.2369362920544f/3.6f;
        }
        return nSpeed;
    }



}