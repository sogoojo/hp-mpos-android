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
package com.handpoint.headstart.client.util;

import java.security.NoSuchAlgorithmException;

import com.handpoint.headstart.client.R;
import com.handpoint.headstart.client.ui.SettingsActivity;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

/**
 * 
 *
 */
public class PasswordValidator {

	private final Context ctx;
	
	public PasswordValidator(Context ctx) {
		this.ctx = ctx;
	}
	
	public void validatePins(String pin, String pin2) throws ValidationException {
		validatePins(pin, pin2, null);
	}
	
	public void validatePins(String pin, String pin2, String oldPin) throws ValidationException {
		if (null != oldPin) {
			validateOldPin(oldPin);
		}
		if (null == pin || pin.length() != 4 || !TextUtils.isDigitsOnly(pin)) {
			throw new ValidationException(ctx.getString(R.string.warn_invalid_password));
		}	
		validatePinsMatch(pin, pin2);
	}
	
	private void validateOldPin(String pin) throws ValidationException {
		try {
			String hash = SecurityUtil.MD5Hash(pin);
			String oldHash = PreferenceManager.getDefaultSharedPreferences(ctx).getString(SettingsActivity.PREFERENCE_PASSWORD, "-");
			if (TextUtils.isEmpty(pin) || !hash.equals(oldHash)) {
				throw new ValidationException(ctx.getString(R.string.warn_invalid_old_password));
			}
		} catch (NoSuchAlgorithmException e) {
			throw new ValidationException(ctx.getString(R.string.warn_invalid_old_password));		}	
	}
	
	private void validatePinsMatch(String pin, String pin2) throws ValidationException {
	      if (null == pin || !pin.equals(pin2)) {
	    	  throw new ValidationException(ctx.getString(R.string.warn_passwords_not_match));
		  }		
	}
	
	public static class ValidationException extends Exception {
		
		private static final long serialVersionUID = -6350301611347257038L;

		public ValidationException(String message) {
			super(message);
		}
	}

}
