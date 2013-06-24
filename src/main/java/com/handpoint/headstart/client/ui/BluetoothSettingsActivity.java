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

import static com.handpoint.headstart.android.HeadstartService.BROADCAST_DEVICE_FOUND;
import static com.handpoint.headstart.android.HeadstartService.BROADCAST_DISCOVERY_FINISHED;
import static com.handpoint.headstart.android.HeadstartService.PREFERENCE_SS_KEY;

import java.util.Vector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

import com.handpoint.headstart.android.HeadstartService;
import com.handpoint.headstart.android.HeadstartServiceConnection.BindListener;
import com.handpoint.headstart.android.ParcelDeviceDescriptor;
import com.handpoint.headstart.android.ui.ProgressCategory;
import com.handpoint.headstart.api.DeviceConnectionState;
import com.handpoint.headstart.api.DeviceDescriptor;
import com.handpoint.headstart.api.HeadstartOperationException;
import com.handpoint.headstart.client.R;
import com.handpoint.headstart.client.util.SecurityUtil;
import com.handpoint.headstart.spi.android.bluetooth.BluetoothDeviceManager;
import com.handpoint.headstart.spi.android.protocol.Mped400;
import com.handpoint.util.logging.ApplicationLogger;
import com.handpoint.util.logging.Level;
import com.handpoint.util.logging.Logger;

/**
 * 
 * POS application preference activity for managing bluetooth state and device discovery.
 *
 */
public class BluetoothSettingsActivity extends ConnectionSettingsActivity {

	private static final String TAG = BluetoothSettingsActivity.class.getSimpleName();
	private static Logger logger = ApplicationLogger.getLogger(TAG);
	
	ProgressCategory mDevicesCategory;
	boolean mRegistered = false;
	boolean isBtConnection = true;
	Preference mScan;
    	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    	isBtConnection = DeviceDescriptor.DEVICE_TYPE_BT.equals(mPreferences.getString(HeadstartService.PREFERENCE_CONNECTION_TYPE, ""));
		mDevicesCategory = (ProgressCategory) getPreferenceScreen().findPreference("bluetooth_devices");
		mScan = getPreferenceScreen().findPreference("bluetooth_scan");
		mScan.setOnPreferenceClickListener(getScanClickListener());
		mScan.setEnabled(isBtConnection);		
    	registerReceiver(mPreferenceBroadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
    	registerReceiver(mPreferenceBroadcastReceiver, new IntentFilter(HeadstartService.BROADCAST_CONNECTION_STATE_CHANGE));
    	registerReceiver(mPreferenceBroadcastReceiver, new IntentFilter(HeadstartService.BROADCAST_DEVICE_FOUND));
    	registerReceiver(mPreferenceBroadcastReceiver, new IntentFilter(HeadstartService.BROADCAST_DISCOVERY_FINISHED));
    	mRegistered = true;
	}
	
	public int getPreferenceResource() {
		return R.xml.bt_preferences;
	}
	
    @Override
    protected void onResume() {
    	super.onResume();    	
    	if (mConnection.isBinded()) {
    		DeviceDescriptor descriptor = mConnection.getService().getConnectedDevice();
    		if (null != descriptor) {
    			setPreferenceSummary(descriptor.getAttribute("Address"), mConnection.getService().getCurrentConnectionState());
    		}
    	}
    }
    
    @Override
    protected BindListener getBindListener() {
    	return new BindListener() {
			
			@Override
			public void onBindCompleted() {
				try {
					if (isBtConnection && null != mBtAdapter && mBtAdapter.getState() == BluetoothAdapter.STATE_ON) {
						startDiscovery(false);
					}
				} catch (HeadstartOperationException e) {
					logger.log(Level.WARNING, "Error on device discovering", e);
				}								
			}
		};
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	if (mRegistered) {
    		unregisterReceiver(mPreferenceBroadcastReceiver);
    	}
    }
    
	public void onDeviceFound(DeviceDescriptor deviceDescriptor) {
		Preference devicePreference = new Preference(BluetoothSettingsActivity.this);
		devicePreference.setKey(deviceDescriptor.getAttribute("Address"));
		devicePreference.setTitle(deviceDescriptor.getName());
		devicePreference.setOnPreferenceClickListener(getPreferenceClickListener());
		mDevicesCategory.addItemFromInflater(devicePreference);
		setPreferenceSummary(deviceDescriptor.getAttribute("Address"), deviceDescriptor.getAttribute("State"));
	}

	void setPreferenceSummary(String address, String stringState) {
		int state = -1;
		try {
			state = Integer.parseInt(stringState);
		} catch (Exception e) {
		}
		setPreferenceSummary(address, state);
	}
	
	void setPreferenceSummary(String address, int state) {
		Preference p = mDevicesCategory.findPreference(address);
		if (null == p) {
			return;
		}
		switch (state) {
		case BluetoothDevice.BOND_NONE:
			p.setSummary(R.string.bt_device_state_non_bonded);
			break;
		case BluetoothDevice.BOND_BONDED:
			p.setSummary(R.string.bt_device_state_bonded);
			break;
		case DeviceConnectionState.DISCONNECTED:
			p.setSummary(R.string.bt_device_state_bonded);
			break;
		case DeviceConnectionState.CONNECTED:
			p.setSummary(com.handpoint.headstart.R.string.connected);
			break;
		default:
			p.setSummary("Unknown");
			break;
		}
		
	}
	
	void startDiscovery(boolean force) throws HeadstartOperationException {
		mConnection.getService().discoverDevices(force);
		mDevicesCategory.setProgress(true);
		mScan.setEnabled(false);		
	}
	
	@SuppressWarnings("rawtypes")
	public void onDiscoveryCompleted(final Vector deviceDescriptors) {
		mDevicesCategory.setProgress(false);
		mScan.setEnabled(true);
		try {
			DeviceDescriptor connected = mConnection.getService().getConnectedDevice();
			if (null != connected) {
				setPreferenceSummary(connected.getAttribute("Address"), mConnection.getService().getCurrentConnectionState());
			}
		} catch (Exception e) {
		}
	}
	
	private OnPreferenceClickListener getPreferenceClickListener() {
		return new OnPreferenceClickListener() {
			
			public boolean onPreferenceClick(Preference preference) {
					mConnection.getService().stopDiscoverDevices();
					Intent intent = new Intent(HeadstartService.ACTION_CONNECTION_PROGRESS);
					DeviceDescriptor descriptor = new DeviceDescriptor(BluetoothDeviceManager.DEVICE_TYPE, preference.getTitle().toString());
					if (null != preference.getKey()) {
						descriptor.setAttribute("Address", preference.getKey());
					}
					descriptor.setAttribute("URL", preference.getTitle().toString());
					if (mConnection.getService().getCurrentConnectionState() == DeviceConnectionState.CONNECTED) {
						try {
							mConnection.getService().disconnectFromDevice();				
						} catch (Exception e) {
							logger.log(Level.SEVERE, "Error disconnecting from device", e);
						}
					}
					if (mConnection.getService().getCurrentConnectionState() != DeviceConnectionState.CONNECTING) {
						byte[] ss = new byte[0];
						try {
							ss = getSharedSecret();
						} catch (Exception e) {
							logger.log(Level.WARNING, "Error getting shared secret", e);
						}						
						try {
							mConnection.getService().connectToDevice(descriptor, Mped400.NAME, ss);
							ss = null;
						} catch (Exception e) {
							logger.log(Level.WARNING, "Error connection initilization", e);
							Toast.makeText(BluetoothSettingsActivity.this, R.string.connect_error_message, Toast.LENGTH_LONG).show();
						}
					}
					intent.putExtra(HeadstartService.EXTRA_REMOTE_DEVICE, new ParcelDeviceDescriptor(descriptor));
					startActivity(intent);
					return true;
			}
		};
	}
	
	byte[] getSharedSecret() {
		byte[] result = new byte[0];
		try {
			result = SecurityUtil.decrypt(HeadstartService.getProperty("auth_token").getBytes(), mPreferences.getString(PREFERENCE_SS_KEY, ""));
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error decrypting shared key", e);
		}
		return result;
	}

	private OnPreferenceClickListener getScanClickListener() {
		return new OnPreferenceClickListener() {			
			public boolean onPreferenceClick(Preference preference) {
				if ((null != mBtAdapter && mBtAdapter.isEnabled() && mConnection.isBinded()) || 
						mPreferences.getBoolean(HeadstartService.PREFERENCE_SIMULATION_MODE, false)) {
					mDevicesCategory.removeAll();
					try {
						startDiscovery(true);
					} catch (HeadstartOperationException e) {
						logger.log(Level.WARNING, "Error on device discovering", e);
					}
					return true;
				}
				return false;
			}
		};
	}

	private BroadcastReceiver mPreferenceBroadcastReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				setPreferenceSummary(device.getAddress(), device.getBondState());
			} else if (HeadstartService.BROADCAST_CONNECTION_STATE_CHANGE.equals(intent.getAction())) {
				ParcelDeviceDescriptor parcel = intent.getParcelableExtra(HeadstartService.EXTRA_REMOTE_DEVICE);
				if (null != parcel) {
					DeviceDescriptor descriptor = parcel.getResult();
					setPreferenceSummary(descriptor.getAttribute("Address"), intent.getIntExtra(HeadstartService.EXTRA_PED_STATE, DeviceConnectionState.DISCONNECTED));
				}
			} else if (BROADCAST_DEVICE_FOUND.equals(intent.getAction())) {
				ParcelDeviceDescriptor dd = intent.getParcelableExtra(HeadstartService.EXTRA_REMOTE_DEVICE);
				onDeviceFound(dd.getResult());
			} else if (BROADCAST_DISCOVERY_FINISHED.equals(intent.getAction())) {
				//skip returned set of remote devices, because we already have them
				onDiscoveryCompleted(new Vector());
			}
			
		}
	};


}
