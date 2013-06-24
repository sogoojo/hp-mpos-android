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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.SpannableString;
import android.widget.Toast;

import com.actionbarsherlock.view.MenuItem;
import com.handpoint.headstart.android.HeadstartService;
import com.handpoint.headstart.android.HeadstartServiceConnection;
import com.handpoint.headstart.android.HeadstartServiceConnection.BindListener;
import com.handpoint.headstart.android.models.CurrencyModel;
import com.handpoint.headstart.api.DeviceConnectionState;
import com.handpoint.headstart.api.FinancialTransactionResult;
import com.handpoint.headstart.client.R;
import com.handpoint.headstart.client.android.Application;
import com.handpoint.headstart.client.data.SenderHelper;
import com.handpoint.headstart.client.ui.widget.EditPinPreference;
import com.handpoint.headstart.client.ui.widget.EditPinPreference.OnPinChangeListener;
import com.handpoint.headstart.client.util.SecurityUtil;
import com.handpoint.headstart.spi.logging.LoggerManagerAndroid;
import com.handpoint.util.HexFormat;
import com.handpoint.util.logging.ApplicationLogger;
import com.handpoint.util.logging.Level;
import com.handpoint.util.logging.Logger;

/**
 * 
 * POS application preference activity implementation. 
 *
 */
public class SettingsActivity extends HeadstartPreferenceActivity implements OnSharedPreferenceChangeListener {

	public final static String SUPPORT_EMAIL_ADDRESS = "support@handpoint.com"; 
    private final static String LINE_SEPARATOR = System.getProperty("line.separator");
    
	protected static final String TAG = SettingsActivity.class.getSimpleName();
	private static Logger logger = ApplicationLogger.getLogger(TAG);	
    
    public static final String PREFERENCE_SUPPORT_MODE = "mode_support";
    public static final String PREFERENCE_PASSWORD = "password";
    public static final String PREFERENCE_LOCK_DELAY = "lock_delay";
    
	protected boolean mRefreshNeeded = false;
	protected boolean mModeChanged = false;
	protected HeadstartServiceConnection mConnection;
	
	private boolean mExternalStorageAvailable = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(getPreferenceResource());
		initCurrencies();
    	mConnection = new HeadstartServiceConnection(this, false, getBindListener());
    	mPreferences.registerOnSharedPreferenceChangeListener(this);
    	
    	EditPinPreference pwdPreference = (EditPinPreference) getPreferenceManager().findPreference(PREFERENCE_PASSWORD);
    	if (null != pwdPreference) {
    		pwdPreference.registerPinChangeListener(getOnPinChangeListener());
    	}
		Preference sendLogs = getPreferenceScreen().findPreference("send_logs");
		if (null != sendLogs) {
			sendLogs.setOnPreferenceClickListener(getSendLogsClickListener());
		}
		Preference finInit = getPreferenceScreen().findPreference("fin_init");
		if (null != finInit) {
			finInit.setOnPreferenceClickListener(getFinInitClickListener());
		}
    	getSupportActionBar().setDisplayShowTitleEnabled(false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getListView().setCacheColorHint(Color.TRANSPARENT);
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    mExternalStorageAvailable = true;
		}		
	}
	
	protected BindListener getBindListener() {
		return new BindListener() {
			
			@Override
			public void onBindCompleted() {
			}
		};
	}
	
	private OnPreferenceClickListener getSendLogsClickListener() {
		return new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				collectAndSendLog();
				return true;
			}
		};
	}
	
	void collectAndSendLog() {
		StringBuilder emailText = new StringBuilder(getString(R.string.logs_addition_info));
		char[] separator = new char[20];
		Arrays.fill(separator, '-');
		try {
			emailText.append(LINE_SEPARATOR)
				.append(getString(R.string.logs_device_info, Build.MODEL, Build.VERSION.RELEASE, getFormattedKernelVersion(), Build.DISPLAY))
				.append(LINE_SEPARATOR)
				.append(getString(R.string.application_version))
				.append(getPackageManager().getPackageInfo(getPackageName(), 0).versionName)
				.append(LINE_SEPARATOR);
		} catch (NameNotFoundException e) {
		}
		//if internal file with logs can be written to external storage - copy them and send e-mail with attachment
		//otherwise - put logs direct into e-mail body
		File externalLogsPath = getExternalPathForLogs();
		ArrayList<Uri> externalLogsFiles = null;
		if (null != externalLogsPath) {
			try {
				externalLogsFiles = copyAllLogsToExternal(
						((Application)getApplication()).getLogsDir(),
						((Application)getApplication()).getLogBaseFileName(), 
						externalLogsPath);
			} catch(IOException e) {
				logger.log(Level.WARNING, "Error copying logs file to external storage", e);
			}
		}
		if (null == externalLogsFiles) {
			emailText.append(separator)
			.append(LINE_SEPARATOR)
			.append(loadLogsFromFiles(((Application)getApplication()).getLogsDir(),((Application)getApplication()).getLogBaseFileName()));
		}
		SenderHelper.sendEmailViaIntent(
				this, 
				SUPPORT_EMAIL_ADDRESS,
				getString(R.string.logs_subject),
				new SpannableString(emailText.toString()),
				externalLogsFiles);
	}
	
	private StringBuilder loadLogsFromFiles(final File dir, final String baseFileName) {
		StringBuilder fileContent = new StringBuilder();
		String[] logFiles = dir.list(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String filename) {				
				return filename.matches(baseFileName + "\\.?\\d*");
			}
		});
		Scanner s = null;
		for (int i = 0, len = logFiles.length; i < len; i++) {
			s = null;
			try {
				s = new Scanner(openFileInput(logFiles[i]));
		
				while (s.hasNextLine()) {
				    fileContent.append(s.nextLine())
				    .append(LINE_SEPARATOR);
				}
			} catch (FileNotFoundException e) {
				logger.log(Level.WARNING, "Logs file not found");
			} finally {
				if (null != s) {
					s.close();
				}				
			}
		}
		return fileContent;
	}
	
    private String getFormattedKernelVersion() {
        String procVersionStr;

        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 256);
            try {
                procVersionStr = reader.readLine();
            } finally {
                reader.close();
            }

            final String PROC_VERSION_REGEX =
                "\\w+\\s+" + /* ignore: Linux */
                "\\w+\\s+" + /* ignore: version */
                "([^\\s]+)\\s+" + /* group 1: 2.6.22-omap1 */
                "\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+" + /* group 2: (xxxxxx@xxxxx.constant) */
                "\\([^)]+\\)\\s+" + /* ignore: (gcc ..) */
                "([^\\s]+)\\s+" + /* group 3: #26 */
                "(?:PREEMPT\\s+)?" + /* ignore: PREEMPT (optional) */
                "(.+)"; /* group 4: date */

            Pattern p = Pattern.compile(PROC_VERSION_REGEX);
            Matcher m = p.matcher(procVersionStr);

            if (!m.matches()) {
            	logger.log(Level.SEVERE, "Regex did not match on /proc/version: " + procVersionStr);
                return "Unavailable";
            } else if (m.groupCount() < 4) {
            	logger.log(Level.SEVERE, "Regex match on /proc/version only returned " + m.groupCount() + " groups");
                return "Unavailable";
            } else {
                return (new StringBuilder(m.group(1)).append("\n").append(
                        m.group(2)).append(" ").append(m.group(3)).append("\n")
                        .append(m.group(4))).toString();
            }
        } catch (IOException e) {
        	logger.log(Level.SEVERE, "IO Exception when getting kernel version for Device Info screen", e);
            return "Unavailable";
        }
    }
	
	private OnPreferenceClickListener getFinInitClickListener() {
		return new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startFinInit();
				return true;
			}
		};
	}
	
	private void startFinInit() {
		if (isReady()) {
			mConnection.getService().startFinancialInitialization();
			Intent intent = new Intent(HeadstartService.ACTION_FT_PROGRESS);
			intent.putExtra(HeadstartService.EXTRA_OPERATION_TYPE, FinancialTransactionResult.FT_TYPE_FINANCIAL_INITIALIZATION);
			startActivity(intent);
		} else {
			Toast.makeText(this, R.string.warn_no_connection, Toast.LENGTH_LONG).show();
		}
	}
	
	private boolean isReady() {
		return null != mConnection && mConnection.isBinded() &&
				mConnection.getService().getCurrentConnectionState() == DeviceConnectionState.CONNECTED &&
				mConnection.getService().isNewTransactionAvailable();
	}

	private OnPinChangeListener getOnPinChangeListener() {
		return new OnPinChangeListener() {
			
			@Override
			public void onPinChanged(String oldPin, String newPin) {
				String ss = mPreferences.getString(HeadstartService.PREFERENCE_SS_KEY, null);
				if (null == ss) {
					return;
				}
				try {
                    HeadstartService.setProperty("auth_token", newPin);
					byte[] decryptedSs = SecurityUtil.decrypt(oldPin.getBytes(), ss);
					EditTextPreference p = (EditTextPreference) getPreferenceScreen().findPreference(HeadstartService.PREFERENCE_SS_KEY);
					if (null != p) {
						p.setText(HexFormat.bytesToHexString(decryptedSs));
					}					
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Error re-encryptin shared key", e);
				}
				
			}
		};
	}
	
	private void initCurrencies () {
		ListPreference preference = (ListPreference)getPreferenceManager().findPreference(HeadstartService.PREFERENCE_DEFAULT_CURRENCY);
		if (null == preference) {
			return;
		}
		List<CurrencyModel> currencies = ((Application)getApplication()).getCurrencies();
		CharSequence[] entries = new String[currencies.size()];
		CharSequence[] entryValues = new String[currencies.size()];
		int i = 0;
		for (Iterator<CurrencyModel> iterator = currencies.iterator(); iterator.hasNext();i++) {
			CurrencyModel currencyModel = iterator.next();
			entries[i] = currencyModel.getName();
			entryValues[i] = currencyModel.getCode();
		}
		preference.setEntries(entries);
		preference.setEntryValues(entryValues);
	}	
	
	protected int getPreferenceResource() {
		return R.xml.preferences;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		updateSimulatorDelaySummary();
		updateCurrencySummary();
		updateMerchantNameSummary();
		updateMerchantEmailSummary();
	}

	@Override
	protected void onPause() {
		super.onPause();		
		if (isFinishing() && mRefreshNeeded) {
			mConnection.getService().refresh();
			mRefreshNeeded = false;
		}
		if (isFinishing() && mModeChanged) {
			removeLastUsed();
			mModeChanged = false;
		}
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
    	if (null != mConnection) {
    		mConnection.doUnbindService(false);
    	}		
    	mPreferences.unregisterOnSharedPreferenceChangeListener(this);
	}
	
	private void removeLastUsed() {
		mPreferences.edit().remove(HeadstartService.PREFERENCE_LAST_USED_NAME)
				.remove(HeadstartService.PREFERENCE_LAST_USED_TYPE)
				.remove(HeadstartService.PREFERENCE_LAST_USED_ADDRESS).commit();
	}
	
	void updateSimulatorDelaySummary() {
		Preference p = getPreferenceScreen().findPreference(HeadstartService.PREFERENCE_SIMULATOR_DELAY);
		if (null != p) {
			p.setSummary(mPreferences.getString(HeadstartService.PREFERENCE_SIMULATOR_DELAY, "0"));
		}		
	}
	
	void updateCurrencySummary() {
		Preference p = getPreferenceScreen().findPreference(HeadstartService.PREFERENCE_DEFAULT_CURRENCY);
		if (null != p) {
			String code = mPreferences.getString(HeadstartService.PREFERENCE_DEFAULT_CURRENCY, "");
			CurrencyModel cm = ((Application)getApplication()).getCurrency(code);
			if (null != cm) {
				
			}
			p.setSummary(cm.getName());
		}		
	}
	
	void updateMerchantNameSummary() {
		Preference p = getPreferenceScreen().findPreference(HeadstartService.PREFERENCE_MERCHANT_NAME);
		if (null != p) {
			p.setSummary(mPreferences.getString(HeadstartService.PREFERENCE_MERCHANT_NAME, ""));
		}		
	}
	
	void updateMerchantEmailSummary() {
		Preference p = getPreferenceScreen().findPreference(HeadstartService.PREFERENCE_MERCHANT_EMAIL_ADDRESS);
		if (null != p) {
			p.setSummary(mPreferences.getString(HeadstartService.PREFERENCE_MERCHANT_EMAIL_ADDRESS, ""));
		}		
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (HeadstartService.PREFERENCE_SIMULATION_MODE.equalsIgnoreCase(key)) {
			mRefreshNeeded = true;
			mModeChanged = true;
		} else if (HeadstartService.PREFERENCE_SS_KEY.equalsIgnoreCase(key)) {
			mRefreshNeeded = true;
		} else if (HeadstartService.PREFERENCE_SIMULATOR_DELAY.equalsIgnoreCase(key)) {
			mRefreshNeeded = true;
			updateSimulatorDelaySummary();
		} else if (PREFERENCE_SUPPORT_MODE.equalsIgnoreCase(key)) {
			Level level = getLogsLevel(sharedPreferences);
			((LoggerManagerAndroid)ApplicationLogger.getLoggerManager()).setLevel(level);
			showSupportModeWarning(sharedPreferences);
		} else if (HeadstartService.PREFERENCE_DEFAULT_CURRENCY.equalsIgnoreCase(key)) {
			updateCurrencySummary();
		} else if (HeadstartService.PREFERENCE_MERCHANT_NAME.equalsIgnoreCase(key)) {
			updateMerchantNameSummary();
		} else if (HeadstartService.PREFERENCE_MERCHANT_EMAIL_ADDRESS.equalsIgnoreCase(key)) {
			updateMerchantEmailSummary();
		}
	}
	
	private void showSupportModeWarning(SharedPreferences sharedPreferences) {
		if (sharedPreferences.getBoolean(PREFERENCE_SUPPORT_MODE, false)) {
			Toast.makeText(this, R.string.support_mode_warning, Toast.LENGTH_LONG).show();
		}
	}
	
	public static Level getLogsLevel(SharedPreferences sharedPreferences) {
		if (sharedPreferences.getBoolean(PREFERENCE_SUPPORT_MODE, false)) {
			return Level.FINEST;
		}
		return Level.INFO;
	}
	
	public String getPreferenceAsString(int arrayTitlesId, int arrayValuesId, String resourceName) {
		String[] types = getResources().getStringArray(arrayTitlesId);
		int index = getPreferenceIndex(arrayValuesId, resourceName);
		try {
			return types[index];
		} catch (ArrayIndexOutOfBoundsException e) {
			return getResources().getString(R.string.unknown);
		}
	}
	
	private int getPreferenceIndex(int arrayValuesId, String resourceName) {
		String[] types = getResources().getStringArray(arrayValuesId);
		for(int i = 0, len = types.length; i < len; i++) {
			if (null != types[i] && types[i].equals(resourceName)) {
				return i;
			}
		}
		return -1;
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
		
	private File getExternalPathForLogs() {
		if (!mExternalStorageAvailable) {
			return null;
		}
		File logsStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		if (! logsStorageDir.exists()){
	        if (! logsStorageDir.mkdirs()) {
	        	logger.log(Level.SEVERE, "Failed to create directory for storing product images");
	            return null;
	        }
	    }
		return logsStorageDir;
	}
	
	private ArrayList<Uri> copyAllLogsToExternal(final File internalLogsDir, final String baseFileName, File externalLogsDir) throws IOException {
		String[] logFiles = internalLogsDir.list(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String filename) {				
				return filename.matches(baseFileName + "\\.?\\d*");
			}
		});
		ArrayList<Uri> uris = new ArrayList<Uri>();
		for (int i = 0, len = logFiles.length; i < len; i++) {
			File extFile = copyLogsFileToExternal(logFiles[i], externalLogsDir);
			uris.add(Uri.fromFile(extFile));
		}
		return uris;
	}
	/**
	 * @param logFileName
	 * @param externalLogsPath
	 */
	private File copyLogsFileToExternal(String logFileName, File externalLogsPath) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = openFileInput(logFileName);
			File externalFile = new File(externalLogsPath, logFileName);
	        out = new FileOutputStream(externalFile, false);        
			byte[] buffer = new byte[1024];
		    int read;
		    while((read = in.read(buffer)) != -1){
		      out.write(buffer, 0, read);
		    }
		    return externalFile;
		} finally {
			if (null != in) {
				in.close();
			}
			if (null != out) {
				out.close();
			}
		}
	}
	
}