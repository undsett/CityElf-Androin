package com.hillelevo.cityelf.activities.map_activity;

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
import com.hillelevo.cityelf.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.location.Address;
import android.location.Geocoder;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

@RequiresApi(api = VERSION_CODES.LOLLIPOP)
public class MapActivity extends FragmentActivity implements OnMapReadyCallback,
    View.OnClickListener, GoogleApiClient.OnConnectionFailedListener,
    GoogleApiClient.ConnectionCallbacks {

  private GoogleMap mMap;
  private LatLng defaultMarker;
  private LatLngBounds LIMIT_OF_SITY = new LatLngBounds(new LatLng(46.400, 30.530),
      new LatLng(46.550, 30.850));
  private LatLng coordinate;
  private UiSettings uiSettings;

  private Button btnAccount;
  private Button btnCheckStatus;

  private Geocoder geocoder;
  private Locale ruLocale = new Locale.Builder().setLanguage("ru").setScript("Cyrl").build();

  private String userAddress = "Канатна, 22";

  private static final String LOG_TAG = "MapActivity";
  private static final int GOOGLE_API_CLIENT_ID = 0;
  private AutoCompleteTextView mAutocompleteTextView;
  private GoogleApiClient mGoogleApiClient;
  private PlaceArrayAdapter mPlaceArrayAdapter;
  private static final LatLngBounds BOUNDS_MOUNTAIN_VIEW = new LatLngBounds(
      new LatLng(46.325628, 30.677791), new LatLng(46.598067, 30.797954));

  private String nameOfStreet = null;
  private MarkerOptions markerOptions;
  Marker marker;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_map);

    btnAccount = (Button) findViewById(R.id.btnSendAddress);
    btnAccount.setOnClickListener(this);
    btnCheckStatus = (Button) findViewById(R.id.btnCheckStatus);
    btnCheckStatus.setOnClickListener(this);

    geocoder = new Geocoder(this, ruLocale);

    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);

    mGoogleApiClient = new GoogleApiClient.Builder(MapActivity.this)
        .addApi(Places.GEO_DATA_API)
        .addApi(Places.PLACE_DETECTION_API)
        .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
        .addConnectionCallbacks(this)
        .build();
    mGoogleApiClient.connect();
    mAutocompleteTextView = (AutoCompleteTextView) findViewById(R.id
        .autoCompleteTextView);
    mAutocompleteTextView.setThreshold(3);
    mAutocompleteTextView.setOnItemClickListener(mAutocompleteClickListener);
    AutocompleteFilter filter = new AutocompleteFilter.Builder()
        .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS)
        .setCountry("UA")
        .build();//country filter
    mPlaceArrayAdapter = new PlaceArrayAdapter(this, android.R.layout.simple_list_item_1,
        BOUNDS_MOUNTAIN_VIEW, filter);
    mAutocompleteTextView.setAdapter(mPlaceArrayAdapter);
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
        coordinate = marker.getPosition();
        userAddress = sendGeo(coordinate, marker);
        System.out.println(userAddress);
      }
    });

    //Animate orientation camera
    CameraPosition cameraPosition = new CameraPosition.Builder()
        .target(defaultMarker)
        .zoom(13)
        .bearing(0)
        .tilt(0)
        .build();

    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

    // Camera move limit
    mMap.setLatLngBoundsForCameraTarget(LIMIT_OF_SITY);


  }

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
      Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();//message on screen
    }

    assert sb != null;
    return address.getAddressLine(0);
  }


  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.btnSendAddress:
        System.out.println(userAddress);//marker address
        //todo send address from server
        break;
      case R.id.btnCheckStatus:
        //todo send request to status
        if (nameOfStreet != null) {
          sendAddressFromCoordinate();
        }
        break;
    }

  }

  private void sendAddressFromCoordinate() {
    marker.remove();

    List<Address> address = new ArrayList<>();
    try {
      address = geocoder.getFromLocationName(nameOfStreet, 1);
    } catch (IOException e) {
      e.printStackTrace();
    }
    LatLng newMarker = new LatLng(address.get(0).getLatitude(), address.get(0).getLongitude());
    marker = mMap.addMarker(markerOptions);
    marker.setPosition(newMarker);
    mMap.moveCamera(CameraUpdateFactory.newLatLng(newMarker));

  }


  private AdapterView.OnItemClickListener mAutocompleteClickListener
      = new AdapterView.OnItemClickListener() {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      final PlaceArrayAdapter.PlaceAutocomplete item = mPlaceArrayAdapter.getItem(position);
      final String placeId = String.valueOf(item.placeId);
      Log.i(LOG_TAG, "Selected: " + item.description);

      nameOfStreet = String.valueOf(item.description);

      PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
          .getPlaceById(mGoogleApiClient, placeId);
      placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
      Log.i(LOG_TAG, "Fetching details for ID: " + item.placeId);
    }
  };

  private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
      = new ResultCallback<PlaceBuffer>() {
    @Override
    public void onResult(PlaceBuffer places) {
      if (!places.getStatus().isSuccess()) {
        Log.e(LOG_TAG, "Place query did not complete. Error: " +
            places.getStatus().toString());
        return;
      }
      // Selecting the first object buffer.
      final Place place = places.get(0);
      CharSequence attributions = places.getAttributions();
/*
      mNameTextView.setText(Html.fromHtml(place.getName() + ""));
      mAddressTextView.setText(Html.fromHtml(place.getAddress() + ""));
      mIdTextView.setText(Html.fromHtml(place.getId() + ""));
      */
    }
  };

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    mPlaceArrayAdapter.setGoogleApiClient(mGoogleApiClient);
    Log.i(LOG_TAG, "Google Places API connected.");
  }

  @Override
  public void onConnectionSuspended(int i) {
    mPlaceArrayAdapter.setGoogleApiClient(null);
    Log.e(LOG_TAG, "Google Places API connection suspended.");
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    Log.e(LOG_TAG, "Google Places API connection failed with error code: "
        + connectionResult.getErrorCode());

    Toast.makeText(this,
        "Google Places API connection failed with error code:" +
            connectionResult.getErrorCode(),
        Toast.LENGTH_LONG).show();
  }
}
