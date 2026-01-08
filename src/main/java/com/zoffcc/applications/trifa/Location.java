package com.zoffcc.applications.trifa;

/**
 * Enhanced Desktop Java implementation of android.location.Location.
 */
public class Location {
    private String provider;
    private double latitude;
    private double longitude;
    private float accuracy;
    private double altitude;
    private float speed;
    private long time;

    // Bearing fields
    private float bearing;
    private boolean hasBearing = false;

    public Location(String provider) {
        this.provider = provider;
        this.time = System.currentTimeMillis();
    }

    // --- Bearing Methods ---

    /**
     * Returns the bearing in degrees at the time of this location.
     * Range: [0.0, 360.0). If hasBearing() is false, returns 0.0.
     */
    public float getBearing() {
        return bearing;
    }

    /**
     * Sets the bearing in degrees. The value will be normalized to [0.0, 360.0).
     */
    public void setBearing(float bearing) {
        // Normalize bearing to ensure it is within [0.0, 360.0)
        while (bearing < 0.0f) bearing += 360.0f;
        while (bearing >= 360.0f) bearing -= 360.0f;
        this.bearing = bearing;
        this.hasBearing = true;
    }

    /**
     * Returns true if this location has a bearing.
     */
    public boolean hasBearing() {
        return hasBearing;
    }

    /**
     * Removes the bearing from this location.
     */
    public void removeBearing() {
        this.bearing = 0.0f;
        this.hasBearing = false;
    }

    // --- Standard Getters and Setters ---

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public float getAccuracy() { return accuracy; }
    public void setAccuracy(float accuracy) { this.accuracy = accuracy; }

    public double getAltitude() { return altitude; }
    public void setAltitude(double altitude) { this.altitude = altitude; }

    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }

    public long getTime() { return time; }
    public void setTime(long time) { this.time = time; }

    /**
     * Calculates distance to another location using the Haversine formula.
     */
    public float distanceTo(Location dest) {
        double earthRadius = 6371000;
        double dLat = Math.toRadians(dest.latitude - this.latitude);
        double dLng = Math.toRadians(dest.longitude - this.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(dest.latitude)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (float) (earthRadius * c);
    }

    @Override
    public String toString() {
        return String.format("Location[provider=%s, lat=%.6f, lon=%.6f, bearing=%s]",
                provider, latitude, longitude, hasBearing ? bearing : "none");
    }
}
