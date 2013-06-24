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

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.handpoint.headstart.client.R;
import com.handpoint.headstart.client.android.Application;
import com.handpoint.util.logging.ApplicationLogger;
import com.handpoint.util.logging.Level;
import com.handpoint.util.logging.Logger;

/**
 * 
 *
 */
public class ImagePreviewActivity extends HeadstartActivity {

	private static final String TAG = ImagePreviewActivity.class.getSimpleName();
	private static Logger logger = ApplicationLogger.getLogger(TAG);

	private ImageView mImageView;
	private Uri mImageUri;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_preview);
		mImageView = (ImageView) findViewById(R.id.preview_image);
		mImageUri = getIntent().getData();
		try {
			setPic();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error loading full-size image from URI=" + mImageUri, e);
			//set retake result
			setResult(Activity.RESULT_FIRST_USER + 1);
			finish();
		}		
		Button retakeButton = (Button) findViewById(R.id.retake_button);
		retakeButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//set retake result
				setResult(Activity.RESULT_FIRST_USER + 1);
				finish();
			}
		});
		Button removeButton = (Button) findViewById(R.id.remove_button);
		removeButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//set remove result
				setResult(Activity.RESULT_FIRST_USER + 2);
				finish();
			}
		});
	}

	private void setPic() {
	    // Get the dimensions of the screen
		Display display = getWindowManager().getDefaultDisplay();
		
	    int targetW = display.getHeight();
	    int targetH = display.getWidth();
	    
	    mImageView.setImageBitmap(((Application)getApplication()).scaleImageToSize(mImageUri, targetW, targetH));
	}
	
	@Override
	protected void onDestroy() {
	    super.onDestroy();
	 
	    unbindDrawables(findViewById(R.id.RootView));
	    System.gc();
	}
	 
	private void unbindDrawables(View view) {
	    if (view.getBackground() != null) {
	        view.getBackground().setCallback(null);
	    }
	    if (view instanceof ViewGroup) {
	        for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
	            unbindDrawables(((ViewGroup) view).getChildAt(i));
	        }
	        ((ViewGroup) view).removeAllViews();
	    }
	}	
}
