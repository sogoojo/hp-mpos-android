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

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.handpoint.headstart.client.R;
import com.handpoint.headstart.client.android.Application;

/**
 * 
 *
 */
public class RawReceiptActivity extends HeadstartActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final ActionBar bar = getSupportActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
		Bundle merchantBundle = new Bundle();
		merchantBundle.putString("receiptText", getIntent().getStringExtra("merchantReceipt"));
		merchantBundle.putString("additionalText", getIntent().getStringExtra("signatureVerificationText"));
		merchantBundle.putString("additionalImage", getIntent().getStringExtra("signatureImage"));
		Bundle customerBundle = new Bundle();
		customerBundle.putString("receiptText", getIntent().getStringExtra("customerReceipt"));
		bar.addTab(bar.newTab()
				.setText(R.string.merchant_tab_label)
				.setTabListener(new TabListener<ReceiptFragment>(this, "merchant", ReceiptFragment.class, merchantBundle)));
		bar.addTab(bar.newTab()
				.setText(R.string.customer_tab_label)
				.setTabListener(new TabListener<ReceiptFragment>(this, "customer", ReceiptFragment.class, customerBundle)));
	}
	
    public static class TabListener<T extends Fragment> implements ActionBar.TabListener {                                                                                                                                                   
        private final SherlockFragmentActivity mActivity;                                                                                                                                                                                                    
        private final String mTag;                                                                                                                                                                                                           
        private final Class<T> mClass;                                                                                                                                                                                                       
        private final Bundle mArgs;                                                                                                                                                                                                          
        private Fragment mFragment;                                                                                                                                                                                                          
                                                                                                                                                                                                                                             
        public TabListener(SherlockFragmentActivity activity, String tag, Class<T> clz) {                                                                                                                                                                    
            this(activity, tag, clz, null);                                                                                                                                                                                                  
        }                                                                                                                                                                                                                                    
                                                                                                                                                                                                                                             
        public TabListener(SherlockFragmentActivity activity, String tag, Class<T> clz, Bundle args) {                                                                                                                                                       
            mActivity = activity;                                                                                                                                                                                                            
            mTag = tag;                                                                                                                                                                                                                      
            mClass = clz;                                                                                                                                                                                                                    
            mArgs = args;                                                                                                                                                                                                                    
                                                                                                                                                                                                                                             
            // Check to see if we already have a fragment for this tab, probably                                                                                                                                                             
            // from a previously saved state.  If so, deactivate it, because our                                                                                                                                                             
            // initial state is that a tab isn't shown.    
            mFragment = mActivity.getSupportFragmentManager().findFragmentByTag(mTag);                                                                                                                                                              
            if (mFragment != null && !mFragment.isDetached()) {                                                                                                                                                                              
                FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();                                                                                                                                                  
                ft.detach(mFragment);                                                                                                                                                                                                        
                ft.commit();                                                                                                                                                                                                                 
            }                                                                                                                                                                                                                                
        }                                                                                                                                                                                                                                    
                                                                                                                                                                                                                                             
        public void onTabSelected(Tab tab, FragmentTransaction ft) {                                                                                                                                                                         
            if (mFragment == null) {                                                                                                                                                                                                         
                mFragment = Fragment.instantiate(mActivity, mClass.getName(), mArgs);                                                                                                                                                        
                ft.add(android.R.id.content, mFragment, mTag);                                                                                                                                                                               
            } else {                                                                                                                                                                                                                         
                ft.attach(mFragment);                                                                                                                                                                                                        
            }                                                                                                                                                                                                                                
        }                                                                                                                                                                                                                                    
                                                                                                                                                                                                                                             
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {                                                                                                                                                                       
            if (mFragment != null) {                                                                                                                                                                                                         
                ft.detach(mFragment);                                                                                                                                                                                                        
            }                                                                                                                                                                                                                                
        }                                                                                                                                                                                                                                    
                                                                                                                                                                                                                                             
        public void onTabReselected(Tab tab, FragmentTransaction ft) {                                                                                                                                                                       
        }                                                                                                                                                                                                                                    
    }
    
    public static class ReceiptFragment extends Fragment {

    	private String receiptText = "";
    	private String additionalText = "";
    	private String additionalImage = null;
    	
    	@Override
    	public void onCreate(Bundle savedInstanceState) {
    		super.onCreate(savedInstanceState);
    		Bundle args = getArguments();
    		if (null != args) {
    			receiptText = args.getString("receiptText");
    			additionalText = args.getString("additionalText");
    			additionalImage = args.getString("additionalImage");
    		}
    	}
    	
    	@Override
    	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    		View v = inflater.inflate(R.layout.raw_receipt, null);
    		WebView receipt = (WebView) v.findViewById(R.id.raw_reciept);
            if(Build.VERSION.SDK_INT < 14)
                receipt.getSettings().setTextSize(WebSettings.TextSize.SMALLER);
            else
                receipt.getSettings().setTextZoom((int)(receipt.getSettings().getTextZoom()*0.6));
    		StringBuilder sb = new StringBuilder();
    		sb.append(receiptText);
    		if (!TextUtils.isEmpty(additionalText)) {
    			sb.append("<br><br>")
    			.append(additionalText);
    		}

            // Copy receipt should not be show in raw view
            String strToReplace = "{COPY_RECEIPT}";
            int pBegin = sb.indexOf(strToReplace);
            if (pBegin >= 0) {
                int pEnd = sb.indexOf(strToReplace) + strToReplace.length();
                sb.replace(pBegin,pEnd,"<br>");
            }

            //Display receipt on screen
            receipt.loadDataWithBaseURL("fake://not/needed", sb.toString(), "text/html", "utf-8", "");

            // Display signature
            if (!TextUtils.isEmpty(additionalImage)) {
    			Display display = getActivity().getWindowManager().getDefaultDisplay();
    			
    		    int targetW = display.getHeight();
    		    int targetH = display.getWidth();
    		    
    			ImageView image = (ImageView) v.findViewById(R.id.some_image);
    			image.setVisibility(View.VISIBLE);
    			Bitmap bitmap = ((Application)getActivity().getApplication()).scaleImageToSize(Uri.parse(additionalImage), targetW, targetH);
    			image.setImageBitmap(bitmap);
    		}
    		return v;
    	}
    }
}
