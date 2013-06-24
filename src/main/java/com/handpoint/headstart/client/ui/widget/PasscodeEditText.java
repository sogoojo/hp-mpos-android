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
package com.handpoint.headstart.client.ui.widget;

import android.content.Context;
import android.graphics.Rect;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

/**
 * 
 *
 */
public class PasscodeEditText extends EditText {

	/**
	 * @param context
	 */
	public PasscodeEditText(Context context) {
		super(context);
		init();
	}

	/**
	 * @param context
	 * @param attrs
	 */
	public PasscodeEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public PasscodeEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		setInputType(InputType.TYPE_CLASS_PHONE);
		setTransformationMethod(PasswordTransformationMethod.getInstance());
	}
	
	@Override
	protected void onFocusChanged(boolean focused, int direction,
			Rect previouslyFocusedRect) {
		super.onFocusChanged(focused, direction, previouslyFocusedRect);
		if (focused){
			setSelection(getText().length());
		}
	}
	
	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		return new PasscodeInputConnection(super.onCreateInputConnection(outAttrs),true);
	}
	
	private class PasscodeInputConnection extends InputConnectionWrapper {

		/**
		 * @param target
		 * @param mutable
		 */
		public PasscodeInputConnection(InputConnection target, boolean mutable) {
			super(target, mutable);
		}
		
		@Override
		public boolean sendKeyEvent(KeyEvent event) {
			if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
				if (PasscodeEditText.this.getText().length() == 0) {
					PasscodeEditText.this.setText(null);
					return true;
				}
			}				
			return super.sendKeyEvent(event);
		}
	}
}
