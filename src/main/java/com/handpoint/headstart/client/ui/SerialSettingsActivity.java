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

import static com.handpoint.headstart.android.HeadstartService.PREFERENCE_BAUD_RATE;
import static com.handpoint.headstart.android.HeadstartService.PREFERENCE_COM_PORT;
import static com.handpoint.headstart.android.HeadstartService.PREFERENCE_FLOW_CONTROL;
import static com.handpoint.headstart.android.HeadstartService.PREFERENCE_HANDSHAKE;
import static com.handpoint.headstart.android.HeadstartService.PREFERENCE_NUMBER_OF_BITS;
import static com.handpoint.headstart.android.HeadstartService.PREFERENCE_PARITY;
import static com.handpoint.headstart.android.HeadstartService.PREFERENCE_STOP_BITS;
import static com.handpoint.headstart.android.HeadstartService.PREFERENCE_SS_KEY;

import android.util.Log;
import android.widget.Toast;
import com.handpoint.headstart.api.DeviceConnectionState;
import com.handpoint.headstart.spi.android.protocol.Mped400;
import com.handpoint.util.HexFormat;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

import com.handpoint.headstart.android.HeadstartService;
import com.handpoint.headstart.android.ParcelDeviceDescriptor;
import com.handpoint.headstart.api.DeviceDescriptor;
import com.handpoint.headstart.client.R;

/**
 *
 *
 */
public class SerialSettingsActivity extends SettingsActivity {

    boolean isSerialConnection = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isSerialConnection = DeviceDescriptor.DEVICE_TYPE_COM.equals(mPreferences.getString(HeadstartService.PREFERENCE_CONNECTION_TYPE, ""));
        initDefaults();
        Preference scan = getPreferenceScreen().findPreference("serial_connect");
        scan.setOnPreferenceClickListener(getConnectClickListener());
        scan.setEnabled(isSerialConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSummaries();
    }

    @Override
    protected int getPreferenceResource() {
        return R.xml.com_preferences;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        super.onSharedPreferenceChanged(sharedPreferences, key);
        if (PREFERENCE_COM_PORT.equals(key)) {
            updateComPortSummary();
        } else if (PREFERENCE_BAUD_RATE.equals(key)) {
            updateBaudRateSummary();
        } else if (PREFERENCE_NUMBER_OF_BITS.equals(key)) {
            updateNumberOfBitsSummary();
        } else if (PREFERENCE_PARITY.equals(key)) {
            updateParitySummary();
        } else if (PREFERENCE_STOP_BITS.equals(key)) {
            updateStopBitsSummary();
        } else if (PREFERENCE_HANDSHAKE.equals(key)) {
            updateHandshakeSummary();
        } else if (PREFERENCE_FLOW_CONTROL.equals(key)) {
            updateFlowControlSummary();
        }

    }

    private void updateSummaries() {
        updateComPortSummary();
        updateBaudRateSummary();
        updateNumberOfBitsSummary();
        updateParitySummary();
        updateStopBitsSummary();
        updateHandshakeSummary();
        updateFlowControlSummary();
    }

    private void initDefaults() {
        boolean isChanged = false;
        isChanged = initDefaultComPort();
        isChanged = initDefaultBaudRate();
        isChanged = initDefaultNumberOfBits();
        isChanged = initDefaultParity();
        isChanged = initDefaultStopBits();
        isChanged = initDefaultHandshake();
        isChanged = initDefaultFlowControl();
        if (isChanged) {
            reload();
        }
    }

    private void reload() {
        setPreferenceScreen(null);
        addPreferencesFromResource(getPreferenceResource());
    }
    //com port
    private void updateComPortSummary() {
        Preference p = getPreferenceScreen().findPreference(PREFERENCE_COM_PORT);
        if (null != p) {
            p.setSummary(
                    getPreferenceAsString(
                            R.array.serial_com_port_titles,
                            R.array.serial_com_port_values,
                            mPreferences.getString(PREFERENCE_COM_PORT, "")));
        }
    }

    private boolean initDefaultComPort() {
        if (mPreferences.getString(PREFERENCE_COM_PORT, "").equals("")) {
            mPreferences.edit().putString(PREFERENCE_COM_PORT, getDefaultComPort()).commit();
            return true;
        }
        mPreferences.edit().putString(PREFERENCE_COM_PORT, getDefaultComPort()).commit();
        return false;
    }
    //baud rate
    private void updateBaudRateSummary() {
        Preference p = getPreferenceScreen().findPreference(PREFERENCE_BAUD_RATE);
        if (null != p) {
            p.setSummary(
                    getPreferenceAsString(
                            R.array.serial_baud_rate_titles,
                            R.array.serial_baud_rate_values,
                            mPreferences.getString(PREFERENCE_BAUD_RATE, "")));
        }
    }

    private boolean initDefaultBaudRate() {
        if (mPreferences.getString(PREFERENCE_BAUD_RATE, "").equals("")) {
            mPreferences.edit().putString(PREFERENCE_BAUD_RATE, getDefaultBaudRate()).commit();
            return true;
        }
        return false;
    }

    //number of bits
    private void updateNumberOfBitsSummary() {
        Preference p = getPreferenceScreen().findPreference(PREFERENCE_NUMBER_OF_BITS);
        if (null != p) {
            p.setSummary(
                    getPreferenceAsString(
                            R.array.serial_number_of_bits_titles,
                            R.array.serial_number_of_bits_values,
                            mPreferences.getString(PREFERENCE_NUMBER_OF_BITS, "")));
        }
    }

    private boolean initDefaultNumberOfBits() {
        if (mPreferences.getString(PREFERENCE_NUMBER_OF_BITS, "").equals("")) {
            mPreferences.edit().putString(PREFERENCE_NUMBER_OF_BITS, getDefaultDataBit()).commit();
            return true;
        }
        return false;
    }

    //parity
    private void updateParitySummary() {
        Preference p = getPreferenceScreen().findPreference(PREFERENCE_PARITY);
        if (null != p) {
            p.setSummary(
                    getPreferenceAsString(
                            R.array.serial_parity_titles,
                            R.array.serial_parity_values,
                            mPreferences.getString(PREFERENCE_PARITY, "")));
        }
    }

    private boolean initDefaultParity() {
        if (mPreferences.getString(PREFERENCE_PARITY, "").equals("")) {
            mPreferences.edit().putString(PREFERENCE_PARITY, getDefaultParity()).commit();
            return true;
        }
        return false;
    }

    //stop bits
    private void updateStopBitsSummary() {
        Preference p = getPreferenceScreen().findPreference(PREFERENCE_STOP_BITS);
        if (null != p) {
            p.setSummary(
                    getPreferenceAsString(
                            R.array.serial_stop_bits_titles,
                            R.array.serial_stop_bits_values,
                            mPreferences.getString(PREFERENCE_STOP_BITS, "")));
        }
    }

    private boolean initDefaultStopBits() {
        if (mPreferences.getString(PREFERENCE_STOP_BITS, "").equals("")) {
            mPreferences.edit().putString(PREFERENCE_STOP_BITS, getDefaultStopBits()).commit();
            return true;
        }
        return false;
    }

    //handshaking
    private void updateHandshakeSummary() {
        Preference p = getPreferenceScreen().findPreference(PREFERENCE_HANDSHAKE);
        if (null != p) {
            p.setSummary(
                    getPreferenceAsString(
                            R.array.serial_handshake_titles,
                            R.array.serial_handshake_values,
                            mPreferences.getString(PREFERENCE_HANDSHAKE, "")));
        }
    }

    private boolean initDefaultHandshake() {
        if (mPreferences.getString(PREFERENCE_HANDSHAKE, "").equals("")) {
            mPreferences.edit().putString(PREFERENCE_HANDSHAKE, getDefaultHandshake()).commit();
            return true;
        }
        return false;
    }

    //flow control
    private void updateFlowControlSummary() {
        Preference p = getPreferenceScreen().findPreference(PREFERENCE_FLOW_CONTROL);
        if (null != p) {
            p.setSummary(
                    getPreferenceAsString(
                            R.array.serial_flow_control_titles,
                            R.array.serial_flow_control_values,
                            mPreferences.getString(PREFERENCE_FLOW_CONTROL, "")));
        }
    }

    private boolean initDefaultFlowControl() {
        if (mPreferences.getString(PREFERENCE_FLOW_CONTROL, "").equals("")) {
            mPreferences.edit().putString(PREFERENCE_FLOW_CONTROL, getDefaultFlowControl()).commit();
            return true;
        }
        return false;
    }

    private OnPreferenceClickListener getConnectClickListener() {
        return new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                if (mConnection.isBinded()) {
                    String tmp = mPreferences.getString(PREFERENCE_COM_PORT, getDefaultComPort());
                    int com = Integer.parseInt(tmp);
                    tmp = mPreferences.getString(PREFERENCE_BAUD_RATE, getDefaultBaudRate());
                    int baudRate = Integer.parseInt(tmp);
                    tmp = mPreferences.getString(PREFERENCE_NUMBER_OF_BITS, getDefaultDataBit());
                    int bitLen = Integer.parseInt(tmp);
                    tmp = mPreferences.getString(PREFERENCE_PARITY, getDefaultParity());
                    int parityBit = Integer.parseInt(tmp);
                    tmp = mPreferences.getString(PREFERENCE_STOP_BITS,getDefaultStopBits());
                    int stopBit = Integer.parseInt(tmp);
                    tmp = mPreferences.getString(PREFERENCE_FLOW_CONTROL, getDefaultFlowControl());
                    int flowCntl = Integer.parseInt(tmp);
                    tmp = mPreferences.getString(PREFERENCE_HANDSHAKE, getDefaultHandshake());
                    int handCntl = Integer.parseInt(tmp);

                    DeviceDescriptor device = new DeviceDescriptor(DeviceDescriptor.DEVICE_TYPE_COM, "COM" + com);
                    device.setAttribute(PREFERENCE_COM_PORT, Integer.toString(com));
                    device.setAttribute(PREFERENCE_BAUD_RATE, Integer.toString(baudRate));
                    device.setAttribute(PREFERENCE_NUMBER_OF_BITS, Integer.toString(bitLen));
                    device.setAttribute(PREFERENCE_PARITY, Integer.toString(parityBit));
                    device.setAttribute(PREFERENCE_STOP_BITS, Integer.toString(stopBit));
                    device.setAttribute(PREFERENCE_FLOW_CONTROL, Integer.toString(flowCntl));
                    device.setAttribute(PREFERENCE_HANDSHAKE, Integer.toString(handCntl));

                    if (mConnection.getService().getCurrentConnectionState() == DeviceConnectionState.CONNECTED) {
                        try {
                            mConnection.getService().disconnectFromDevice();
                        } catch (Exception e) {
                            Log.e(TAG, "Error disconnecting from device", e);
                        }
                    }
                    if (mConnection.getService().getCurrentConnectionState() != DeviceConnectionState.CONNECTING) {
                        try {
                            mConnection.getService().connectToDevice(device, Mped400.NAME, getSharedSecret());
                        } catch (Exception e) {
                            Log.w(TAG,"ConnectionDialogActivity: Error. Connection initialization");
                            Toast.makeText(getApplicationContext(), R.string.connect_error_message, Toast.LENGTH_LONG);
                        }
                    }

                    Intent intent = new Intent(HeadstartService.ACTION_CONNECTION_PROGRESS);
                    intent.putExtra(HeadstartService.EXTRA_REMOTE_DEVICE, new ParcelDeviceDescriptor(device));
                    startActivity(intent);

                    return true;
                }
                return false;
            }
        };
    }
    private byte[] getSharedSecret() {
        return HexFormat.hexToBytes(mPreferences.getString(PREFERENCE_SS_KEY, ""));
    }

    private String getDefaultComPort(){
        String[] ports = getResources().getStringArray(R.array.serial_com_port_values);
        return ports[0];
    }

    private String getDefaultBaudRate(){
        String[] baud = getResources().getStringArray(R.array.serial_baud_rate_values);
        return baud[0];
    }

    private String getDefaultDataBit(){
        String[] data_bits = getResources().getStringArray(R.array.serial_number_of_bits_values);
        return data_bits[1];
    }

    private String getDefaultParity(){
        String[] parity = getResources().getStringArray(R.array.serial_parity_values);
        return parity[0];
    }

    private String getDefaultStopBits(){
        String[] stop_bits = getResources().getStringArray(R.array.serial_stop_bits_values);
        return stop_bits[1];
    }

    private String getDefaultHandshake(){
        String[] handshake = getResources().getStringArray(R.array.serial_handshake_values);
        return handshake[0];
    }

    private String getDefaultFlowControl(){
        String[] flow = getResources().getStringArray(R.array.serial_flow_control_values);
        return flow[0];
    }
}