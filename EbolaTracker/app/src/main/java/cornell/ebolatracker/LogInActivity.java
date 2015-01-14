package cornell.ebolatracker;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


public class LogInActivity extends Activity {

    private Session my_session;
    private UiLifecycleHelper uiHelper;
    Intent callingIntent = null;

    private String facebookUserID;
    private String facebookUserName;
    private LoginButton facebookLogin;
    private TextView facebookStatus;

    BroadcastReceiver receiver;
    boolean reciever_registered = false;

    public void PrintEnvironmentHash()
    {
         try { // if hash key doesn't work!
            PackageInfo info = getPackageManager().getPackageInfo(
                    "cornell.ebolatracker",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.i("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }


    private Session.StatusCallback statusCallback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state,
                         Exception exception) {
            if (state.isOpened()) {
                Log.i("FacebookSampleActivity", "Facebook session opened");

                my_session = session;

            } else if (state.isClosed()) {
                Log.i("FacebookSampleActivity", "Facebook session closed");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //PrintEnvironmentHash();

        startService(new Intent(this, BackgroundLocationService.class));

        /*******************
         * Facebook Connect
         *******************/
        callingIntent = getIntent();
        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(this, statusCallback);
        uiHelper.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        facebookStatus = (TextView) findViewById(R.id.facebookt);
        facebookLogin = (LoginButton) findViewById(R.id.authButton);
        facebookLogin.setReadPermissions(Arrays.asList("user_friends", "user_likes"));
        facebookLogin.setUserInfoChangedCallback(new LoginButton.UserInfoChangedCallback() {
            @Override
            public void onUserInfoFetched(GraphUser user) {
                if (user != null) {
                    facebookStatus.setText("Hello, " + user.getName());
                    facebookUserID = user.getId();
                    facebookUserName =  user.getName();
                } else {
                    facebookStatus.setText("Facebook not connected.");
                }
            }
        });

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String msg = intent.getStringExtra(BackgroundLocationService.LOCATION_RESULT);
                String lat = intent.getStringExtra(BackgroundLocationService.LOCATION_LAT);
                String lon = intent.getStringExtra(BackgroundLocationService.LOCATION_LON);

                Log.d("Location Received: ", lat +" , "+lon);

                TextView LocationView = (TextView) findViewById(R.id.location);
                LocationView.setText("Location: "+msg);

                Notify(msg);

            }
        };
    }

    public void Notify(String loc)
    {
        NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
        bigStyle.setBigContentTitle("Ebola Tracker");
        bigStyle.bigText(loc);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.ic_launcher);
        mBuilder.setContentTitle("Ebola Tracker");
        mBuilder.setContentText(loc);
        mBuilder.setStyle(bigStyle);
        Notification note = mBuilder.build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(1, note);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.log_in, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        uiHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!reciever_registered)
        {
            LocalBroadcastManager.getInstance(this).registerReceiver((receiver), new IntentFilter(BackgroundLocationService.LOCATION_RESULT));
            reciever_registered = true;
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        uiHelper.onStop();
       // LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
        if(reciever_registered)
        {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
            reciever_registered = false;
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        uiHelper.onSaveInstanceState(savedState);
    }
}
