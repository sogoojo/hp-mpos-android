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

import java.util.Timer;
import java.util.TimerTask;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import com.handpoint.headstart.client.R;

public class SplashActivity extends HeadstartActivity {
	private static final long SPLASH_DELAY = 3000;
	private Timer mTimer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		init();
		
		TimerTask timerTask = new TimerTask() {

			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						finish();
						Intent intent = new Intent(SplashActivity.this, MainActivity.class);
						startActivity(intent);
						overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
					}
				});
			}
		};
		mTimer = new Timer();
		mTimer.schedule(timerTask, SPLASH_DELAY);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (null != mTimer) {
			mTimer.cancel();
		}
	}
	
	private void init() {
		setContentView(R.layout.splash);
		TextView appVersionText = (TextView) findViewById(R.id.splash_version);

		String appVersionString = getResources().getString(R.string.version);
		try {
			appVersionString = 
					String.format(appVersionString, getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			appVersionString = String.format(appVersionString, "X");
		}

		appVersionText.setText(appVersionString);
		TextView copyRightText = (TextView) findViewById(R.id.splash_copyright);
		copyRightText.setText(Html.fromHtml(getResources().getString(R.string.copyright)));
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		init();
	}
}
