package com.hillelevo.cityelf.firebase;

import static com.hillelevo.cityelf.Constants.TAG;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.hillelevo.cityelf.Constants.Actions;
import com.hillelevo.cityelf.Constants.Params;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


public class FirebaseTokenService extends FirebaseInstanceIdService {

  @Override
  public void onTokenRefresh() {
    // Get updated InstanceID token
    String refreshedToken = FirebaseInstanceId.getInstance().getToken();
    Log.d(TAG, "FBTokenService refreshed token: " + refreshedToken);

    // Send the Instance ID token to MainActivity
    sendLocalBroadcast(Actions.BROADCAST_ACTION_FIREBASE_TOKEN, refreshedToken);
  }

  /**
   * Send local broadcast to MainActivity
   */
  private void sendLocalBroadcast(String action, String token) {
    Intent localIntent = new Intent(action);
      localIntent.putExtra(Params.FIREBASE_TOKEN, token);

    LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
  }
}
