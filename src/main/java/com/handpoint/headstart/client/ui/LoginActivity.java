/*  Copyright 2013 Handpoint

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.handpoint.headstart.client.ui;

import java.security.NoSuchAlgorithmException;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.handpoint.headstart.android.HeadstartService;
import com.handpoint.headstart.client.R;
import com.handpoint.headstart.client.data.DaoHelper;
import com.handpoint.headstart.client.ui.ForgotPasscodeDialog.NoticeDialogListener;
import com.handpoint.headstart.client.util.SecurityUtil;

public class LoginActivity extends HeadstartActivity implements OnClickListener, NoticeDialogListener {

	private static final String ATTEMPT_COUNT = "attempt_count";
	private static final String LAST_ATTEMPT = "last_attempt";
	private static final long ATTEMPT_DELAY = 5*60*1000;
	
	protected SharedPreferences mPreferences;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        HeadstartService.removeProperty("last_activity");
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		long elapsedTime = SystemClock.elapsedRealtime();
		long lastAttempt = getLastAttempt();
		if (elapsedTime > lastAttempt && lastAttempt > 0 && lastAttempt + ATTEMPT_DELAY > elapsedTime) {
			long timeToWait = ATTEMPT_DELAY - (elapsedTime - lastAttempt);
			Toast.makeText(this, getString(R.string.warn_wait_message, formatTime(timeToWait)), Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		setLastAttempt(0);
		setContentView(R.layout.login);
		
		Button loginButton = (Button) findViewById(R.id.login_button);
		loginButton.setOnClickListener(this);

		TextView forgotLink = (TextView) findViewById(R.id.forgot_password_link);
		SpannableString content = new SpannableString(getString(R.string.forgot_password_link_label));
		content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
		forgotLink.setText(content);
		forgotLink.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				DialogFragment newFragment = new ForgotPasscodeDialog();
				newFragment.show(getSupportFragmentManager(), "forgot_passcode");
			}
		});
	}
	
	private boolean isPasswordValid(String password){
		try {
			return SecurityUtil.MD5Hash(password).equals(mPreferences.getString(SettingsActivity.PREFERENCE_PASSWORD, "-"));
		} catch (NoSuchAlgorithmException e) {
			return false;
		}
	}

	private void setAttemptCount(int count) {
        HeadstartService.setProperty(ATTEMPT_COUNT, Integer.toString(count));
		mPreferences.edit().putInt(ATTEMPT_COUNT, count).commit();
	}
	
	private int getAttemptCount() {
		String tmp = HeadstartService.getProperty(ATTEMPT_COUNT);
		if (null != tmp) {
			try {
				return Integer.parseInt(tmp);
			} catch (NumberFormatException e) {
			}
		}
		return mPreferences.getInt(ATTEMPT_COUNT, 0);
	}
	
	private void setLastAttempt(long time) {
        HeadstartService.setProperty(LAST_ATTEMPT, Long.toString(time));
		mPreferences.edit().putLong(LAST_ATTEMPT, time).commit();
	}
	
	private long getLastAttempt() {
		String tmp = HeadstartService.getProperty(LAST_ATTEMPT);
		if (null != tmp) {
			try {
				return Long.parseLong(tmp);
			} catch (NumberFormatException e) {
			}
		}
		return mPreferences.getLong(LAST_ATTEMPT, 0);
	}
	
	private String formatTime(long millis) {
		int seconds = (int) (millis / 1000) % 60 ;
		int minutes = (int) ((millis / (1000*60)) % 60);
		String minString = getString(R.string.minutes_short);
		String secString = getString(R.string.seconds_short);
		if (seconds == 0) {
			return String.format("%d " + minString, minutes);
		} else if (minutes == 0) {
			return String.format("%d " + secString, seconds);
		}
		return String.format("%d " + minString + ", %d " + secString, minutes,seconds);
	}

	@Override
	public void onClick(View v) {
		PasscodeFragment pf = (PasscodeFragment)getSupportFragmentManager().findFragmentById(R.id.password_layout);
		
		if (isPasswordValid(pf.getPassword())) {
			Intent resultIntent = new Intent();
			resultIntent.putExtra("password", pf.getPassword());
			setResult(RESULT_OK, resultIntent);
			setAttemptCount(0);
			setLastAttempt(0);
			finish();
		} else {
			setAttemptCount(getAttemptCount() + 1);
			if (getAttemptCount() > 2) {
				setAttemptCount(0);
				setLastAttempt(SystemClock.elapsedRealtime());
				Toast.makeText(this, getString(R.string.warn_wait_message, formatTime(ATTEMPT_DELAY)), Toast.LENGTH_LONG).show();
				setResult(RESULT_CANCELED);
				finish();
				return;
			}
			Toast.makeText(this, R.string.warn_authentication_failed, Toast.LENGTH_SHORT).show();
			pf.resetPassword();
		}		
	}

	@Override
	public void onDialogPositiveClick() {
		deleteDatabase(DaoHelper.DATABASE_NAME);
		mPreferences.edit().clear().commit();
		Intent resultIntent = new Intent();
		setResult(RESULT_OK, resultIntent);
		finish();
	}

	@Override
	public void onDialogNegativeClick() {
		//do nothing
	}
	
}
