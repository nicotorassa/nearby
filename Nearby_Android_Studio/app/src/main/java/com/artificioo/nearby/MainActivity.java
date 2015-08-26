package com.artificioo.nearby;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;

import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "Nearby";
    private GoogleApiClient mGoogleApiClient;
    private MessageListener mMessageListener;
    private Message mMessage;
    private boolean mResolvingError;
    private int REQUEST_RESOLVE_ERROR = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCraete");
        setContentView(R.layout.activity_main);

        // Create an instance of MessageListener
        mMessageListener = new MessageListener() {
            @Override
            public void onFound(final Message message) {
                Log.e(TAG, "holaaaaa**********");

                runOnUiThread(new Runnable() {
                    @TargetApi(Build.VERSION_CODES.KITKAT)
                    @Override
                    public void run() {
                        String recibido = new String(message.getContent(), StandardCharsets.UTF_8);
                        Toast.makeText(getApplicationContext(), recibido, Toast.LENGTH_LONG).show();
                        Log.e(TAG, recibido);
                    }
                });


            }
        };

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mMessage = new Message("HOLAAAA".getBytes());

        Button b_send = (Button) findViewById(R.id.b_send);

        b_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText et_send = (EditText) findViewById(R.id.et_send);
                mMessage = new Message(et_send.getText().toString().getBytes());
                publishAndSubscribe();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient.isConnected()) {
            // Clean up when the user leaves the activity.
            Nearby.Messages.unpublish(mGoogleApiClient, mMessage)
                    .setResultCallback(new ErrorCheckingCallback("unpublish()"));
            Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener)
                    .setResultCallback(new ErrorCheckingCallback("unsubscribe()"));
        }
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    // GoogleApiClient connection callback.
    @Override
    public void onConnected(Bundle connectionHint) {
        Nearby.Messages.getPermissionStatus(mGoogleApiClient).setResultCallback(
                new ErrorCheckingCallback("getPermissionStatus", new Runnable() {
                    @Override
                    public void run() {
                        publishAndSubscribe();
                    }
                })
        );
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    // This is called in response to a button tap in the Nearby permission dialog.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Permission granted or error resolved successfully then we proceed
                // with publish and subscribe..
                publishAndSubscribe();
            } else {
                // This may mean that user had rejected to grant nearby permission.
                Log.e(TAG, "Failed to resolve error with code " + resultCode);
            }
        }
    }

    private void publishAndSubscribe() {
        // We automatically subscribe to messages from nearby devices once
        // GoogleApiClient is connected. If we arrive here more than once during
        // an activity's lifetime, we may end up with multiple calls to
        // subscribe(). Repeated subscriptions using the same MessageListener
        // are ignored.
        Nearby.Messages.publish(mGoogleApiClient, mMessage)
                .setResultCallback(new ErrorCheckingCallback("publish()"));
        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener)
                .setResultCallback(new ErrorCheckingCallback("subscribe()"));
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    /**
     * A simple ResultCallback that displays a toast when errors occur.
     * It also displays the Nearby opt-in dialog when necessary.
     */
    private class ErrorCheckingCallback implements ResultCallback<Status> {
        private final String method;
        private final Runnable runOnSuccess;

        private ErrorCheckingCallback(String method) {
            this(method, null);
        }

        private ErrorCheckingCallback(String method, @Nullable Runnable runOnSuccess) {
            this.method = method;
            this.runOnSuccess = runOnSuccess;
        }

        @Override
        public void onResult(@NonNull Status status) {
            if (status.isSuccess()) {
                Log.e(TAG, method + " succeeded.");
                if (runOnSuccess != null) {
                    runOnSuccess.run();
                }
            } else {
                // Currently, the only resolvable error is that the device is not opted
                // in to Nearby. Starting the resolution displays an opt-in dialog.
                if (status.hasResolution()) {
                    if (!mResolvingError) {
                        try {
                            status.startResolutionForResult(MainActivity.this,
                                    REQUEST_RESOLVE_ERROR);
                            mResolvingError = true;
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(TAG, method + " failed with exception: " + e);
                        }
                    } else {
                        // This will be encountered on initial startup because we do
                        // both publish and subscribe together.  So having a toast while
                        // resolving dialog is in progress is confusing, so just log it.
                        Log.e(TAG, method + " failed with status: " + status
                                + " while resolving error.");
                    }
                } else {
                    Log.e(TAG, method + " failed with : " + status
                            + " resolving error: " + mResolvingError);
                }
            }
        }
    }
}
