package com.hillelevo.cityelf.activities;

import static com.hillelevo.cityelf.Constants.TAG;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable.ClassLoaderCreator;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hillelevo.cityelf.Constants;
import com.hillelevo.cityelf.Constants.Actions;
import com.hillelevo.cityelf.Constants.Prefs;
import com.hillelevo.cityelf.Constants.WebUrls;
import com.hillelevo.cityelf.R;
import com.hillelevo.cityelf.data.UserLocalStore;
import com.hillelevo.cityelf.activities.map_activity.MapActivity;
import com.hillelevo.cityelf.activities.setting_activity.SettingsActivity;
import com.hillelevo.cityelf.data.Advert;
import com.hillelevo.cityelf.data.Notification;
import com.hillelevo.cityelf.data.Poll;
import com.hillelevo.cityelf.fragments.AdvertFragment;
import com.hillelevo.cityelf.fragments.BottomDialogFragment;
import com.hillelevo.cityelf.fragments.NotificationFragment;
import com.hillelevo.cityelf.fragments.PollFragment;
import com.hillelevo.cityelf.webutils.JsonMessageTask;
import com.hillelevo.cityelf.webutils.JsonMessageTask.JsonMessageResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements JsonMessageResponse {

  private static String result;
  private boolean registered;
  private boolean osmd_admin;
  private boolean active;
  private UserLocalStore userLocalStore = null;
  private CustomPagerAdapter pagerAdapter;
  private ArrayList<Notification> notifications = new ArrayList<>();
  private ArrayList<Advert> adverts = new ArrayList<>();
  private ArrayList<Poll> polls = new ArrayList<>();

  private TabLayout tabLayout;

  private static SharedPreferences settings;
  private FirstStartApp firstStartApp;
  private JSONObject jsonObject = null;

  @Override
  protected void onResume() {
    super.onResume();
    active = true;
  }

  @Override
  protected void onPause() {
    super.onPause();
    active = false;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Check intent, send AddNewUser request to server
    Intent intent = getIntent();
    if (intent.hasExtra("AddUser")) {
      Toast.makeText(getApplicationContext(), "AddUser request sent", Toast.LENGTH_SHORT).show();
      //TODO Send AddNewUser request to server
      String firebseId = UserLocalStore
          .loadStringFromSharedPrefs(getApplicationContext(), Prefs.FIREBASE_ID);
      String address = UserLocalStore
          .loadStringFromSharedPrefs(getApplicationContext(), Prefs.ADDRESS_1);
      String bodyParams = "firebaseid=" + firebseId + "&address=" + address;

      new JsonMessageTask(MainActivity.this)
          .execute(WebUrls.ADD_NEW_USER, Constants.POST, bodyParams);
    }

    firstStartApp = new FirstStartApp(this);
    settings = getSharedPreferences(Prefs.APP_PREFERENCES, Context.MODE_PRIVATE);

    if (firstStartApp.isFirstLaunch()) {
      launchFirstTime();
      finish();
    }

    //showLoadingAlertDialog();

    // Load registered status from Shared Prefs
    registered = UserLocalStore
        .loadBooleanFromSharedPrefs(getApplicationContext(), Prefs.REGISTERED);
    osmd_admin = UserLocalStore
        .loadBooleanFromSharedPrefs(getApplicationContext(), Prefs.OSMD_ADMIN);

    Button buttonReport = (Button) findViewById(R.id.buttonReport);

    // Fill ViewPager with data
    startJsonResponse();
    ViewPager pager = (ViewPager) findViewById(R.id.viewpager);
    pagerAdapter = new CustomPagerAdapter(getSupportFragmentManager());
    pager.setAdapter(pagerAdapter);

    // Set custom tabs for ViewPager
    tabLayout = (TabLayout) findViewById(R.id.tabs);
    tabLayout.setupWithViewPager(pager);
    setupTabs();

    // Show Report dialog - BottomDialogFragment
    buttonReport.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        android.support.v4.app.FragmentTransaction ft = getSupportFragmentManager()
            .beginTransaction();
        android.support.v4.app.Fragment prev = getSupportFragmentManager()
            .findFragmentByTag("dialog");
        if (prev != null) {
          ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        BottomDialogFragment newFragment = BottomDialogFragment
            .newInstance(registered);
        newFragment.show(ft, "dialog");
      }

    });

    // Create LocalBroadcastManager and register it to all actions;
    LocalBroadcastManager messageBroadcastManager = LocalBroadcastManager.getInstance(this);
    messageBroadcastManager.registerReceiver(MessageReceiver,
        new IntentFilter(Actions.BROADCAST_ACTION_FIREBASE_TOKEN));
    messageBroadcastManager.registerReceiver(MessageReceiver,
        new IntentFilter(Actions.BROADCAST_ACTION_FIREBASE_MESSAGE));
  }

  private void launchFirstTime() {
    firstStartApp.setFirstLaunch(false);
    Intent firstStart = new Intent(MainActivity.this, MapActivity.class);
    startActivity(firstStart);
    finish();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    if (osmd_admin) {
      getMenuInflater().inflate(R.menu.menu2, menu);
    } else {
      getMenuInflater().inflate(R.menu.menu, menu);
    }
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.addPoll:
        Intent intent = new Intent(MainActivity.this, AdminActivity.class);
        startActivity(intent);
        return true;
      case R.id.settings:

        //// TODO: 17.07.17 This step depends from status-registred
        Intent intentLogin = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intentLogin);

        return true;
      case R.id.btnMap:
        Intent intentMap = new Intent(MainActivity.this, MapActivity.class);
        startActivity(intentMap);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private String getB64Auth(String login, String pass) {
    String source = login + ":" + pass;
    String ret =
        "Basic " + Base64.encodeToString(source.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
    return ret;
  }

  /**
   * BroadcastReceiver for local broadcasts
   */
  private BroadcastReceiver MessageReceiver = new BroadcastReceiver() {

    @Override
    public void onReceive(Context context, Intent intent) {

      String action = intent.getAction();
      String token = intent.getStringExtra(Prefs.FIREBASE_ID);
      Log.d(TAG, "MainActivity onReceive: " + action);
      Log.d(TAG, "MainActivity onReceive: " + token);
      if (active) {
        showDebugAlertDialog(token);
      }
    }
  };

  //Save and load data to Shared Prefs

//  public static void saveToSharedPrefs(String type, String data) {
//    Log.d(TAG, "MainActivity savedToSharedPrefs: " + type + ", " + data);
//    SharedPreferences.Editor editor = settings.edit();
//    editor.putString(type, data);
//    editor.apply();
//  }
//
//  private void saveToSharedPrefs(String type, boolean registered) {
//    Log.d(TAG, "MainActivity savedToSharedPrefs: " + type + ", " + registered);
//    SharedPreferences.Editor editor = settings.edit();
//    editor.putBoolean(type, registered);
//    editor.apply();
//  }
//
//  public static String loadStringFromSharedPRefs(String prefKey) {
//    if (settings != null && settings.contains(prefKey)) {
//      Log.d(TAG, "MainActivity mSettings != null, loading registration status");
//      return settings.getString(prefKey, "");
//    } else {
//      Log.d(TAG, "MainActivity mSettings != null, no registration status");
//      return "";
//    }
//  }
//
//  public static boolean loadBooleanStatusFromSharedPrefs(String prefKey) {
//    //Check for data by id
//    if (settings != null && settings.contains(prefKey)) {
//      Log.d(TAG, "MainActivity mSettings != null, loading registration status");
//      return settings.getBoolean(prefKey, true);
//    } else {
//      Log.d(TAG, "MainActivity mSettings != null, no registration status");
//      return false;
//    }
//  }

  // AlertDialog for firebase testing

  private void showDebugAlertDialog(String token) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Firebase id");

    // Set up the input
    final EditText input = new EditText(this);
    input.setInputType(InputType.TYPE_CLASS_TEXT);
    input.setText(token.toCharArray(), 0, token.length());
    builder.setView(input);

    // Set up the button
    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.cancel();
      }
    });

    builder.show();
  }

  // ProgressDialog for loading data

  private ProgressDialog progressDialog;

  private void showLoadingAlertDialog() {
    progressDialog = ProgressDialog.show(MainActivity.this, "", "Загрузка данных...", true);
  }

  /**
   * Custom Adapter for ViewPager
   */
  private class CustomPagerAdapter extends FragmentPagerAdapter {

    public CustomPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public int getItemPosition(Object object) {
      return POSITION_NONE;
    }

    @Override
    public Fragment getItem(int pos) {
      switch (pos) {
        case 0:
          return NotificationFragment.newInstance(notifications);
        case 1:
          return AdvertFragment.newInstance(adverts);
        case 2:
          return PollFragment.newInstance(polls);
        default:
          return NotificationFragment.newInstance(notifications);
      }


    }

    /**
     * Tabs in ViewPager
     *
     * @return tabs amount
     */
    @Override
    public int getCount() {
      // Registered user has 3 tabs
      if (registered) {
        return 3;
      }
      // Unregistered - one tab, Notifications
      else {
        return 1;
      }
    }
  }

  private void startJsonResponse() {
    if (firstStartApp.isFirstLaunch()) {
      UserLocalStore.saveStringToSharedPrefs(getApplicationContext(), Prefs.ADDRESS_1, null);
    } else {
      String address = UserLocalStore.loadStringFromSharedPrefs(getApplicationContext(),
          Prefs.ADDRESS_1);
      try {
        new JsonMessageTask(this)
            .execute(WebUrls.GET_ALL_FORECASTS + URLEncoder.encode(address, "UTF-8"),
                Constants.GET);
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
    }
  }

  //message from JsonMessageTask
  @Override
  public void messageResponse(String output) {
    try {
      JSONObject jsn = new JSONObject(output);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    showMessage(output);
    fillData(output);
  }


  public void showMessage(String message) {
    Toast toast = Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG);
    toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
    toast.show();
  }

  // Hardcoded method to fill up test Notifications, Adverts and Polls
  private void fillData(String message) {
    JSONObject jsonObject = null;
    JSONObject addressJsonObject = null;
    String title = null;
    String start = null;
    String estimatedStop = null;
    String address = null;
    int count = 0;

    if (message == null || message.isEmpty()) {
      showMessage("No Forecast");
    } else {
      try {
        jsonObject = new JSONObject(message);

        while (count < jsonObject.length()) {

          if (jsonObject.getJSONObject("Water") != null) {

            JSONObject waterJsonObject = jsonObject.getJSONObject("Water");
            title = "Отключение воды";
            start = waterJsonObject.getString("start");
            estimatedStop = waterJsonObject.getString("estimatedStop");

            addressJsonObject = waterJsonObject.getJSONObject("address");
            address = addressJsonObject.getString("address");
            notifications
                .add(new Notification(title, address, "2 часа", start,
                    "", 0));
            count++;
            continue;
          } else if (jsonObject.getJSONObject("Gas") != null) {
            JSONObject gasJsonObject = jsonObject.getJSONObject("Gas");
            title = "Отключение газа";
            start = gasJsonObject.getString("start");
            estimatedStop = gasJsonObject.getString("estimatedStop");

            addressJsonObject = gasJsonObject.getJSONObject("address");
            address = addressJsonObject.getString("address");
            notifications
                .add(new Notification(title, address, "2 часа", start,
                    "", 0));
            count++;
            continue;
          } else if (jsonObject.getJSONObject("Electricity") != null) {

            JSONObject electricityJsonObject = jsonObject.getJSONObject("Electricity");

            title = "Отключение света";
            start = electricityJsonObject.getString("start");
            estimatedStop = electricityJsonObject.getString("estimatedStop");

            addressJsonObject = electricityJsonObject.getJSONObject("address");
            address = addressJsonObject.getString("address");
            notifications
                .add(new Notification(title, address, "2 часа", start,
                    "", 0));
            count++;
            continue;
          }

          count++;

//      adverts.add(new Advert("Объявление 1", "Тестовая улица, 1", "сегодня",
//          "Тестовый опрос тест тест тест тест тест тест тест тест тест тест тест "
//              + "тест тест тест тест тест тест тест тест тест тест тест тест тест тест "
//              + "тест тест тест тест тест тест тест "));
//
//      polls.add(new Poll("Опрос 1", "Тестовая улица, 1", "2 часа", "сегодня",
//          "Тестовый опрос тест тест тест тест тест тест тест тест тест тест тест "
//              + "тест тест тест тест тест тест тест тест тест тест тест тест тест тест "
//              + "тест тест тест тест тест тест тест ", "Вариант 1", "Вариант 2",
//          "Вариант 3", "Вариант 4", 10));
//
        }

        // Add new data to ViewPager
        pagerAdapter.notifyDataSetChanged();
        setupTabs();
        progressDialog.dismiss();

      } catch (JSONException e) {
        e.printStackTrace();
      }

    }
  }

  /**
   * Set up tabs for ViewPager
   */
  private void setupTabs() {
    TextView tabOne = (TextView) LayoutInflater.from(this).inflate(R.layout.view_pager_tab, null);
    tabOne.setText(R.string.tab_notifications_title);
    tabLayout.getTabAt(0).setCustomView(tabOne);
    if (!registered) {
      tabLayout.setSelectedTabIndicatorColor(00000000);
    }

    if (registered) {
      TextView tabTwo = (TextView) LayoutInflater.from(this).inflate(R.layout.view_pager_tab, null);
      tabTwo.setText(R.string.tab_adverts_title);
      tabLayout.getTabAt(1).setCustomView(tabTwo);

      TextView tabThree = (TextView) LayoutInflater.from(this)
          .inflate(R.layout.view_pager_tab, null);
      tabThree.setText(R.string.tab_polls_title);
      tabLayout.getTabAt(2).setCustomView(tabThree);
    }
  }
}