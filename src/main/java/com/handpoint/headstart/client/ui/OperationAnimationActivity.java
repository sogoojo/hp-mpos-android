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

import static com.handpoint.headstart.android.HeadstartService.ACTION_HANDLE_TIMEOUT;
import static com.handpoint.headstart.android.HeadstartService.ACTION_VERIFY_SIGNATURE;
import static com.handpoint.headstart.android.HeadstartService.BROADCAST_ERROR;
import static com.handpoint.headstart.android.HeadstartService.BROADCAST_FINANCIAL_TRANSACTION_FINISHED;
import static com.handpoint.headstart.android.HeadstartService.BROADCAST_OPERATION_CANCELL;
import static com.handpoint.headstart.android.HeadstartService.BROADCAST_TRANSACTION_STATE_CHANGE;
import static com.handpoint.headstart.android.HeadstartService.EXTRA_ERROR_DESCRIPTION;
import static com.handpoint.headstart.android.HeadstartService.EXTRA_FINANCIAL_RESULT;
import static com.handpoint.headstart.android.HeadstartService.EXTRA_MERCHANT_RECEIPT_TEXT;
import static com.handpoint.headstart.android.HeadstartService.EXTRA_TRANSACTION_STATE;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.handpoint.headstart.android.HeadstartService;
import com.handpoint.headstart.android.HeadstartServiceConnection;
import com.handpoint.headstart.android.ParcelDeviceState;
import com.handpoint.headstart.android.ParcelFinancialTransactionResult;
import com.handpoint.headstart.api.FinancialTransactionResult;
import com.handpoint.headstart.api.HeadstartOperationException;
import com.handpoint.headstart.client.R;
import com.handpoint.headstart.eft.DeviceState;
import com.handpoint.headstart.eft.TransactionStatus;
import com.handpoint.util.logging.ApplicationLogger;
import com.handpoint.util.logging.Level;
import com.handpoint.util.logging.Logger;

/**
 * 
 * Custom animation handler of progress dialog during financial transaction 
 *
 */
public class OperationAnimationActivity extends HeadstartActivity {

	private static final String TAG = OperationAnimationActivity.class.getSimpleName();
	private static Logger logger = ApplicationLogger.getLogger(TAG);

	private static final int BUTTON_TAG_CANCEL = 1;
	private static final int BUTTON_TAG_DONE = 2;
	
	private static final int DIALOG_TIMEOUT_ID = 0;	
	
    private PowerManager.WakeLock mWakeLock;
    HeadstartServiceConnection mConnection;
    DeviceState mDeviceDescriptor;
	int mOperationType = 0;
	String mCurrency;
	Button mButton;
	SharedPreferences mPreference;
	private FrameLayout frameLayout;	
	protected TextView transactionStepView;
	boolean mRegistered;
	String mErrorMessage;
	private LayoutInflater mInflater;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.connect_animation);
		
		this.transactionStepView = (TextView) findViewById(R.id.transaction_step_text);
        
		this.frameLayout = (FrameLayout) findViewById(R.id.animationFrame);

        mButton = (Button) findViewById(R.id.bottom_button);        
        mButton.setText(R.string.cancel);
        mButton.setTag(BUTTON_TAG_CANCEL);
        mButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				Integer action = (Integer)v.getTag();
				
				switch (action.intValue()) {
				case BUTTON_TAG_CANCEL:
					cancelCurrentOperation();
					break;

				default:
					doneCurrentOperation();
					break;
				}
			}
		});
		final Intent creator = getIntent();
		initErrorState(savedInstanceState, creator);
		try {
			mOperationType = creator.getIntExtra(HeadstartService.EXTRA_OPERATION_TYPE, 0);
		} catch (Exception e) {
		}
		mCurrency = creator.getStringExtra(HeadstartService.EXTRA_PAYMENT_CURRENCY);
    	mConnection = new HeadstartServiceConnection(this, false, new HeadstartServiceConnection.BindListener() {
			
			public void onBindCompleted() {
		        updateViewState();
			}
		});
		mPreference = PreferenceManager.getDefaultSharedPreferences(this);
		
		mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(
        		PowerManager.SCREEN_DIM_WAKE_LOCK |
        		PowerManager.ON_AFTER_RELEASE, "com.handpoint.headstart.hal");
		mWakeLock.acquire();
		
	}
	
	void updateViewState() {
		if (!mConnection.isBinded()) {
			return;
		}
		transactionStepView.setText(com.handpoint.headstart.R.string.ft_starting);
		if (null != mErrorMessage) {
			onTransactionError(mErrorMessage);
		} else if (!mConnection.getService().isInTransaction()) {
			finish();
		} else {
			onDeviceState(mConnection.getService().getDeviceState());
		}
	}

	private void initErrorState(Bundle savedInstanceState, final Intent creator) {
		mErrorMessage = 
				creator.hasExtra(HeadstartService.EXTRA_ERROR_DESCRIPTION) ?
						creator.getStringExtra(HeadstartService.EXTRA_ERROR_DESCRIPTION) :
							null != savedInstanceState ?
						savedInstanceState.getString(HeadstartService.EXTRA_ERROR_DESCRIPTION) :
							null;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(HeadstartService.EXTRA_ERROR_DESCRIPTION, mErrorMessage);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
    	if (null != mConnection) {
    		mConnection.doUnbindService(false);
    	}
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerBroadcastReceivers();
		mRegistered = true;
		updateViewState();
		if (HeadstartService.ACTION_HANDLE_TIMEOUT.equals(getIntent().getAction())) {
			showDialog(DIALOG_TIMEOUT_ID);
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (mRegistered) {
			unregisterReceiver(mHdstServiceReceiver);
			mRegistered = false;
		}
	}

	/**
	 * Sends command for cancel current operation to service
	 * 
	 */
	protected void cancelCurrentOperation() {
		try {
			mConnection.getService().cancelFinancialTransaction();
			transactionStepView.setText(com.handpoint.headstart.R.string.cancelling);
			animateUserCancel();
		} catch (HeadstartOperationException e) {
			logger.log(Level.SEVERE, "Error canceling Financial Transaction", e);
		}
	}
	
	/**
	 * Handles pressing on "Done" button. 
	 */
	protected void doneCurrentOperation() {
		finish();
	}
	
	int previousState = 0;
	boolean inTransaction = false;
	
	public void onDeviceState(DeviceState deviceState) {
		if (previousState != deviceState.getState()) {
			previousState = deviceState.getState();			
			switch (deviceState.getState()) {
			case DeviceState.WAITING_CARD:
				animateInsertCard();
				break;
			case DeviceState.PIN_INPUT:
			case DeviceState.AMOUNT_VALIDATION:
				animateEnterPin();
				break;
			case DeviceState.WAITING_CARD_REMOVAL:
				animateRemoveCard();
				break;
			case DeviceState.HOST_CONNECTING:
			case DeviceState.HOST_SENDING_REQUEST:
			case DeviceState.HOST_RECEIVING_RESPONSE:
				if (!inTransaction) {
					animateTransaction();				
					inTransaction = true;
				}
				break;
			case DeviceState.HOST_DISCONNECTING:
				animateFinishingTransaction();
				break;
			}
			mButton.setEnabled(deviceState.isCancelAllowed());
			transactionStepView.setText(deviceState.getStatusMessage());				
		}
	}

	public void onTransactionCompleted(FinancialTransactionResult result) {
		logger.log(Level.FINE, "Financial Transaction completed:" + result);
		if (result.transactionStatus == TransactionStatus.EFT_TRANSACTION_NOT_PROCESSED)	{
			animateErrorOrCancel();
			mButton.setEnabled(true);
			mButton.setTag(BUTTON_TAG_DONE);
	        mButton.setText(R.string.done);
			transactionStepView.setText(com.handpoint.headstart.R.string.error_common);					
			return;
		}
		finish();
	}


	public void onTransactionError(String error) {
		logger.log(Level.FINE, "Error occured during Financial Transaction: " + error);
		animateErrorOrCancel();
		mButton.setEnabled(true);
		mButton.setTag(BUTTON_TAG_DONE);
        mButton.setText(R.string.done);
		transactionStepView.setText(com.handpoint.headstart.R.string.error_common);		
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
	    Dialog dialog;
	    switch(id) {
	    case DIALOG_TIMEOUT_ID:
	    	dialog = createTimeoutDialog();
	        break;
	    default:
	        dialog = null;
	    }
	    return dialog;
	}
	
	private Dialog createTimeoutDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.th_message)
		       .setCancelable(false)
		       .setPositiveButton(R.string.th_positive_button_label, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   mConnection.getService().timeoutHandleResult(true);
		           }
		       })
		       .setNegativeButton(R.string.th_negative_button_label, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   mConnection.getService().timeoutHandleResult(false);
		           }
		       });
		return builder.create();
	}
	
	public void handleTimeout() {
		logger.log(Level.FINE, "Timeout is occured");
		showDialog(DIALOG_TIMEOUT_ID);				
	}
	
	public void verifySignature(String merchantReceiptText) {
		Intent intent = new Intent(HeadstartService.ACTION_VERIFY_SIGNATURE);
		intent.putExtra(HeadstartService.EXTRA_MERCHANT_RECEIPT_TEXT, merchantReceiptText);
		startActivity(intent);
	}
	private void registerBroadcastReceivers() {
		IntentFilter btServiceFilter = new IntentFilter();
        btServiceFilter.addAction(BROADCAST_TRANSACTION_STATE_CHANGE);
        btServiceFilter.addAction(BROADCAST_FINANCIAL_TRANSACTION_FINISHED);
        btServiceFilter.addAction(BROADCAST_ERROR);
        btServiceFilter.addAction(BROADCAST_OPERATION_CANCELL);
        btServiceFilter.addAction(ACTION_VERIFY_SIGNATURE);
        btServiceFilter.addAction(ACTION_HANDLE_TIMEOUT);
        
        registerReceiver(mHdstServiceReceiver, btServiceFilter);
	}
	
	/**
	 * Broadcast receiver for handling events from service 
	 */
	private BroadcastReceiver mHdstServiceReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (BROADCAST_TRANSACTION_STATE_CHANGE.equals(intent.getAction())) {
				ParcelDeviceState parcel = intent.getParcelableExtra(EXTRA_TRANSACTION_STATE);
				if (null != parcel) {
					onDeviceState(parcel.getResult());
				}
			} else if (BROADCAST_FINANCIAL_TRANSACTION_FINISHED.equals(intent.getAction())) {
				ParcelFinancialTransactionResult parcel = intent.getParcelableExtra(EXTRA_FINANCIAL_RESULT);
				if (null != parcel) {
					onTransactionCompleted(parcel.getResult());
				}
			} else if (ACTION_VERIFY_SIGNATURE.equals(intent.getAction())) {
				String merchantReceiptText = intent.getStringExtra(EXTRA_MERCHANT_RECEIPT_TEXT);
				verifySignature(merchantReceiptText);
			} else if (ACTION_HANDLE_TIMEOUT.equals(intent.getAction())) {
				handleTimeout();
			} else if (BROADCAST_ERROR.equals(intent.getAction())) {
				String errorMessage = intent.getStringExtra(EXTRA_ERROR_DESCRIPTION);
				onTransactionError(errorMessage);
				mErrorMessage = errorMessage;
				mConnection.getService().setLastTransactionHandled();
			} else if (BROADCAST_OPERATION_CANCELL.equals(intent.getAction())) {
				doneCurrentOperation();
			}
		}
		
	};	
	
	private abstract class AnimationAdaptor implements AnimationListener {

		public void onAnimationStart(Animation animation) {
			//
		}
		
		public void onAnimationRepeat(Animation animation) {
			//
		}		
	}
	
	/**
	 * Animation of card inserting
	 */
	void animateInsertCard() {
		logger.log(Level.FINEST, "Start insert card animation");
		try {
			
			clearAnimationView();
			
			final ImageView card = createImage();
			card.setImageResource(R.drawable.hand_with_card);
			card.setPadding(10, 0, 0, 0);
			int cardHeight = card.getDrawable().getMinimumHeight();
			frameLayout.addView(card);
			
			final ImageView cardreader = createImage();
			cardreader.setImageResource(R.drawable.cardreader);
			cardreader.setPadding(0, 0, 0, cardHeight * 2);
			frameLayout.addView(cardreader);
			
			Animation cardInsertAnimation = AnimationUtils.loadAnimation(OperationAnimationActivity.this, R.anim.card_insert);
			card.startAnimation(cardInsertAnimation);
			
		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Error is occured during animation: " + ex.getMessage(), ex);
		}
	}

	ImageView createImage() {
		ImageView result = new ImageView(this);
		LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.gravity = Gravity.CENTER;
		result.setLayoutParams(lp);
		return result;
	}

	/**
	 * Animation of card removing
	 */
	void animateRemoveCard() {
		logger.log(Level.FINEST, "Start remove card animation");
		try {
			
			clearAnimationView();
			
			final ImageView card = createImage();
			card.setImageResource(R.drawable.hand_with_card);
			card.setPadding(10, 0, 0, 0);
			int cardHeight = card.getDrawable().getMinimumHeight();
			frameLayout.addView(card);
			
			final ImageView cardreader = createImage();
			cardreader.setImageResource(R.drawable.cardreader);
			cardreader.setPadding(0, 0, 0, cardHeight * 2);
			frameLayout.addView(cardreader);
			
			Animation cardRemoveAnimation = AnimationUtils.loadAnimation(OperationAnimationActivity.this, R.anim.card_remove);
			card.startAnimation(cardRemoveAnimation);
			
		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Error is occured during animation: " + ex.getMessage(), ex);
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

	/**
	 * Animation of pin entering
	 */
	
	void animateEnterPin() {
		logger.log(Level.FINEST, "Start enter pin animation");
		try {
			
			clearAnimationView();
			
			LinearLayout v = (LinearLayout) mInflater.inflate(R.layout.pin_enter_animation, null);
			v.setGravity(Gravity.CENTER);
			frameLayout.addView(v);
			final View item1 = v.findViewById(R.id.item1);
			final View item2 = v.findViewById(R.id.item2);
			final View item3 = v.findViewById(R.id.item3);
			final View item4 = v.findViewById(R.id.item4);
			final Animation animationFadeIn1 = AnimationUtils.loadAnimation(this, R.anim.fadein);
			animationFadeIn1.setStartOffset(500);
			animationFadeIn1.setAnimationListener(new AnimationAdaptor() {
				@Override
				public void onAnimationStart(Animation animation) {
					item1.setVisibility(View.INVISIBLE);
					item2.setVisibility(View.INVISIBLE);
					item3.setVisibility(View.INVISIBLE);
					item4.setVisibility(View.INVISIBLE);
				}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					item1.setVisibility(View.VISIBLE);
				}
			});
			final Animation animationFadeIn2 = AnimationUtils.loadAnimation(this, R.anim.fadein);
			animationFadeIn2.setStartOffset(1500);
			animationFadeIn2.setAnimationListener(new AnimationAdaptor() {
				
				@Override
				public void onAnimationEnd(Animation animation) {
					item2.setVisibility(View.VISIBLE);
				}
			});
			final Animation animationFadeIn3 = AnimationUtils.loadAnimation(this, R.anim.fadein);
			animationFadeIn3.setStartOffset(2500);
			animationFadeIn3.setAnimationListener(new AnimationAdaptor() {
				
				@Override
				public void onAnimationEnd(Animation animation) {
					item3.setVisibility(View.VISIBLE);
				}
			});
			final Animation animationFadeIn4 = AnimationUtils.loadAnimation(this, R.anim.fadein);
			animationFadeIn4.setStartOffset(3500);
			animationFadeIn4.setAnimationListener(new AnimationAdaptor() {
				
				@Override
				public void onAnimationEnd(Animation animation) {
					item4.setVisibility(View.VISIBLE);
					item1.startAnimation(animationFadeIn1);
					item2.startAnimation(animationFadeIn2);
					item3.startAnimation(animationFadeIn3);
					item4.startAnimation(animationFadeIn4);
				}
			});

			item1.startAnimation(animationFadeIn1);
			item2.startAnimation(animationFadeIn2);
			item3.startAnimation(animationFadeIn3);
			item4.startAnimation(animationFadeIn4);
			
		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Error is occured during animation: " + ex.getMessage(), ex);
		}
	}

	LinearLayout mTransactionView;
	/**
	 * Animation for transaction
	 */
	void animateTransaction() {
		logger.log(Level.FINEST, "Start begin transaction animation");
		try {
			
			clearAnimationView();
			
			mTransactionView = (LinearLayout) mInflater.inflate(R.layout.host_communication_animation, null);
			mTransactionView.setGravity(Gravity.CENTER);
			frameLayout.addView(mTransactionView);
			final View arrows = mTransactionView.findViewById(R.id.item2);
			
			Animation rotateAnimation = AnimationUtils.loadAnimation(OperationAnimationActivity.this, R.anim.rotate);
			arrows.startAnimation(rotateAnimation);
			

		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Error is occured during animation: " + ex.getMessage(), ex);
		}
	}
	
	void animateFinishingTransaction() {
		if (null != mTransactionView) {
			final View arrows = mTransactionView.findViewById(R.id.item2);
			arrows.clearAnimation();
			final View face = mTransactionView.findViewById(R.id.item3);
			final Animation animationFadeIn = AnimationUtils.loadAnimation(this, R.anim.fadein_slow);
			animationFadeIn.setAnimationListener(new AnimationAdaptor() {
				
				@Override
				public void onAnimationStart(Animation animation) {
					face.setVisibility(View.VISIBLE);
					arrows.setVisibility(View.GONE);
				}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					face.setVisibility(View.VISIBLE);
				}
			});
			face.startAnimation(animationFadeIn);
		}
	}
	/**
	 * Animation of error/cancel
	 */
	void animateErrorOrCancel() {
		logger.log(Level.FINEST, "Start error/cancel animation");
		try {
			
			clearAnimationView();
			
			final ImageView errorImage = createImage();
			errorImage.setImageResource(R.drawable.error_animation);
			frameLayout.addView(errorImage);
			
			AnimationDrawable errorAnimation = (AnimationDrawable) errorImage.getDrawable();
			errorAnimation.start();

		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Error is occured during animation: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Animation of user cancel action
	 */
	void animateUserCancel() {
		logger.log(Level.FINEST, "Start user cancel animation");
		try {
			clearAnimationView();
			
			final ImageView cancelCenterImage = createImage();
			cancelCenterImage.setImageResource(R.drawable.cancel_center);
			
			final ImageView cancelArrowImage = createImage();
			cancelArrowImage.setImageResource(R.drawable.cancel_arrow);
			
			frameLayout.addView(cancelCenterImage);
			frameLayout.addView(cancelArrowImage);
			
			Animation cancelAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);
			
			cancelArrowImage.startAnimation(cancelAnimation);
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error is occured during animation: " + e.getMessage(), e);
		}
		
	}

}