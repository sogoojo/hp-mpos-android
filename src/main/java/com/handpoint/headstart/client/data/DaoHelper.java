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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.handpoint.headstart.client.data.models.Basket;
import com.handpoint.headstart.client.data.models.BasketItem;
import com.handpoint.headstart.client.data.models.FinancialTransaction;
import com.handpoint.util.logging.ApplicationLogger;
import com.handpoint.util.logging.Level;
import com.handpoint.util.logging.Logger;

/**
 * Helper class for using local DB
 *
 */
public class DaoHelper {

	private static final String TAG = DaoHelper.class.getSimpleName();
	private static Logger logger = ApplicationLogger.getLogger(TAG);
	
    static final int DATABASE_VERSION = 8;
    public static final String DATABASE_NAME = "headstart_db";
    static final String FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME = "financial_transaction";
    static final String FINANCIAL_TRANSACTION_HISTORY_VIEW_NAME = "payment_history";
    static final String BASKET_TABLE_NAME = "basket";
    static final String BASKET_ITEM_TABLE_NAME = "basket_item";
    
    static final String QUERY_ALL_BASKET_ITEMS =
    		 "SELECT * FROM " + BASKET_ITEM_TABLE_NAME + " JOIN " + BASKET_TABLE_NAME + 
    		 " ON " + BASKET_TABLE_NAME + "." + Basket._ID + " = " + BASKET_ITEM_TABLE_NAME + "." + BasketItem.BASKET_ID;

    
	private class DatabaseHelper extends SQLiteOpenHelper {

	    private static final String TABLE_HISTORY_CREATE_SQL =
	                "CREATE TABLE " + FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME + " (" +
	                FinancialTransaction._ID + " INTEGER PRIMARY KEY," +
	                FinancialTransaction.OPERATION_STATUS + " INTEGER," +
	                FinancialTransaction.TRANSACTION_STATUS  + " INTEGER," +
	                FinancialTransaction.AUTHORISED_AMOUNT  + " INTEGER," +
	                FinancialTransaction.TRANSACTION_ID + " TEXT," +
	                FinancialTransaction.MERCHANT_RECEIPT + " TEXT," +
	                FinancialTransaction.CUSTOMER_RECEIPT + " TEXT," +
	                FinancialTransaction.STATUS_MESSAGE + " TEXT," +
	                FinancialTransaction.TYPE_ID + " INTEGER," +	                
	                FinancialTransaction.TYPE + " TEXT," +	                
	                FinancialTransaction.FINANCIAL_STATUS + " TEXT," +
	                FinancialTransaction.REQUEST_AMOUNT  + " INTEGER," +
	                FinancialTransaction.GRATUITY_AMOUNT  + " INTEGER," +
	                FinancialTransaction.GRATUITY_PERCENTAGE  + " INTEGER," +
	                FinancialTransaction.TOTOAL_AMOUNT  + " INTEGER," +
	                FinancialTransaction.CURRENCY_CODE + " TEXT," +
	                FinancialTransaction.EFT_TRANSACTION_ID + " TEXT," +	                
	                FinancialTransaction.EFT_TIMESTAMP + " TEXT," +	                
	                FinancialTransaction.AUTHORISATION_CODE + " TEXT," +	                
	                FinancialTransaction.CVM + " TEXT," +	                
	                FinancialTransaction.CARD_ENTRY_TYPE + " TEXT," +	                
	                FinancialTransaction.CARD_SCHEME_NAME + " TEXT," +	                
	                FinancialTransaction.ERROR_MESSAGE + " TEXT," +	                	                
	                FinancialTransaction.DATE_TIME + " INTEGER," +
	                FinancialTransaction.VOIDED_ID + " INTEGER," +
	                FinancialTransaction.CUSTOMER_SIGNATURE  + " VARCHAR," +
	                FinancialTransaction.CUSTOMER_RECEIPT_COPY + " INTEGER," +
	                FinancialTransaction.MERCHANT_RECEIPT_COPY + " INTEGER," +
	                FinancialTransaction.SIGNATURE_VERIFICATION_TEXT  + " TEXT" +
		    		");";

	    private static final String TABLE_BASKET_CREATE_SQL = 
	    		"CREATE TABLE " + BASKET_TABLE_NAME + " (" +
	    		Basket._ID + " INTEGER PRIMARY KEY," +
	    		Basket.TOTOAL_AMOUNT  + " INTEGER," +
	    		Basket.TRANSACTION_ID  + " INTEGER" +
	    		");";
	    
	    private static final String TABLE_BASKET_ITEM_CREATE_SQL = 
	    		"CREATE TABLE " + BASKET_ITEM_TABLE_NAME + " (" +
	    		BasketItem._ID + " INTEGER PRIMARY KEY," +
	    		BasketItem.DESCRIPTION  + " TEXT," +
	    		BasketItem.THUMBNAIL  + " BLOB," +
	    		BasketItem.FULL_SIZE_PHOTO_PATH  + " TEXT," +
	    		BasketItem.BASKET_ID  + " INTEGER" +
	    		");";

	    private static final String VIEW_HISTORY_CREATE_SQL =
                "CREATE VIEW " + FINANCIAL_TRANSACTION_HISTORY_VIEW_NAME + 
                " AS SELECT " + FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME + "." + FinancialTransaction._ID + " AS " + FinancialTransaction._ID + ", " +
                FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME + "." + FinancialTransaction.TRANSACTION_STATUS + " AS " + FinancialTransaction.TRANSACTION_STATUS + ", " +
                FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME + "." + FinancialTransaction.TOTOAL_AMOUNT + " AS " + FinancialTransaction.TOTOAL_AMOUNT + ", " +
                FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME + "." + FinancialTransaction.CURRENCY_CODE + " AS " + FinancialTransaction.CURRENCY_CODE + ", " +
                FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME + "." + FinancialTransaction.DATE_TIME + " AS " + FinancialTransaction.DATE_TIME + ", " +
                FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME + "." + FinancialTransaction.CARD_SCHEME_NAME + " AS " + FinancialTransaction.CARD_SCHEME_NAME + ", " +
                FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME + "." + FinancialTransaction.VOIDED_ID + " AS " + FinancialTransaction.VOIDED_ID + ", " +
                FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME + "." + FinancialTransaction.TYPE_ID + " AS " + FinancialTransaction.TYPE_ID + ", " +
                BASKET_ITEM_TABLE_NAME + "." + BasketItem.DESCRIPTION + " AS " + BasketItem.DESCRIPTION + 
                " FROM " + FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME + 
                " LEFT OUTER JOIN " + BASKET_TABLE_NAME + 
                " ON " + FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME + "." + FinancialTransaction._ID + " = " + BASKET_TABLE_NAME + "." + Basket.TRANSACTION_ID +
                " LEFT OUTER JOIN " + BASKET_ITEM_TABLE_NAME +
                " ON " + BASKET_TABLE_NAME + "." + Basket._ID + " = " + BASKET_ITEM_TABLE_NAME + "." + BasketItem.BASKET_ID ;

	    DatabaseHelper(Context context) {
	        super(context, DATABASE_NAME, null, DATABASE_VERSION);
	    }

	    @Override
	    public void onCreate(SQLiteDatabase db) {
	        db.execSQL(TABLE_HISTORY_CREATE_SQL);
	        db.execSQL(TABLE_BASKET_CREATE_SQL);
	        db.execSQL(TABLE_BASKET_ITEM_CREATE_SQL);
	        db.execSQL(VIEW_HISTORY_CREATE_SQL);
	    }

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			logger.log(Level.WARNING, "Upgrading database from version " + oldVersion + " to "
					+ newVersion);
			for (int curVer = oldVersion + 1; curVer <= newVersion; curVer++) {
			    switch ( curVer ) {
			        case 4:
				        db.execSQL(TABLE_BASKET_CREATE_SQL);
				        db.execSQL(TABLE_BASKET_ITEM_CREATE_SQL);
				        db.execSQL(VIEW_HISTORY_CREATE_SQL);
			            break;
			        case 5:
			        	String alterTableSql5 = "ALTER TABLE " + FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME + 
			        		" ADD COLUMN " + FinancialTransaction.CUSTOMER_SIGNATURE  + " VARCHAR";
			        	db.execSQL(alterTableSql5);
			        	break;
			        case 6:
			        	String alterTableSql6 = "ALTER TABLE " + FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME + 
		        		" ADD COLUMN " + FinancialTransaction.CUSTOMER_RECEIPT_COPY  + " INTEGER";
			        	db.execSQL(alterTableSql6);
			        	alterTableSql6 = "ALTER TABLE " + FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME + 
		        		" ADD COLUMN " + FinancialTransaction.MERCHANT_RECEIPT_COPY  + " INTEGER";
			        	db.execSQL(alterTableSql6);
			        	alterTableSql6 = "ALTER TABLE " + FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME + 
		        		" ADD COLUMN " + FinancialTransaction.AUTHORISED_AMOUNT  + " INTEGER";
			        	db.execSQL(alterTableSql6);
			        	break;
			        case 7:
			        	db.execSQL("DROP VIEW IF EXISTS " + FINANCIAL_TRANSACTION_HISTORY_VIEW_NAME);
				        db.execSQL(VIEW_HISTORY_CREATE_SQL);
				        break;
			        case 8:
			        	String alterTableSql8 = "ALTER TABLE " + FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME + 
		        		" ADD COLUMN " + FinancialTransaction.SIGNATURE_VERIFICATION_TEXT  + " TEXT";
			        	db.execSQL(alterTableSql8);
			        	break;
			    }
			}
		}

	}
	
	private DatabaseHelper dbHelper;
	private SQLiteDatabase db;

	public DaoHelper(Context ctx) {
		super();
		dbHelper = new DatabaseHelper(ctx);
	}

	/**
	 * Opens database in writable or readable mode
	 * @param isWritable - mode
	 */
	public void open(boolean isWritable) {
		if (isWritable) {
			db = dbHelper.getWritableDatabase();
		} else {
			db = dbHelper.getReadableDatabase();
		}
	}
	
	public void close() {
		db.close();
	}
	/**
	 * Returns financial transaction history record by ID
	 * @param id
	 * @return
	 */
	public FinancialTransaction getFinancialTransaction(long id) {
		Cursor c = null;
		try {
			FinancialTransaction result = null;
			c = db.query(FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME, null, FinancialTransaction._ID + "=" + id, null, null, null, null);
			if (c.moveToFirst()) {
				result = FinancialTransaction.createFromCursor(c);
			}
			return result;
		} finally {
			if (null != c) {
				c.close();
			}
		}		
	}

	/**
	 * Returns last financial transaction from history
	 * @return
	 */
	public FinancialTransaction getLastFinancialTransaction() {
		Cursor c = null;
		try {
			FinancialTransaction result = null;
			c = db.query(FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME, null, null, null, null, null, FinancialTransaction.DEFAULT_SORT_ORDER);
			if (c.moveToFirst()) {
				result = FinancialTransaction.createFromCursor(c);
			}
			return result;
		} finally {
			if (null != c) {
				c.close();
			}
		}		
	}
	/**
	 * Returns all financial transaction history records as cursor
	 * @return
	 */
	public Cursor getFinancialTransactions() {
		return db.query(FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME, null, null, null, null, null, FinancialTransaction.DATE_SORT_ORDER);
	}
	
	public Cursor getFinancialTransactionsWithDescription() {
		return db.query(FINANCIAL_TRANSACTION_HISTORY_VIEW_NAME, null, null, null, null, null, FinancialTransaction.DATE_SORT_ORDER);
	}
	
	/**
	 * Adds financial transaction to history.
	 * @param ft
	 */
	public long insertFinancialTransaction(FinancialTransaction ft) {
		ContentValues values = FinancialTransaction.toContentValues(ft);
		try {
			//TODO: transacton needed?
			long voidedId = db.insert(FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME, FinancialTransaction.TRANSACTION_ID, values);
			if (null != ft.getOriginalId() && voidedId != -1) {
				FinancialTransaction originTransaction = getFinancialTransaction(ft.getOriginalId());
				originTransaction.setVoidedId(voidedId);
				updateFinancialTransaction(originTransaction);				
			}
			Basket basket = getCurrentBasket();
			if (null != basket && voidedId != -1) {
				basket.setTransactionId(voidedId);
				updateBasket(basket);
			}
			return voidedId;
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Error inserting in financial transaction history", e);
		}
		return -1;
	}

	/**
	 * Updates financial transaction in history
	 * @param ft
	 */
	public void updateFinancialTransaction(FinancialTransaction ft) {
		ContentValues values = FinancialTransaction.toContentValues(ft);
		try {
			db.update(FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME, values, FinancialTransaction._ID + "=" + ft.getId(), null);
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Error updating financial transaction history", e);
		}
	}

	
	
	/**
	 * Deletes financial transaction from history
	 * @param id
	 */
	public void deleteFinancialTransaction(long id) {
		try {
			db.delete(FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME, FinancialTransaction._ID + " = " + id, null);
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Error updating financial transaction history", e);
		}
	}
	/**
	 * Deletes all financial transaction from history
	 */
	public void deleteAllFinancialTransactions() {
		try {
			db.delete(FINANCIAL_TRANSACTION_HISTORY_TABLE_NAME, null, null);
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Error updating financial transaction history", e);
		}
	}
	
	/**
	 * Returns current basket (where transaction ID is empty)
	 * @return
	 */
	public Basket getCurrentBasket() {
		Cursor c = null;
		try {
			Basket result = null;
			c = db.query(BASKET_TABLE_NAME, null, Basket.TRANSACTION_ID + " = 0", null, null, null, null);
			if (c.moveToFirst()) {
				result = Basket.createFromCursor(c);
			}
			return result;
		} finally {
			if (null != c) {
				c.close();
			}
		}		
	}

	/**
	 * Deletes current basket with all items
	 */
	public void deleteBasket(long id) {
		db.beginTransaction();
		try {
			db.delete(BASKET_ITEM_TABLE_NAME, BasketItem.BASKET_ID + "= ?", new String[] {Long.toString(id)});
			db.delete(BASKET_TABLE_NAME, Basket._ID + "=?", new String[] {Long.toString(id)});
			db.setTransactionSuccessful();
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Error deleting of current basket", e);
		} finally {
			db.endTransaction();
	   }
	}
	/**
	 * Returns basket by ID
	 * @param id
	 * @return
	 */
	public Basket getBasket(long id) {
		Cursor c = null;
		try {
			Basket result = null;
			c = db.query(BASKET_TABLE_NAME, null, Basket._ID + "=" + id, null, null, null, null);
			if (c.moveToFirst()) {
				result = Basket.createFromCursor(c);
			}
			return result;
		} finally {
			if (null != c) {
				c.close();
			}
		}		
	}
	
	/**
	 * Returns basket by transaction ID
	 * @param transactionId
	 * @return
	 */
	public Basket getBasketByTransaction(long transactionId) {
		Cursor c = null;
		try {
			Basket result = null;
			c = db.query(BASKET_TABLE_NAME, null, Basket.TRANSACTION_ID + "=" + transactionId, null, null, null, null);
			if (c.moveToFirst()) {
				result = Basket.createFromCursor(c);
			}
			return result;
		} finally {
			if (null != c) {
				c.close();
			}
		}		
	}
	
	/**
	 * Persists basket in DB.
	 * @param basket
	 */
	public long insertBasket(Basket basket) {
		ContentValues values = Basket.toContentValues(basket);
		try {
			return db.insert(BASKET_TABLE_NAME, null, values);
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Error inserting in basket table", e);
		}
		return -1;
	}
	
	/**
	 * Updates basket in DB.
	 * @param basket
	 */
	public void updateBasket(Basket basket) {
		ContentValues values = Basket.toContentValues(basket);
		try {
			db.update(BASKET_TABLE_NAME, values, Basket._ID + " = " + basket.getId(), null);
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Error updating of basket table", e);
		}
	}
		
	/**
	 * Returns basket item by basket ID
	 * @param basketId
	 * @return
	 */
	public BasketItem getBasketItem(Long basketId) {
		Cursor c = null;
		try {
			BasketItem basketItem = null;
			c = db.query(BASKET_ITEM_TABLE_NAME, null, BasketItem.BASKET_ID + "=" + basketId, null, null, null, null);
			if (c.moveToFirst()) {
				basketItem = BasketItem.createFromCursor(c, true);
			}
			return basketItem;
		} finally {
			if (null != c) {
				c.close();
			}
		}		
	}

	/**
	 * Persists basket item in DB.
	 * @param item
	 */
	public void insertBasketItem(BasketItem item) {
		try {
			ContentValues values = BasketItem.toContentValues(item);
			db.insert(BASKET_ITEM_TABLE_NAME, null, values);
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Error inserting in basket item table", e);
		}
	}

	/**
	 * Updates basket item in DB.
	 * @param item
	 */
	public void updateBasketItem(BasketItem item) {
		ContentValues values = BasketItem.toContentValues(item);
		try {
			db.update(BASKET_ITEM_TABLE_NAME, values, BasketItem._ID + " = " + item.getId(), null);
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Error updating of basket item table", e);
		}
	}

}
