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

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.handpoint.headstart.android.HeadstartService;
import com.handpoint.headstart.api.FinancialTransactionResult;
import com.handpoint.headstart.client.R;
import com.handpoint.headstart.client.android.Application;
import com.handpoint.headstart.client.data.DaoHelper;
import com.handpoint.headstart.client.data.SenderHelper;
import com.handpoint.headstart.client.data.SenderHelper.MerchantEmailServerSettings;
import com.handpoint.headstart.client.data.models.Basket;
import com.handpoint.headstart.client.data.models.BasketItem;
import com.handpoint.headstart.client.data.models.FinancialTransaction;
import com.handpoint.headstart.eft.TransactionStatus;
import com.handpoint.util.logging.ApplicationLogger;
import com.handpoint.util.logging.Level;
import com.handpoint.util.logging.Logger;
import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import org.jsoup.Jsoup;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ReceiptActivity extends HeadstartActivity {
	protected static final String TAG = ReceiptActivity.class.getSimpleName();
	private static Logger logger = ApplicationLogger.getLogger(TAG);	

    public static final String EXTRA_TRANSACTION_ID = "com.handpoint.headstart.hal.extra_transaction_id";
    public static final String EXTRA_SEND_MERCHANT_RECEIPT = "com.handpoint.headstart.hal.extra_send_merchant_receipt";

	protected static final int MENU_ITEM_SEND_EMAIL = 1;
	protected static final int MENU_ITEM_SEND_SMS = 2;

	enum EmailTypes {MERCHANT, CUSTOMER};
	
	private FinancialTransaction mResult;
	BasketItem mBasketItem;
	private EditText mEmailView;
	private EditText mSmsView;
	private Button mRevertButton;
	private Button mDetailsButton;

    ProgressDialog mPdfDlg;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.receipt);
		
		long transactionId = getIntent().getLongExtra(EXTRA_TRANSACTION_ID, -1);
		if (transactionId == -1) {
			finish();
			return;
		}
		
		DaoHelper daoHelper = new DaoHelper(this);
		daoHelper.open(false);
		mResult = daoHelper.getFinancialTransaction(transactionId);
		Basket basket = daoHelper.getBasketByTransaction(transactionId);
		if (null != basket) {
			mBasketItem = daoHelper.getBasketItem(basket.getId());
		}
		daoHelper.close();
		Button buttonDone = (Button) findViewById(R.id.bottom_button);
		buttonDone.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				finish();
			}
		});

		mRevertButton = (Button) findViewById(R.id.revert_button);
		if ((mResult.getType() == FinancialTransactionResult.FT_TYPE_SALE || mResult.getType() == FinancialTransactionResult.FT_TYPE_REFUND)
				&& null != mResult.getTransactionId()
				&& mResult.getTransactionId().length() > 0
				&& mResult.getTransactionStatus() == TransactionStatus.EFT_TRANSACTION_APPROVED
				&& 0 == mResult.getVoidedId()) 
		{
			mRevertButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent startVoidIntent = new Intent(HeadstartService.ACTION_MAIN);
					startVoidIntent.putExtra(HeadstartService.EXTRA_OPERATION_TYPE,
							mResult.getType() == FinancialTransactionResult.FT_TYPE_SALE ? 
									FinancialTransactionResult.FT_TYPE_SALE_VOID : FinancialTransactionResult.FT_TYPE_REFUND_VOID);
					startVoidIntent.putExtra(HeadstartService.EXTRA_PAYMENT_VALUE, mResult.getAuthorizedAmount());
					startVoidIntent.putExtra(HeadstartService.EXTRA_PAYMENT_CURRENCY, mResult.getCurrency());
					startVoidIntent.putExtra(HeadstartService.EXTRA_PAYMENT_TRANSACTION_ID, mResult.getTransactionId());
					startVoidIntent.putExtra("com.handpoint.headstart.client.VOIDED_ID", mResult.getId());
					startActivity(startVoidIntent);
					finish();
				}
			});
		} else {
			mRevertButton.setVisibility(View.GONE);
		}
		
		mDetailsButton = (Button) findViewById(R.id.details_button);
		if (!TextUtils.isEmpty(mResult.getMerchantReceipt()) || !TextUtils.isEmpty(mResult.getCustomerReceipt())) {
			mDetailsButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(ReceiptActivity.this, RawReceiptActivity.class);
					intent.putExtra("merchantReceipt", mResult.getMerchantReceipt());
					intent.putExtra("customerReceipt", mResult.getCustomerReceipt());
					intent.putExtra("signatureVerificationText", mResult.getSignatureVerificationText());
					intent.putExtra("signatureImage", mResult.getSignaturePath());
					startActivity(intent);					
				}
			});
		} else {
			mDetailsButton.setVisibility(View.GONE);
		}
		
		initReceiptView();

		final ImageView sendEmailView = (ImageView) findViewById(R.id.email_send_image);
		mEmailView = (EditText) findViewById(R.id.email_address);

		final ImageView sendSmsView = (ImageView) findViewById(R.id.sms_send_image);
		mSmsView = (EditText) findViewById(R.id.sms_address);		

		sendEmailView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				hideSoftKeyboard(mEmailView);
                mPdfDlg = new ProgressDialog(v.getContext());
                mPdfDlg.setMessage(getString(R.string.pdf_dlg_msg));
                mPdfDlg.setTitle(getString(R.string.pdf_dlg_title));
                mPdfDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mPdfDlg.setCancelable(true);
                mPdfDlg.show();

                new Thread(new Runnable() {
                    public void run()
                    {
                        ArrayList<File> attachments = null;
                        try {
                            Thread.sleep(1000);
                            attachments = getCustomerEmailAttachments();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (attachments != null) {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mPdfDlg.dismiss();

                            SenderHelper.MerchantEmailServerSettings serverSettings = new SenderHelper.MerchantEmailServerSettings(ReceiptActivity.this);
                            SendAsyncTask sendTask = new SendAsyncTask();
                            sendTask.execute(new EmailDataHolder(
                                    ReceiptActivity.this,
                                    serverSettings,
                                    mEmailView.getText().toString(),
                                    new SpannableString(""),
                                    attachments,
                                    EmailTypes.CUSTOMER));
                        }
                    }
                }).start();
			}
		});

		sendSmsView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				hideSoftKeyboard(mSmsView);
				SenderHelper.sendReceiptSms(ReceiptActivity.this, mSmsView.getText().toString(), mResult);
				//mark customer receipt as "copy"
				markCustomerReceiptAsCopied(ReceiptActivity.this, mResult);
			}
		});

		if (getIntent().getBooleanExtra(EXTRA_SEND_MERCHANT_RECEIPT, false) && savedInstanceState == null) {
			sendMerchantReceipt();
	    }

    	// repeat image bug fix
//		View background = findViewById(R.id.rootLayout);
//		((BitmapDrawable)background.getBackground()).setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
		
//		getSupportActionBar().setTitle(R.string.abs_title);
//		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

	}

	void markCustomerReceiptAsCopied(Context ctx, FinancialTransaction result) {
		if (!result.isCustomerReceiptCopy()) {
			DaoHelper dh = new DaoHelper(ctx);
			dh.open(true);
			result.setCustomerReceiptCopy(true);
			dh.updateFinancialTransaction(result);
			dh.close();
		}				
	}
	
	void markMerchantReceiptAsCopied(Context ctx, FinancialTransaction result) {
		if (!result.isMerchantReceiptCopy()) {
			DaoHelper dh = new DaoHelper(ctx);
			dh.open(true);
			result.setMerchantReceiptCopy(true);
			dh.updateFinancialTransaction(result);
			dh.close();
		}				
	}
	
	void hideSoftKeyboard(View v) {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getWindowToken(), 0);							
	}
	
	private void initReceiptView() {
		Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/roboto-regular.ttf");

		TextView merchantName = (TextView) findViewById(R.id.merchant_name);
		merchantName.setTypeface(tf, Typeface.BOLD);
		merchantName.setText(getMerchantName());

		TextView transactionDateView = (TextView) findViewById(R.id.date);
		java.text.DateFormat df = DateFormat.getDateFormat(this);
		transactionDateView.setTypeface(tf);
		transactionDateView.setText(" " + df.format(mResult.getDateTime()));
		
		TextView transactionResult = (TextView) findViewById(R.id.transaction_result);
		transactionResult.setTypeface(tf, Typeface.BOLD);
		transactionResult.setText(mResult.getFinancialStatus());

		TextView messageView = (TextView) findViewById(R.id.message);
		messageView.setTypeface(tf);
		String message = ((Application)getApplication()).formatErrorMessage(mResult.getStatusMessage(), mResult.getErrorMessage());
		messageView.setText(message);
		
		ImageView cardSchemeLogo = (ImageView) findViewById(R.id.card_scheme_logo);
		cardSchemeLogo.setImageDrawable(getCardSchemeLogo());

		TextView amountView = (TextView) findViewById(R.id.amount_text);
		amountView.setTypeface(tf);
		amountView.setText(getTotalAmount(mResult.getAuthorizedAmount(), mResult.getCurrency()));
		
		TextView descriptionView = (TextView) findViewById(R.id.item_description_text);
		descriptionView.setTypeface(tf);
		descriptionView.setText(getDescriptionText());
		
		ImageView productImageView = (ImageView) findViewById(R.id.picture);
		productImageView.setImageBitmap(getImageBitmap());
	}

    ArrayList<File> getCustomerEmailAttachments(){
        ArrayList<File> attachments = new ArrayList<File>();
        //get receipt if exists
        File receipt = getCustomerEmailAttachmentFile();
        if(null != receipt)
            attachments.add(receipt);

        //get image if exists
        if(null != mBasketItem && null != mBasketItem.getFullSizePhotoPath()){
            Uri uri =  Uri.parse(mBasketItem.getFullSizePhotoPath());
            File photo = new File(uri.getPath());
            attachments.add(photo);
        }
        return attachments;
    }

    File getCustomerEmailAttachmentFile(){
        String template = getResources().getString(R.string.email_template);
        String html = String.format(
                template,
                getDescriptionText(),
                mResult.getCustomerReceipt());

        return createPdfFromReceipt(html, "customer_");
    }

    private File createPdfFromReceipt(String html, String fileNamePrefix){
        //We cannot trust that the html is well formed
        org.jsoup.nodes.Document doc = Jsoup.parse(html);
        html = doc.toString();

        File file=null;
        try{
            Document document = new Document(PageSize.LETTER);
            file = getOutputMediaFile(fileNamePrefix);
            PdfWriter instance = PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();
            InputStream is = new ByteArrayInputStream(html.getBytes());
            XMLWorkerHelper worker = XMLWorkerHelper.getInstance();
            worker.parseXHtml(instance, document, is);
            document.close();

        }catch (Exception e){
            logger.log(Level.SEVERE, TAG+" :Failed to create .pdf document from receipt.",e);
            Toast.makeText(ReceiptActivity.this,getString(R.string.create_pdf),Toast.LENGTH_LONG).show();
        }
        return file;
    }

    private File getOutputMediaFile(String prefix) {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return null;
        }
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+ File.separator + "HandpointProducts");
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()) {
                logger.log(Level.SEVERE, TAG+ " :Failed to create directory for storing receipt");
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir, prefix + "receipt_"+ timeStamp + ".pdf");
        return mediaFile;
    }

    ArrayList<File> getMerchantEmailAttachments(){
        ArrayList<File> attachments = new ArrayList<File>();
        boolean isSignature = false;
        //get signature if any
        if(null != mResult && null != mResult.getSignaturePath()){
            String path = getFilePathFromContentUrl(mResult.getSignaturePath());
            Uri uri =  Uri.parse(path);
            File signature = new File(uri.getPath());
            attachments.add(signature);
            isSignature = true;
        }

        //get receipt if exists
        File receipt = getMerchantEmailAttachmentFile(isSignature);
        if(null != receipt)
            attachments.add(receipt);

        return attachments;
    }

    File getMerchantEmailAttachmentFile(boolean isSignature){
        String receipt;
        if(isSignature)
            receipt = mResult.getSignatureVerificationText();
        else
            receipt = mResult.getMerchantReceipt();

        String template = getResources().getString(R.string.email_template);
        String html = String.format(
                template,
                getDescriptionText(),
                receipt);

        return createPdfFromReceipt(html, "merchant_");
    }

	Spanned getCustomerEmailText() {
		String template = getResources().getString(R.string.email_template);
		String text = String.format(
				template, 
				getDescriptionText(),
				getTotalAmount(mResult.getAuthorizedAmount(), mResult.getCurrency()),
				mResult.getCustomerReceipt());
		return Html.fromHtml(text);

	}

	Spanned getMerchantEmailText() {
		String template = getResources().getString(R.string.email_template);
		String text = String.format(
				template, 
				getDescriptionText(),						
				getTotalAmount(mResult.getAuthorizedAmount(), mResult.getCurrency()),
				mResult.getMerchantReceipt());
        return Html.fromHtml(text);
	}

	private String getDescriptionText() {
		String description = getResources().getString(R.string.no_description);
		if (null != mBasketItem && !TextUtils.isEmpty(mBasketItem.getDescription())) {
			description = mBasketItem.getDescription();
		}
		return description;
	}
	
	private Bitmap getImageBitmap() {
		if (null != mBasketItem && null != mBasketItem.getThumbnail()) {
			int len = mBasketItem.getThumbnail().length;
			return BitmapFactory.decodeByteArray(mBasketItem.getThumbnail(), 0, len);
		}
		return null;
	}

	private Drawable getCardSchemeLogo() {
		return ((Application) getApplication()).getCardSchemeLogo(mResult.getCardSchemeName());
	}

	private String getMerchantName() {
		SharedPreferences prefrences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		return prefrences.getString(HeadstartService.PREFERENCE_MERCHANT_NAME, "");
	}

	private String getMerchantEmailAddress() {
		SharedPreferences prefrences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		return prefrences.getString(HeadstartService.PREFERENCE_MERCHANT_EMAIL_ADDRESS, null);
	}

	private String getTotalAmount(Integer totalAmount, String currencyCode) {
		return ((Application) getApplication()).getFormattedAmount(totalAmount, currencyCode);
	}

	private void sendMerchantReceipt() {
		SenderHelper.MerchantEmailServerSettings serverSettings = new SenderHelper.MerchantEmailServerSettings(this);
		SendAsyncTask sendTask = new SendAsyncTask();
		sendTask.execute(new EmailDataHolder(
				this,
				serverSettings,
				getMerchantEmailAddress(),
                new SpannableString(""),
				getMerchantEmailAttachments(),
				EmailTypes.MERCHANT));
	}

	private String getFilePathFromContentUrl(String url) {
		String[] proj = {MediaStore.Images.Media.DATA};
	    CursorLoader loader = new CursorLoader(this, Uri.parse(url), proj, null, null, null);
	    Cursor cursor = loader.loadInBackground();
	    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	    cursor.moveToFirst();
	    return cursor.getString(column_index);
	}
	
	private class EmailDataHolder {
		Context ctx;
		MerchantEmailServerSettings serverSettings;
		String emailAddress;
		Spanned emailText;
        ArrayList<File> attachments;
		EmailTypes type;

		public EmailDataHolder(Context ctx, MerchantEmailServerSettings serverSettings,
				String emailAddress, Spanned emailText, ArrayList<File> attachments, EmailTypes type) {
			this.ctx = ctx;
			this.serverSettings = serverSettings;
			this.emailAddress = emailAddress;
			this.emailText = emailText;
			this.attachments = attachments;
			this.type = type;
		}
	}
	
	private class SendAsyncTask extends AsyncTask<EmailDataHolder, Void, Void> {

		@Override
		protected Void doInBackground(EmailDataHolder... params) {
			try {
				
				switch (params[0].type) {
				case MERCHANT:
					boolean resultM = 
						SenderHelper.sendMerchantReceiptEmail(
							params[0].ctx,
							params[0].serverSettings,
							params[0].emailAddress,
							params[0].emailText,
							params[0].attachments);
					//mark customer receipt as "copy"
					if (resultM) {
						markMerchantReceiptAsCopied(params[0].ctx, mResult);
					}
					break;
				case CUSTOMER:
					boolean resultC = 
						SenderHelper.sendCustomerReceiptEmail(
							params[0].ctx,
							params[0].serverSettings,
							params[0].emailAddress,
							params[0].emailText,
							params[0].attachments);
					//mark customer receipt as "copy"
					if (resultC) {
						markCustomerReceiptAsCopied(params[0].ctx, mResult);
					}
					break;
				}
			} catch (AddressException e) {
				logger.log(Level.SEVERE, "Email sending failed",e);
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						Toast.makeText(ReceiptActivity.this,
								getString(R.string.error_send_email_failed) + ". " + getString(R.string.no_merchant_email_error_prompt),
								Toast.LENGTH_LONG).show();						
					}
				});
			} catch (MessagingException e) {
				logger.log(Level.SEVERE, "Email sending failed",e);
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						Toast.makeText(ReceiptActivity.this,
								getString(R.string.error_send_email_failed)  + ". " + getString(R.string.no_server_settings_error_prompt),
								Toast.LENGTH_LONG) .show();
					}
				});
			}
			return null;
		}
		
	}
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		menu.add(Menu.NONE, MENU_ITEM_SEND_EMAIL, Menu.NONE, R.string.send_email)
//			.setIcon(R.drawable.menu_ic_email)
//			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
//		menu.add(Menu.NONE, MENU_ITEM_SEND_SMS, Menu.NONE, R.string.send_sms)
//			.setIcon(R.drawable.menu_ic_sms)
//			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
//		return true;
//	}
//	
//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//    	if (null == mResult) {
//    		MenuItem item = menu.findItem(MENU_ITEM_SEND_EMAIL);
//    		item.setEnabled(false);
//    		item = menu.findItem(MENU_ITEM_SEND_SMS);
//    		item.setEnabled(false);
//    	}    	
//    	return true;
//    }
//    
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		switch (item.getItemId()) {
//		case MENU_ITEM_SEND_EMAIL:
//			SenderHelper.sendReceiptEmail(this, "", null, mResult.customerReceipt);
//			break;
//		case MENU_ITEM_SEND_SMS:
//			SenderHelper.sendReceiptSms(this, mResult);
//			break;
//		case android.R.id.home:
//			finish();
//			break;
//		}
//		return super.onOptionsItemSelected(item);
//	}
	
}