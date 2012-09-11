/* 
        Copyright 2012 Android-Hispano
 
        This file is part of FotoCach.

    FotoCach is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FotoCach is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FotoCach.  If not, see <http://www.gnu.org/licenses/>.
*/
package android.hispano.fotocach;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.util.Log;

public class ImageFetcher extends ImageWorker {
	

	public ImageFetcher(Context context) {
		super(context);
		mContext = context;
	}

	private static final String TAG = "ImageFetcher";
	    private static final int CACHE_SIZE = 10 * 1024 * 1024; // 10MB
	    public static final String CACHE_DIR = "photos";

	    /**
	     * El método del proceso principal, el cual será llamado por el ImageWorker en el AsyncTask en el 
	     * hilo en background.
	     *
	     * @param data La data para cargar el bitmap, en este caso, un ID de un contacto
	     * @return El bitmap recuperado
	     */
	    private Bitmap processBitmap(String idContact) {
	        Log.d(TAG, "Contact ID to process - " + idContact);
	            
	        // Recupera un bitmap, lo escribe a un archivo
	        retrieveBitmap(mContext, idContact);
	        return null;
	    }
	    
	    @Override
	    protected Bitmap processBitmap(Object data) {
	        return processBitmap(String.valueOf(data));
	    }
	    

	    /**
	     * Obtiene un bitmap desde una cuenta de email, lo escribe a disco y devuelve un puntero a un File. Esta es
	     * implementación de una caché de disco simple.
	     *
	     * @param context El contexto a utilizar
	     * @param id El ID del contacto a recuperar
	     * @return Un File apuntando al bitmap recuperado
	     */
		public static File retrieveBitmap(Context context, String idContact) {
	    	
	        final File cacheDir = DiskLruCache.getDiskCacheDir(context, CACHE_DIR);

	        final DiskLruCache cache = DiskLruCache.openCache(context, cacheDir, CACHE_SIZE);

	        final File cacheFile = new File(cache.createFilePath(idContact));
	        
	        // Si el bitmap está en la caché devuelve el cacheFile
	        if (cache.containsKey(idContact)) {
	            Log.d(TAG, "retrieveBitmap - Encontrado un idContact en la caché : " + idContact);
	            return cacheFile;
	        } 
	        	// Recupera el Bitmap dado un idContact y devuelve el cacheFile
		        Log.d(TAG, "retrieveBitmap - recuperando - " + idContact);
		        
		        
		        final Bitmap bitmap = getPhotoBitmapFromContactId(idContact);
		        if(bitmap!=null){
			        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
			        bitmap.compress(ImageCache.DEFAULT_COMPRESS_FORMAT,
			        		ImageCache.DEFAULT_COMPRESS_QUALITY, stream);
			        
			        try {
						FileOutputStream fos = new FileOutputStream(cacheFile);
						fos.write(stream.toByteArray());
						fos.close();
						stream.close();
						return cacheFile;
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
		        }
		        
				return null;
	        }
		
		
		private static Bitmap getPhotoBitmapFromContactId(String contactId) {
			final String[] projection = new String[] {
					Contacts.PHOTO_ID				// el id de la columna en la tabla de datos para la imagen
			};

			@SuppressWarnings("deprecation")
			final Cursor contact = ((Activity) mContext).managedQuery(
					Contacts.CONTENT_URI,
					projection,
					Contacts._ID + "=?",			// entradas filtradas basadas en el id de contacto
					new String[]{contactId},	
					null);
			
			if(contact.moveToFirst()) {
				final String photoId = contact.getString(
						contact.getColumnIndex(Contacts.PHOTO_ID));
				final Bitmap photo;
				if(photoId != null) {
					photo = getPhotoBitmapFromPhotoId(photoId);
				} else {
					photo = null;
				}
				contact.close();
				return photo;
			}
			contact.close();
			return null;
		}

		private static Bitmap getPhotoBitmapFromPhotoId(String photoId) {
			@SuppressWarnings("deprecation")
			final Cursor photo = ((Activity) mContext).managedQuery(
					Data.CONTENT_URI,
					new String[] {Photo.PHOTO},		// columna donde está guardado el blob
					Data._ID + "=?",				// fila seleccionada por id
					new String[]{photoId},			// filtrado por el idPhoto dado
					null);
			
			final Bitmap photoBitmap;
			if(photo.moveToFirst()) {
				byte[] photoBlob = photo.getBlob(
						photo.getColumnIndex(Photo.PHOTO));
				photoBitmap = BitmapFactory.decodeByteArray(
						photoBlob, 0, photoBlob.length);
			} else {
				photoBitmap = null;
			}
			photo.close();
			return photoBitmap;
		}
		
}
