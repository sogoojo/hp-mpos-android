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
package com.handpoint.headstart.client.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;

import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.MailcapCommandMap;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.Toast;

import com.handpoint.headstart.client.R;
import com.handpoint.headstart.client.data.models.FinancialTransaction;
import com.handpoint.headstart.client.util.EmailValidator;
import com.handpoint.headstart.client.util.EmailValidator.ValidationException;
import com.handpoint.headstart.client.util.ReceiptTagHandler;
import com.handpoint.util.logging.ApplicationLogger;
import com.handpoint.util.logging.Level;
import com.handpoint.util.logging.Logger;

public class SenderHelper {
	
	private static final String TAG = SenderHelper.class.getSimpleName();
	private static Logger logger = ApplicationLogger.getLogger(TAG);
	
	private static final String EMAIL_CUSTOMER_RECEIPT_SUBJECT = "Customer receipt";
	private static final String EMAIL_MERCHANT_RECEIPT_SUBJECT = "Merchant receipt";

	public static final String USER = "merchant_email";
	public static final String PSWD = "merchant_email_pswd";
	public static final String HOST = "merchant_email_host";
	public static final String PORT = "merchant_email_port";
	public static final String PROTOCOL = "supported_protocol";

	public static final String PROTOCOL_TLS = "2";
	public static final String PROTOCOL_SSL = "1";

	public static final String DEFAULT_HOST = "smtp.gmail.com";
	public static final int DEFAULT_PORT = 587;
	public static final String DEFAULT_PROTOCOL = "TLS";

	public static boolean sendEmail(
			Context ctx, 
			final MerchantEmailServerSettings serverSettings, 
			String emailAddress, 
			String subject, 
			Spanned emailText,
            ArrayList<File> attachmentFiles,
			boolean useMSOnly) 
	throws AddressException, MessagingException {
		logger.log(Level.INFO, "Sending e-mail ...");

		EmailValidator ev = new EmailValidator(ctx);
		try {
			ev.validateEmailSettings(serverSettings, emailAddress);
		} catch (ValidationException e) {
			logger.log(Level.WARNING, "Sending e-mail failed: " + e.getMessage());
			if (!useMSOnly) {
				logger.log(Level.INFO, "Sending e-mail via intent ...");
				return sendAndPrepareEmailViaIntent(ctx, emailAddress, subject, emailText, attachmentFiles);
			}
			return false;			
		}
	    // There is something wrong with MailCap, javamail can not find a handler for the multipart/mixed part, so this bit needs to be added.
	    MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
	    mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
	    mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
	    mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
	    mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
	    mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
	    CommandMap.setDefaultCommandMap(mc);

	    Properties props = getSessionProperties(serverSettings);
		Session session = Session.getInstance(props, new Authenticator() {
			@Override 
			public PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(serverSettings.user, serverSettings.pswd);
			}
		});
		//for debug SMTP server connection uncomment following line 
//	    session.setDebug(true);

		MimeMessage msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress(serverSettings.user));

		InternetAddress[] addressTo = new InternetAddress[1];
		addressTo[0] = new InternetAddress(emailAddress);
	    msg.setRecipients(MimeMessage.RecipientType.TO, addressTo);

	    msg.setSubject(subject);
	    msg.setSentDate(Calendar.getInstance().getTime());
	 
	    // setup message body
	    BodyPart messageBodyPart = new MimeBodyPart();
	    messageBodyPart.setText(emailText.toString());
	    Multipart multipart = new MimeMultipart();
	    multipart.addBodyPart(messageBodyPart);

        //add attachments
        if (!attachmentFiles.isEmpty()) {
            for(File file: attachmentFiles)
            {
                Uri uri = Uri.fromFile(file);
                if (file.exists())
                    addAttachment(multipart, uri.getPath(), uri.getLastPathSegment());
            }
	    }

	    // Put parts in message
	    msg.setContent(multipart);
	 
	    // send email
	    Transport.send(msg);
	    return true;
	}

    public static boolean sendAndPrepareEmailViaIntent(Context ctx, String emailAddress, String subject, Spanned emailText, ArrayList<File> attachmentFiles) {
        ArrayList<Uri> attachments  = null;
        if (!attachmentFiles.isEmpty()) {
            attachments = new ArrayList<Uri>();
            for(File file : attachmentFiles)
            {
                Uri uri = Uri.fromFile(file);
                attachments.add(uri);
            }
        }
        return sendEmailViaIntent(ctx, emailAddress, subject, emailText, attachments);
    }
	

	public static boolean sendEmailViaIntent(Context ctx, String emailAddress, String subject, Spanned emailText, ArrayList<Uri> attachments) {
        Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
        i.setType("text/plain");
        if (null == emailAddress) {
			emailAddress = "";
		}
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{emailAddress});
		i.putExtra(Intent.EXTRA_SUBJECT, subject);
		i.putExtra(Intent.EXTRA_TEXT, emailText);
		if (null != attachments) {
    			i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachments);
		}
		try {
		    ctx.startActivity(Intent.createChooser(i, ctx.getResources().getString(R.string.sending_email)));
		} catch (android.content.ActivityNotFoundException ex) {
		    Toast.makeText(ctx, R.string.error_no_email_client, Toast.LENGTH_SHORT).show();
		    return false;
		}
		return true;
		
	}
	
	public static boolean sendMerchantReceiptEmail(Context ctx, MerchantEmailServerSettings serverSettings, String emailAddress, Spanned emailText, ArrayList<File> attachments) throws AddressException, MessagingException {
		return sendEmail(ctx, serverSettings, emailAddress, EMAIL_MERCHANT_RECEIPT_SUBJECT, emailText, attachments, true);
	}

	public static boolean sendCustomerReceiptEmail(Context ctx, MerchantEmailServerSettings serverSettings, String emailAddress, Spanned emailText, ArrayList<File> attachments) throws AddressException, MessagingException {
		return sendEmail(ctx, serverSettings, emailAddress, EMAIL_CUSTOMER_RECEIPT_SUBJECT, emailText, attachments, false);
	}
	
	public static void sendReceiptSms(Context ctx, String smsNumber, FinancialTransaction result) {
//		SharedPreferences prefrences = PreferenceManager.getDefaultSharedPreferences(ctx);
//		String merchantName = prefrences.getString(BluetoothService.PREFERENCE_MERCHANT_NAME, SMS_DEFAULT_MERCHANT_NAME);
//		String template = ctx.getResources().getString(R.string.sms_template);
//		BigDecimal amount = new BigDecimal(result.getAuthorizedAmount()).divide(HANDRED);
//		String text = String.format(
//				template, 
//				merchantName, 
//				result.getTransactionId(), 
//				TranslationHelper.getTypeDescription(ctx, result.getType()), 
//				amount, 
//				CurrencyHelper.getHelper(ctx).getCurrency(result.getCurrency()).getShortName(), 
//				TranslationHelper.getTransactionStatusDescription(ctx, result.getTransactionStatus()));
        ReceiptTagHandler receiptTagHandler = new ReceiptTagHandler();
        Spanned text = Html.fromHtml(result.getCustomerReceipt(), null, receiptTagHandler);

		//Spanned text = Html.fromHtml(result.getCustomerReceipt());
//		Uri uri = Uri.parse("smsto:" + smsNumber);
//	    Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
//	    intent.putExtra("sms_body", text.toString());  
//	    ctx.startActivity(intent);
	    		
		Intent sendIntent = new Intent(Intent.ACTION_VIEW);
		sendIntent.putExtra("address", smsNumber);
        sendIntent.putExtra("sms_body", text.toString()); 
        sendIntent.setType("vnd.android-dir/mms-sms");
        ctx.startActivity(sendIntent);	
	}

	private static void addAttachment(Multipart multipart, String path, String filename) throws MessagingException {
		BodyPart messageBodyPart = new MimeBodyPart();
		DataSource source = new FileDataSource(path);
		messageBodyPart.setDataHandler(new DataHandler(source));
		messageBodyPart.setFileName(filename);
		multipart.addBodyPart(messageBodyPart);
	  }

	private static Properties getSessionProperties(MerchantEmailServerSettings settings) {
	    Properties props = new Properties();

	    props.put("mail.smtp.host", settings.host);
	    props.put("mail.smtp.port", String.valueOf(settings.port));
	    props.put("mail.smtp.auth", "true");
//    	props.put("mail.debug", "true");

	    if (settings.protocol.equals("2")) {
		    props.put("mail.smtp.starttls.enable", "true");
		    props.put("mail.smtp.starttls.required", "true");
		    props.put("mail.smtp.sasl.enable", "true");
//		    props.put("mail.smtp.sasl.mechanisms", "XOAUTH2");
//		    props.put("mail.imaps.sasl.mechanisms.oauth2.oauthToken", oauth2AccessToken);
	    }
	    else if (settings.protocol.equals("1")){
		    props.put("mail.smtp.socketFactory.port", settings.port); 
		    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); 
		    props.put("mail.smtp.socketFactory.fallback", "false");
	    }
	    else {
	    	
	    }
	 
	    return props; 
	}

	public static class MerchantEmailServerSettings {
		public String user;
		public String pswd;
		public String host;
		public int port;
		public String protocol;

		public MerchantEmailServerSettings(Context ctx) {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
			user = preferences.getString(SenderHelper.USER, null);
			pswd = preferences.getString(SenderHelper.PSWD, null);
			host = preferences.getString(SenderHelper.HOST, null);
			port = Integer.valueOf(preferences.getString(SenderHelper.PORT, Integer.toString(DEFAULT_PORT)));
			protocol = preferences.getString(SenderHelper.PROTOCOL, null);
		}
	}
}
