package com.hillelevo.cityelf.activities.setting_activity;


import static android.R.attr.key;

import com.hillelevo.cityelf.Constants.Prefs;
import com.hillelevo.cityelf.Constants.WebUrls;
import com.hillelevo.cityelf.R;
import com.hillelevo.cityelf.activities.MainActivity;
import com.hillelevo.cityelf.data.UserLocalStore;
import com.hillelevo.cityelf.webutils.JsonMessageTask;
import com.hillelevo.cityelf.webutils.JsonMessageTask.JsonMessageResponse;

import android.support.annotation.NonNull;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
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
import android.view.MenuItem;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity implements
    OnPreferenceChangeListener, OnSharedPreferenceChangeListener, JsonMessageResponse {

  private SwitchPreference notificationSwitch;
  private SwitchPreference notificationSMS;
  private ListPreference languagePref;
  private EditTextPreference addressPref;
  private EditTextPreference emailPref;
  private Preference exit;
  private RingtonePreference ringtonePref;
  private Preference pref;

  private String key;
  private String res = null;
  private boolean registered;

  private SharedPreferences sharedPreferences;

  private AppCompatDelegate delegate;
  PreferenceCategory category;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setupActionBar();
    PreferenceManager prefMgr = getPreferenceManager();
    prefMgr.setSharedPreferencesName(Prefs.APP_PREFERENCES);
    prefMgr.setSharedPreferencesMode(Context.MODE_PRIVATE);

    //HARDCODE
    //UserLocalStore.saveBooleanToSharedPrefs(getApplicationContext(), Prefs.REGISTERED, true);

    addPreferencesFromResource(R.xml.preferences);
    registered = UserLocalStore
        .loadBooleanFromSharedPrefs(getApplicationContext(), Prefs.REGISTERED);
    sharedPreferences = prefMgr.getSharedPreferences();
    category = (PreferenceCategory) findPreference("registered_user");

    if (!registered) {
      Preference logout1 = findPreference("email");
      category.removePreference(logout1);
      Preference logout2 = findPreference("password");
      category.removePreference(logout2);
      Preference logout3 = findPreference("address");
      category.removePreference(logout3);
      Preference logout4 = findPreference("manyAddressPref");
      category.removePreference(logout4);
      PreferenceCategory category2 = (PreferenceCategory) findPreference("aboutPref");
      Preference logout5 = findPreference("osmdReg");
      category2.removePreference(logout5);
      PreferenceScreen screen = getPreferenceScreen();
      Preference pref = getPreferenceManager().findPreference("exitCategory");
      screen.removePreference(pref);

    } else {
      Preference logout = findPreference("register");
      category.removePreference(logout);

      emailPref = (EditTextPreference) findPreference("email");
      emailPref.setSummary(getShortAddress(
          UserLocalStore.loadStringFromSharedPrefs(getApplicationContext(), Prefs.EMAIL)));
      emailPref.setText(getShortAddress(
          (UserLocalStore.loadStringFromSharedPrefs(getApplicationContext(), Prefs.EMAIL))));
      emailPref.setOnPreferenceChangeListener(this);

      addressPref = (EditTextPreference) findPreference("address");
      addressPref.setSummary(getFormatedStreetName(
          UserLocalStore.loadStringFromSharedPrefs(getApplicationContext(), Prefs.ADDRESS_1)));
      addressPref.setText(getFormatedStreetName(
          UserLocalStore.loadStringFromSharedPrefs(getApplicationContext(), Prefs.ADDRESS_1)));
      addressPref.setOnPreferenceChangeListener(this);

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

    languagePref = (ListPreference) findPreference("languagePref");
    languagePref.setOnPreferenceChangeListener(this);

    ringtonePref = (RingtonePreference) findPreference("ringtonePref");
    ringtonePref.setOnPreferenceChangeListener(this);


  }


  private void getToast(Object obj) {
    Toast toast = Toast.makeText(getApplicationContext(),
        String.valueOf(obj), Toast.LENGTH_SHORT);
    toast.show();
  }

  private void setupActionBar() {
    ActionBar actionBar = getDelegate().getSupportActionBar();
    if (actionBar != null) {
      actionBar.setHomeButtonEnabled(true);
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
  }


  //btnBack home
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        startActivity(new Intent(this, MainActivity.class));
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
        //// TODO: 17.06.17 send sms status
      case "notificationPush":
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(80L);
        break;
      case "languagePref":
        Integer language = Integer.valueOf(String.valueOf(newValue));
        if (language == 1) {
          getToast("Русский");
        } else {
          getToast("Украинский");
        }
        break;
      case "address":
        addressPref.setSummary(addressPref.getText());
        break;
      case "email":
        emailPref.setSummary(getShortAddress(emailPref.getText()));
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

  private String getFormatedStreetName(String userAddress) {
    if (userAddress != null && !userAddress.equals("")) {
      if (userAddress.contains(", Одес")) {
        return userAddress.substring(0, userAddress.indexOf(", Одес"));
      } else {
        return userAddress;
      }
    } else {
      return "";
    }
  }

  private static String firstWord(String firstPart) {
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
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String _key) {
    pref = findPreference(_key);
    key = _key;

    if (pref instanceof EditTextPreference && !key.equals("password")) {
      EditTextPreference editTextPref = (EditTextPreference) pref;
      //// TODO: 27.07.17 send to server
      JSONObject updatePreferenceObject = new JSONObject();
/*
      try {
        // HARDCODED!
        updatePreferenceObject.put("id", "13");
        //updatePreferenceObject.put("phone", "0975555555");
//        updatePreferenceObject.put("id", sharedPref.getId);
        updatePreferenceObject.put(key, editTextPref.getText());

      } catch (JSONException e) {
        e.printStackTrace();
      }
      String jsonData = updatePreferenceObject.toString();

      new JsonMessageTask(SettingsActivity.this).execute(WebUrls.UPDATE_USER_URL, "PUT", jsonData);
*/
      String s = ((EditTextPreference) pref).getText();
      if (key.equals("email")) {
        pref.setSummary(getShortAddress(s));
      } else if (key.equals("address")) {
        pref.setSummary(s);
      }

    }
  }

  @Override

  public void messageResponse(String output) {
    res = output;

    if (output.isEmpty()) {
//   TODO
    }
  }
}