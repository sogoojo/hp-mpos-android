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

import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.handpoint.headstart.android.HeadstartService;
import com.handpoint.headstart.api.FinancialTransactionResult;
import com.handpoint.headstart.client.R;

/**
 * 
 *
 */
public class OptionFragment extends SherlockFragment implements OnClickListener{
	
	/**
	 * 
	 */
	private static final String PAYMENT_DEFAULT_OPERATION = "payment_default_operation";
	
	private static final String FAQ_URL = "http://support.handpoint.com";
	private static final String ABOUT_URL = "http://handpoint.com/company/about/";
//	private static final String CALCULATOR_PACKAGE = "com.android.calculator2";  
//	private static final String CALCULATOR_CLASS = "com.android.calculator2.Calculator"; 
			
	private ImageView mConnectAction;
	private ImageView mOperationAction;
	private TextView mOperationLabel;
	private ImageView mLastTransactionAction;
	private ImageView mSettingsAction;
	private ImageView mFaqAction;
	private ImageView mAboutAction;
	private ImageView mCalculatorAction;
	private ImageView mLogoutAction;
	
    private SharedPreferences mPreferences;
	private OnOptionActionListener mListener; 

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());		
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		initOptionsView();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.options, container, false);
		return view;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
        try {
            mListener = (OnOptionActionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnOptionActionListener");
        }
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}
	
	private void initOptionsView() {
		mConnectAction = (ImageView) getView().findViewById(R.id.o_connect);
		mConnectAction.setOnClickListener(this);
		mOperationAction = (ImageView) getView().findViewById(R.id.o_operation);
		mOperationAction.setOnClickListener(this);
		mOperationLabel = (TextView) getView().findViewById(R.id.o_operation_label);
		mLastTransactionAction = (ImageView) getView().findViewById(R.id.o_last_transaction);
		mLastTransactionAction.setOnClickListener(this);
		mSettingsAction = (ImageView) getView().findViewById(R.id.o_settings);
		mSettingsAction.setOnClickListener(this);
		mFaqAction = (ImageView) getView().findViewById(R.id.o_faq);
		mFaqAction.setOnClickListener(this);
		mAboutAction = (ImageView) getView().findViewById(R.id.o_about);
		mAboutAction.setOnClickListener(this);		
		mCalculatorAction = (ImageView) getView().findViewById(R.id.o_calculator);
		mCalculatorAction.setOnClickListener(this);		
		mLogoutAction = (ImageView) getView().findViewById(R.id.o_logout);
		mLogoutAction.setOnClickListener(this);
		initOptionsIcons();
	}

    @Override
	public void onClick(View v) {
		if (v.getId() == R.id.o_connect) {
			showDeviceChooser();
		} else if (v.getId() == R.id.o_operation) {
			switchToAlternativeOperation();
			initOptionsIcons();
			showMainAction();
		} else if (v.getId() == R.id.o_last_transaction) {
			showPaymentHistory();
		} else if (v.getId() == R.id.o_settings) {
			showSettings();
		} else if( v.getId() == R.id.o_faq) {
			showUrl(FAQ_URL);
		} else if (v.getId() == R.id.o_about) {
			showUrl(ABOUT_URL);
		} else if (v.getId() == R.id.o_calculator) {
			showCalculator();
		} else if (v.getId() == R.id.o_logout) {
			showLogout();
		}
	}
	
	private void showDeviceChooser() {
		Intent i = new Intent(HeadstartService.ACTION_DEVICE_PICK);
		startActivity(i);
	}
	
	private void switchToAlternativeOperation() {
		int defaultOperation = FinancialTransactionResult.FT_TYPE_SALE;
		if (getPrimaryFinancialTransactionType() == FinancialTransactionResult.FT_TYPE_SALE) {
			defaultOperation = FinancialTransactionResult.FT_TYPE_REFUND;
		}
		mPreferences.edit().putInt(PAYMENT_DEFAULT_OPERATION, defaultOperation).commit();
		initOptionsIcons();
	}
	
	private void initOptionsIcons() {
		if (getPrimaryFinancialTransactionType() == FinancialTransactionResult.FT_TYPE_SALE) {
			mOperationLabel.setText(R.string.refund);
		} else {
			mOperationLabel.setText(R.string.pay);
		}
	}
	
	private void showMainAction() {
		mListener.onShowMainAction();
	}
	
	private int getPrimaryFinancialTransactionType() {		
		return mPreferences.getInt(PAYMENT_DEFAULT_OPERATION, FinancialTransactionResult.FT_TYPE_SALE);
	}
	
	private void showCalculator() {
		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		final List<ResolveInfo> pkgAppsList = getActivity().getPackageManager().queryIntentActivities( mainIntent, 0);
		for (ResolveInfo resolveInfo : pkgAppsList) {
			if (null != resolveInfo && null != resolveInfo.activityInfo && resolveInfo.activityInfo.name.matches(".*\\.Calculator.*")) {
				Intent i = new Intent();
				i.setAction(Intent.ACTION_MAIN);
				i.addCategory(Intent.CATEGORY_LAUNCHER);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
				i.setComponent(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
				startActivity(i);
			}
		}
		
	}
	
	private void showLogout() {
		mListener.onLogoutAction();
	}
	
	private void showPaymentHistory() {
		mListener.onShowHistoryAction();
	}
	
	private void showSettings() {
		startActivity(new Intent(getActivity(), SettingsActivity.class));
	}
	
	private void showUrl(String url) {
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		startActivity(i);		
	}		

	public interface OnOptionActionListener {
		
		public void onShowMainAction();
		
		public void onShowHistoryAction();
		
		public void onLogoutAction();
	}
}
