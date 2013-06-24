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

import static android.bluetooth.BluetoothAdapter.getDefaultAdapter;
import static com.handpoint.headstart.android.HeadstartService.BROADCAST_CONNECTION_STATE_CHANGE;
import static com.handpoint.headstart.android.HeadstartService.BROADCAST_ERROR;
import static com.handpoint.headstart.android.HeadstartService.PREFERENCE_SS_KEY;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.handpoint.headstart.android.HeadstartService;
import com.handpoint.headstart.android.HeadstartServiceConnection;
import com.handpoint.headstart.android.HeadstartServiceConnection.BindListener;
import com.handpoint.headstart.android.ParcelDeviceDescriptor;
import com.handpoint.headstart.api.DeviceConnectionState;
import com.handpoint.headstart.api.DeviceDescriptor;
import com.handpoint.headstart.api.FinancialTransactionResult;
import com.handpoint.headstart.client.R;
import com.handpoint.headstart.client.android.Application;
import com.handpoint.headstart.client.data.DaoHelper;
import com.handpoint.headstart.client.data.models.Basket;
import com.handpoint.headstart.client.data.models.BasketItem;
import com.handpoint.headstart.client.data.models.FinancialTransaction;
import com.handpoint.headstart.client.ui.OptionFragment.OnOptionActionListener;
import com.handpoint.headstart.client.util.SecurityUtil;
import com.handpoint.headstart.eft.DeviceState;
import com.handpoint.headstart.spi.android.bluetooth.BluetoothDeviceManager;
import com.handpoint.headstart.spi.android.protocol.Mped400;
import com.handpoint.util.HexFormat;
import com.handpoint.util.logging.ApplicationLogger;
import com.handpoint.util.logging.Level;
import com.handpoint.util.logging.Logger;
import com.itextpdf.tool.xml.html.Break;

/**
 * 
 * POS application main activity implementation.
 *
 */
public class MainActivity extends HeadstartActivity implements OnOptionActionListener, OnPaymentListener {

	private static final String TAG = MainActivity.class.getSimpleName();

    private static final int HISTORY_SCREEN = 0;
    private static final int NUMPAD_SCREEN = 1;
    private static final int OPTIONS_SCREEN = 2;

	private static final int MENU_ITEM_EXIT = 1;
    private static final int MENU_ITEM_SETTINGS = 2;
	
	private static final int ACTIVATION_DIALOG_ID = 1;

	private static final int REQUEST_CODE_LOGIN = 10;
	private static final int REQUEST_CODE_NEW_ACCOUNT = 20;

	private static final String CURRENT_PAGE = "CURRENT_PAGE";
	private static final String VOIDED_ID = "VOIDED_ID";
	private static final String DEFERRED_TRANSACTION = "DEFERRED_TRANSACTION";
	private static final String ACTIVATION_DIALOG_SHOWED = "ACTIVATION_DIALOG_SHOWED";
	
	private static Logger logger = ApplicationLogger.getLogger(TAG);

    HeadstartServiceConnection mConnection;
	private ViewPager mPager;
	
	//Markers
	ImageView mMarkerReceipt;
	ImageView mMarkerNumpad;
	ImageView mMarkerOptions;
	Drawable mDot;
	Drawable mDotActive;
	
	//field for storing original ID on void transaction execution
	private Long mOriginalId;
	//activation intent holder
	Intent mImportIntent;
	boolean isImportAccepted;
	boolean isActivationDialogShowed;
	
	//void transaction intent holder
	Intent mVoidIntent;
	//flag will be set if new intent callback was called
	private boolean isNewIntent = false;
	//deferred transaction
	DeferredTransactionHolder mDeferredTransaction;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.pager);
		
    	// repeat image bug fix
//		View background = findViewById(R.id.rootLayout);
//		((BitmapDrawable)background.getBackground()).setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
		
//		getSupportActionBar().setTitle(R.string.abs_title);
		registerBroadcastReceivers();
		
    	mConnection = new HeadstartServiceConnection(this, true, getBindListener());
    	
		if (null != savedInstanceState && savedInstanceState.containsKey(ACTIVATION_DIALOG_SHOWED)) {
			isActivationDialogShowed = true;
		}
    	
    	if (isImportSsAction(getIntent()) && (null == savedInstanceState || isActivationDialogShowed)) {
    		mImportIntent = getIntent();
    		isImportAccepted = false;
    	} else if (isVoidAction(getIntent()) && null == savedInstanceState) {
    		mVoidIntent = getIntent();
    	}
    	initPaging();
	}
	
	protected BindListener getBindListener() {
		return new BindListener() {
			
			@Override
			public void onBindCompleted() {
				dispatchDeviceState();
			}
		};
	}
	
	boolean isImportSsAction(Intent intent) {
	    String action = intent.getAction();
		if (Intent.ACTION_VIEW.equals(action)) {
			return true;
		}
		return false;
	}

	boolean isVoidAction(Intent intent) {
		String action = intent.getAction();
		int type = intent.getIntExtra(HeadstartService.EXTRA_OPERATION_TYPE, -1);
		if (HeadstartService.ACTION_MAIN.equals(action) && type != -1) {
			return true;
		}
		return false;
	}
	
	void dispatchDeviceState() {
		boolean dispatched = false;
		//if not connected to service nothing to do
		if (null == mConnection || !mConnection.isBinded()) {
			return; 
		}
		//transaction is in progress and signature verification is required
		if (mConnection.getService().isInTransaction() &&
				null != mConnection.getService().getDeviceState() &&
				DeviceState.WAITING_SIGNATURE == mConnection.getService().getDeviceState().getState()) 
		{
            //get receipt and signature timeout and start signature activity
            String receipt = mConnection.getService().getSignatureVerificationText();
            String timeout = mConnection.getService().getSignatureTimeoutSeconds();
            startSignatureVerificationActivity(receipt, timeout);
			dispatched = true;
			//transaction is in progress and timeout handling is required
		} else if (mConnection.getService().isInTransaction() &&
				null != mConnection.getService().getDeviceState() &&
                HeadstartService.ADDITIONAL_DEVICE_STATE_HANDLING_TIMEOUT == mConnection.getService().getDeviceState().getState())
		{
			startTimeoutHandlingActivity();
			dispatched = true;
			//financial transaction is in progress
		} else if (mConnection.getService().isInTransaction()) {
			startFTProgressActivity();
			dispatched = true;			
			//last financial transaction result was not handled yet
		} else if (!mConnection.getService().isLastTransactionHandled()) {
			FinancialTransactionResult result = mConnection.getService().getLastTransaction();
			// transaction was completed successfully
			if (null != result) {
				//save and start receipt activity
				dispatched = handleFinishFinancialTransaction(result);
			} else {
			//error is occurred during transaction
				Intent tmp = new Intent(HeadstartService.ACTION_FT_PROGRESS);
				tmp.putExtra(HeadstartService.EXTRA_ERROR_DESCRIPTION, mConnection.getService().getLastTransactionError().getMessage());
				startActivity(tmp);
				mConnection.getService().setLastTransactionHandled();
				dispatched = true;
			}
		}
		// if connecting and there is a deferred transaction then show connection dialog
		if (DeviceConnectionState.CONNECTING == mConnection.getService().getCurrentConnectionState() &&
				null != mDeferredTransaction) {
			startConnectionActivity(getLastUsedDevice());
			dispatched = true;
		}
		
		if (dispatched) {
			return;
		}
		if (null != mImportIntent && !isImportAccepted) {
			isActivationDialogShowed = true;
			showDialog(ACTIVATION_DIALOG_ID);
			return;
		}
		//if not logged in - request authorization
		if (null == HeadstartService.getProperty("auth_token")) {
			requestAuthentication();
			return;
		}
		//if activity started for import ini file - handle intent attachment
		if (null != mImportIntent && isImportAccepted) {
			handleIniFile(mImportIntent);
			mImportIntent = null;
			isImportAccepted = false;
		}
		//if activity started for void operation - start it
		if (null != mVoidIntent) {
			handleVoidOperation(mVoidIntent);
			mVoidIntent = null;
		}
		//
		//check is auto connect needed
	    DeviceDescriptor device = getLastUsedDevice();
		if (null != device && 
				mConnection.getService().getCurrentConnectionState() == DeviceConnectionState.DISCONNECTED &&
				!mConnection.getService().wasConnectionAttempt()) 
		{
			startConnect(device, true);
		}
		
	}


	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
    	if (isImportSsAction(intent)) {
    		setIntent(intent);
    		mImportIntent = intent;
    		isImportAccepted = false;
    	} else if (isVoidAction(intent)) {
    		mVoidIntent = intent;
    	}
    	isNewIntent = true;
    	if (!isFinishing()) {
    		dispatchDeviceState();
    	}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (!isNewIntent) {
	    	dispatchDeviceState();
		}
		isNewIntent = false;
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
    	if (null != mConnection) {
    		mConnection.doUnbindService(false);
    	}
		unregisterReceiver(mBtServiceReceiver);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		this.mPager.setCurrentItem(savedInstanceState.getInt(CURRENT_PAGE));
		long voidedId = savedInstanceState.getLong(VOIDED_ID, -1);
		if (voidedId != -1) {
			mOriginalId = Long.valueOf(voidedId);
		}
		ParcelDeferredTransaction parcel = savedInstanceState.getParcelable(DEFERRED_TRANSACTION);
		if (null != parcel) {
			mDeferredTransaction = parcel.getResult();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(CURRENT_PAGE, this.mPager.getCurrentItem());
		if (null != mOriginalId) {
			outState.putLong(VOIDED_ID, mOriginalId);
		}
		if (null != mDeferredTransaction) {
			outState.putParcelable(DEFERRED_TRANSACTION, new ParcelDeferredTransaction(mDeferredTransaction));
		}
		if (isActivationDialogShowed) {
			outState.putBoolean(ACTIVATION_DIALOG_SHOWED, true);
		}
	}

	private void initPaging() {
		mDot = getResources().getDrawable(R.drawable.dot);
		mDotActive = getResources().getDrawable(R.drawable.dot_active);
    	mMarkerReceipt = (ImageView) findViewById(R.id.marker_0);
    	mMarkerNumpad = (ImageView) findViewById(R.id.marker_1);
    	mMarkerOptions = (ImageView) findViewById(R.id.marker_2);
    	
    	List<Fragment> fragments = new ArrayList<Fragment>();
    	fragments.add(Fragment.instantiate(this, PaymentHistoryFragment.class.getName()));
    	fragments.add((NumpadFragment) Fragment.instantiate(this, NumpadFragment.class.getName()));
    	fragments.add(Fragment.instantiate(this, OptionFragment.class.getName()));
	    PagerAdapter pagerAdapter  = new PagerAdapter(getSupportFragmentManager(), fragments);
		this.mPager = (ViewPager)super.findViewById(R.id.viewpager);		
		this.mPager.setAdapter(pagerAdapter);
		this.mPager.setOnPageChangeListener(getPageChangeListener());
		this.mPager.setCurrentItem(NUMPAD_SCREEN);
    	mMarkerNumpad.setImageDrawable(mDotActive);
	}
	
	boolean handleFinishFinancialTransaction(FinancialTransactionResult result) {
		String signaturePath = mConnection.getService().getPath();
		String signatureVerificationText = mConnection.getService().getSignatureVerificationText();

		if (!TextUtils.isEmpty(result.merchantReceipt) || !TextUtils.isEmpty(result.customerReceipt)) {
			long transactionId = saveFinancialTransaction(result, mOriginalId, signaturePath, signatureVerificationText);
			if (transactionId > 0) {
				mConnection.getService().setLastTransactionHandled();
				Intent tmp = new Intent(this, ReceiptActivity.class);				
				tmp.putExtra(ReceiptActivity.EXTRA_TRANSACTION_ID, transactionId);
				tmp.putExtra(ReceiptActivity.EXTRA_SEND_MERCHANT_RECEIPT, true);
				startActivity(tmp);
				return true;
			} else {
				Toast.makeText(this, R.string.error_save_financial_transaction, Toast.LENGTH_LONG).show();
			}
		} else {
			//otherwise just reset last transaction
			mConnection.getService().setLastTransactionHandled();			
			String message = ((Application)getApplication()).formatErrorMessage(result.statusMessage, result.errorMessage);
			Toast.makeText(this, message, Toast.LENGTH_LONG).show();
		}
		return false;
	}
	
	long saveFinancialTransaction(FinancialTransactionResult result, Long originId, String signaturePath, String signatureVerificationText) {
		FinancialTransaction ft = new FinancialTransaction(result, originId, signaturePath, signatureVerificationText);
		DaoHelper daoHelper = new DaoHelper(this);
		daoHelper.open(true);
		long transactionId = daoHelper.insertFinancialTransaction(ft);
		daoHelper.close();
		return transactionId;
	}

	private DeviceDescriptor getLastUsedDevice() {
		DeviceDescriptor dd = null;
        String type = mPreferences.getString(HeadstartService.PREFERENCE_LAST_USED_TYPE, null);
        if(null != type){
            if(type.equals(DeviceDescriptor.DEVICE_TYPE_BT)){
                String name = mPreferences.getString(HeadstartService.PREFERENCE_LAST_USED_NAME, null);
                if (null != name) {
                    dd = new DeviceDescriptor(
                            mPreferences.getString(HeadstartService.PREFERENCE_LAST_USED_TYPE, BluetoothDeviceManager.DEVICE_TYPE),
                            mPreferences.getString(HeadstartService.PREFERENCE_LAST_USED_NAME, BluetoothDeviceManager.DEFAULT_NAME)
                    );
                    dd.setAttribute("Address", mPreferences.getString(HeadstartService.PREFERENCE_LAST_USED_ADDRESS, ""));
                }
            }
            if(type.equals(DeviceDescriptor.DEVICE_TYPE_COM))
                dd = mConnection.getService().getSerialDevice();
        }
		return dd;
	}
	
	void startConnect(DeviceDescriptor descriptor, boolean silentMode) {
		if (null == descriptor) {
			Toast.makeText(this, R.string.warn_no_saved_device, Toast.LENGTH_LONG).show();
			return;
		}
		if (null == mConnection || !mConnection.isBinded()) {
			Toast.makeText(this, R.string.warn_connection_unavailble, Toast.LENGTH_LONG).show();
			return;
		}
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
				logger.log(Level.WARNING, "Error obtaining shared secret", e);
			}
			try {
				mConnection.getService().connectToDevice(descriptor, Mped400.NAME, ss);
				ss = null;
			} catch (Exception e) {
				logger.log(Level.WARNING, "Error connection initialization", e);
				Toast.makeText(this, R.string.connect_error_message, Toast.LENGTH_LONG).show();
				return;
			}
		}
		if (!silentMode) {
			startConnectionActivity(descriptor);
		}
	}

	void startConnectionActivity(DeviceDescriptor descriptor) {
		Intent intent = new Intent(HeadstartService.ACTION_CONNECTION_PROGRESS);
		intent.putExtra(HeadstartService.EXTRA_REMOTE_DEVICE, new ParcelDeviceDescriptor(descriptor));
		startActivity(intent);
	}
	
	private byte[] getSharedSecret() {
		byte[] result = new byte[0];
		try {
			result = SecurityUtil.decrypt(HeadstartService.getProperty("auth_token").getBytes(), mPreferences.getString(PREFERENCE_SS_KEY, ""));
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error decrypting shared key", e);
		}
		return result;
	}
	
	private void startSignatureVerificationActivity(String receipt, String timeout) {
		Intent intent = new Intent(HeadstartService.ACTION_VERIFY_SIGNATURE);
		intent.putExtra(HeadstartService.EXTRA_MERCHANT_RECEIPT_TEXT, receipt);
        intent.putExtra(HeadstartService.EXTRA_SIGNATURE_TIMEOUT_SECONDS, timeout);
		startActivity(intent);
	}
	
	private void startTimeoutHandlingActivity() {
		Intent intent = new Intent(HeadstartService.ACTION_HANDLE_TIMEOUT);
		startActivity(intent);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//		menu.add(Menu.NONE, MENU_ITEM_CONNECT_TO_LAST, Menu.NONE, R.string.connect_to_last)
//			.setIcon(R.drawable.menu_ic_connect)
//			.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(Menu.NONE, MENU_ITEM_SETTINGS, Menu.NONE, R.string.settings)
                .setIcon(R.drawable.icon_settings)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(Menu.NONE, MENU_ITEM_EXIT, Menu.NONE, R.string.exit)
                .setIcon(R.drawable.menu_ic_exit)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_EXIT:
			fullUnbindFromService();
			finish();
			break;
        case MENU_ITEM_SETTINGS:
            startActivity(new Intent(this, SettingsActivity.class));
            break;
//		case MENU_ITEM_CONNECT_TO_LAST:
//			startConnect(getLastUsedDevice());
//			break;
		}
		return super.onOptionsItemSelected(item);
	}
		
	private Fragment getPagerFragment(int item) {
		return getSupportFragmentManager().findFragmentByTag("android:switcher:"+R.id.viewpager+":" + item);
	}

	private OnPageChangeListener getPageChangeListener() {
		return new OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
//				PaymentHistoryFragment frag = (PaymentHistoryFragment) getPagerFragment(HISTORY_SCREEN);
				switch (position) {
				case HISTORY_SCREEN:
					mMarkerReceipt.setImageDrawable(mDotActive);
					mMarkerNumpad.setImageDrawable(mDot);
					mMarkerOptions.setImageDrawable(mDot);
//					if (null != frag) {
//						frag.startActionModeIfNeed();
//					}
					break;
				case NUMPAD_SCREEN:
					mMarkerReceipt.setImageDrawable(mDot);
					mMarkerNumpad.setImageDrawable(mDotActive);
					mMarkerOptions.setImageDrawable(mDot);
//					if (null != frag) {
//						frag.finishActionMode(false);
//					}
					break;
				case OPTIONS_SCREEN:
					mMarkerReceipt.setImageDrawable(mDot);
					mMarkerNumpad.setImageDrawable(mDot);
					mMarkerOptions.setImageDrawable(mDotActive);
					break;
				}
			}

			@Override
			public void onPageScrolled(int page, float arg1, int arg2) {				
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
				
			}
		};
	}

    @Override
    public void onBackPressed() {
        switch (mPager.getCurrentItem())
        {
            case HISTORY_SCREEN:
                mPager.setCurrentItem(NUMPAD_SCREEN, true);
                break;
            case NUMPAD_SCREEN:
                break;
            case OPTIONS_SCREEN:
                mPager.setCurrentItem(NUMPAD_SCREEN, true);
                break;
        }

    }

	@Override
	public void onShowMainAction() {
		NumpadFragment frag = (NumpadFragment) getPagerFragment(NUMPAD_SCREEN);
		if (null != frag) {
			frag.initBottomButton();
		}
		mPager.setCurrentItem(NUMPAD_SCREEN, true);
	}

	@Override
	public void onShowHistoryAction() {
		mPager.setCurrentItem(HISTORY_SCREEN, true);
	}

	@Override
	public void onLogoutAction() {
		fullUnbindFromService();
		finish();		
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}
	
	@Override
	public void onPaymentStarted(int financialTransactionType, int amount, String currency, boolean cardHolderPresent) {
		startPayment(financialTransactionType, amount, currency, cardHolderPresent, null, null);
	}
	
	@Override
	public void onVoidPaymentStarted(int financialTransactionType,
			int amount, String currency, boolean cardHolderPresent,
			String originalTransactionId, long originalId) 
	{
		startPayment(financialTransactionType, amount, currency, cardHolderPresent, originalTransactionId, originalId);		
	}
	
	private void startPayment(int financialTransactionType, int amount,
			String currency, boolean cardHolderPresent,
			String originalTransactionId, Long originalId) 
	{
		if (!isServiceReady()) {
			Toast.makeText(this, R.string.warn_connection_unavailble, Toast.LENGTH_LONG).show();
			return;
		}

		switch (getConnectionState()) {
			case DeviceConnectionState.DISCONNECTED:
				if (null == getLastUsedDevice()) {
					Toast.makeText(this, R.string.warn_no_saved_device, Toast.LENGTH_LONG).show();
					return;					
				}
				mDeferredTransaction = new DeferredTransactionHolder(financialTransactionType, amount, currency, cardHolderPresent, originalTransactionId, originalId);
				startConnect(getLastUsedDevice(), false);
				break;
			case DeviceConnectionState.CONNECTING:
				mDeferredTransaction = new DeferredTransactionHolder(financialTransactionType, amount, currency, cardHolderPresent, originalTransactionId, originalId);
				startConnectionActivity(getLastUsedDevice());
				break;	
			case DeviceConnectionState.CONNECTED:
				if (!isTransactionAmountValid(amount)) {
					Toast.makeText(this, R.string.warn_empty_amount, Toast.LENGTH_LONG).show();
					return;
				}
				if (!isNewTransactionAvailable()) {
					Toast.makeText(this, R.string.warn_transaction_in_progress, Toast.LENGTH_LONG).show();
					return;
				}
				mOriginalId = originalId;
				invoikeServiceForTransaction(
						financialTransactionType, 
						amount,
						currency, 
						cardHolderPresent, 
						originalTransactionId);
				break;
			default:
				Toast.makeText(this, R.string.warn_connection_unavailble, Toast.LENGTH_LONG).show();
				return;
		}
	}

	void invoikeServiceForTransaction(int financialTransactionType,
			int amount, String currency, boolean cardHolderPresent,
			String originalTransactionId) {
		switch (financialTransactionType) {
			case FinancialTransactionResult.FT_TYPE_SALE:
				mConnection.getService().startSaleTransaction(
						amount, 
						currency, 
						cardHolderPresent);
				break;
			case FinancialTransactionResult.FT_TYPE_REFUND:
				mConnection.getService().startRefundTransaction(
						amount, 
						currency, 
						cardHolderPresent);
				break;
			case FinancialTransactionResult.FT_TYPE_SALE_VOID:
				mConnection.getService().startSaleVoidTransaction(
						originalTransactionId,
						amount, 
						currency, 
						cardHolderPresent);
				break;
			case FinancialTransactionResult.FT_TYPE_REFUND_VOID:
				mConnection.getService().startRefundVoidTransaction(
						originalTransactionId,
						amount, 
						currency, 
						cardHolderPresent);
				break;
			case FinancialTransactionResult.FT_TYPE_FINANCIAL_INITIALIZATION:
				mConnection.getService().startFinancialInitialization();
				break;

			default:
				return;
			}

			startFTProgressActivity(financialTransactionType, amount, currency, originalTransactionId);
	}
	
//	private boolean isFinancialTransactionAvailable(BigDecimal amount) {
//		return amount.compareTo(BigDecimal.ZERO) == 1 && isReady();
//	}
//	
//	private boolean isReady() {
//		return null != mConnection && mConnection.isBinded() &&
//				mConnection.getService().getCurrentConnectionState() == DeviceConnectionState.CONNECTED &&
//				mConnection.getService().isNewTransactionAvailable();
//	}

	private boolean isServiceReady() {
		return null != mConnection && mConnection.isBinded();
	}
	
	private int getConnectionState() {
		return mConnection.getService().getCurrentConnectionState();
	}
	
	private boolean isNewTransactionAvailable() {
		return mConnection.getService().isNewTransactionAvailable();
	}
	
	private boolean isTransactionAmountValid(int amount) {
		return amount > 0;
	}
	
	private void startFTProgressActivity() {
		startFTProgressActivity(0, 0, null, null);
	}
	
	private void startFTProgressActivity(int financialTransactionType, int amountValue, String currency, String originalTransactionId) {
		Intent intent = new Intent(HeadstartService.ACTION_FT_PROGRESS);
		intent.putExtra(HeadstartService.EXTRA_OPERATION_TYPE, financialTransactionType);
		intent.putExtra(HeadstartService.EXTRA_PAYMENT_CURRENCY,currency);
		intent.putExtra(HeadstartService.EXTRA_PAYMENT_VALUE, amountValue);
		intent.putExtra(HeadstartService.EXTRA_PAYMENT_TRANSACTION_ID, originalTransactionId);
		startActivity(intent);
	}
	
	void handleIniFile(Intent intent) {
		if (intent.getData() == null) {
			Toast.makeText(getApplicationContext(), R.string.error_invalid_data, Toast.LENGTH_LONG).show();
			logger.log(Level.SEVERE, "Import ss file failed: no URI in intent");
		} else {
			try {
				Properties ini = loadIniProperties(intent);
				if (null == ini || ini.size() == 0) {
					Toast.makeText(getApplicationContext(), R.string.error_invalid_data, Toast.LENGTH_LONG).show();
					logger.log(Level.SEVERE, "Import ss file failed: properties are not loaded (probably invalid ss file format)");
					return;
				}
				String ss = ini.getProperty("ss");
				if (!TextUtils.isEmpty(ss)) {
					mPreferences.edit().putString(
                            HeadstartService.PREFERENCE_SS_KEY,
							SecurityUtil.encrypt(HeadstartService.getProperty("auth_token").getBytes(), HexFormat.hexToBytes(ss))).commit();
				}
				String merchantName = ini.getProperty("merchant.name");
				if (!TextUtils.isEmpty(merchantName)) {
					mPreferences.edit().putString(
                            HeadstartService.PREFERENCE_MERCHANT_NAME, merchantName).commit();
				}
				Toast.makeText(this, R.string.ini_data_loaded, Toast.LENGTH_LONG).show();
				mConnection.getService().refresh();
			} catch (Exception e) {
				Toast.makeText(getApplicationContext(), R.string.ini_data_load_failed, Toast.LENGTH_LONG).show();
				logger.log(Level.SEVERE, "Import ss file failed", e);
			}
		}
	}
	
	void handleVoidOperation(Intent intent) {
		int financialTransactionType = intent.getIntExtra(HeadstartService.EXTRA_OPERATION_TYPE, -1);
		int amount = intent.getIntExtra(HeadstartService.EXTRA_PAYMENT_VALUE, 0);
		String currency = intent.getStringExtra(HeadstartService.EXTRA_PAYMENT_CURRENCY);
		String originalTransactionId = intent.getStringExtra(HeadstartService.EXTRA_PAYMENT_TRANSACTION_ID);
		Long originalId = intent.getLongExtra("com.handpoint.headstart.client.VOIDED_ID", -1);
		
		startPayment(financialTransactionType, amount, currency, true, originalTransactionId, originalId);
	}
	
	void requestAuthentication() {
		Intent requestIntent = null;
		int requestCode = REQUEST_CODE_LOGIN;
		if (mPreferences.contains(SettingsActivity.PREFERENCE_PASSWORD)) {
			requestIntent = new Intent(this, LoginActivity.class);
		} else {
			requestCode = REQUEST_CODE_NEW_ACCOUNT;
			requestIntent = new Intent(this, RegisterActivity.class);
		}
		startActivityForResult(requestIntent, requestCode);
	}
	
	Properties loadIniProperties(Intent iniIntent) throws IOException {
		InputStream attachment = null;
		Uri returnedUri = iniIntent.getData();
		try {
			if (iniIntent.getScheme().equals("file")) {
				attachment = new FileInputStream(returnedUri.getPath());
			} else if (iniIntent.getScheme().equals("content")) {
				attachment = getContentResolver().openInputStream(returnedUri);
			}
			
			Properties p = new Properties();
			p.load(attachment);
			return p;
		} finally {
			if (null != attachment) {
				try {
					attachment.close();
				} catch (IOException ignore) {
				}
			}			
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CODE_LOGIN:
			if (resultCode == RESULT_OK) {
				if (!data.hasExtra("password")) {
					return;
				}
                HeadstartService.setProperty("auth_token", data.getStringExtra("password"));
				if (null != mImportIntent) {
					handleIniFile(mImportIntent);
					mImportIntent = null;
				}
				if (null != mVoidIntent) {
					handleVoidOperation(mVoidIntent);
					mVoidIntent = null;
				}
			} else {
				if (!isConnected()) {
					fullUnbindFromService();
				}
				finish();
			}
			break;

		case REQUEST_CODE_NEW_ACCOUNT:
			if (resultCode == RESULT_OK) {
				String pwd = data.getStringExtra("password");
				try {
					mPreferences.edit().putString(SettingsActivity.PREFERENCE_PASSWORD, SecurityUtil.MD5Hash(pwd)).commit();
				} catch (NoSuchAlgorithmException e) {
					Toast.makeText(this, R.string.error_create_account_failed, Toast.LENGTH_LONG).show();
					mImportIntent = null;
					logger.log(Level.SEVERE, "Error of account creation", e);
					return;
				}
                HeadstartService.setProperty("auth_token", pwd);
				if (null != mImportIntent) {
					handleIniFile(mImportIntent);
					mImportIntent = null;
				}
			} else {
				if (!isConnected()) {
					fullUnbindFromService();
				}
				finish();
			}
			break;
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	private boolean isConnected() {
		return null != mConnection && mConnection.isBinded() &&
				mConnection.getService().getCurrentConnectionState() == DeviceConnectionState.CONNECTED;		
	}
	
	private void fullUnbindFromService() {
		if (null != mConnection) {
			mConnection.doUnbindService(true);
		}
		// delete current basket
		DaoHelper daoHelper = new DaoHelper(this);
		daoHelper.open(true);
		Basket basket = daoHelper.getCurrentBasket();
		if (null != basket) {
			BasketItem item = daoHelper.getBasketItem(basket.getId());
			if (null != item) {
				((Application)getApplication()).removePhoto(item);
			}
			daoHelper.deleteBasket(basket.getId());
		}
		daoHelper.close();
	}
	
	private static class DeferredTransactionHolder {
		int financialTransactionType;
		int amount;
		String currency;
		boolean cardHolderPresent;
		String originalTransactionId;
		Long originalId;
		
		public DeferredTransactionHolder(int financialTransactionType,
				int amount, String currency, boolean cardHolderPresent,
				String originalTransactionId, Long originalId) {
			super();
			this.financialTransactionType = financialTransactionType;
			this.amount = amount;
			this.currency = currency;
			this.cardHolderPresent = cardHolderPresent;
			this.originalTransactionId = originalTransactionId;
			this.originalId = originalId;
		}
	}
	
	private static class ParcelDeferredTransaction implements Parcelable {

	    public static final Creator CREATOR = new Creator();

	    private DeferredTransactionHolder deferredTransaction;
	    
	    public ParcelDeferredTransaction(DeferredTransactionHolder deferredTransaction) {
	    	this.deferredTransaction = deferredTransaction;
	    }
	    
	    public int describeContents() {
	        return 0;
	    }

	    public void writeToParcel(Parcel parcel, int flags) {
	        CREATOR.writeToParcel(this, parcel, flags);
	    }

	    public DeferredTransactionHolder getResult() {
	        return this.deferredTransaction;
	    }
	    
	    public static class Creator implements Parcelable.Creator<ParcelDeferredTransaction> {

			public ParcelDeferredTransaction createFromParcel(Parcel source) {
				
				int financialTransactionType = source.readInt();
				int amount = source.readInt();
				String currency = source.readString();
				boolean cardHolderPresent = source.readInt() == 1;
				String originalTransactionId = source.readString();
				long tmp = source.readLong();
				Long originalId = tmp == -1 ? null: tmp;
				DeferredTransactionHolder result = 
						new DeferredTransactionHolder(
								financialTransactionType, 
								amount, 
								currency, 
								cardHolderPresent, 
								originalTransactionId, 
								originalId);
				return new ParcelDeferredTransaction(result);
			}
			
	        public void writeToParcel(ParcelDeferredTransaction data, Parcel parcel, int flags) {
	        	parcel.writeInt(data.deferredTransaction.financialTransactionType);
	        	parcel.writeInt(data.deferredTransaction.amount);
	        	parcel.writeString(data.deferredTransaction.currency);
	        	parcel.writeInt(data.deferredTransaction.cardHolderPresent ? 1:0);
	        	parcel.writeString(data.deferredTransaction.originalTransactionId);
	        	parcel.writeLong(data.deferredTransaction.originalId == null ? -1:data.deferredTransaction.originalId);
	        }

			public ParcelDeferredTransaction[] newArray(int size) {
				return new ParcelDeferredTransaction[size];
			}
	    	
	    }
	}
	
	private void registerBroadcastReceivers() {
		IntentFilter btServiceFilter = new IntentFilter();
        btServiceFilter.addAction(BROADCAST_CONNECTION_STATE_CHANGE);
        btServiceFilter.addAction(BROADCAST_ERROR);
        btServiceFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mBtServiceReceiver, btServiceFilter);
	}
	
	private BroadcastReceiver mBtServiceReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
            //In case the connection is physically lost before the headstart library has been notified
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())) {
                Toast.makeText(MainActivity.this, R.string.warn_connection_lost, Toast.LENGTH_LONG).show();
                logger.log(Level.SEVERE, TAG+" Lost connection to card reader.");
                mConnection.getService().disconnectFromDevice();
            }
            else if (BROADCAST_CONNECTION_STATE_CHANGE.equals(intent.getAction())) {
				int status = intent.getIntExtra(HeadstartService.EXTRA_PED_STATE, 0);
				switch (status) {
					case DeviceConnectionState.CONNECTED:
						if (null != mDeferredTransaction) {
							startPayment(
									mDeferredTransaction.financialTransactionType, 
									mDeferredTransaction.amount, 
									mDeferredTransaction.currency, 
									mDeferredTransaction.cardHolderPresent, 
									mDeferredTransaction.originalTransactionId, 
									mDeferredTransaction.originalId);
							mDeferredTransaction = null;
						}
						break;
					case DeviceConnectionState.DISCONNECTED:
						mDeferredTransaction = null;
						break;
				}
			} else if (BROADCAST_ERROR.equals(intent.getAction())) {
				Toast.makeText(MainActivity.this, R.string.warn_turn_on_ped, Toast.LENGTH_LONG).show();
				mDeferredTransaction = null;
			}
		}
		
	};

//	private void showActivationDialog() {
//		createActivationDialog().show();
//	}

	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case ACTIVATION_DIALOG_ID:
			return createActivationDialog();

		default:
			return null;
		}
	};
	
	public Dialog createActivationDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.activation_dialog_message)
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dismissDialog(ACTIVATION_DIALOG_ID);
								isImportAccepted = true;
								isActivationDialogShowed = false;
								dispatchDeviceState();
							}
						})
				.setNegativeButton(android.R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dismissDialog(ACTIVATION_DIALOG_ID);
								isActivationDialogShowed = false;
								mImportIntent = null;
								if (null == HeadstartService
										.getProperty("auth_token")) {
									fullUnbindFromService();
									finish();
								}
							}
						});
		return builder.create();
	}
}