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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.handpoint.headstart.android.HeadstartService;
import com.handpoint.headstart.client.android.Application;

public class HeadstartPreferenceActivity extends SherlockPreferenceActivity {

    protected SharedPreferences mPreferences;
    
    @Override
    protected void onCreate(Bundle arg0) {
    	super.onCreate(arg0);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);    	
    }
    
	@Override
	protected void onResume() {
		super.onResume();
		((Application) getApplication()).validateActivity(mPreferences.getString(SettingsActivity.PREFERENCE_LOCK_DELAY, "0"));
		if (null == HeadstartService.getProperty("auth_token")) {
			finish();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		((Application) getApplication()).setLastActivityTime();
	}	
}
