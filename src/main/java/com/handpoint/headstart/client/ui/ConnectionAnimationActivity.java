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

import static com.handpoint.headstart.android.HeadstartService.BROADCAST_CONNECTION_STATE_CHANGE;
import static com.handpoint.headstart.android.HeadstartService.BROADCAST_ERROR;

import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.handpoint.headstart.android.HeadstartService;
import com.handpoint.headstart.android.HeadstartServiceConnection;
import com.handpoint.headstart.android.ParcelDeviceDescriptor;
import com.handpoint.headstart.api.DeviceConnectionState;
import com.handpoint.headstart.api.DeviceDescriptor;
import com.handpoint.headstart.client.R;
import com.handpoint.util.logging.ApplicationLogger;
import com.handpoint.util.logging.Level;
import com.handpoint.util.logging.Logger;

/**
 * 
 * Default progress dialog during connection to remote device.
 *
 */
public class ConnectionAnimationActivity extends HeadstartActivity {

	private static final String TAG = ConnectionAnimationActivity.class.getSimpleName();
	private static Logger logger = ApplicationLogger.getLogger(TAG);
	
	private static final long FACE_DELAY = 2000;
	
    private PowerManager.WakeLock mWakeLock;
    HeadstartServiceConnection mConnection;
	private LayoutInflater mInflater;    
    DeviceDescriptor mDeviceDescriptor;
	private FrameLayout frameLayout;	
	private TextView transactionStepView;
	Button mButton;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ParcelDeviceDescriptor parcel = getIntent().getParcelableExtra(HeadstartService.EXTRA_REMOTE_DEVICE);
		if (null != parcel) {
			mDeviceDescriptor = parcel.getResult();
		}
		logger.log(Level.FINEST, "onCreate()::device descriptor: " + mDeviceDescriptor);
        registerBroadcastReceivers();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.connect_animation);

		this.transactionStepView = (TextView) findViewById(R.id.transaction_step_text);
        this.transactionStepView.setText(getString(R.string.connecting));
        
		this.frameLayout = (FrameLayout) findViewById(R.id.animationFrame);

        mButton = (Button) findViewById(R.id.bottom_button);        
        mButton.setText(R.string.cancel);
        mButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				cancelCurrentOperation();
			}
		});
        
		mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
    	mConnection = new HeadstartServiceConnection(this, false, new HeadstartServiceConnection.BindListener() {
			
			public void onBindCompleted() {
				if (mConnection.getService().getCurrentConnectionState() == DeviceConnectionState.CONNECTED) {
					finish();
					return;
				}
			}
		});
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(
        		PowerManager.SCREEN_DIM_WAKE_LOCK |
        		PowerManager.ON_AFTER_RELEASE, "com.handpoint.headstart.hal");
		mWakeLock.acquire();
	}
	
	private void registerBroadcastReceivers() {
		IntentFilter btServiceFilter = new IntentFilter();
        btServiceFilter.addAction(BROADCAST_CONNECTION_STATE_CHANGE);
        btServiceFilter.addAction(BROADCAST_ERROR);
        registerReceiver(mBtServiceReceiver, btServiceFilter);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		animateConnection();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
		unregisterReceiver(mBtServiceReceiver);
    	if (null != mConnection) {
    		mConnection.doUnbindService(false);
    	}
	}
	/**
	 * Sends command for cancel current operation to service
	 * 
	 */
	protected void cancelCurrentOperation() {
		if (mConnection.isBinded() && mConnection.getService().getCurrentConnectionState() == DeviceConnectionState.CONNECTING) {
			mConnection.getService().cancelConnectionProccess();
		}
		finish();
	}
		
	/**
	 * Broadcast receiver for handling events from service 
	 */
	private BroadcastReceiver mBtServiceReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Timer timer = new Timer();

			if (BROADCAST_CONNECTION_STATE_CHANGE.equals(intent.getAction())) {
				int status = intent.getIntExtra(HeadstartService.EXTRA_PED_STATE, 0);
				switch (status) {
					case DeviceConnectionState.CONNECTED:
						animateFinishingConnection(true);
						timer.schedule(timerTask, FACE_DELAY);
						break;
				}
			} else if (BROADCAST_ERROR.equals(intent.getAction())) {
				animateFinishingConnection(false);
				timer.schedule(timerTask, FACE_DELAY);
			}
		}
		
	};
	
	LinearLayout mTransactionView;
	/**
	 * Animation for connection process
	 */
	void animateConnection() {
		logger.log(Level.FINE, "Start connection animation");
		try {
			
			clearAnimationView();
			
			mTransactionView = (LinearLayout) mInflater.inflate(R.layout.mped_connection_animation, null);
			mTransactionView.setGravity(Gravity.CENTER);
			frameLayout.addView(mTransactionView);
			final View arrows = mTransactionView.findViewById(R.id.item2);
			
			Animation rotateAnimation = AnimationUtils.loadAnimation(ConnectionAnimationActivity.this, R.anim.rotate);
			arrows.startAnimation(rotateAnimation);
			

		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Error is occured during animation:" + ex.getMessage(), ex);
		}
	}
	
	void animateFinishingConnection(boolean success) {
		mButton.setEnabled(false);
		if (null != mTransactionView) {
			final View arrows = mTransactionView.findViewById(R.id.item2);
			arrows.clearAnimation();
			final ImageView face = (ImageView) mTransactionView.findViewById(R.id.item3);
			if (!success) {
				face.setImageResource(R.drawable.face2);
			}
			final Animation animationFadeIn = AnimationUtils.loadAnimation(this, R.anim.fadein_slow);
			animationFadeIn.setAnimationListener(new Animation.AnimationListener() {
				
				@Override
				public void onAnimationStart(Animation animation) {
					face.setVisibility(View.VISIBLE);
					arrows.setVisibility(View.GONE);
				}
				
				@Override
				public void onAnimationRepeat(Animation animation) {
					
				}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					face.setVisibility(View.VISIBLE);
				}
			});
			face.startAnimation(animationFadeIn);
		}
	}
	
	void clearAnimationView() {
		while (frameLayout.getChildCount() > 0 ) {
			View child = frameLayout.getChildAt(0);
			if (child instanceof ImageView) {
				ImageView imageView = (ImageView) child;
				imageView.clearAnimation();
			}
			frameLayout.removeView(child);
		}
	}
	
	TimerTask timerTask = new TimerTask() {

		@Override
		public void run() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					finish();
				}
			});
		}
	};	
}
