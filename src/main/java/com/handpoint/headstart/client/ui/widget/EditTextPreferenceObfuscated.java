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

import com.handpoint.headstart.android.HeadstartService;
import com.handpoint.headstart.client.util.SecurityUtil;
import com.handpoint.util.HexFormat;
import com.handpoint.util.logging.ApplicationLogger;
import com.handpoint.util.logging.Level;
import com.handpoint.util.logging.Logger;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * 
 *
 */
public class EditTextPreferenceObfuscated extends EditTextPreference {

	protected static final String TAG = EditTextPreferenceObfuscated.class.getSimpleName();
	protected static Logger logger = ApplicationLogger.getLogger(TAG);
	
	/**
	 * @param context
	 */
	public EditTextPreferenceObfuscated(Context context) {
		super(context);
	}
	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public EditTextPreferenceObfuscated(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	/**
	 * @param context
	 * @param attrs
	 */
	public EditTextPreferenceObfuscated(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	public String getText() {
		return null;
	}
	
	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		super.setText(restoreValue ? getPersistedString(null) : (String) defaultValue);
	}
	
	@Override
	public void setText(String text) {
		if (null == text || text.length() == 0 || null == HeadstartService.getProperty("auth_token")) {
			return;
		}
		try {
			super.setText(SecurityUtil.encrypt(HeadstartService.getProperty("auth_token").getBytes(), HexFormat.hexToBytes(text)));
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error setting text", e);
		}
	}
}
