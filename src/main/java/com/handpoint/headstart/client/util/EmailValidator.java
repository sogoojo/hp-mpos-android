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

import android.content.Context;
import android.text.TextUtils;

import com.handpoint.headstart.client.R;
import com.handpoint.headstart.client.data.SenderHelper;

/**
 * 
 *
 */
public class EmailValidator {

	private final Context ctx;
	
	public EmailValidator(Context ctx) {
		this.ctx = ctx;
	}

	public void validateEmailSettings(SenderHelper.MerchantEmailServerSettings settings, String receiverAddress) throws ValidationException{
		if (TextUtils.isEmpty(settings.user)) {
			throw new ValidationException(ctx.getString(R.string.invalid_server_settings, ctx.getString(R.string.merchant_email_user)));
		}
		if (TextUtils.isEmpty(settings.protocol)) {
			throw new ValidationException(ctx.getString(R.string.invalid_server_settings, ctx.getString(R.string.merchant_email_protocol)));
		}
		if (TextUtils.isEmpty(settings.pswd)) {
			throw new ValidationException(ctx.getString(R.string.invalid_server_settings, ctx.getString(R.string.merchant_email_pswd)));
		}
		if (TextUtils.isEmpty(settings.host)) {
			throw new ValidationException(ctx.getString(R.string.invalid_server_settings, ctx.getString(R.string.merchant_email_host)));
		}
		if (TextUtils.isEmpty(receiverAddress)) {
			throw new ValidationException(ctx.getString(R.string.invalid_server_settings, ctx.getString(R.string.merchant_email_address)));
		}
		
	}
	
	public static class ValidationException extends Exception {
		
		private static final long serialVersionUID = 3521980003447345566L;

		public ValidationException(String message) {
			super(message);
		}
	}

}
