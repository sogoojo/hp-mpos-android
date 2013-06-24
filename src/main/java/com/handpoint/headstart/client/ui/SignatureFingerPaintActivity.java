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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.Button;
import android.widget.TextView;

import com.handpoint.headstart.android.HeadstartService;
import com.handpoint.headstart.android.HeadstartServiceConnection;
import com.handpoint.headstart.client.R;
import com.handpoint.headstart.client.android.Application;
import com.handpoint.headstart.client.ui.widget.PaintView;
import com.handpoint.util.logging.ApplicationLogger;
import com.handpoint.util.logging.Level;
import com.handpoint.util.logging.Logger;

/**
 * Custom signature request handler with ability of make signature via finger.
 *
 */
public class SignatureFingerPaintActivity extends Activity {

	private static final String TAG = SignatureFingerPaintActivity.class.getSimpleName();
	private static Logger logger = ApplicationLogger.getLogger(TAG);
	
    private SharedPreferences mPreferences;

	private Button mAcceptButton;
	private Button mDeclineButton;
	private Button mResignButton;
	private PaintView mPaintView;
	
    private HeadstartServiceConnection mConnection;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.signature);    	
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);    	
    	
    	mAcceptButton = (Button) findViewById(R.id.accept_button);
    	mDeclineButton = (Button) findViewById(R.id.decline_button);
    	mResignButton = (Button) findViewById(R.id.resign_button);
    	mPaintView = (PaintView) findViewById(R.id.signature_view);
    	TextView merchantName = (TextView) findViewById(R.id.merchant_name);
    	merchantName.setText(mPreferences.getString(HeadstartService.PREFERENCE_MERCHANT_NAME, ""));
    	
    	mResignButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				initForUserUsage();
			}
		});
    	mAcceptButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				try {
					String signaturePath = saveSignature();
					mConnection.getService().signatureAccept(signaturePath);
					finish();
				} catch (IOException e) {
					logger.log(Level.SEVERE, "Fail to save signature bitmap", e);
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Probably null pointer exception on a bitmap creation");
				}
				
			}
		});
    	mDeclineButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				mConnection.getService().signatureDecline();
				finish();				
			}
		});
    	mConnection = new HeadstartServiceConnection(this, false);

    }
	@Override
	protected void onResume() {
		super.onResume();
		((Application) getApplication()).validateActivity(mPreferences.getString(SettingsActivity.PREFERENCE_LOCK_DELAY, "0"));		
	}
    
	@Override
	protected void onPause() {
		super.onPause();
		((Application) getApplication()).setLastActivityTime();
	}
    
    void initForUserUsage() {
    	mPaintView.clear();
    	mPaintView.setPaintEnable(true);
    }
    
    void initForMerchantUsage() {
    	mPaintView.setPaintEnable(false);
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	if (null != mConnection) {
    		mConnection.doUnbindService(false);
    	}
    }
    
    private String saveSignature() throws IOException {
    	PaintView v = mPaintView;
    	v.setDrawingCacheEnabled(true);
    	// this is the important code :)
    	// Without it the view will have a
    	// dimension of 0,0 and the bitmap will
    	// be null
    	v.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    	v.layout(0, 0, v.getWidth(), v.getHeight());
    	v.buildDrawingCache(true);
    	Bitmap bm = Bitmap.createBitmap(v.getDrawingCache());
    	v.setDrawingCacheEnabled(false);
        String path = Environment.getExternalStorageDirectory().toString();
        OutputStream fOut = null;
    	File file = new File(path, "screentest.jpg");
    	fOut = new FileOutputStream(file);
    	bm.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
    	fOut.flush();
    	fOut.close();
    	String imagePath = MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
    	logger.log(Level.FINE, "Signature saved to: " + imagePath);

        return imagePath;
    }

}
