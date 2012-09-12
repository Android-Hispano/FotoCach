FotoCach
========

Añade las fotos de los contactos a los ViewGroups de tus aplicaciones (ListView, GridView, etc...) sin bloqueos ni largas esperas.



Inicializar FotoCach
====================

public static ImageFecher fotoCach;
public static final String IMAGE_CACHE_DIR = "fotos";

 Por ejemplo en el onCreate de un ListFragment.

        ImageCacheParams cacheParams = new ImageCacheParams(IMAGE_CACHE_DIR);
		
        cacheParams.memCacheSize = 1024 * 1024 * Utils.getMemoryClass(getActivity()) / 3;        
        fotoCach = new ImageFetcher(contexto);
        fotoCach.setLoadingImage(drawable);
        fotoCach.setImageCache(ImageCache.findOrCreateCache(getActivity(), cacheParams));


Usar FotoCach
=============

Ejemplo:

  /**
  * Muestra las imágenes dado un ID de Contacto
  **/
  fotoCach.loadImage(idContacto, null, null, viewHolder.imageView);


#Licencia

Copyright 2012 Javier Hernández

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

