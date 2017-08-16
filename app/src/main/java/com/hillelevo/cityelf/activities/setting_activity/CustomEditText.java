package com.hillelevo.cityelf.activities.setting_activity;

import com.hillelevo.cityelf.Constants.Prefs;
import com.hillelevo.cityelf.R;
import com.hillelevo.cityelf.RxEditText;
import com.hillelevo.cityelf.activities.MainActivity;
import com.hillelevo.cityelf.data.UserLocalStore;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func2;

public class CustomEditText extends DialogPreference implements OnClickListener {

  private EditText etPassword1;
  private EditText etPassword2;
  private Button positiveButton;

  public CustomEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
    setDialogLayoutResource(R.layout.custom_edit_text);
  }

  @Override
  protected void showDialog(Bundle state) {
    super.showDialog(state);

    positiveButton = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
    positiveButton.setEnabled(false);
    positiveButton.setOnClickListener(this);
  }

  @Override
  protected void onBindDialogView(View view) {

    etPassword1 = (EditText) view.findViewById(R.id.first_password);
    etPassword2 = (EditText) view.findViewById(R.id.second_password);

    Observable<String> password1 = RxEditText.getTextWatcherObserv(etPassword1);
    Observable<String> password2 = RxEditText.getTextWatcherObserv(etPassword2);
    Observable.combineLatest(password1, password2, new Func2<String, String, Boolean>() {
      @Override
      public Boolean call(String s, String s2) {
        if (!s2.equals(s) || s.isEmpty() || s2.isEmpty() || s.length() < 6 || s.length() < 6) {
          return true;
        } else {
          return false;
        }
      }

    }).subscribe(new Action1<Boolean>() {
      @Override
      public void call(Boolean aBoolean) {
        if (aBoolean) {
          etPassword1.setBackgroundResource(R.drawable.background_authorization_et_error);
          etPassword2.setBackgroundResource(R.drawable.background_authorization_et_error);
          positiveButton.setEnabled(false);
        } else {
          etPassword1.setBackgroundResource(R.drawable.background_authorization_et);
          etPassword2.setBackgroundResource(R.drawable.background_authorization_et);
          positiveButton.setEnabled(true);
        }

      }
    });
    super.onBindDialogView(view);
  }

  @Override
  public OnPreferenceChangeListener getOnPreferenceChangeListener() {
    return super.getOnPreferenceChangeListener();
  }

  @Override
  public void onClick(View view) {
    UserLocalStore.saveStringToSharedPrefs(getContext(), Prefs.PASSWORD,
        String.valueOf(etPassword1.getText()));
        getDialog().dismiss();
  }

  public String getText() {
    return String.valueOf(etPassword1.getText());
  }
}
