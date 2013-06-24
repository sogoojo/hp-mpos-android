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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.handpoint.headstart.android.HeadstartService;
import com.handpoint.headstart.client.android.Application;

import java.util.Locale;

public class HeadstartActivity extends SherlockFragmentActivity {

    protected SharedPreferences mPreferences;
    
    @Override
    protected void onCreate(Bundle arg0) {
    	super.onCreate(arg0);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

	@Override
	protected void onResume() {
		super.onResume();
		setOrientation();
		((Application) getApplication()).validateActivity(mPreferences.getString(SettingsActivity.PREFERENCE_LOCK_DELAY, "0"));		
	}

	@Override
	protected void onPause() {
		super.onPause();
		((Application) getApplication()).setLastActivityTime();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		((Application) getApplication()).validateActivity(mPreferences.getString(SettingsActivity.PREFERENCE_LOCK_DELAY, "0"));		
	}
	
	protected void setOrientation() {

		DisplayMetrics metrics = this.getResources().getDisplayMetrics();
	    float physicalScreenWidth  = metrics.widthPixels  / metrics.xdpi;
	    float physicalScreenHeight = metrics.heightPixels / metrics.ydpi;

        if((getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else if (Math.sqrt(Math.pow(physicalScreenWidth, 2) + Math.pow(physicalScreenHeight, 2))  > 5) {
	    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	    }
	    else {
	    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    }
	}
	
	
}
