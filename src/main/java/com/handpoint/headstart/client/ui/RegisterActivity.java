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
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.handpoint.headstart.client.R;
import com.handpoint.headstart.client.util.PasswordValidator;
import com.handpoint.headstart.client.util.PasswordValidator.ValidationException;

/**
 * 
 *
 */
public class RegisterActivity extends HeadstartActivity implements OnClickListener {
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
		setContentView(R.layout.register);
		Button loginButton = (Button) findViewById(R.id.register_button);
		loginButton.setOnClickListener(this);		
    }
    	
	@Override
	public void onClick(View v) {
		PasscodeFragment pf = (PasscodeFragment)getSupportFragmentManager().findFragmentById(R.id.password_layout);
		PasscodeFragment pcf = (PasscodeFragment)getSupportFragmentManager().findFragmentById(R.id.password_confirm_layout);
		
		PasswordValidator pv = new PasswordValidator(this);
		try {
			pv.validatePins(pf.getPassword(), pcf.getPassword());
			Intent resultIntent = new Intent();
			resultIntent.putExtra("password", pf.getPassword());
			setResult(RESULT_OK, resultIntent);
			finish();
		} catch (ValidationException e) {
			Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
			pf.resetPassword();
			pcf.resetPassword();
		}
		
	}
    
}
