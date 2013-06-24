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

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.actionbarsherlock.view.MenuItem;
import com.handpoint.headstart.android.HeadstartService;
import com.handpoint.headstart.client.R;
import com.handpoint.headstart.client.data.SenderHelper;

public class MerchantEmailServerSettingsActivity extends HeadstartPreferenceActivity  implements OnPreferenceChangeListener {//implements OnSharedPreferenceChangeListener {
	private static final int NONE = 0;
	private static final int SSL = 1;
	private static final int TLS = 2;

	EditTextPreference mUser;
	EditTextPreference mPswd;
	EditTextPreference mHost;
	EditTextPreference mPort;
	ListPreference mSupportedProtocol;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.merchant_email_server_preferences);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		initPreferences();
		setListeners();
		initDefaults();
    	getSupportActionBar().setDisplayShowTitleEnabled(false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getListView().setCacheColorHint(Color.TRANSPARENT);				
	}
	
	private void initDefaults() {
		mUser.setText(mPreferences.getString(SenderHelper.USER, ""));
		mPswd.setText(mPreferences.getString(SenderHelper.PSWD, ""));
		if (mPreferences.getString(SenderHelper.HOST, "").equals("")) {
			mPreferences.edit().putString(SenderHelper.HOST, SenderHelper.DEFAULT_HOST).commit();
			mHost.setText(SenderHelper.DEFAULT_HOST);
		}
		if (mPreferences.getString(SenderHelper.PORT, "").equals("")) {
			mPreferences.edit().putString(SenderHelper.PORT, Integer.toString(SenderHelper.DEFAULT_PORT)).commit();
			mPort.setText(Integer.toString(SenderHelper.DEFAULT_PORT));
		}
		if (mPreferences.getString(SenderHelper.PROTOCOL, "").equals("")) {
			int currentEntryIndex = Integer.valueOf(mSupportedProtocol.getEntry().toString());
			mPreferences.edit().putString(SenderHelper.PROTOCOL, mSupportedProtocol.getEntries()[currentEntryIndex].toString()).commit();
			
		}
	}

	private void initPreferences() {
		PreferenceScreen prefs = getPreferenceScreen();
		mUser = (EditTextPreference) prefs.findPreference(SenderHelper.USER);
		mPswd = (EditTextPreference) prefs.findPreference(SenderHelper.PSWD);
		mHost = (EditTextPreference) prefs.findPreference(SenderHelper.HOST);
		mPort = (EditTextPreference) prefs.findPreference(SenderHelper.PORT);
		mSupportedProtocol = (ListPreference) prefs.findPreference(SenderHelper.PROTOCOL);
	}

	private void setListeners() {
		mUser.setOnPreferenceChangeListener(this);
		mPswd.setOnPreferenceChangeListener(this);
		mHost.setOnPreferenceChangeListener(this);
		mPort.setOnPreferenceChangeListener(this);
		mSupportedProtocol.setOnPreferenceChangeListener(this);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		boolean result = false;
		if (preference == mUser) {
			mPreferences.edit().putString(SenderHelper.USER, (String) newValue).commit();
			result = true;
		}
		else if (preference == mPswd) {
			mPreferences.edit().putString(SenderHelper.PSWD, (String) newValue).commit();
			result = true;
		}
		else if (preference == mHost) {
			mPreferences.edit().putString(SenderHelper.HOST, (String) newValue).commit();
			result = true;
		}
		else if (preference == mPort) {
			mPreferences.edit().putString(SenderHelper.PORT, (String) newValue).commit();
			result = true;
		}
		else if (preference == mSupportedProtocol) {
			switch (Integer.valueOf((String) newValue)) {
			case NONE:
				result = true;
				break;
			case SSL:
				result = true;
				break;
			case TLS:
				result = true;
				break;
			}
//			mSharedPreferences.edit().putString(SenderHelper.PROTOCOL, protocol).commit();
//			mSharedPreferences.getString(SenderHelper.PROTOCOL, "");
		}
		return result;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(HeadstartService.ACTION_MAIN);
			startActivity(intent);    	
			finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

}
