package android.hispano.fotocach;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.hispano.fotocach.utils.Utils;
import android.os.Environment;
import android.util.Log;

public class DiskLruCache {
	private static final String TAG = "DiskLruCache";
    private static final String CACHE_FILENAME_PREFIX = "cache_";
    private static final int MAX_REMOVALS = 4;
    private static final int INITIAL_CAPACITY = 32;
    private static final float LOAD_FACTOR = 0.75f;

    private final File mCacheDir;
    private int cacheSize = 0;
    private int cacheByteSize = 0;
    private final int maxCacheItemSize = 64; // 64 item default
    private long maxCacheByteSize = 1024 * 1024 * 5; // 5MB default
    private CompressFormat mCompressFormat = CompressFormat.JPEG;
    private int mCompressQuality = 70;
    
    
    private final Map<String, String> mLinkedHashMap =
            Collections.synchronizedMap(new LinkedHashMap<String, String>(
                    INITIAL_CAPACITY, LOAD_FACTOR, true));
    
    /**
     * Constructor que no se debe llamar directamente, en lugar de utilizar
     * {@link DiskLruCache#openCache(Context, File, long)} el cual ejecuta comprobaciones adicionales antes de
     * crear una instancia DiskLruCache.
     *
     * @param cacheDir
     * @param maxByteSize
     */
    private DiskLruCache(File cacheDir, long maxByteSize) {
        mCacheDir = cacheDir;
        maxCacheByteSize = maxByteSize;
    }

    
    /**
     * Un nombre de archivo de filtro para identificar los nombres de los archivos cache que tengan
     * antepuesto CACHE_FILENAME_PREFIX.
     */
    private static final FilenameFilter cacheFileFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            return filename.startsWith(CACHE_FILENAME_PREFIX);
        }
    };

    /**
     * Para recuperar una instancia de DiskLruCache.
     *
     * @param context
     * @param cacheDir
     * @param maxByteSize
     * @return
     */
    public static DiskLruCache openCache(Context context, File cacheDir, long maxByteSize) {
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }

        if (cacheDir.isDirectory() && cacheDir.canWrite()
                && Utils.getUsableSpace(cacheDir) > maxByteSize) {
            return new DiskLruCache(cacheDir, maxByteSize);
        }

        return null;
    }
    
    /**
     * Añade un bitmap a la cache del disco.
     *
     * @param key Un identificador único para el bitmap.
     * @param data El store del bitmap.
     */
    public void put(String key, Bitmap data) {
        synchronized (mLinkedHashMap) {
            if (mLinkedHashMap.get(key) == null) {
                try {
                    final String file = createFilePath(mCacheDir, key);
                    if (writeBitmapToFile(data, file)) {
                        put(key, file);
                        flushCache();
                    }
                } catch (final FileNotFoundException e) {
                    Log.e(TAG, "Error en put: " + e.getMessage());
                } catch (final IOException e) {
                    Log.e(TAG, "Error en put: " + e.getMessage());
                }
            }
        }
    }

    private void put(String key, String file) {
        mLinkedHashMap.put(key, file);
        cacheSize = mLinkedHashMap.size();
        cacheByteSize += new File(file).length();
    }
    
    /**
     * Flush the cache, removing oldest entries if the total size is over the specified cache size.
     * Note that this isn't keeping track of stale files in the cache directory that aren't in the
     * HashMap. If the images and keys in the disk cache change often then they probably won't ever
     * be removed.
     */
    private void flushCache() {
        Entry<String, String> eldestEntry;
        File eldestFile;
        long eldestFileSize;
        int count = 0;

        while (count < MAX_REMOVALS &&
                (cacheSize > maxCacheItemSize || cacheByteSize > maxCacheByteSize)) {
            eldestEntry = mLinkedHashMap.entrySet().iterator().next();
            eldestFile = new File(eldestEntry.getValue());
            eldestFileSize = eldestFile.length();
            mLinkedHashMap.remove(eldestEntry.getKey());
            eldestFile.delete();
            cacheSize = mLinkedHashMap.size();
            cacheByteSize -= eldestFileSize;
            count++;
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "flushCache - Eliminado el archivo de la caché, " + eldestFile + ", "
                        + eldestFileSize);
            }
        }
    }

    /**
     * Obtiene un bitmap desde la cache de disco.
     *
     * @param key El identificador único para el bitmap
     * @return El bitmap o null sino lo encuentra
     */
    public Bitmap get(String key) {
        synchronized (mLinkedHashMap) {
            final String file = mLinkedHashMap.get(key);
            if (file != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Se tocó la caché de disco");
                }
                return BitmapFactory.decodeFile(file);
            } else {
                final String existingFile = createFilePath(mCacheDir, key);
                if (new File(existingFile).exists()) {
                    put(key, existingFile);
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Se tocó la caché de disco (archivo existente)");
                    }
                    return BitmapFactory.decodeFile(existingFile);
                }
            }
            return null;
        }
    }

    /**
     * Comprueba si una clave específica existe en la cache.
     *
     * @param key El identificador único para el bitmap
     * @return true si lo encuentra, de lo contrario false
     */
    public boolean containsKey(String key) {
        // Mira si la clave está en nuestro HashMap
        if (mLinkedHashMap.containsKey(key)) {
            return true;
        }

        // Comprueba si el fichero existe basado en la clave
        final String existingFile = createFilePath(mCacheDir, key);
        if (new File(existingFile).exists()) {
            // Fichero encontrado, lo añade al HashMap para usos futuros
            put(key, existingFile);
            return true;
        }
        return false;
    }

    /**
     * Elimina todas las entradas de la cache de disco desde esta instancia del directorio cache
    */
    public void clearCache() {
        DiskLruCache.clearCache(mCacheDir);
    }

    /**
     * Elimina todas las entradas de cache desde el directorio de cache de la aplicación en el uniqueName
     * sub-directory.
     *
     * @param context El contexto a usar
     * @param uniqueName Un nombre de directorio de cache único para anexar el directorio de cache de la app
     */
    public static void clearCache(Context context, String uniqueName) {
        File cacheDir = getDiskCacheDir(context, uniqueName);
        clearCache(cacheDir);
    }

    /**
     * Elimina todas las entradas de la cache de disco desde el directorio dado. Esto no debería ser llamado directamente,
     * llama a {@link DiskLruCache#clearCache(Context, String)} o {@link DiskLruCache#clearCache()}
     *
     * @param cacheDir El directorio para eliminar los archivos de cache
     */
    private static void clearCache(File cacheDir) {
        final File[] files = cacheDir.listFiles(cacheFileFilter);
        for (int i=0; i<files.length; i++) {
            files[i].delete();
        }
    }

    /**
     * Obtiene un directorio de cache usable (externo si está disponible, de lo contrario interno).
     *
     * @param context El contexto a usar
     * @param uniqueName Un nombre de directorio único para anexar el directorio de la cache
     * @return El directorio de la cache
     */
    public static File getDiskCacheDir(Context context, String uniqueName) {

        // Comprueba si hay almacenamiento interno, de ser así, intenta usar la caché de directorio externa 
        // de lo contario usa la caché de directorio interna
        final String cachePath =
                Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED ||
                        !Utils.isExternalStorageRemovable() ?
                        Utils.getExternalCacheDir(context).getPath() :
                        context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }


    public static String createFilePath(File cacheDir, String key) {
        // Utiliza URLEncoder para asegurar que tenemos un nombre de archivo válido
            try {
				return cacheDir.getAbsolutePath() + File.separator +
				        CACHE_FILENAME_PREFIX + URLEncoder.encode(key.replace("*", ""), "UTF-8") + ".jpg";
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return null;
    }

    public String createFilePath(String key) {
        return createFilePath(mCacheDir, key);
    }

    /**
     * Establece el formato de compresión de destino y la calidad de las imágenes escritas a la caché de disco.
     *
     * @param compressFormat
     * @param quality
     */
    public void setCompressParams(CompressFormat compressFormat, int quality) {
        mCompressFormat = compressFormat;
        mCompressQuality = quality;
    }

    /**
     * Escribe un bitmap a un archivo. Call {@link DiskLruCache#setCompressParams(CompressFormat, int)}
     * primero establece la compresión del bitmap de destino y formato
     *
     * @param bitmap
     * @param file
     * @return
     */
    private boolean writeBitmapToFile(Bitmap bitmap, String file)
            throws IOException, FileNotFoundException {

        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(file), Utils.IO_BUFFER_SIZE);
            return bitmap.compress(mCompressFormat, mCompressQuality, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
