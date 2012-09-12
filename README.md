FotoCach
========

Añade las fotos de los contactos a los ViewGroups de tus aplicaciones (ListView, GridView, etc...) sin bloqueos ni largas esperas.



Inicializar FotoCach
====================

public static ImageFecher fotoCach;
public static final String IMAGE_CACHE_DIR = "fotos";

# Por ejemplo en el onCreate de un ListFragment.

        ImageCacheParams cacheParams = new ImageCacheParams(IMAGE_CACHE_DIR);
		
        cacheParams.memCacheSize = 1024 * 1024 * Utils.getMemoryClass(getActivity()) / 3;        
        fotoCach = new ImageFetcher(contexto);
        fotoCach.setLoadingImage(drawable);
        fotoCach.setImageCache(ImageCache.findOrCreateCache(getActivity(), cacheParams));


Usar FotoCach
=============

Ejemplo:

  // Muestra las imágenes dado un ID de Contacto
  fotoCach.loadImage(idContacto, null, null, viewHolder.imageView);




