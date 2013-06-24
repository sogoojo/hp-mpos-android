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

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

import com.handpoint.headstart.android.HeadstartService;
import com.handpoint.headstart.client.R;

/**
 * 
 *
 */
public class ConnectionSettingsActivity extends SettingsActivity {
	
	private static final String BLUETOOTH_STATE = "bluetooth_state";

	protected BluetoothAdapter mBtAdapter;
	private boolean mRegistered = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		getPreferenceManager().findPreference(BLUETOOTH_STATE)
		.setOnPreferenceClickListener(
				new BtStatePreferenceClickListener(mBtAdapter));
		registerReceiver(mBluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
		mRegistered = true;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		updateBTSummary();
		updateConnectionTypeSummary();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mRegistered) {
			unregisterReceiver(mBluetoothReceiver);
		}
	}
	
	@Override
	protected int getPreferenceResource() {
		return R.xml.connection_preferences;
	}
	
	void updateBTSummary() {
		if (null != mBtAdapter) {			
			CheckBoxPreference p = (CheckBoxPreference) getPreferenceScreen().findPreference(BLUETOOTH_STATE); 
			switch (mBtAdapter.getState()) {
			case BluetoothAdapter.STATE_OFF:
				p.setSummary(R.string.bluetooth_turn_on);
				p.setChecked(false);
				p.setEnabled(true);
				break;
			case BluetoothAdapter.STATE_TURNING_OFF:
				p.setSummary(R.string.bluetooth_turning_off);
				p.setEnabled(false);
				break;
				
			case BluetoothAdapter.STATE_TURNING_ON:
				p.setSummary(R.string.bluetooth_turning_on);
				p.setEnabled(false);
				break;

			default:
				p.setSummary(null);
				p.setChecked(true);
				p.setEnabled(true);
			}			
		}
	}
	
	void updateConnectionTypeSummary() {
		Preference p = getPreferenceScreen().findPreference(HeadstartService.PREFERENCE_CONNECTION_TYPE);
		if (null != p) {
			p.setSummary(
					getPreferenceAsString(
							R.array.connection_type_titles, 
							R.array.connection_type_values, 
							mPreferences.getString(HeadstartService.PREFERENCE_CONNECTION_TYPE, "")));
		}		
	}
	
	BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
				updateBTSummary();
			}
		}
	};


	protected class BtStatePreferenceClickListener implements OnPreferenceClickListener {
		private BluetoothAdapter btAdapter;
		
		public BtStatePreferenceClickListener(BluetoothAdapter btAdapter) {
			this.btAdapter = btAdapter;
		}
		
		public boolean onPreferenceClick(Preference preference) {
			if (null != btAdapter && BLUETOOTH_STATE.equals(preference.getKey())) {
				switch (btAdapter.getState()) {
				case BluetoothAdapter.STATE_OFF:
					btAdapter.enable();
					break;

				case BluetoothAdapter.STATE_ON:
					btAdapter.disable();
					break;
				}
				return true;
			}
			return false;
		}		
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		super.onSharedPreferenceChanged(sharedPreferences, key);
		if (HeadstartService.PREFERENCE_CONNECTION_TYPE.equals(key)) {
			updateConnectionTypeSummary();
			mRefreshNeeded = true;
		}
	}
}
