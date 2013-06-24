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


public class BasketItem implements BaseColumns {

	public static final String DESCRIPTION = "description";
	
	public static final String THUMBNAIL = "thumbnail";
	
	public static final String FULL_SIZE_PHOTO_PATH = "full_size_photo_path";
	
	public static final String BASKET_ID = "basket_id";
	
	private long id;
	private String description;
	private byte[] thumbnail;
	private String fullSizePhotoPath;
	private long basketId;
	
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public byte[] getThumbnail() {
		return thumbnail;
	}
	public void setThumbnail(byte[] thumbnail) {
		this.thumbnail = thumbnail;
	}
	public String getFullSizePhotoPath() {
		return fullSizePhotoPath;
	}
	public void setFullSizePhotoPath(String fullSizePhotoPath) {
		this.fullSizePhotoPath = fullSizePhotoPath;
	}
	public long getBasketId() {
		return basketId;
	}
	public void setBasketId(long basketId) {
		this.basketId = basketId;
	}
	
	/**
	 * Creates basket object from cursor row 
	 * @param c
	 * @return
	 */
	public static BasketItem createFromCursor(Cursor c, boolean loadImages) {
		BasketItem basketItem = new BasketItem();
		basketItem.setId(c.getLong(c.getColumnIndex(_ID)));
		basketItem.setBasketId(c.getLong(c.getColumnIndex(BASKET_ID)));
		basketItem.setDescription(c.getString(c.getColumnIndex(DESCRIPTION)));
		basketItem.setFullSizePhotoPath(c.getString(c.getColumnIndex(FULL_SIZE_PHOTO_PATH)));
		if (loadImages) {
			basketItem.setThumbnail(c.getBlob(c.getColumnIndex(THUMBNAIL)));
		}
		return basketItem;
	}
	
	/**
	 * Creates ContentValues object from basket item
	 * @param item
	 * @return
	 */
	public static ContentValues toContentValues(BasketItem item) {
		ContentValues values = new ContentValues();
		values.put(DESCRIPTION, item.getDescription());
		values.put(FULL_SIZE_PHOTO_PATH, item.getFullSizePhotoPath());
		values.put(BASKET_ID, item.getBasketId());
		values.put(THUMBNAIL, item.getThumbnail());
		return values;
	}
	
}
