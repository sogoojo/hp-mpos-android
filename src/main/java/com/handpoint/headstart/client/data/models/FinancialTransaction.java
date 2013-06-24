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
package com.handpoint.headstart.client.data.models;

import java.util.Calendar;
import java.util.Date;

import android.content.res.Resources;
import com.handpoint.headstart.android.Application;
import com.handpoint.headstart.api.FinancialTransactionResult;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;
import com.handpoint.headstart.client.R;

/**
 * Model for storing financial transaction data
 *
 */
public class FinancialTransaction implements BaseColumns {

	public static final String PLACEHOLDER = "{COPY_RECEIPT}";

	//columns description
	
	public static final String OPERATION_STATUS = "operation_status";
	
	public static final String TRANSACTION_STATUS = "transaction_status";
	
	public static final String AUTHORISED_AMOUNT = "authorised_amount";
	
	public static final String TRANSACTION_ID = "transaction_id";
	
	public static final String MERCHANT_RECEIPT = "merchant_receipt";
	
	public static final String CUSTOMER_RECEIPT = "customer_receipt";
	
	public static final String STATUS_MESSAGE = "status_message";
	
	public static final String TYPE_ID = "type_id";
	
	public static final String TYPE = "type";
	
	public static final String FINANCIAL_STATUS = "financial_status";
	
	public static final String REQUEST_AMOUNT = "request_amount";
	
	public static final String GRATUITY_AMOUNT = "gratuity_amount";
	
	public static final String GRATUITY_PERCENTAGE = "gratuity_percentage";
	
	public static final String TOTOAL_AMOUNT = "total_amount";
	
	public static final String CURRENCY_CODE = "currency_code";
	
	public static final String EFT_TRANSACTION_ID = "eft_transaction_id";
	
	public static final String EFT_TIMESTAMP = "eft_timestamp";
	
	public static final String AUTHORISATION_CODE = "authorization_code";
	
	public static final String CVM = "cvm";
	
	public static final String CARD_ENTRY_TYPE = "card_entry_type";
	
	public static final String CARD_SCHEME_NAME = "card_scheme_name";
	
	public static final String ERROR_MESSAGE = "error_message";
			
	public static final String DATE_TIME = "date_time";
	
	public static final String VOIDED_ID = "voided_id";
	
	public static final String DEFAULT_SORT_ORDER = _ID  + " DESC";
	
	public static final String DATE_SORT_ORDER =   DATE_TIME + " DESC";

	public static final String CUSTOMER_SIGNATURE = "customer_signature";
	
	public static final String CUSTOMER_RECEIPT_COPY = "customer_receipt_copy";
	
	public static final String MERCHANT_RECEIPT_COPY = "merchant_receipt_copy";
	
	public static final String SIGNATURE_VERIFICATION_TEXT = "signature_verification_text";
	
	private FinancialTransactionResult delegate;
	// fields
	private Long id;
	private Long voidedId;
	private Date dateTime;
	//transient field for void transaction
	private Long originalId;
	// path to signature image
	private String signaturePath;
	// flag of customer receipt copy
	private boolean customerReceiptCopy;
	// flag of merchant receipt copy
	private boolean merchantReceiptCopy;
	// signature verification text
	private String signatureVerificationText;
	
	public FinancialTransaction() {
		this(0, 0, 0, 0, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
	}
	
	public FinancialTransaction (FinancialTransactionResult result, Long originalId, String signaturePath, String signatureVerificationText) {
		this.delegate = result;
		this.originalId = originalId;
		this.signaturePath = signaturePath;
		this.signatureVerificationText = signatureVerificationText;
	}
	
	public FinancialTransaction(
			int type,
			int operationStatus,
			int transactionStatus, 
			int authorizedAmount, 
			String transactionId, 
			String merchantReceipt, 
			String customerReceipt,
			String statusMessage,
			String transactionType,
			String financialStatus,
			Integer requestAmount,
			Integer gratuityAmount,
			Integer gratuityPercentage,
			Integer totalAmount,
			String currency,
			String eftTransactionId,
			String eftTimestamp,
			String authorisationCode,
			String cvm,
			String cardEntryType,
			String cardSchemeName,
			String errorMessage,
			Long originalId,
			String signaturePath,
			String signatureVerificationText)
	{
		this.delegate = new FinancialTransactionResult(
				operationStatus,
				transactionStatus, 
				authorizedAmount, 
				transactionId, 
				merchantReceipt, 
				customerReceipt,
				statusMessage,
				transactionType,
				financialStatus,
				requestAmount,
				gratuityAmount,
				gratuityPercentage,
				totalAmount,
				currency,
				eftTransactionId,
				eftTimestamp,
				authorisationCode,
				cvm,
				cardEntryType,
				cardSchemeName,
				errorMessage);
		this.delegate.type = type;
		this.setOriginalId(originalId);
		this.signaturePath = signaturePath;
		this.signatureVerificationText = signatureVerificationText;
	}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	public int getOperationStatus() {
		return this.delegate.operationStatus;
	}
	
	public void setOperationStatus(int operationStatus) {
		this.delegate.operationStatus = operationStatus;
	}
	
	public int getTransactionStatus() {
		return this.delegate.transactionStatus;
	}
	
	public void setTransactionStatus(int transactionStatus) {
		this.delegate.transactionStatus = transactionStatus;
	}

	public int getType() {
		return this.delegate.type;
	}
	
	public void setType(int type) {
		this.delegate.type = type;
	}
	
	public int getAuthorizedAmount() {
		return this.delegate.authorizedAmount;
	}
	
	public void setAuthorizedAmount(int authorizedAmount) {
		this.delegate.authorizedAmount = authorizedAmount;
	}
	
	public String getCurrency() {
		return this.delegate.currency;
	}
	
	public void setCurrency(String currency) {
		this.delegate.currency = currency;
	}
	
	public String getTransactionId() {
		return this.delegate.transactionId;
	}
	
	public void setTransactionId(String transactionId) {
		this.delegate.transactionId = transactionId;
	}
	
	public String getRawMerchantReceipt() {
		return this.delegate.merchantReceipt;
	}
	
	public String getMerchantReceipt() {
		String receipt;
		if (this.isMerchantReceiptCopy()) {
            Resources res = Application.getInstance().getResources();
            receipt = this.delegate.merchantReceipt.replace(PLACEHOLDER, res.getString(R.string.copy_receipt));
		}
		else {
			receipt = this.delegate.merchantReceipt.replace(PLACEHOLDER, "");
		}
		return receipt;
	}
	
	public void setMerchantReceipt(String merchantReceipt) {
		this.delegate.merchantReceipt = merchantReceipt;
	}
	
	public String getRawCustomerReceipt() {
		return this.delegate.customerReceipt;
	}
	
	public String getCustomerReceipt() {
		String receipt;
		if (this.isCustomerReceiptCopy()) {
            Resources res = Application.getInstance().getResources();
			receipt = this.delegate.customerReceipt.replace(PLACEHOLDER, res.getString(R.string.copy_receipt));
		}
		else {
			receipt = this.delegate.customerReceipt.replace(PLACEHOLDER, "");
		}
		return receipt;
	}
	
	public void setCustomerReceipt(String customerReceipt) {
		this.delegate.customerReceipt = customerReceipt;
	}

	public String getStatusMessage() {
		return this.delegate.statusMessage;
	}
	
	public void setStatusMessage(String statusMessage) {
		this.delegate.statusMessage = statusMessage;
	}
	
	public String getTransactionType() {
		return this.delegate.transactionType;
	}
	
	public void setTransactionType(String transactionType) {
		this.delegate.transactionType = transactionType;
	}
	
	public String getFinancialStatus() {
		return this.delegate.financialStatus;
	}
	
	public void setFinancialStatus(String financialStatus) {
		this.delegate.financialStatus = financialStatus;
	}
	
	public Integer getRequestAmount() {
		return this.delegate.requestAmount;
	}
	
	public void setRequestAmount(Integer requestAmount) {
		this.delegate.requestAmount = requestAmount;
	}
	
	public Integer getGratuityAmount() {
		return this.delegate.gratuityAmount;
	}
	
	public void setGratuityAmount(Integer gratuityAmount) {
		this.delegate.gratuityAmount = gratuityAmount;
	}
	
	public Integer getGratuityPercentage() {
		return this.delegate.gratuityPercentage;
	}
	
	public void setGratuityPercentage(Integer gratuityPercentage) {
		this.delegate.gratuityPercentage = gratuityPercentage;
	}
	
	public Integer getTotalAmount() {
		return this.delegate.totalAmount;
	}
	
	public void setTotalAmount(Integer totalAmount) {
		this.delegate.totalAmount = totalAmount;
	}
	
	public String getEftTransactionId() {
		return this.delegate.eftTransactionId;
	}
	
	public void setEftTransactionId(String eftTransactionId) {
		this.delegate.eftTransactionId = eftTransactionId;
	}
	
	public String getEftTimestamp() {
		return this.delegate.eftTimestamp;
	}
	
	public void setEftTimestamp(String eftTimestamp) {
		this.delegate.eftTimestamp = eftTimestamp;
	}
	
	public String getAuthorisationCode() {
		return this.delegate.authorisationCode;
	}
	
	public void setAuthorisationCode(String authorisationCode) {
		this.delegate.authorisationCode = authorisationCode;
	}
	
	public String getCvm() {
		return this.delegate.cvm;
	}
	
	public void setCvm(String cvm) {
		this.delegate.cvm = cvm;
	}
	
	public String getCardEntryType() {
		return this.delegate.cardEntryType;
	}
	
	public void setCardEntryType(String cardEntryType) {
		this.delegate.cardEntryType = cardEntryType;
	}
	
	public String getCardSchemeName() {
		return this.delegate.cardSchemeName;
	}
	
	public void setCardSchemeName(String cardSchemeName) {
		this.delegate.cardSchemeName = cardSchemeName;
	}
	
	public String getErrorMessage() {
		return this.delegate.errorMessage;
	}
	
	public void setErrorMessage(String errorMessage) {
		this.delegate.errorMessage = errorMessage;
	}
	public Date getDateTime() {
		return dateTime;
	}
	public void setDateTime(Date dateTime) {
		this.dateTime = dateTime;
	}

	public Long getVoidedId() {
		return voidedId;
	}

	public void setVoidedId(Long voidedId) {
		this.voidedId = voidedId;
	}

	public Long getOriginalId() {
		return originalId;
	}

	public void setOriginalId(Long originalId) {
		this.originalId = originalId;
	}

	public String getSignaturePath() {
		return signaturePath;
	}

	public void setSignaturePath(String signaturePath) {
		this.signaturePath = signaturePath;
	}
	
	public boolean isCustomerReceiptCopy() {
		return customerReceiptCopy;
	}

	public void setCustomerReceiptCopy(boolean customerReceiptCopy) {
		this.customerReceiptCopy = customerReceiptCopy;
	}
	
	public boolean isMerchantReceiptCopy() {
		return merchantReceiptCopy;
	}

	public void setMerchantReceiptCopy(boolean customerReceiptCopy) {
		this.merchantReceiptCopy = customerReceiptCopy;
	}
	
	public String getSignatureVerificationText() {
		return signatureVerificationText;
	}

	public void setSignatureVerificationText(String signatureVerificationText) {
		this.signatureVerificationText = signatureVerificationText;
	}


	/**
	 * Creates financial transaction object from cursor row 
	 * @param c
	 * @return
	 */
	public static FinancialTransaction createFromCursor(Cursor c) {
		FinancialTransaction ft = new FinancialTransaction();
		ft.setId(c.getLong(c.getColumnIndex(_ID)));
		ft.setOperationStatus(c.getInt(c.getColumnIndex(OPERATION_STATUS)));
		ft.setAuthorizedAmount(c.getInt(c.getColumnIndex(AUTHORISED_AMOUNT)));
		ft.setTransactionStatus(c.getInt(c.getColumnIndex(TRANSACTION_STATUS)));
		ft.setTransactionId(c.getString(c.getColumnIndex(TRANSACTION_ID)));
		ft.setMerchantReceipt(c.getString(c.getColumnIndex(MERCHANT_RECEIPT)));
		ft.setCustomerReceipt(c.getString(c.getColumnIndex(CUSTOMER_RECEIPT)));
		ft.setStatusMessage(c.getString(c.getColumnIndex(STATUS_MESSAGE)));
		ft.setType(c.getInt(c.getColumnIndex(TYPE_ID)));
		ft.setTransactionType(c.getString(c.getColumnIndex(TYPE)));
		ft.setFinancialStatus(c.getString(c.getColumnIndex(FINANCIAL_STATUS)));
		ft.setRequestAmount(c.getInt(c.getColumnIndex(REQUEST_AMOUNT)));
		ft.setGratuityAmount(c.getInt(c.getColumnIndex(GRATUITY_AMOUNT)));
		ft.setGratuityPercentage(c.getInt(c.getColumnIndex(GRATUITY_PERCENTAGE)));
		ft.setTotalAmount(c.getInt(c.getColumnIndex(TOTOAL_AMOUNT)));
		ft.setAuthorisationCode(c.getString(c.getColumnIndex(AUTHORISATION_CODE)));
		ft.setCurrency(c.getString(c.getColumnIndex(CURRENCY_CODE)));
		ft.setEftTransactionId(c.getString(c.getColumnIndex(EFT_TRANSACTION_ID)));
		ft.setEftTimestamp(c.getString(c.getColumnIndex(EFT_TIMESTAMP)));
		ft.setCvm(c.getString(c.getColumnIndex(CVM)));
		ft.setCardEntryType(c.getString(c.getColumnIndex(CARD_ENTRY_TYPE)));
		ft.setCardSchemeName(c.getString(c.getColumnIndex(CARD_SCHEME_NAME)));
		ft.setErrorMessage(c.getString(c.getColumnIndex(ERROR_MESSAGE)));
		ft.setDateTime(new Date(c.getLong(c.getColumnIndex(DATE_TIME))));
		ft.setVoidedId(c.getLong(c.getColumnIndex(VOIDED_ID)));
		ft.setSignaturePath(c.getString(c.getColumnIndex(CUSTOMER_SIGNATURE)));
		ft.setCustomerReceiptCopy(c.getInt(c.getColumnIndex(CUSTOMER_RECEIPT_COPY)) > 0);
		ft.setSignatureVerificationText(c.getString(c.getColumnIndex(SIGNATURE_VERIFICATION_TEXT)));
		return ft;
	}
	
	/**
	 * Creates ContentValues object from financial transaction
	 * @param ft
	 * @return
	 */
	public static ContentValues toContentValues(FinancialTransaction ft) {
		ContentValues values = new ContentValues();
		values.put(TYPE_ID, ft.getType());
		values.put(OPERATION_STATUS, ft.getOperationStatus());
		values.put(TRANSACTION_STATUS, ft.getTransactionStatus());
		values.put(AUTHORISED_AMOUNT, ft.getAuthorizedAmount());
		values.put(TRANSACTION_ID, ft.getTransactionId());
		values.put(MERCHANT_RECEIPT, ft.getRawMerchantReceipt());
		values.put(CUSTOMER_RECEIPT, ft.getRawCustomerReceipt());
		values.put(STATUS_MESSAGE, ft.getStatusMessage());
		values.put(TYPE, ft.getTransactionType());
		values.put(FINANCIAL_STATUS, ft.getFinancialStatus());
		values.put(REQUEST_AMOUNT, ft.getRequestAmount());
		values.put(GRATUITY_AMOUNT, ft.getGratuityAmount());
		values.put(GRATUITY_PERCENTAGE, ft.getGratuityPercentage());
		values.put(TOTOAL_AMOUNT, ft.getTotalAmount());
		values.put(CURRENCY_CODE, ft.getCurrency());
		values.put(EFT_TRANSACTION_ID, ft.getEftTransactionId());
		values.put(EFT_TIMESTAMP, ft.getEftTimestamp());
		values.put(AUTHORISATION_CODE, ft.getAuthorisationCode());
		values.put(CVM, ft.getCvm());
		values.put(CARD_ENTRY_TYPE, ft.getCardEntryType());
		values.put(CARD_SCHEME_NAME, ft.getCardSchemeName());
		values.put(ERROR_MESSAGE, ft.getErrorMessage());
		values.put(VOIDED_ID, ft.getVoidedId());
		if (null == ft.getDateTime()) {
			ft.setDateTime(new Date());
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(ft.getDateTime());
		values.put(DATE_TIME, cal.getTimeInMillis());
		values.put(CUSTOMER_SIGNATURE, ft.getSignaturePath());
		values.put(CUSTOMER_RECEIPT_COPY, ft.isCustomerReceiptCopy() ? 1 : 0);
		values.put(SIGNATURE_VERIFICATION_TEXT, ft.getSignatureVerificationText());
		
		return values;
	}

}
