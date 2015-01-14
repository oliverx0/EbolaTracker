package cornell.ebolatracker;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import java.text.DateFormat;
import java.util.Date;

public class BackgroundLocationService extends Service implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener {


    IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        public BackgroundLocationService getServerInstance() {
            return BackgroundLocationService.this;
        }
    }

    static final public String LOCATION_RESULT = "cornell.ebolatracker.LOCATION_CHANGED_MSG";
    static final public String LOCATION_LAT = "cornell.ebolatracker.LOCATION_CHANGED_LAT";
    static final public String LOCATION_LON = "cornell.ebolatracker.LOCATION_CHANGED_LON";

    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;
    LocalBroadcastManager broadcaster;

    // Flag that indicates if a request is underway.
    private boolean mInProgress;
    private Boolean servicesAvailable = false;


    @Override
    public void onCreate() {
        super.onCreate();
        broadcaster = LocalBroadcastManager.getInstance(this);

        servicesAvailable = servicesConnected();
        Log.d("Google Play Services Available? ", servicesAvailable.toString());

        if(servicesAvailable)
        {
            mInProgress = false;
            mLocationRequest = LocationRequest.create();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            mLocationRequest.setInterval(10*60*5);
            mLocationRequest.setFastestInterval(10*60*2);
            mLocationClient = new LocationClient(this, this, this);
        }

    }
    private boolean servicesConnected() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        } else {
            return false;
        }
    }

    public int onStartCommand (Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        setUpLocationClientIfNeeded();

        if(!servicesAvailable || mLocationClient.isConnected() || mInProgress)
            return START_STICKY;

        if(!mLocationClient.isConnected() || !mLocationClient.isConnecting() && !mInProgress)
        {
            Log.d("Connect Location", DateFormat.getDateTimeInstance().format(new Date()) + ": Started");
            mInProgress = true;
            mLocationClient.connect();
        }
        return START_STICKY;
    }

    private void setUpLocationClientIfNeeded()
    {
        if(mLocationClient == null)
            mLocationClient = new LocationClient(this, this, this);
    }


    @Override
    public void onLocationChanged(Location location) {
        String msg = Double.toString(location.getLatitude()) + "," + Double.toString(location.getLongitude()) + " -- "+DateFormat.getDateTimeInstance().format(new Date());
        Log.d("Location", msg);

        /******************************************
         * Send location update to activity
         *****************************************/
        Intent intent = new Intent(LOCATION_RESULT);
        if(msg != null)
        {
            intent.putExtra(LOCATION_RESULT, msg);
            intent.putExtra(LOCATION_LAT, Double.toString(location.getLatitude()));
            intent.putExtra(LOCATION_LON, Double.toString(location.getLongitude()));
        }

        broadcaster.sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy(){
        mInProgress = false;
        if(servicesAvailable && mLocationClient != null) {
            mLocationClient.removeLocationUpdates(this);
            mLocationClient = null;
        }
        Log.d("Connect Location", DateFormat.getDateTimeInstance().format(new Date()) + ": Stopped");
        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocationClient.requestLocationUpdates(mLocationRequest, this);
        Log.d("Background Location Service: ", DateFormat.getDateTimeInstance().format(new Date()) + ": Connected");
    }

    @Override
    public void onDisconnected() {
        mInProgress = false;
        mLocationClient = null;
        Log.d("Background Location Service: ", DateFormat.getDateTimeInstance().format(new Date()) + ": Disconnected");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mInProgress = false;

        if (connectionResult.hasResolution()) {

        } else {

        }
    }
}
