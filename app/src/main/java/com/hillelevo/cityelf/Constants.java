package com.hillelevo.cityelf;

public class Constants {
  public static final String TAG = "CityElfLog";

  public static final String SEND_MASSAGE_URL = "http://localhost:8088/services/users/all";
  public static final String TEST_URL = "https://learn.javascript.ru/json";

  public class Actions {
    public static final String BROADCAST_ACTION_FIREBASE_TOKEN = "FirebaseTokenAction";
    public static final String BROADCAST_ACTION_FIREBASE_MESSAGE = "FirebaseMessageAction";
  }

  public class Params {
    public static final String FIREBASE_TOKEN = "FirebaseToken";
    public static final String FIREBASE_MESSAGE = "FirebaseMessage";
  }
}
