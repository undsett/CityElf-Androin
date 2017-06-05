package com.hillelevo.cityelf.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.hillelevo.cityelf.Constants;
import com.hillelevo.cityelf.Constants.Actions;
import com.hillelevo.cityelf.Constants.Params;
import com.hillelevo.cityelf.R;
import com.hillelevo.cityelf.activities.map_activity.MapActivity;
import com.hillelevo.cityelf.webutils.JsonMassageTask;
import static com.hillelevo.cityelf.Constants.TAG;

public class MainActivity extends AppCompatActivity {


  private Button btnTest;
  private Button btnJsonTest;
  private static String result;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    btnTest = (Button) findViewById(R.id.btnTest);
    btnTest.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent = new Intent(MainActivity.this, MapActivity.class);
        startActivity(intent);
      }
    });

    btnJsonTest = (Button) findViewById(R.id.btnJsonTest);
    btnJsonTest.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        new JsonMassageTask().execute(Constants.TEST_URL);
        Toast toast = Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.show();
      }
    });

    // Create LocalBroadcastManager and register it to all actions;
    LocalBroadcastManager messageBroadcastManager = LocalBroadcastManager.getInstance(this);
    messageBroadcastManager.registerReceiver(MessageReceiver,
            new IntentFilter(Actions.BROADCAST_ACTION_FIREBASE_TOKEN));
    messageBroadcastManager.registerReceiver(MessageReceiver,
            new IntentFilter(Actions.BROADCAST_ACTION_FIREBASE_MESSAGE));
  }

  /**
   * BroadcastReceiver for local broadcasts
   */
  private BroadcastReceiver MessageReceiver = new BroadcastReceiver() {

    @Override
    public void onReceive(Context context, Intent intent) {

      String action = intent.getAction();
      String token = intent.getStringExtra(Params.FIREBASE_TOKEN);
      Log.d(TAG, "MainActivity onReceive: " + action);
      Log.d(TAG, "MainActivity onReceive: " + token);
    }
  };

  public static void reciveResult(String output){
    result = output;
  }
}