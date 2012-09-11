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
package android.hispano.fotocach.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

public class Utils {
	
	public static final int IO_BUFFER_SIZE = 8 * 1024;
	private static final int DEFAULT_BUFFER_SIZE = 1024;
	
	private Utils() {};
	
    
    /**
     * Comprueba si el almacenamiento externo es empotrado o desmontable (como una SDcard).
     *
     * @return True si es desmontable, de lo contrario false.
     */
    @SuppressLint("NewApi")
    public static boolean isExternalStorageRemovable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }
    /**
     * Devuelve el directorio de cache de la app.
     *
     * @param context El contexto a usar
     * @return El directorio de cache externa
     */
    @SuppressLint("NewApi")
    public static File getExternalCacheDir(Context context) {
        if (hasExternalCacheDir()) {
            return context.getExternalCacheDir();
        }

        // Anterior a Froyo necesitamos construir el directorio de cache externa nosotros mismos
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
    }

    /**
     * Comprueba cuanto espacio usable hay disponible en el path dado.
     *
     * @param path El path a comprobar
     * @return El espacio disponible en bytes
     */
    @SuppressLint("NewApi")
    public static long getUsableSpace(File path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null) {
            return activeNetworkInfo.isConnected();
        }
        return false;
    }
    

    /**
     * Comprueba si la versión del OS tiene un directorio para la cache externa empotrada.
     *
     * @return
     */
    public static boolean hasExternalCacheDir() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetworkInfo != null) {
            return wifiNetworkInfo.isConnected();
        }
        return false;
    }


    public static int copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
    
	
	 /**
     * Obtiene el tamaño en bytes del bitmap.
     * @param bitmap
     * @return tamaño en bytes
     */
    @SuppressLint("NewApi")
    public static int getBitmapSize(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return bitmap.getByteCount();
        }
        // Pre HC-MR1
        return bitmap.getRowBytes() * bitmap.getHeight();
    }
    
    public static int getMemoryClass(Context context) {
        return ((ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE)).getMemoryClass();
    }
	
}
