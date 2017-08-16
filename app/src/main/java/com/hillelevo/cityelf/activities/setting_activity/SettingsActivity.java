package com.hillelevo.cityelf.activities.setting_activity;


import static com.hillelevo.cityelf.Constants.TAG;

import android.util.Base64;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.model.LatLng;
import com.hillelevo.cityelf.Constants;
import com.hillelevo.cityelf.Constants.Prefs;
import com.hillelevo.cityelf.Constants.WebUrls;
import com.hillelevo.cityelf.R;
import com.hillelevo.cityelf.activities.AuthorizationActivity;
import com.hillelevo.cityelf.activities.MainActivity;
import com.hillelevo.cityelf.activities.map_activity.MapActivity;
import com.hillelevo.cityelf.data.UserLocalStore;
import com.hillelevo.cityelf.webutils.JsonMessageTask;
import com.hillelevo.cityelf.webutils.JsonMessageTask.JsonMessageResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SettingsActivity extends PreferenceActivity implements
    OnPreferenceChangeListener, OnSharedPreferenceChangeListener, JsonMessageResponse,
    OnPreferenceClickListener {

  private SwitchPreference notificationSwitch;
  private SwitchPreference notificationSMS;
  private ListPreference languagePref;
  private Preference addressPref;
  private EditTextPreference emailPref;
  private Preference exit;
  private RingtonePreference ringtonePref;
  private Preference pref;
  private Geocoder geocoder;
  private String userAddress;
  private Preference login;
  private CustomEditText password;
  private String oldPrefValue;
  private boolean resolutionUpdate = true;

  private int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;


  private String key;
  private String res = null;
  private boolean registered;

  private SharedPreferences sharedPreferences;

  private AppCompatDelegate delegate;
  private PreferenceCategory category;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setupActionBar();
    PreferenceManager prefMgr = getPreferenceManager();
    prefMgr.setSharedPreferencesName(Prefs.APP_PREFERENCES);
    prefMgr.setSharedPreferencesMode(Context.MODE_PRIVATE);
    geocoder = new Geocoder(this, new Locale("ru", "RU"));

    //HARDCODE
    //UserLocalStore.saveBooleanToSharedPrefs(getApplicationContext(), Prefs.REGISTERED, true);

    addPreferencesFromResource(R.xml.preferences);
    registered = UserLocalStore
        .loadBooleanFromSharedPrefs(getApplicationContext(), Prefs.REGISTERED);
    sharedPreferences = prefMgr.getSharedPreferences();
    category = (PreferenceCategory) findPreference("registered_user");

    addressPref = (Preference) findPreference("address");
    addressPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        try {
          AutocompleteFilter filter = new AutocompleteFilter.Builder()
              .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS)
              .setCountry("UA")
              .build();
          Intent intent =
              new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                  .setBoundsBias(MapActivity.BOUNDS_VIEW)
                  .setFilter(filter)
                  .build(SettingsActivity.this);

          startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
          e.printStackTrace();
        }
        return false;
      }
    });
    String str3 = UserLocalStore
        .loadStringFromSharedPrefs(getApplicationContext(), Prefs.ADDRESS_1);

    addressPref.setSummary(getFormatedStreetName(
        UserLocalStore.loadStringFromSharedPrefs(getApplicationContext(), Prefs.ADDRESS_1)));

    if (!registered) {
      Preference logout1 = findPreference("email");
      category.removePreference(logout1);
      Preference logout2 = findPreference("password");
      category.removePreference(logout2);
      /*Preference logout4 = findPreference("manyAddressPref");
      category.removePreference(logout4);*/
      PreferenceCategory category2 = (PreferenceCategory) findPreference("aboutPref");
      Preference logout5 = findPreference("osmdReg");
      category2.removePreference(logout5);
      PreferenceScreen screen = getPreferenceScreen();
      Preference pref = getPreferenceManager().findPreference("exitCategory");
      screen.removePreference(pref);

      login = (Preference) findPreference("login");
      login.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
          if (UserLocalStore.loadBooleanFromSharedPrefs(getApplicationContext(), Prefs.ANOMYMOUS)) {
            Intent intent = new Intent(SettingsActivity.this, AuthorizationActivity.class);
            startActivity(intent);
          } else {
            Intent firstStart = new Intent(SettingsActivity.this, MapActivity.class);
            startActivity(firstStart);
            Toast.makeText(SettingsActivity.this, "Please enter your address first",
                Toast.LENGTH_SHORT).show();
          }
          return false;
        }
      });


    } else {
      Preference logout = findPreference("login");
      category.removePreference(logout);

      password = (CustomEditText) findPreference("password");
      password.setSummary("");
      password.setOnPreferenceClickListener(this);

      emailPref = (EditTextPreference) findPreference("email");
      emailPref.setSummary(
          UserLocalStore.loadStringFromSharedPrefs(getApplicationContext(), Prefs.EMAIL));
      emailPref.setText(
          (UserLocalStore.loadStringFromSharedPrefs(getApplicationContext(), Prefs.EMAIL)));
      emailPref.setOnPreferenceChangeListener(this);

      exit = (Preference) findPreference("exit");
      exit.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
          UserLocalStore.clearUserData(getApplicationContext());
          return false;
        }
      });

    }

    notificationSwitch = (SwitchPreference) findPreference("notificationPush");
    notificationSwitch.setOnPreferenceChangeListener(this);

    notificationSMS = (SwitchPreference) findPreference("notificationSms");
    notificationSMS.setOnPreferenceChangeListener(this);

    /*languagePref = (ListPreference) findPreference("languagePref");
    languagePref.setOnPreferenceChangeListener(this);*/

    ringtonePref = (RingtonePreference) findPreference("ringtonePref");
    ringtonePref.setOnPreferenceChangeListener(this);


  }


  private void getToast(Object obj, int length) {
    Toast toast = Toast.makeText(getApplicationContext(),
        String.valueOf(obj), length);
    toast.show();
  }

  private void setupActionBar() {
    ActionBar actionBar = getDelegate().getSupportActionBar();
    if (actionBar != null) {
      actionBar.setHomeButtonEnabled(true);
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
      if (resultCode == RESULT_OK) {
        Place place = PlaceAutocomplete.getPlace(this, data);
        String engNameStreet = String.valueOf(place.getAddress());

        key = "googleapi";
        try {
          new JsonMessageTask(this)
              .execute("https://maps.googleapis.com/maps/api/geocode/json?address="
                      + URLEncoder.encode(engNameStreet, "UTF-8")+"&key=AIzaSyCvCVjPsoJyCifJNO9EtlJuBW53eQHPHpY&language=ru",
                  Constants.GET, null);
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }

      } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
        Status status = PlaceAutocomplete.getStatus(this, data);
        Log.d(TAG, status.getStatusMessage());

      }
    }
  }

  private void updateUserAddress(String address) {
    key = "address";
    JSONObject updatePreferenceObject = new JSONObject();
    try {
      updatePreferenceObject
          .put("id", UserLocalStore.loadIntFromSharedPrefs(getApplicationContext(),
              Prefs.USER_ID));

      JSONObject newAddress = new JSONObject();
      newAddress.put("id", 0);
      newAddress.put("address", address);
      newAddress.put("addressUa", address);
      JSONArray array = new JSONArray();
      array.put(newAddress);

      updatePreferenceObject.put("addresses", array);

      String jsonData = updatePreferenceObject.toString();

      new JsonMessageTask(SettingsActivity.this)
          .execute(WebUrls.UPDATE_USER_URL, "PUT", jsonData, UserLocalStore
              .loadStringFromSharedPrefs(getApplicationContext(), Prefs.AUTH_CERTIFICATE));

    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  /*
    private String sendGeo(LatLng coordinate) {
      List<Address> addresses = new ArrayList<>();
      try {
        addresses = geocoder.getFromLocation(coordinate.latitude, coordinate.longitude, 1);
      } catch (IOException e) {
        e.printStackTrace();
      }

      android.location.Address address = addresses.get(0);

      String str = address.getAddressLine(0);
      return address.getAddressLine(0);
    }
  */
  //btnBack home
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }


  @Override
  protected void onResume() {
    super.onResume();
    // Set up a listener whenever a key changes
    getPreferenceScreen().getSharedPreferences()
        .registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    // Unregister the listener whenever a key changes
    getPreferenceScreen().getSharedPreferences()
        .unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public boolean onPreferenceChange(Preference preference, Object newValue) {
    switch (preference.getKey()) {
      case "notificationSms":
      case "notificationPush":
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(80L);
        break;
      case "languagePref":
        Integer language = Integer.valueOf(String.valueOf(newValue));
        if (language == 1) {
          getToast("Русский", Toast.LENGTH_SHORT);
        } else {
          getToast("Украинский", Toast.LENGTH_SHORT);
        }
        break;
      case "password":
        oldPrefValue = UserLocalStore
            .loadStringFromSharedPrefs(getApplicationContext(), "password");
        break;
      case "email":
        //emailPref.setSummary(getShortAddress(emailPref.getText()));
        oldPrefValue = UserLocalStore.loadStringFromSharedPrefs(getApplicationContext(), "email");
        break;
    }
    return true;
  }


  private AppCompatDelegate getDelegate() {
    if (delegate == null) {
      delegate = AppCompatDelegate.create(this, null);
    }
    return delegate;
  }

  /*
    private String getShortAddress(String address) {
      if (address.contains("@")) {

        StringBuilder shortAddress = new StringBuilder();
        String[] twoWords = address.split("@");

        shortAddress.append(firstWord(twoWords[0]));
        shortAddress.append('@').append(twoWords[1]);

        return shortAddress.toString();
      } else {
        if (address.equals("")) {
          return "";
        }
        Toast toast = Toast.makeText(this,
            "Некорректный email", Toast.LENGTH_LONG);
        toast.show();
        emailPref.setText("");
        return "";
      }
    }
  */
  public static String getFormatedStreetName(String userAddress) {
    if (userAddress != null && !userAddress.equals("")) {
      if (userAddress.contains(", Одес")) {
        if (userAddress.contains("улица ")) {
          return userAddress
              .substring(userAddress.indexOf("улица "), userAddress.indexOf(", Одес"));
        } else if (userAddress.contains("вулиця ")) {
          return userAddress
              .substring(userAddress.indexOf("вулиця "), userAddress.indexOf(", Одес"));
        } else {
          return userAddress.substring(0, userAddress.indexOf(", Одес"));
        }
      } else {
        return userAddress;
      }
    }
    return "";
  }

/*  private static String firstWord(String firstPart) {
    char[] word = firstPart.toCharArray();
    StringBuilder str = new StringBuilder();
    for (int i = 0; i < firstPart.length(); i++) {
      if (i < 2) {
        str.append(word[i]);
      } else {
        str.append('*');
      }
    }
    return str.toString();
  }*/

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String _key) {

    if (resolutionUpdate) {
      pref = findPreference(_key.toLowerCase());
      key = _key;

      if (pref instanceof EditTextPreference || pref instanceof CustomEditText) {
        //EditTextPreference editTextPref = (EditTextPreference) pref;
        String str;
        if (pref instanceof CustomEditText) {
          str = ((CustomEditText) pref).getText();
        } else {
          str = ((EditTextPreference) pref).getText();

        }
        JSONObject updatePreferenceObject = new JSONObject();

        try {
          int userId = (UserLocalStore.loadIntFromSharedPrefs(getApplicationContext(),
              Prefs.USER_ID));
          updatePreferenceObject.put("id", userId);
          updatePreferenceObject.put(key, str);

          JSONObject newAddress = new JSONObject();
          newAddress.put("id",
              UserLocalStore.loadIntFromSharedPrefs(getApplicationContext(), Prefs.ADDRESS_1_ID));
          newAddress.put("address", "");
          newAddress.put("addressUa", "");
          JSONArray array = new JSONArray();
          array.put(newAddress);

          updatePreferenceObject.put("addresses", array);


        } catch (JSONException e) {
          e.printStackTrace();
        }
        String jsonData = updatePreferenceObject.toString();

        new JsonMessageTask(SettingsActivity.this)
            .execute(WebUrls.UPDATE_USER_URL, Constants.PUT, jsonData, UserLocalStore
                .loadStringFromSharedPrefs(getApplicationContext(), Prefs.AUTH_CERTIFICATE));
      }
    }
    resolutionUpdate = true;
  }

  @Override
  public void messageResponse(String output) {
    if (output.contains("Error")) {
      Log.i(TAG, output);
      saveOldVariable(oldPrefValue);
      String authCertificate = "Basic " + Base64.encodeToString(
          (UserLocalStore.loadStringFromSharedPrefs(getApplicationContext(), Prefs.EMAIL) + ":"
              + UserLocalStore
              .loadStringFromSharedPrefs(getApplicationContext(), Prefs.PASSWORD)).getBytes(),
          Base64.URL_SAFE | Base64.NO_WRAP);
      UserLocalStore.saveStringToSharedPrefs(getApplicationContext(), Prefs.AUTH_CERTIFICATE,
          authCertificate);

    } else {
      switch (key.toLowerCase()) {
        case "email":
          if (pref != null) {
            String value = ((EditTextPreference) pref).getText();
            Log.i(TAG, "New email " + value);
            pref.setSummary(value);
            resolutionUpdate = false;
            saveNewAuthSertificate(value);
          }
          break;
        case "password":
          //if (output.isEmpty() && pref != null) {
          String value = ((CustomEditText) pref).getText();
          Log.i(TAG, "New password " + value);
          resolutionUpdate = false;
          pref.setSummary("Изменён!");
          saveNewAuthSertificate(value);
          //}
          break;
        case "address1":
          //if (output.isEmpty()) {
          resolutionUpdate = false;
          loadUserData();
//          //}
          break;
        case "get_user":
          saveAddressId(output);
          break;
        case "googleapi":
          key = null;
          res = output;

          checkAddress();
//        updateUserAddress(userAddress);
          break;
      }
    }
  }

  private void saveOldVariable(String oldPrefValue) {
    if (key.equals("email")) {
      UserLocalStore
          .saveStringToSharedPrefs(getApplicationContext(), Prefs.EMAIL,
              oldPrefValue);
    }
    if (key.equals("password")) {
      UserLocalStore
          .saveStringToSharedPrefs(getApplicationContext(), Prefs.PASSWORD, oldPrefValue);
    }
  }

  private void saveNewAuthSertificate(String value) {

    if (key.equals("email")) {
      UserLocalStore
          .saveStringToSharedPrefs(getApplicationContext(), Prefs.EMAIL,
              value);
      resolutionUpdate = false;
      String authCertificate = "Basic " + Base64.encodeToString(
          (UserLocalStore.loadStringFromSharedPrefs(getApplicationContext(), Prefs.EMAIL) + ":"
              + UserLocalStore
              .loadStringFromSharedPrefs(getApplicationContext(), Prefs.PASSWORD)).getBytes(),
          Base64.URL_SAFE | Base64.NO_WRAP);
      UserLocalStore.saveStringToSharedPrefs(getApplicationContext(), Prefs.AUTH_CERTIFICATE,
          authCertificate);
    }
    if (key.equals("Password")) {
      UserLocalStore
          .saveStringToSharedPrefs(getApplicationContext(), Prefs.PASSWORD, value);
      String authCertificate = "Basic " + Base64.encodeToString((UserLocalStore
          .loadStringFromSharedPrefs(getApplicationContext(), Prefs.EMAIL + ":" + value))
          .getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
      UserLocalStore.saveStringToSharedPrefs(getApplicationContext(), Prefs.AUTH_CERTIFICATE,
          authCertificate);
    }
  }

  private void checkAddress() {
    userAddress = getAddressFromCoordinate();
    if (userAddress.contains(", Одес")) {
      addressPref.setSummary(getFormatedStreetName(userAddress));
      updateUserAddress(userAddress);
      UserLocalStore
          .saveStringToSharedPrefs(getApplicationContext(), Prefs.ADDRESS_1, userAddress);
    } else {
      getToast(Constants.ERROR_INPUT_ADDRESS, Toast.LENGTH_LONG);
    }
  }

  private void loadUserData() {
    int userId = UserLocalStore
        .loadIntFromSharedPrefs(getApplicationContext(), Prefs.USER_ID);
    try {
      new JsonMessageTask(this)
          .execute(WebUrls.GET_USERDATA_URL + URLEncoder.encode(String.valueOf(userId), "UTF-8"),
              Constants.GET, UserLocalStore
                  .loadStringFromSharedPrefs(getApplicationContext(), Prefs.AUTH_CERTIFICATE));
      key = "get_user";
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }

  private void saveAddressId(String output) {
    JSONObject jsonObject = null;
    try {
      jsonObject = new JSONObject(output);

      JSONArray addressJsonArray = (JSONArray) jsonObject.get("addresses");
      JSONObject addressJsonObject = addressJsonArray.getJSONObject(0);
      if (addressJsonArray.getJSONObject(0) == null) {
//              showMessage(message);
      }
      int addressId = addressJsonObject.getInt("id");
      UserLocalStore
          .saveIntToSharedPrefs(getApplicationContext(), Prefs.ADDRESS_1_ID, addressId);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  private String getAddressFromCoordinate() {
    String resultAddress = null;
    if (res != null && !res.contains("Error")) {
      JSONObject jsonObject = null;
      try {
        jsonObject = new JSONObject(res);

        JSONArray resultsArray = jsonObject.getJSONArray("results");
        JSONObject result = resultsArray.getJSONObject(0);
        resultAddress = result.getString("formatted_address");

      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
    return resultAddress;
  }


  @Override
  public boolean onPreferenceClick(Preference preference) {
    oldPrefValue = UserLocalStore
        .loadStringFromSharedPrefs(getApplicationContext(), Prefs.PASSWORD);

    return false;
  }
}