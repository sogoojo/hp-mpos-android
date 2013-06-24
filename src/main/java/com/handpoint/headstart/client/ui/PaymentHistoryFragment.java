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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.handpoint.headstart.api.FinancialTransactionResult;
import com.handpoint.headstart.client.R;
import com.handpoint.headstart.client.android.Application;
import com.handpoint.headstart.client.data.DaoHelper;
import com.handpoint.headstart.client.data.models.BasketItem;
import com.handpoint.headstart.client.data.models.FinancialTransaction;
import com.handpoint.headstart.eft.TransactionStatus;

/**
 * 
 *
 */
public class PaymentHistoryFragment extends SherlockListFragment {


	DaoHelper mDaoHelper;
    TransactionPaymentAdapter mPaymentHistoryAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
		mDaoHelper = new DaoHelper(getActivity());
		mDaoHelper.open(false);
		mPaymentHistoryAdapter = new TransactionPaymentAdapter(getActivity(), mDaoHelper.getFinancialTransactionsWithDescription());
        setListAdapter(mPaymentHistoryAdapter);
    }
    

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    		Bundle savedInstanceState) {
    	return inflater.inflate(R.layout.history_list, container, false);
    }
    
	@Override
	public void onResume() {
		super.onResume();
		refreshHistoryViewData();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mPaymentHistoryAdapter.close();
		mDaoHelper.close();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(getActivity(), ReceiptActivity.class);
		intent.putExtra(ReceiptActivity.EXTRA_TRANSACTION_ID, id);
		startActivity(intent);
		
	}

	void refreshHistoryViewData() {
		mPaymentHistoryAdapter.refresh();
	}

	/**
	 * Overriding standard cursor class
	 *
	 */
	private class TransactionPaymentAdapter extends CursorAdapter {

        private HashMap<Integer,String> IcelandicMonthsLocale = new HashMap<Integer,String>();
	    private final NumberFormat nf = NumberFormat.getNumberInstance();
	    private final SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy",Locale.getDefault());
		private final java.text.DateFormat df;
		private final int voidColor;
	    
		private final BigDecimal HUNDRED = new BigDecimal(100.0);
	    
		private Cursor c;
		private Application applicationHelper;

		public TransactionPaymentAdapter(Context context, Cursor c) {
			super(context, c, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
			this.c = c;
			df = DateFormat.getDateFormat(context);
			applicationHelper = (Application)((Activity)context).getApplication();
			voidColor = getResources().getColor(R.color.purple);
            initMonthsMap();
		}

        private void initMonthsMap()
        {
            IcelandicMonthsLocale.put(0, "Janúar");
            IcelandicMonthsLocale.put(1, "Febrúar");
            IcelandicMonthsLocale.put(2, "Mars");
            IcelandicMonthsLocale.put(3, "Apríl");
            IcelandicMonthsLocale.put(4, "Maí");
            IcelandicMonthsLocale.put(5, "Júní");
            IcelandicMonthsLocale.put(6, "Júlí");
            IcelandicMonthsLocale.put(7, "Ágúst");
            IcelandicMonthsLocale.put(8, "September");
            IcelandicMonthsLocale.put(9, "Október");
            IcelandicMonthsLocale.put(10, "Nóvember");
            IcelandicMonthsLocale.put(11, "Desember");

        }

        /**Due to problems with Icelandic Locale the Icelandic months have to be mapped*/
        private String getIcelandicMonth(Date date){
            Calendar cal = Calendar.getInstance(Locale.getDefault());
            cal.setTime(date);
            int month = cal.get(Calendar.MONTH);
            return IcelandicMonthsLocale.get(month).toString();
        }
		
		private void findAndCacheViews(View view) {
			HistoryItemViews cache = new HistoryItemViews();
			cache.monthHeader = (TextView) view.findViewById(R.id.month_header);
			cache.monthDivider = view.findViewById(R.id.month_divider);
			cache.cardLogo = (ImageView) view.findViewById(R.id.card_type_image);
			cache.transactionDateView = (TextView) view.findViewById(R.id.transaction_date);
			cache.transactionDescriptionView = (TextView) view.findViewById(R.id.transaction_description);
			cache.transactionAmountView = (TextView) view.findViewById(R.id.transaction_amount);
			cache.transactionType = (TextView) view.findViewById(R.id.transaction_type);
			view.setTag(cache);
		}
		
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(R.layout.history_list_item, parent,false);
			findAndCacheViews(view);
			return view;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			HistoryItemViews cache = (HistoryItemViews) view.getTag();
			String cardSchemaName = cursor.getString(cursor.getColumnIndex(FinancialTransaction.CARD_SCHEME_NAME));
			cache.cardLogo.setImageDrawable(applicationHelper.getCardSchemeLogo(cardSchemaName));
			
			Date date = new Date(cursor.getLong(cursor.getColumnIndex(FinancialTransaction.DATE_TIME)));
            String thisDate = sdf.format(date);

            //here comes the exception, but we have to map Icelandic months
            if(Locale.getDefault().getLanguage().equals("is")){
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy",Locale.getDefault());
                thisDate = sdf2.format(date);
                String temp = getIcelandicMonth(date) + " " + thisDate;
                thisDate = temp;
            }

			cache.monthHeader.setText(thisDate);
            String prevDate = null;

			if (cursor.getPosition() > 0 && cursor.moveToPrevious()) {
                if(Locale.getDefault().getLanguage().equals("is"))
                    prevDate = thisDate;
                else
                    prevDate = sdf.format(new Date(cursor.getLong(cursor.getColumnIndex(FinancialTransaction.DATE_TIME))));
		        cursor.moveToNext();
		    }
			if (prevDate == null || !prevDate.equals(thisDate)) {
				cache.monthHeader.setVisibility(View.VISIBLE);
				cache.monthDivider.setVisibility(View.VISIBLE);
		    } else {
				cache.monthHeader.setVisibility(View.GONE);
				cache.monthDivider.setVisibility(View.GONE);
		    }
			cache.transactionDateView.setText(df.format(date));
			int status = cursor.getInt(cursor.getColumnIndex(FinancialTransaction.TRANSACTION_STATUS));
			boolean changeColor = false;
			if (!(TransactionStatus.EFT_TRANSACTION_APPROVED == status
					|| TransactionStatus.EFT_TRANSACTION_PROCESSED == status)) 
			{
				changeColor = true;
			}
			
			cache.transactionDescriptionView.setText(cursor.getString(cursor.getColumnIndex(BasketItem.DESCRIPTION)));
			String currencyCode = cursor.getString(cursor.getColumnIndex(FinancialTransaction.CURRENCY_CODE));
			cache.transactionAmountView.setText(applicationHelper.getFormattedAmount(cursor.getInt(cursor.getColumnIndex(FinancialTransaction.TOTOAL_AMOUNT)), currencyCode));
			if (changeColor) {
				cache.transactionDescriptionView.setTextColor(Color.RED);
				cache.transactionAmountView.setTextColor(Color.RED);
			} else {
				cache.transactionDescriptionView.setTextColor(Color.GRAY);
				cache.transactionAmountView.setTextColor(Color.BLACK);
			}
			long voidedId = cursor.getLong(cursor.getColumnIndex(FinancialTransaction.VOIDED_ID));
			int type = cursor.getInt(cursor.getColumnIndex(FinancialTransaction.TYPE_ID));
			cache.transactionType.setVisibility(View.VISIBLE);
			if (voidedId > 0) {
				cache.transactionType.setText(R.string.voided_mark);
				cache.transactionType.setTextColor(voidColor);
			} else if (type == FinancialTransactionResult.FT_TYPE_SALE_VOID || type == FinancialTransactionResult.FT_TYPE_REFUND_VOID) {
				cache.transactionType.setText(R.string.void_mark);
				cache.transactionType.setTextColor(voidColor);
			} else if (type == FinancialTransactionResult.FT_TYPE_SALE) {
				cache.transactionType.setText(R.string.sale_mark);
				cache.transactionType.setTextColor(Color.BLACK);
			} else if (type == FinancialTransactionResult.FT_TYPE_REFUND) {
				cache.transactionType.setText(R.string.refund_mark);
				cache.transactionType.setTextColor(Color.RED);
			} else {
				cache.transactionType.setVisibility(View.GONE);
			}
		}
		
		public void refresh() {
			c.requery();
			notifyDataSetChanged();
		}

		public void close() {
			c.close();
		}
		
	}
	
	private class HistoryItemViews {
		TextView monthHeader;
		View monthDivider;
		ImageView cardLogo;
		TextView transactionDateView;
		TextView transactionDescriptionView;
		TextView transactionAmountView;
		TextView transactionType;
	}
}
