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
package com.handpoint.headstart.client.ui.widget;

import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.handpoint.headstart.client.R;
import com.handpoint.headstart.client.ui.SettingsActivity;
import com.handpoint.headstart.client.util.PasswordValidator;
import com.handpoint.headstart.client.util.PasswordValidator.ValidationException;
import com.handpoint.headstart.client.util.SecurityUtil;
import com.handpoint.util.logging.ApplicationLogger;
import com.handpoint.util.logging.Level;
import com.handpoint.util.logging.Logger;

/**
 * 
 *
 */
public class EditPinPreference extends EditTextPreference {

	protected static final String TAG = EditPinPreference.class.getSimpleName();
	protected static Logger logger = ApplicationLogger.getLogger(TAG);
	
	private OnPinChangeListener mListener;
	
	/**
	 * @param context
	 */
	public EditPinPreference(Context context) {
		super(context);
	}
	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public EditPinPreference(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	/**
	 * @param context
	 * @param attrs
	 */
	public EditPinPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	private EditText pinView;
	private EditText pin2View;
	private EditText pinOldView;
	private TextView pinOldLabel;
	
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		this.pinView = (EditText) view.findViewById(R.id.pin_view);
		this.pin2View = (EditText) view.findViewById(R.id.pin2_view);
		this.pinOldView = (EditText) view.findViewById(R.id.pin_old_view);
		this.pinOldLabel = (TextView) view.findViewById(R.id.pin_old_view_label);
		if (getSharedPreferences().contains(SettingsActivity.PREFERENCE_PASSWORD)) {
			pinOldView.setVisibility(View.VISIBLE);
			pinOldLabel.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		if (positiveResult) {
			try {
				PasswordValidator pv = new PasswordValidator(getContext());
				if (pinOldView.getVisibility() == View.VISIBLE) {
					pv.validatePins(						 
							pinView.getText().toString(), 
							pin2View.getText().toString(),
							pinOldView.getText().toString());
				} else {
					pv.validatePins(						 
							pinView.getText().toString(), 
							pin2View.getText().toString());
				}
				persistString(SecurityUtil.MD5Hash(pinView.getText().toString()));
				if (null != mListener) {
					mListener.onPinChanged(pinOldView.getText().toString(), pinView.getText().toString());
				}
			} catch (NoSuchAlgorithmException e) {
				logger.log(Level.SEVERE, "Error saving PIN", e);
			} catch (ValidationException e) {
				Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
	}
	
	public void registerPinChangeListener(OnPinChangeListener listener) {
		this.mListener = listener;
	}
		
	public static interface OnPinChangeListener {
		
		public void onPinChanged(String oldPin, String newPin);
		
	}
}
