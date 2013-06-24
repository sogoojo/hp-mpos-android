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

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;


public class Basket implements BaseColumns {

	public static final String TOTOAL_AMOUNT = "total_amount";
	
	public static final String TRANSACTION_ID = "transaction_id";
	
	private long id;	
	private int totalAmount;	
	private long transactionId;
	
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public int getTotalAmount() {
		return totalAmount;
	}
	public void setTotalAmount(int totalAmount) {
		this.totalAmount = totalAmount;
	}
	public long getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(long transactionId) {
		this.transactionId = transactionId;
	}
	
	/**
	 * Creates basket object from cursor row 
	 * @param c
	 * @return
	 */
	public static Basket createFromCursor(Cursor c) {
		Basket basket = new Basket();
		basket.setId(c.getLong(c.getColumnIndex(_ID)));
		basket.setTotalAmount(c.getInt(c.getColumnIndex(TOTOAL_AMOUNT)));
		basket.setTransactionId(c.getLong(c.getColumnIndex(TRANSACTION_ID)));		
		return basket;
	}
	
	/**
	 * Creates ContentValues object from basket
	 * @param basket
	 * @return
	 */
	public static ContentValues toContentValues(Basket basket) {
		ContentValues values = new ContentValues();
		values.put(TOTOAL_AMOUNT, basket.getTotalAmount());
		values.put(TRANSACTION_ID, basket.getTransactionId());
		return values;
	}
	
}
