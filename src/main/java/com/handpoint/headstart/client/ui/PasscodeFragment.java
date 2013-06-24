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

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.handpoint.headstart.client.R;


/**
 * 
 *
 */
public class PasscodeFragment extends Fragment implements TextWatcher, OnTouchListener {
	private Handler handler = new Handler();
	
	private EditText mPassword1View;
	private EditText mPassword2View;
	private EditText mPassword3View;
	private EditText mPassword4View;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.passcode_layout, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		this.mPassword1View = (EditText) getView().findViewById(R.id.password1);
		mPassword1View.addTextChangedListener(this);
		mPassword1View.setOnTouchListener(this);
		this.mPassword2View = (EditText) getView().findViewById(R.id.password2);
		mPassword2View.addTextChangedListener(this);
		mPassword2View.setOnTouchListener(this);
		this.mPassword3View = (EditText) getView().findViewById(R.id.password3);
		mPassword3View.addTextChangedListener(this);
		mPassword3View.setOnTouchListener(this);
		this.mPassword4View = (EditText) getView().findViewById(R.id.password4);
		mPassword4View.addTextChangedListener(this);		
		mPassword4View.setOnTouchListener(this);
	}
	
	public String getPassword() {
		return mPassword1View.getText().toString() +
				mPassword2View.getText().toString() +
				mPassword3View.getText().toString() +
				mPassword4View.getText().toString();
	}
	
	public void resetPassword() {
		mPassword1View.setText(null);
		mPassword2View.setText(null);
		mPassword3View.setText(null);
		mPassword4View.setText(null);		
	}

	@Override
	public void afterTextChanged(Editable s) {
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}

	private static final int FORWARD = 1;
	private static final int BACKWARD = 2;
	private static final int STAY = 3;
	
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		int direction = STAY;
		if (before == 0 && count == 1) {
			direction = FORWARD;
		} else if (before == 0 && count == 0) {
			direction = BACKWARD;
		}
		View v = getActivity().getCurrentFocus();
		if (null == v) {
			return;
		}
		int currentView = getActivity().getCurrentFocus().getId();
		if (currentView == mPassword1View.getId()) {
			if (direction == FORWARD) {
				handler.postDelayed(new Runnable() {
					
					@Override
					public void run() {
						mPassword1View.clearFocus();
						mPassword2View.requestFocus();						
					}
				}, 100);
			} else if (direction == BACKWARD) {
				closeSoftKeyboard(mPassword1View);
			}
		} else if (currentView == mPassword2View.getId()) {
			if (direction == FORWARD) {
				handler.postDelayed(new Runnable() {
					
					@Override
					public void run() {
						mPassword2View.clearFocus();
						mPassword3View.requestFocus();
					}
				}, 100);
			} else if (direction == BACKWARD) {
				mPassword1View.requestFocus();
				mPassword1View.setText(null);
			}
		} else if (currentView == mPassword3View.getId()) {
			if (direction == FORWARD) {
				handler.postDelayed(new Runnable() {
					
					@Override
					public void run() {
						mPassword3View.clearFocus();
						mPassword4View.requestFocus();
					}
				}, 100);
			} else if (direction == BACKWARD) {
				mPassword2View.requestFocus();
				mPassword2View.setText(null);
			}
		} else {
			if (direction == FORWARD) {
				handler.postDelayed(new Runnable() {
					
					@Override
					public void run() {
						mPassword4View.clearFocus();
						closeSoftKeyboard(mPassword4View);
					}
				}, 100);
			} else if (direction == BACKWARD) {
				mPassword3View.requestFocus();
				mPassword3View.setText(null);
			}
		}
	}
	
	private void closeSoftKeyboard(View v) {
		InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		v.onTouchEvent(event);
		if (event.getAction() == MotionEvent.ACTION_UP) {
			if (mPassword1View.getText().length() == 0) {
				mPassword1View.requestFocus();
			} else if (mPassword2View.getText().length() == 0) {
				mPassword2View.requestFocus();
			} else if (mPassword3View.getText().length() == 0) {
				mPassword3View.requestFocus();
			} else {
				mPassword4View.requestFocus();
			}
			return true;
		}
		return false;
	}
}
