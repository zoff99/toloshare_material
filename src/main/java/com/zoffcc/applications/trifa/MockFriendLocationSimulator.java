package com.zoffcc.applications.trifa;

import static com.zoffcc.applications.trifa.MainActivity.INVALID_BEARING;
import static com.zoffcc.applications.trifa.MainActivity.android_tox_callback_friend_lossless_packet_cb_method;
import static com.zoffcc.applications.trifa.TRIFAGlobals.GEO_COORDS_CUSTOM_LOSSLESS_ID;

@SuppressWarnings("SpellCheckingInspection")
public class MockFriendLocationSimulator {

    private static final String TAG = "MockFrLSim";

    private double currentLat = 37.7749;
    private double currentLng = -122.4194;
    private float currentSpeedMs = 10.0f; // Start at ~36 km/h
    private float internalBearing = 0.0f;
    private boolean isStopped = false;
    private long friendnumber = 0;
    Handler mainHandler;
    Handler actionHandler;

    final static String GEO_COORD_PROTO_MAGIC = "TzGeo"; // must be exactly 5 char wide
    final static String GEO_COORD_PROTO_VERSION = "00"; // must be exactly 2 char wide

    /** @noinspection unused*/
    public MockFriendLocationSimulator(long friend_number) {
        this.friendnumber = friend_number;

        Handler.Looper looper = new Handler.Looper();
        looper.start();

        mainHandler = new Handler(looper);
        actionHandler = new Handler(looper);
    }

    public void startSimulation() {
        // Start the 1Hz location update loop
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                updateMockLocation();
                mainHandler.postDelayed(this, 1000);
            }
        });

        // Start the automated "Driving Script"
        runDrivingScript();
    }

    // Constant: Approximately 111,111 meters per degree of latitude
    private static final double METERS_PER_DEGREE = 111111.1;

    public static double addMetersToLatitude(double currentLat, double metersToAdd) {
        // Convert meters to degree offset
        double degreeOffset = metersToAdd / METERS_PER_DEGREE;

        // Return the new latitude
        return currentLat + degreeOffset;
    }

    // Earth's radius in meters
    private static final double EARTH_RADIUS = 6378137.0;

    public static double addMetersToLongitude(double currentLat, double currentLon, double metersToAdd) {
        // 1. Convert current latitude to radians for trigonometric functions
        double latInRadians = Math.toRadians(currentLat);

        // 2. Calculate the longitudinal offset in degrees
        // Formula: offset = (meters / (EARTH_RADIUS * cos(lat))) * (180 / PI)
        double lonOffset = (metersToAdd / (EARTH_RADIUS * Math.cos(latInRadians))) * (180.0 / Math.PI);

        // 3. Return the new longitude
        return currentLon + lonOffset;
    }

    /**
     * Schedules a sequence of driving behaviors over time.
     */
    private void runDrivingScript() {
        // 0s: Start driving straight at 10m/s

        // 5s: Speed up to 25m/s (~90 km/h)
        actionHandler.postDelayed(() -> setSpeed(25.0f), 5000);

        // 10s: Turn 90 degrees right (East)
        actionHandler.postDelayed(() -> turn(90), 10000);

        // 15s: Stop at a red light
        actionHandler.postDelayed(() -> setStopped(true), 15000);

        // 20s: Resume driving and turn 45 degrees left
        actionHandler.postDelayed(() -> {
            setStopped(false);
            setSpeed(15.0f);
            turn(-45);
        }, 20000);

        // 30s: Stop at a red light
        actionHandler.postDelayed(() -> setStopped(true), 30000);

        // 35s: Slow down to 1m/s (~3 km/h)
        actionHandler.postDelayed(() -> {
            setStopped(false);
            setSpeed(1.0f);
        }, 35000);
    }

    private void updateMockLocation() {
        if (!isStopped) {
            double distanceMoved = currentSpeedMs * 1.0;
            double latChange = (distanceMoved * Math.cos(Math.toRadians(internalBearing))) / 111111.0;
            double lngChange = (distanceMoved * Math.sin(Math.toRadians(internalBearing))) /
                    (111111.0 * Math.cos(Math.toRadians(currentLat)));

            currentLat += latChange;
            currentLng += lngChange;
        }

        Location mockLocation = new Location("gps");
        mockLocation.setLatitude(addMetersToLatitude(currentLat, 40 * this.friendnumber));
        mockLocation.setLongitude(addMetersToLongitude(currentLat, currentLng, 30 * this.friendnumber));
        mockLocation.setSpeed(isStopped ? 0.0f : currentSpeedMs);
        mockLocation.setAccuracy(1.0f);
        mockLocation.setTime(System.currentTimeMillis());

        if (isStopped)
        {
            mockLocation.setBearing(0.0f);
            mockLocation.removeBearing();
        }
        else
        {
            mockLocation.setBearing(internalBearing);
        }

        // set friends location here -----------------
        final byte[] data_bin = getGeoMsg(mockLocation);
        int data_bin_len = data_bin.length;
        data_bin[0] = (byte) GEO_COORDS_CUSTOM_LOSSLESS_ID;
        // Log.i(TAG, "fn=" + this.friendnumber + " " + mockLocation);
        android_tox_callback_friend_lossless_packet_cb_method(this.friendnumber, data_bin, data_bin_len);
        // set friends location here -----------------
    }

    public void setStopped(boolean stopped) { this.isStopped = stopped; }
    public void turn(float degrees) { this.internalBearing = (this.internalBearing + degrees) % 360; }
    public void setSpeed(float speedMs) { this.currentSpeedMs = speedMs; }

    public void stopSimulation() {
        mainHandler.removeCallbacksAndMessages(null);
        actionHandler.removeCallbacksAndMessages(null);
    }

    /** @noinspection UnnecessaryLocalVariable*/
    static byte[] getGeoMsg(Location location)
    {
        String bearing = "" + location.getBearing();
        if (!location.hasBearing())
        {
            bearing = INVALID_BEARING;
        }

        String temp_string = "X" + // the pkt ID will be added here later. needs to be exactly 1 char!
                GEO_COORD_PROTO_MAGIC +
                GEO_COORD_PROTO_VERSION  + ":BEGINGEO:" +
                location.getLatitude() + ":" +
                location.getLongitude() + ":" +
                location.getAltitude() + ":" +
                location.getAccuracy() + ":" +
                bearing + ":ENDGEO";
        // Log.i(TAG, "raw:" + temp_string);
        // Log.i(TAG, "rawlen:" + temp_string.length());

        byte[] data_bin = temp_string.getBytes(); // TODO: use specific characterset
        return data_bin;
    }
}
