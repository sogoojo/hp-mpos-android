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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.handpoint.headstart.android.HeadstartService;
import com.handpoint.headstart.android.models.CurrencyModel;
import com.handpoint.headstart.api.FinancialTransactionResult;
import com.handpoint.headstart.client.R;
import com.handpoint.headstart.client.android.Application;
import com.handpoint.headstart.client.data.DaoHelper;
import com.handpoint.headstart.client.data.models.Basket;
import com.handpoint.headstart.client.data.models.BasketItem;
import com.handpoint.util.logging.ApplicationLogger;
import com.handpoint.util.logging.Level;
import com.handpoint.util.logging.Logger;

/**
 * 
 *
 */
public class NumpadFragment extends SherlockFragment  implements OnClickListener,  OnLongClickListener {

	private static final String TAG = NumpadFragment.class.getSimpleName();
	private static Logger logger = ApplicationLogger.getLogger(TAG);
	
	private static final BigDecimal TEN = new BigDecimal(10.0);
	private static final BigDecimal HUNDRED = new BigDecimal(100.0);
	private static final int THUMBNAIL_SIZE = 64;
	
	private static final String PAYMENT_DEFAULT_OPERATION = "payment_default_operation";
	private static final int TAKE_PHOTO_ACTION = 1;
	private static final int PREVIEW_ACTION = 2;
	
	private static final int LCD_SYMBOLS_MAX = 8;
	
    private SharedPreferences mPreferences;
    private OnPaymentListener mListener;
    
	private ImageButton buttonDel;
	private Button mNumpadButtonPay;
	private TextView mNumpadLcdView;
	// description panel items
	private ImageButton mCameraButton;
	private EditText mDescriptionView;

	DaoHelper mDaoHelper;
	Basket mBasket;
	BasketItem mBasketItem;
	CurrencyModel mCurrency;
	
	boolean mExternalStorageAvailable = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mDaoHelper = new DaoHelper(getActivity());
		mDaoHelper.open(true);
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    mExternalStorageAvailable = true;
		}
		initBasket();		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.numpad, container, false);
		view.setTag("numpad_view");
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		initNumpadView();
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnPaymentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPaymentListener");
        }
    }
	
	@Override
	public void onResume() {
		super.onResume();
		refreshCurrency();
		initBasket();
		initBottomButton();
		refreshDescriptionPanelStatus();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		persistBasket();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mDaoHelper.close();
	}
	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}
	
	private void refreshCurrency() {
		String defaultCurrency = mPreferences.getString(HeadstartService.PREFERENCE_DEFAULT_CURRENCY, "-");
		mCurrency = ((Application)getActivity().getApplication()).getCurrency(defaultCurrency);
	}
	
	private void initNumpadView() {
		mNumpadLcdView = (TextView) getView().findViewById(R.id.lcdView);

		Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "fonts/roboto-regular.ttf");
		mNumpadLcdView.setTypeface(tf);

		buttonDel = (ImageButton) getView().findViewById(R.id.buttonDel);
		buttonDel.setOnClickListener(this);
		buttonDel.setOnLongClickListener(this);

		Button button00 = (Button) getView().findViewById(R.id.button00);
		button00.setOnClickListener(this);

		initNumButtons();

		mNumpadButtonPay = (Button) getView().findViewById(R.id.pay_button);	
		
		mCameraButton = (ImageButton) getView().findViewById(R.id.camera_button);
		mCameraButton.setOnClickListener(getCameraButtonOnClickListener());

		mDescriptionView = (EditText) getView().findViewById(R.id.description_input);
	}
	
	private void initBasket() {
		mBasket = mDaoHelper.getCurrentBasket();
		if (null == mBasket) {
			mBasket = new Basket();
		}
		mBasketItem = mDaoHelper.getBasketItem(mBasket.getId());
		if (null == mBasketItem) {
			mBasketItem = new BasketItem();
		}
		
	}
	
	private void persistBasket() {
		if (mBasket.getId() > 0) {
			mDaoHelper.updateBasket(mBasket);
		} else {
			mBasket.setId(mDaoHelper.insertBasket(mBasket));
		}
		if (mBasket.getId() > 0) {
			mBasketItem.setBasketId(mBasket.getId());
		}
		mBasketItem.setDescription(mDescriptionView.getEditableText().toString());
		if (mBasketItem.getId() > 0) {
			mDaoHelper.updateBasketItem(mBasketItem);
		} else {
			mDaoHelper.insertBasketItem(mBasketItem);
		}
	}
	
	public void initBottomButton() {
		mNumpadButtonPay.setOnClickListener(this);
    	switch (getPrimaryFinancialTransactionType()) {
		case FinancialTransactionResult.FT_TYPE_REFUND:
			mNumpadButtonPay.setBackgroundResource(R.drawable.button_red_selector);
			mNumpadButtonPay.setText(R.string.refund);
			break;
		default:
			mNumpadButtonPay.setBackgroundResource(R.drawable.button_orange_selector);
			mNumpadButtonPay.setText(R.string.pay);
			break;
		}
		mNumpadButtonPay.setPadding(0, 0, 0, 0);
	}
	
	private void initNumButtons() {
		initButton(R.id.button0, 0);
		initButton(R.id.button1, 1);
		initButton(R.id.button2, 2);
		initButton(R.id.button3, 3);
		initButton(R.id.button4, 4);
		initButton(R.id.button5, 5);
		initButton(R.id.button6, 6);
		initButton(R.id.button7, 7);
		initButton(R.id.button8, 8);
		initButton(R.id.button9, 9);
	}

	private void initButton(int id, Integer tag) {
		Button button = (Button) getView().findViewById(id);
		button.setTag(tag);
		button.setOnClickListener(this);

		Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "fonts/roboto-regular.ttf");
		button.setTypeface(tf);
	}

	public void setAmount(BigDecimal amount) {
		mBasket.setTotalAmount(getAmountAsInt(amount));
		this.mNumpadLcdView.setText(formatAmount(amount));
	}
	
	private String formatAmount(BigDecimal amount) {
		return ((Application) getActivity().getApplication()).getDecimalFormat(mCurrency.getCode(), false).format(amount);
	}
	
	public void onClick(View v) {
		if (v.getTag() != null) {
			String value = v.getTag().toString();
			Integer intValue = Integer.valueOf(value);
			numkeyPressed(intValue);
		} else if (v.getId() == R.id.buttonDel) {
			setAmount(getAmountAsBigDecimal(mBasket.getTotalAmount()).divide(TEN, mCurrency.getDecimalPart(), RoundingMode.FLOOR));
		} else if (v.getId() == R.id.button00) {
			if (this.mNumpadLcdView.getText().length() < LCD_SYMBOLS_MAX-1) {
				setAmount(getAmountAsBigDecimal(mBasket.getTotalAmount()).multiply(HUNDRED));
			}
		} else if (v.getId() == R.id.pay_button) {
			mListener.onPaymentStarted(getPrimaryFinancialTransactionType(), mBasket.getTotalAmount(), mCurrency.getCode(), true);
		}
	}
	
	public boolean onLongClick(View v) {
		if (v.getId() == R.id.buttonDel) {
			setAmount(BigDecimal.ZERO);
			this.buttonDel.setPressed(false);
			return true;
		}
		return false;
	}
	
	private OnClickListener getCameraButtonOnClickListener() {
		return new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (null != mBasketItem.getFullSizePhotoPath()) {
					Intent intent = new Intent("com.handpoint.headstart.client.ACTION_PREVIEW_IMAGE");
					intent.setData(Uri.parse(mBasketItem.getFullSizePhotoPath()));
					startActivityForResult(intent, PREVIEW_ACTION);
				} else {
					takePhotoAction();
				}
			}
		};
	}

	void takePhotoAction() {
		if (isIntentAvailable(getActivity(), MediaStore.ACTION_IMAGE_CAPTURE)) {
			Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			Uri fileUri = null;
			//if already have path to full-size photo
			if (null != mBasketItem.getFullSizePhotoPath()) {
				//use it
				fileUri = Uri.parse(mBasketItem.getFullSizePhotoPath());
			} else {
				//create new path
				fileUri = getOutputMediaFileUri();
			}
			//if path was created successfully 
			if (null != fileUri) {
				//set output file for intent
				mBasketItem.setFullSizePhotoPath(fileUri.toString());
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
			} else {
				//clear path in model
				mBasketItem.setFullSizePhotoPath(null);
			}
		    startActivityForResult(takePictureIntent, TAKE_PHOTO_ACTION);					
		} else {
			Toast.makeText(getActivity(), R.string.no_photo_ability, Toast.LENGTH_LONG).show();
		}		
	}
	
	private boolean isIntentAvailable(Context context, String action) {
	    final PackageManager packageManager = context.getPackageManager();
	    final Intent intent = new Intent(action);
	    List<ResolveInfo> list =
	            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
	    return list.size() > 0;
	}
	
	private void numkeyPressed(Integer intValue) {
		if (mNumpadLcdView.getText().length() < LCD_SYMBOLS_MAX) {
			BigDecimal appendValue = new BigDecimal(intValue);
			appendValue = appendValue.divide(mCurrency.getFactor(), mCurrency.getDecimalPart(), RoundingMode.HALF_UP);
			setAmount(getAmountAsBigDecimal(mBasket.getTotalAmount()).multiply(TEN).add(appendValue));
		}
	}
	
	private int getPrimaryFinancialTransactionType() {		
		return mPreferences.getInt(PAYMENT_DEFAULT_OPERATION, FinancialTransactionResult.FT_TYPE_SALE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (TAKE_PHOTO_ACTION == requestCode) {
			if (Activity.RESULT_OK ==resultCode) {
				handleSmallCameraPhoto(data);
				persistBasket();
			} else {
				if (null == mBasketItem.getThumbnail()) {
					mBasketItem.setFullSizePhotoPath(null);
					persistBasket();
				}
			}
		} else if (PREVIEW_ACTION == requestCode) {
			if (resultCode == Activity.RESULT_FIRST_USER + 1) {
				//retake photo
				takePhotoAction();
			} else if (resultCode == Activity.RESULT_FIRST_USER + 2) {
				//remove photo
				mBasketItem.setThumbnail(null);
				((Application)getActivity().getApplication()).removePhoto(mBasketItem);
				mBasketItem.setFullSizePhotoPath(null);
			}
			persistBasket();			
		}
	}
		
	private void handleSmallCameraPhoto(Intent intent) {
		Bitmap imageBitmap = null;
	    if (null != intent) {
	    	// take thumbnail from intent data
		    Bundle extras = intent.getExtras();
	    	imageBitmap = (Bitmap) extras.get("data");
	    } else if (null != mBasketItem.getFullSizePhotoPath()) {
	    	// get thumbnail from full-size photo	    	
	    	imageBitmap = ((Application)getActivity().getApplication()).scaleImageToSize(Uri.parse(mBasketItem.getFullSizePhotoPath()), THUMBNAIL_SIZE, THUMBNAIL_SIZE);
	    }
	    if (null != imageBitmap) {
		    ByteArrayOutputStream stream = new ByteArrayOutputStream();
		    imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
		    mBasketItem.setThumbnail(stream.toByteArray());
	    } else {
	    	mBasketItem.setThumbnail(null);
	    }
	    if (null == mBasketItem.getThumbnail()) {
	    	mBasketItem.setFullSizePhotoPath(null);
	    }
	}

	void refreshDescriptionPanelStatus() {
		if (null == mBasketItem.getThumbnail()) {
			mCameraButton.setImageResource(R.drawable.icon_camera);
			mCameraButton.setBackgroundResource(R.drawable.icon_camera_selector);
			mCameraButton.setContentDescription(getResources().getText(R.string.take_photo));
		} else {
			int len = mBasketItem.getThumbnail().length;
			mCameraButton.setImageBitmap(BitmapFactory.decodeByteArray(mBasketItem.getThumbnail(), 0, len));
			mCameraButton.setBackgroundResource(R.drawable.icon_camera_for_photo);
			mCameraButton.setContentDescription(getResources().getText(R.string.product_photo));
		}
		setAmount(getAmountAsBigDecimal(mBasket.getTotalAmount()));
		mDescriptionView.setText(mBasketItem.getDescription());
	}

	private Uri getOutputMediaFileUri() {
		File f = getOutputMediaFile();
		if (null == f) {
			return null;
		}
	    return Uri.fromFile(f);
	}

	private File getOutputMediaFile() {
		if (!mExternalStorageAvailable) {
			return null;
		}
		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+ File.separator + "HandpointProducts");
		if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()) {
	        	logger.log(Level.SEVERE, "Failed to create directory for storing product images");
	            return null;
	        }
	    }
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File mediaFile = new File(mediaStorageDir, "IMG_"+ timeStamp + ".jpg");
		return mediaFile;
	}
	
	private BigDecimal getAmountAsBigDecimal(int amount) {
		return new BigDecimal(amount).divide(HUNDRED, mCurrency.getDecimalPart(), RoundingMode.FLOOR);
	}
	
	private int getAmountAsInt(BigDecimal amount) {
		return amount.multiply(HUNDRED).intValue();
	}
	
	//TODO: ???
//	@Override
//	public boolean dispatchKeyEvent(KeyEvent event) {
//		if (event.getAction() == KeyEvent.ACTION_UP) {
//			int keyCode = event.getKeyCode(); 
//			if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
//				numkeyPressed(keyCode - 7);
//				return true;
//			}
//		}
//		return super.dispatchKeyEvent(event);
//	}

}
