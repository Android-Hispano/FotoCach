package android.hispano.fotocach;


import java.io.File;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.hispano.fotocach.utils.Utils;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.LruCache;
import android.util.Log;

public class ImageCache {
    private static final String TAG = "ImageCache";

    // Tamaño de la caché de memoria por defecto
    private static final int DEFAULT_MEM_CACHE_SIZE = 1024 * 1024 * 5; // 5MB

    // Tamaño de la caché de disco por defecto
    private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB

    // Ajustes de Compresión cuando está escribiendo imágenes a la caché de disco
    static final CompressFormat DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG;
    static final int DEFAULT_COMPRESS_QUALITY = 70;

    // Constantes para cambiar de estado fácilmente varias cachés
    private static final boolean DEFAULT_MEM_CACHE_ENABLED = true;
    private static final boolean DEFAULT_DISK_CACHE_ENABLED = true;
    private static final boolean DEFAULT_CLEAR_DISK_CACHE_ON_START = false;

    private DiskLruCache mDiskCache;
    private LruCache<String, Bitmap> mMemoryCache;

    /**
     * Crea un nuevo objeto ImageCache usando los parámetros especificados.
     *
     * @param context El contexto a utilizar
     * @param cacheParams Los parámetros de la caché para ser usados para inicializar la caché
     */
    public ImageCache(Context context, ImageCacheParams cacheParams) {
        init(context, cacheParams);
    }

    /**
     * Crea un nuevo objeto ImageCache usando los parámetros por defecto.
     *
     * @param context El contexto a utilizar
     * @param uniqueName Un nombre único que será adjunto a el directorio de la caché
     */
    public ImageCache(Context context, String uniqueName) {
        init(context, new ImageCacheParams(uniqueName));
    }

    /**
     * Find and return an existing ImageCache stored in a {@link RetainFragment}, if not found a new
     * one is created with defaults and saved to a {@link RetainFragment}.
     *
     * @param activity The calling {@link FragmentActivity}
     * @param uniqueName A unique name to append to the cache directory
     * @return An existing retained ImageCache object or a new one if one did not exist.
     */
    public static ImageCache findOrCreateCache(
            final FragmentActivity activity, final String uniqueName) {
        return findOrCreateCache(activity, new ImageCacheParams(uniqueName));
    }

    /**
     * Find and return an existing ImageCache stored in a {@link RetainFragment}, if not found a new
     * one is created using the supplied params and saved to a {@link RetainFragment}.
     *
     * @param activity The calling {@link FragmentActivity}
     * @param cacheParams The cache parameters to use if creating the ImageCache
     * @return An existing retained ImageCache object or a new one if one did not exist
     */
    public static ImageCache findOrCreateCache(
            final FragmentActivity activity, ImageCacheParams cacheParams) {

    	 // Search for, or create an instance of the non-UI RetainFragment
        final RetainFragment mRetainFragment = RetainFragment.findOrCreateRetainFragment(
                activity.getSupportFragmentManager());

        // See if we already have an ImageCache stored in RetainFragment
        ImageCache imageCache = (ImageCache) mRetainFragment.getObject();

        // No existing ImageCache, create one and store it in RetainFragment
        if (imageCache == null) {
            imageCache = new ImageCache(activity, cacheParams);
            mRetainFragment.setObject(imageCache);
        }

        return imageCache;
    }

    /**
     * Inicializa la caché, suministrando todos los parámetros.
     *
     * @param context El contexto a utilizar
     * @param cacheParams Los parámetros de cache para inicializar la caché
     */
    @SuppressLint("NewApi") private void init(Context context, ImageCacheParams cacheParams) {
        final File diskCacheDir = DiskLruCache.getDiskCacheDir(context, cacheParams.uniqueName);

        // Establece una caché de disco
        if (cacheParams.diskCacheEnabled) {
            mDiskCache = DiskLruCache.openCache(context, diskCacheDir, cacheParams.diskCacheSize);
            mDiskCache.setCompressParams(cacheParams.compressFormat, cacheParams.compressQuality);
            if (cacheParams.clearDiskCacheOnStart) {
                mDiskCache.clearCache();
            }
        }

        // Establece una caché de memoria
        if (cacheParams.memoryCacheEnabled) {
            mMemoryCache = new LruCache<String, Bitmap>(cacheParams.memCacheSize) {
                /**
                 * Mide el tamaño del elemento en bytes en lugar de unidades que es más práctico una caché de bitmaps
                 */
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return Utils.getBitmapSize(bitmap);
                }
            };
        }
    }

    public void addBitmapToCache(String data, Bitmap bitmap) {
        if (data == null || bitmap == null) {
            return;
        }

        // Añade a la caché de memoria
        if (mMemoryCache != null && mMemoryCache.get(data) == null) {
            mMemoryCache.put(data, bitmap);
        }

        // Añade a la caché de disco
        if (mDiskCache != null && !mDiskCache.containsKey(data)) {
            mDiskCache.put(data, bitmap);
        }
    }

    /**
     * Obtiene el bitmap desde la caché de memoria.
     *
     * @param data Identificador único para el cual se va a obtener el item.
     * @return El bitmap si lo encuentra en la caché, de lo contrario null
     */
    public Bitmap getBitmapFromMemCache(String data) {
        if (mMemoryCache != null) {
            final Bitmap memBitmap = mMemoryCache.get(data);
            if (memBitmap != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Memory cache hit");
                }
                return memBitmap;
            }
        }
        return null;
    }

    /**
     * Obtiene el bitmap desla caché de disco.
     *
     * @param data Identificador único para el item el cual se va a obtener
     * @return El bitmap si lo encuentra en la caché, de lo contrario null:
     */
    public Bitmap getBitmapFromDiskCache(String data) {	
        if (mDiskCache != null) {
            return mDiskCache.get(data);
        }
        return null;
    }

    public void clearCaches() {
        mDiskCache.clearCache();
        mMemoryCache.evictAll();
    }

    /**
     * A holder class that contains cache parameters.
     */
    public static class ImageCacheParams {
        public String uniqueName;
        public int memCacheSize = DEFAULT_MEM_CACHE_SIZE;
        public int diskCacheSize = DEFAULT_DISK_CACHE_SIZE;
        public CompressFormat compressFormat = DEFAULT_COMPRESS_FORMAT;
        public int compressQuality = DEFAULT_COMPRESS_QUALITY;
        public boolean memoryCacheEnabled = DEFAULT_MEM_CACHE_ENABLED;
        public boolean diskCacheEnabled = DEFAULT_DISK_CACHE_ENABLED;
        public boolean clearDiskCacheOnStart = DEFAULT_CLEAR_DISK_CACHE_ON_START;

        public ImageCacheParams(String uniqueName) {
            this.uniqueName = uniqueName;
        }
    }
}
