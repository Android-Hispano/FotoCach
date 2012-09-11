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


import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.widget.ImageView;

public abstract class ImageWorker {
    private static final String TAG = "ImageWorker";
    private static final int FADE_IN_TIME = 200;

    private ImageCache mImageCache;
    private Bitmap mLoadingBitmap;
    private boolean mFadeInBitmap = true;
    private boolean mExitTasksEarly = false;

    protected static Context mContext;

    protected ImageWorker(Context context) {
        mContext = context;
    }
    
    private static Map<String, String> contactsMap;
    public static void setContactsMap(Map<String, String> result) {
		contactsMap = result;
	}

    /**
     * Carga una imagen dado un EMAIL dentro de un ImageView (sobreescribe
     * {@link ImageWorker#processBitmap(Object)} para definir la lógica de precesamiento). Una caché de memoria
     * y disco se usarán si un {@link ImageCache} ha sido establecido usando
     * {@link ImageWorker#setImageCache(ImageCache)}. Si se encuentra la imagen en la caché de memoria, la
     * establece imediatamente, de lo contrario un {@link AsyncTask} será creado para de manera asíncrona cargue el
     * bitmap.
     *
     * @param data El MAIL de la imagen a descargar.
     * @param imageView El ImageView para unir la imagen recuperada.
     */
    public void loadImage(String email, ImageView imageView) {
        Bitmap bitmap = null;
        String idContact = null;
        
       
        if(contactsMap!=null){
        	idContact = contactsMap.get(email);
        } else {
        	contactsMap = getContactsMapWithOutTask();
        	idContact = contactsMap.get(email);
        }
        if(idContact!=null){
	        if (mImageCache != null) {
	            bitmap = mImageCache.getBitmapFromMemCache(idContact);
	        }
	
	        if (bitmap != null) {
	            // Bitmap encontrado en la caché de memoria
	            imageView.setImageBitmap(bitmap);
	        } else if (cancelPotentialWork(email, imageView)) {
	        	// De lo contrario ejecuta el AsyncTask para recuperar el bitmap
	            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
	            final AsyncDrawable asyncDrawable =
	                    new AsyncDrawable(mContext.getResources(), mLoadingBitmap, task);
	            imageView.setImageDrawable(asyncDrawable);
	            task.execute(idContact);
	        }
        }
    }


    private Map<String, String> getContactsMapWithOutTask() {
    	String id;
        Uri uri = Contacts.CONTENT_URI;
        String[] projection = new String[] {
                Contacts._ID,
        };
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = null;
        Cursor cursor = ((Activity) mContext).managedQuery(uri, projection, selection, selectionArgs, sortOrder);

        ContentResolver cResolver = mContext.getContentResolver();
    	Map<String, String> mapa = new HashMap<String, String>();
        if(cursor.getCount() > 0){
        	while(cursor.moveToNext()){
        		id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
        		
        		Cursor cmail = cResolver.query(CommonDataKinds.Email.CONTENT_URI,
                        new String[] { CommonDataKinds.Email.DATA,
                            CommonDataKinds.Email.TYPE},
                            CommonDataKinds.Email.CONTACT_ID + "='" + id + "'", null, null);
		           while(cmail.moveToNext()){
		             mapa.put(cmail.getString(cmail.getColumnIndex(CommonDataKinds.Email.DATA)), id);
             	}
		         cmail.close();
   	          }
        }
        cursor.close();
		return mapa;
	}
    
    
    

	/**
     * Establece el placeholder para el bitmap que se mostrará cuando el hilo den background
     * esté ejecutándose.
     *
     * @param bitmap
     */
    public void setLoadingImage(Bitmap bitmap) {
        mLoadingBitmap = bitmap;
    }

    /**
     * Establece el placeholder para el bitmap que se mostrará cuando el hilo den background
     * esté ejecutándose.
     * 
     * @param resId
     */
    public void setLoadingImage(int resId) {
        mLoadingBitmap = BitmapFactory.decodeResource(mContext.getResources(), resId);
    }

    /**
     * Establece el objeto {@link ImageCache} para ser utilizado con este ImageWorker.
     *
     * @param cacheCallback
     */
    public void setImageCache(ImageCache cacheCallback) {
        mImageCache = cacheCallback;
    }

    public ImageCache getImageCache() {
        return mImageCache;
    }

    /**
     * Si está establecido a true, la imagen será fade-in una vez que ha sido cargada por el hilo en background.
     *
     * @param fadeIn
     */
    public void setImageFadeIn(boolean fadeIn) {
        mFadeInBitmap = fadeIn;
    }

    public void setExitTasksEarly(boolean exitTasksEarly) {
        mExitTasksEarly = exitTasksEarly;
    }

    /**
     * Las Subclases deben reemplazar esto para definir cualquier proceso o trabajo que debería de ocurrir
     * para producir el bitmap final. Esto se ejecuta en un subproceso en segundo plano y será una larga 
     * ejecución. Por ejemplo, se podría cambiar el tamaño de un bitmap aquí, o bajar una imagen desde la red.
     *
     * @param data Los datos para identificar el bitmap a procesar, conforme a lo dispuesto por
     *            {@link ImageWorker#loadImage(Object, ImageView)}
     * @return El bitmap procesado
     */
    protected abstract Bitmap processBitmap(Object data);

    public static void cancelWork(ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask != null) {
            bitmapWorkerTask.cancel(true);
            if (BuildConfig.DEBUG) {
                final Object bitmapData = bitmapWorkerTask.data;
                Log.d(TAG, "cancelWork - work cancelado para " + bitmapData);
            }
        }
    }

    /**
     * Devuelve true si la tarea actual ha sido cancelada o si no estaba trabajando
     * sobre este imageView.
     */
    public static boolean cancelPotentialWork(Object data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final Object bitmapData = bitmapWorkerTask.data;
            if (bitmapData == null || !bitmapData.equals(data)) {
                bitmapWorkerTask.cancel(true);
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "cancelPotentialWork - tarea cancelada para " + data);
                }
            } else {
                // El mismo trabajo estaba ya en progreso.
                return false;
            }
        }
        return true;
    }

    /**
     * @param imageView Algún ImageView
     * @return Recupera el actual work task activo (si lo hay) asociado con este imageView.
     * null si no hay tal work task.
     */
    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    /**
     * El AsyncTask asíncronamente procesará el bitmap.
     */
    private class BitmapWorkerTask extends AsyncTask<Object, Void, Bitmap> {
        private Object data;
        private final WeakReference<ImageView> imageViewReference;

        public BitmapWorkerTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        /**
         * Proceso en Background
         */
        @Override
        protected Bitmap doInBackground(Object... params) {
        	Bitmap bitmap = null;
        	data = params[0];
        	final String idPhoto = String.valueOf(data);
		
		            // Si la caché de imagen está disponible y este task no ha sido cancelado por otro 
		            // hilo y el ImageView que fue desde un principio destinado a este task está adjunto y nuestro 
		            // flag de "retirada-prematura" no estaba establecido, entonces intentará recuperar el bitmap 
		            // desde la caché.
		            if (mImageCache != null && !isCancelled() && getAttachedImageView() != null
		                    && !mExitTasksEarly) {
		                bitmap = mImageCache.getBitmapFromDiskCache(idPhoto);
		            }
		
		            // Si el bitmap no fue encontrado en la caché y este task no ha sido cancelado por otro
		            // hilo y el ImageView que estaba originalmente destinado a este task continúa adjunto y nuestro  
		            // flag de "retirada-prematura" no estaba establecido, entonces 
		            // llamará al método de proceso principal (aplicado por una subclase).
		            if (bitmap == null && !isCancelled() && getAttachedImageView() != null
		                    && !mExitTasksEarly) {
		                bitmap = processBitmap(idPhoto);
		            }
		
		            // Si el bitmap se procesó y la caché de imágenes está disponible, a continuación,
		            // añade el bitmap procesado en la caché de memoria para su utilización futura.
		            // A tener en cuenta que no comprueba si el task se ha cancelado aquí, si lo fue, el hilo
		            // se estaría ejecutando aún, podemos también añadir el bitmap procesado a nuestra caché de memoria,
		            // ya que podría ser utilizado de nuevo en el futuro.
		            if (bitmap != null && mImageCache != null) {
		                mImageCache.addBitmapToCache(idPhoto, bitmap);
		            }
			return bitmap;
        }
        	

        /**
         * Una vez la imagen es procesada, la asocia al imageView
         */
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            // if cancel was called on this task or the "exit early" flag is set then we're done
            if (isCancelled() || mExitTasksEarly) {
                bitmap = null;
            }

            final ImageView imageView = getAttachedImageView();
            if (bitmap != null && imageView != null) {
                setImageBitmap(imageView, bitmap);
            }
        }

        /**
         * Devuelve el ImageView asociado con este task como mucho como el task del ImageView apunta
         * a este task también. De lo contrario devuelve null.
         */
        private ImageView getAttachedImageView() {
            final ImageView imageView = imageViewReference.get();
            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

            if (this == bitmapWorkerTask) {
                return imageView;
            }

            return null;
        }
    }

    /**
     * Un Drawable personalizado que será adjunto al imageView mientras el trabajo está en progerso
     * Contiene una referencia al actual work task, así puede ser parado si un binding nuevo es 
     * requerido, y asegura que solo el último proceso worker iniciado puede unir el resultado,
     * independientemente del orden de llegada.
     */
    private static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);

            bitmapWorkerTaskReference =
                new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    /**
     * Llamado cuando el proceso ha sido completado y el bitmap final ha sido establecido al ImageView.
     *
     * @param imageView
     * @param bitmap
     */
    @SuppressWarnings("deprecation")
	private void setImageBitmap(ImageView imageView, Bitmap bitmap) {
        if (mFadeInBitmap) {
            // Transición del drawable con un drawable transparente y el bitmal final.
            final TransitionDrawable td =
                    new TransitionDrawable(new Drawable[] {
                            new ColorDrawable(android.R.color.transparent),
                            new BitmapDrawable(mContext.getResources(), bitmap)
                    });
            // Establece la carga del bitamp en background
            imageView.setBackgroundDrawable(
                    new BitmapDrawable(mContext.getResources(), mLoadingBitmap));

            imageView.setImageDrawable(td);
            td.startTransition(FADE_IN_TIME);
        } else {
            imageView.setImageBitmap(bitmap);
        }
    }

}
