package com.hillelevo.cityelf.activities.map_activity;

import static com.hillelevo.cityelf.Constants.TAG;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.hillelevo.cityelf.Constants;
import com.hillelevo.cityelf.Constants.Actions;
import com.hillelevo.cityelf.Constants.Prefs;
import com.hillelevo.cityelf.Constants.WebUrls;
import com.hillelevo.cityelf.R;
import com.hillelevo.cityelf.activities.MainActivity;
import com.hillelevo.cityelf.activities.AuthorizationActivity;
import com.hillelevo.cityelf.data.UserLocalStore;
import com.hillelevo.cityelf.activities.setting_activity.SettingsActivity;
import com.hillelevo.cityelf.webutils.JsonMessageTask;
import com.hillelevo.cityelf.webutils.JsonMessageTask.JsonMessageResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback,
    View.OnClickListener, GoogleApiClient.OnConnectionFailedListener,
    GoogleApiClient.ConnectionCallbacks, JsonMessageResponse {

  private String jsonMassageResult;
  private boolean registered;
  private boolean active;


  private GoogleMap mMap;
  private LatLng defaultMarker;
  private LatLngBounds LIMIT_OF_SITY = new LatLngBounds(new LatLng(46.313394, 30.650575),
      new LatLng(46.683114, 30.940929));
  private LatLng coordinate;
  private UiSettings uiSettings;

  private ImageButton btnSearchAddress;
  private ImageButton btnClear;
  private Button btnCheckStatus;

  private Geocoder geocoder;
  private Locale ruLocale = new Locale("ru", "RU");

  private String userAddress = "Канатна, 22";

  private static final String LOG_TAG = "MapActivity";
  private static final int GOOGLE_API_CLIENT_ID = 0;
  private AutoCompleteTextView mAutocompleteTextView;
  private static GoogleApiClient mGoogleApiClient;
  private PlaceArrayAdapter mPlaceArrayAdapter;
  private static final LatLngBounds BOUNDS_VIEW = new LatLngBounds(
      new LatLng(46.325628, 30.677791), new LatLng(46.598067, 30.797954));
  private CameraPosition cameraPosition;

  private String nameOfStreet = null;
  private MarkerOptions markerOptions;
  private Marker marker;

  boolean status;

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
    setContentView(R.layout.activity_map);

    btnSearchAddress = (ImageButton) findViewById(R.id.btnSearchAddress);
    btnSearchAddress.setOnClickListener(this);
    btnCheckStatus = (Button) findViewById(R.id.btnCheckStatus);
    btnCheckStatus.setOnClickListener(this);
    btnClear = (ImageButton) findViewById(R.id.btnClear);
    btnClear.setOnClickListener(this);
    btnClear.setVisibility(View.INVISIBLE);
    mAutocompleteTextView = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);

    registered = UserLocalStore.loadBooleanFromSharedPrefs(getApplicationContext(), Prefs.OSMD_ADMIN);

    geocoder = new Geocoder(this, ruLocale);

    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);

    showHideImageBtnClearInputText(mAutocompleteTextView, btnClear);

    registerConnectToGoogle();

    autocompleteInputStreet(mAutocompleteTextView);

    // Create LocalBroadcastManager and register it to all actions;
    LocalBroadcastManager messageBroadcastManager = LocalBroadcastManager.getInstance(this);
    messageBroadcastManager.registerReceiver(MessageReceiver,
        new IntentFilter(Actions.BROADCAST_ACTION_FIREBASE_TOKEN));
    messageBroadcastManager.registerReceiver(MessageReceiver,
        new IntentFilter(Actions.BROADCAST_ACTION_FIREBASE_MESSAGE));
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.settings:
        //// TODO: 17.07.17 This step depends from status-registered
          Intent intentLogin = new Intent(MapActivity.this, SettingsActivity.class);
          startActivity(intentLogin);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }


  private void showHideImageBtnClearInputText(EditText inputText, final ImageButton button) {
    inputText.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence inputText, int i, int i1, int i2) {
        if (inputText.toString().trim().length() == 0) {
          button.setVisibility(View.INVISIBLE);
        } else {
          button.setVisibility(View.VISIBLE);
        }
      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

      }

      @Override
      public void afterTextChanged(Editable editable) {

      }
    });
  }

  public void autocompleteInputStreet(AutoCompleteTextView mAutocompleteTextView) {
    mAutocompleteTextView.setThreshold(3);
    mAutocompleteTextView.setOnItemClickListener(mAutocompleteClickListener);
    AutocompleteFilter filter = new AutocompleteFilter.Builder()
        .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS)
        .setCountry("UA")
        .build();//country filter
    mPlaceArrayAdapter = new PlaceArrayAdapter(this, android.R.layout.simple_list_item_1,
        BOUNDS_VIEW, filter);
    mAutocompleteTextView.setAdapter(mPlaceArrayAdapter);
  }

  public void registerConnectToGoogle() {
    mGoogleApiClient = new GoogleApiClient.Builder(this)
        .addApi(Places.GEO_DATA_API)
        .addApi(Places.PLACE_DETECTION_API)
        .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
        .addConnectionCallbacks(this)
        .build();
    mGoogleApiClient.connect();
  }


  @Override
  public void onMapReady(GoogleMap googleMap) {
    mMap = googleMap;
    uiSettings = mMap.getUiSettings();
    uiSettings.setMapToolbarEnabled(false); //hide google icons
    // Default marker from center Odessa
    defaultMarker = new LatLng(46.4796777, 30.7457675);
    //create default marker
    markerOptions = new MarkerOptions()
        .position(defaultMarker)
        .title("Вы тут")
        .icon(BitmapDescriptorFactory.defaultMarker(
            BitmapDescriptorFactory.HUE_AZURE))
        .draggable(true);    //move marker

    marker = mMap.addMarker(markerOptions);

    mMap.setOnMarkerDragListener(new OnMarkerDragListener() {
      @Override
      public void onMarkerDragStart(Marker marker) {

      }

      @Override
      public void onMarkerDrag(Marker marker) {

      }

      @Override
      public void onMarkerDragEnd(Marker marker) {
        status = true;
        coordinate = marker.getPosition();
        userAddress = sendGeo(coordinate, marker);
        mAutocompleteTextView.setText(shortAddress(userAddress) + " ");
        nameOfStreet = userAddress;
        mAutocompleteTextView.setSelection(mAutocompleteTextView.getText().length());
        getToast(userAddress);
      }
    });

    //Animate orientation camera
    cameraPosition = new CameraPosition.Builder()
        .target(defaultMarker)
        .zoom(13)
        .bearing(0)
        .tilt(0)
        .build();

    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

    // Camera move limit
    mMap.setLatLngBoundsForCameraTarget(LIMIT_OF_SITY);

  }

  public boolean getVerificationCity(String street) {
    return street.contains("Одеса") || street.contains("Одесса");
  }

  private CharSequence shortAddress(String userAddress) {
    if (getVerificationCity(userAddress)) {
      return userAddress.substring(0, userAddress.indexOf(", Одес"));
    } else {
      getToast("Возможно этот адрес не находится в Одессе");
      return userAddress;
    }
  }

  //marker return name of street
  private String sendGeo(LatLng point, Marker marker) {

    List<Address> addresses = new ArrayList<>();
    try {
      addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1);
    } catch (IOException e) {
      e.printStackTrace();
    }

    android.location.Address address = addresses.get(0);
    StringBuilder sb = null;
    if (address != null) {
      sb = new StringBuilder();
      for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
        if (i == 1) {
          continue;
        }
        sb.append(address.getAddressLine(i) + "\n");
      }
    }

    assert sb != null;
    return address.getAddressLine(0);
  }


  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.btnSearchAddress:
        //todo send request to status
        hideKeyboard();
        if (nameOfStreet != null) {
          new JsonMessageTask(this)
              .execute(WebUrls.ADDRESS_URL + getFormatedAddressToJSON(nameOfStreet)
                      + WebUrls.API_KEY_URL,
                  Constants.GET);
          mMap.animateCamera(CameraUpdateFactory.zoomTo(19));
        } else {
          getToast("Неверный адрес");
        }
        break;
      case R.id.btnClear:
        mAutocompleteTextView.setText("");
        btnClear.setVisibility(View.INVISIBLE);
        break;
      case R.id.btnCheckStatus:
        if (status) {
          status = false;
          if (nameOfStreet != null && mAutocompleteTextView != null
              && mAutocompleteTextView.length() != 0) {
            if (getVerificationCity(nameOfStreet)) {
              //todo If the street is not in Odessa
              UserLocalStore
                  .saveStringToSharedPrefs(getApplicationContext(), Prefs.ADDRESS_1, nameOfStreet);
              Intent intentMain = new Intent(MapActivity.this, MainActivity.class);
              intentMain.putExtra("AddUser", true);
              startActivity(intentMain);
            }
          }
        } else {
          getToast("Неверный адрес");
        }
        break;
    }
  }



  private String getFormatedAddressToJSON(String userAddress) {
    String result = null;
    result = userAddress.replaceAll(",", "").replaceAll(" ", "+");

    return result;
  }

  private double[] parseJsonResponse(String response) {
    if (response == null) {
      return null;
    }

    JSONObject jsonObject = null;
    try {
      double[] LatLng = new double[2];
      jsonObject = new JSONObject(response);

      JSONArray resultsArray = jsonObject.getJSONArray("results");
      JSONObject result = resultsArray.getJSONObject(0);
      JSONObject geometry = result.getJSONObject("geometry");
      JSONObject location = geometry.getJSONObject("location");

      String locationLat = location.getString("lat");
      LatLng[0] = Double.valueOf(locationLat);
      String locationLng = location.getString("lng");
      LatLng[1] = Double.valueOf(locationLng);

      return LatLng;
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return null;

  }

  private void sendAddressFromCoordinate() {

    if (jsonMassageResult != null) {
      marker.remove();

      LatLng newMarker = new LatLng(parseJsonResponse(jsonMassageResult)[0], parseJsonResponse(
          jsonMassageResult)[1]);
      marker = mMap.addMarker(markerOptions);
      marker.setPosition(newMarker);
      mMap.moveCamera(CameraUpdateFactory.newLatLng(newMarker));
    } else {
      getToast("Empty");
    }
  }


  private AdapterView.OnItemClickListener mAutocompleteClickListener
      = new AdapterView.OnItemClickListener() {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      final PlaceArrayAdapter.PlaceAutocomplete item = mPlaceArrayAdapter.getItem(position);
      final String placeId = String.valueOf(item.placeId);
      Log.d(TAG, "Selected: " + item.description);

      nameOfStreet = String.valueOf(item.description);
      mAutocompleteTextView.setText(shortAddress(nameOfStreet) + " ");
      mAutocompleteTextView.setSelection(mAutocompleteTextView.getText().length());
      hideKeyboard();


      PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
          .getPlaceById(mGoogleApiClient, placeId);
      placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
      Log.d(TAG, "Fetching details for ID: " + item.placeId);
    }
  };

  private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
      = new ResultCallback<PlaceBuffer>() {
    @Override
    public void onResult(PlaceBuffer places) {
      if (!places.getStatus().isSuccess()) {
        Log.d(TAG, "Place query did not complete. Error: " +
            places.getStatus().toString());
        return;
      }
      // Selecting the first object buffer.
      final Place place = places.get(0);
      status = true;
      CharSequence attributions = places.getAttributions();
    }
  };

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    mPlaceArrayAdapter.setGoogleApiClient(mGoogleApiClient);
    Log.d(TAG, "Google Places API connected.");
  }

  @Override
  public void onConnectionSuspended(int i) {
    mPlaceArrayAdapter.setGoogleApiClient(null);
    Log.d(TAG, "Google Places API connection suspended.");
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    Log.d(TAG, "Google Places API connection failed with error code: "
        + connectionResult.getErrorCode());

    Toast.makeText(this,
        "Google Places API connection failed with error code:" +
            connectionResult.getErrorCode(),
        Toast.LENGTH_LONG).show();
  }

  private void getToast(Object object) {
    Toast toast = Toast.makeText(getApplicationContext(),
        String.valueOf(object), Toast.LENGTH_LONG);
    toast.show();
  }

  @Override
  public void messageResponse(String output) {
    jsonMassageResult = output;
    sendAddressFromCoordinate();
  }

  /**
   * BroadcastReceiver for local broadcasts
   */
  private BroadcastReceiver MessageReceiver = new BroadcastReceiver() {

    @Override
    public void onReceive(Context context, Intent intent) {

      String action = intent.getAction();
      String token = intent.getStringExtra(Prefs.FIREBASE_ID);
      Log.d(TAG, "MapActivity onReceive: " + action);
      Log.d(TAG, "MapActivity onReceive: " + token);
      if (active) {
        showDebugAlertDialog(token);
      }
    }
  };

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

  private void hideKeyboard() {
    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
  }
}