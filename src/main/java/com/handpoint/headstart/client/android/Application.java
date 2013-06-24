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
package com.handpoint.headstart.client.android;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.handpoint.headstart.android.HeadstartService;
import com.handpoint.headstart.client.R;
import com.handpoint.headstart.client.data.models.BasketItem;
import com.handpoint.headstart.client.ui.SettingsActivity;
import com.handpoint.headstart.spi.logging.LoggerManagerAndroid;
import com.handpoint.util.logging.ApplicationLogger;
import com.handpoint.util.logging.Level;
import com.handpoint.util.logging.Logger;

public class Application extends com.handpoint.headstart.android.Application {

	private static final String TAG = Application.class.getSimpleName();
	private static final Logger logger = ApplicationLogger.getLogger(TAG);
	
	@Override
	public void onCreate() {
		super.onCreate();
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		ApplicationLogger.setLoggerManager(
				new LoggerManagerAndroid(
						getLogFilePath(),
						LoggerManagerAndroid.DEFAULT_MAX_FILE_SIZE,
						LoggerManagerAndroid.DEFAULT_MAX_BACKUP_SIZE,
						LoggerManagerAndroid.DEFAULT_FILE_PATTERN,
						SettingsActivity.getLogsLevel(preferences)));			
	}
	
	@Override
	protected int getCurrenciesResource() {
		return R.xml.currencies;
	}
	
	public String getLogFilePath() {
		return getLogsDir().getAbsolutePath() + File.separator + getLogBaseFileName();
	}
	
	public File getLogsDir() {
		return getFilesDir();
	}
	public String getLogBaseFileName() {
		return "handpoint.log";
	}
	
	public Drawable getCardSchemeLogo(String cardSchemeName) {
		Drawable result = getResources().getDrawable(R.drawable.logo_card_unknown);
		if (null == cardSchemeName) {
			return result;
		}
		
		if (cardSchemeName.matches("(?i).*ELECTRON.*") || cardSchemeName.matches("(?i).*DEBIT.*")) {
			result = getResources().getDrawable(R.drawable.logo_visa_electron);
		} else if (cardSchemeName.matches("(?i).*visa.*") || cardSchemeName.matches("(?i).*CREDIT.*")) {
			result = getResources().getDrawable(R.drawable.logo_visa);
		} else if (cardSchemeName.matches("(?i).*mastercard")) {
			result = getResources().getDrawable(R.drawable.logo_mastercard);
		} else if (cardSchemeName.matches("(?i).*MAESTRO.*")) {
			result = getResources().getDrawable(R.drawable.logo_maestro);
		} else if (cardSchemeName.matches("(?i).*AMEX.*")) {
			result = getResources().getDrawable(R.drawable.logo_amex);
		} else if (cardSchemeName.matches("(?i).*JCB.*")) {
			result = getResources().getDrawable(R.drawable.logo_jcb);
		} else if (cardSchemeName.matches("(?i).*UNIONPAY.*")) {
			result = getResources().getDrawable(R.drawable.logo_union_pay);
		} else if (cardSchemeName.matches("(?i).*DISCOVER.*")) {
			result = getResources().getDrawable(R.drawable.logo_discover);
		} else if (cardSchemeName.matches("(?i).*DINERS.*")) {
			result = getResources().getDrawable(R.drawable.logo_diners_club);
		}

		return result;
	}
	
	public void validateActivity(String delayStr) {
		long lastActivity = SystemClock.elapsedRealtime();
		int delay = 0;
		String lastActivityStr = HeadstartService.getProperty("last_activity");
        HeadstartService.removeProperty("last_activity");
		try {
			if (null != lastActivityStr) {
				lastActivity = Long.parseLong(lastActivityStr);
			}
			delay = Integer.parseInt(delayStr) * 1000;
		} catch (NumberFormatException ignore) {
		}
		if (delay == 0) {
			delay = Integer.MAX_VALUE;
		}
		long inactiveTime = SystemClock.elapsedRealtime() - lastActivity;
		if (inactiveTime > delay || inactiveTime < 0) {
            HeadstartService.removeProperty("auth_token");
		}
	}
	
	public void setLastActivityTime() {
        HeadstartService.setProperty("last_activity", Long.toString(SystemClock.elapsedRealtime()));
	}
	
	/**
	 * @param statusMessage
	 * @param errorMessage
	 * @return
	 */
	public String formatErrorMessage(String statusMessage, String errorMessage) {
		StringBuilder sb = new StringBuilder();
		boolean statusMessageExists = false;
		if (null != statusMessage && statusMessage.length() > 0) {
			sb.append(statusMessage);
			statusMessageExists = true;
		}
		if (null != errorMessage && errorMessage.length() > 0) {
			if (statusMessageExists) {
				sb.append(" (");
			}
			sb.append(errorMessage);
			if (statusMessageExists) {
				sb.append(")");
			}
		}
		return sb.length() > 0 ? sb.toString() : getString(R.string.error_unknown);
	}
	
	public boolean removePhoto(BasketItem basketItem) {
		if (null == basketItem.getFullSizePhotoPath()) {
			return true;
		}
		File f = new File(Uri.parse(basketItem.getFullSizePhotoPath()).getPath());
		return f.delete();
	}
	
	public Bitmap scaleImageToSize(Uri source, int width, int height) {
		Bitmap bitmap = null;
		try {
	        //Decode image size
	        BitmapFactory.Options o = new BitmapFactory.Options();
	        o.inJustDecodeBounds = true;
	        bitmap = decodeBitmap(source, o);
	        int scale=1;
	        while(o.outWidth/scale/2 >= width && o.outHeight/scale/2 >= height) {
	            scale*=2;
	        }
	        //Decode with inSampleSize
	        o.inJustDecodeBounds = false;
	        o.inSampleSize=scale;
	        o.inPurgeable = true;
	        bitmap = decodeBitmap(source, o);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error loading full-size image from URI: " + source, e);
		}
		return bitmap;
	}

	private Bitmap decodeBitmap(Uri source, BitmapFactory.Options o) throws FileNotFoundException {
		Bitmap bitmap = null;
		if (source.getScheme().equals("file")) {
			bitmap = BitmapFactory.decodeFile(source.getPath(), o);
		} else if (source.getScheme().equals("content")) {
			bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(source), null, o);
		}
		return bitmap;
	}

}
