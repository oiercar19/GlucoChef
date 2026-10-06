# GlucoChef

Aplicación Android nativa para contar hidratos de carbono (HC). Una ración de HC equivale a 10 g HC. Los cálculos usan `BigDecimal` y solo redondean al mostrar. No calcula dosis de insulina.

## Abrir y ejecutar

1. Abre esta carpeta en Android Studio.
2. Instala JDK 17, Android SDK Platform 35 y Build Tools 35.0.0 desde el SDK Manager si se solicitan.
3. Sincroniza Gradle y ejecuta la configuración `app` en un dispositivo o emulador con Android 8.0 (API 26) o posterior.
4. Para verificar desde terminal: `gradlew.bat :domain:test :app:testDebugUnitTest :app:assembleDebug` en Windows, o `./gradlew` en macOS/Linux.

El APK de desarrollo aparece en `app/build/outputs/apk/debug/app-debug.apk` después de compilar. La versión 0.7.0 también se copia a la raíz como `GlucoChef-v0.7.0.apk` para enviarla al móvil. Los datos y las fotos de platos se guardan en el dispositivo; las preferencias de tema y tamaño del texto usan DataStore. No hay cuenta, red, publicidad ni analítica. La copia de seguridad automática de Android está desactivada.

## Uso sencillo

La pantalla de Inicio presenta tres acciones: nueva comida, buscar alimento y guardar plato. Los formularios piden primero solo nombre, cantidad y valor de hidratos; los demás campos están en «Más detalles» o «Más opciones». Al añadir un alimento o un plato a una comida, se puede crearlo desde el selector y volver a la comida sin perder lo ya introducido. El editor de platos también permite crear un alimento desde su selector. Al abrir un alimento, «Usar otro valor» está visible junto al cálculo. Puedes hacer una foto opcional del plato con la cámara; aparece en la biblioteca, al elegir el plato para una comida y al editarlo. La biblioteca muestra las raciones de HC por porción, el total del plato si contiene varias porciones y un resumen breve de ingredientes. Los totales se muestran en tarjetas grandes y quedan visibles al editar un plato o una comida. En Ajustes se puede activar «Texto más grande». La app también respeta el tamaño de letra configurado en Android.

## Estructura

- `domain/`: funciones puras para calcular ingredientes, recetas, porciones, peso final y sumas; pruebas unitarias.
- `app/src/main/java/es/racionest1d/data/`: entidades Room, DAO, catálogo de prueba y repositorio.
- `app/src/main/java/es/racionest1d/`: ViewModel, navegación y pantallas Compose Material 3.
- `app/src/test/`: prueba de integración de Room para el historial y la repetición.

## Funciones incluidas

- Catálogo local de 66 alimentos y variantes con los pesos por ración facilitados por el usuario, búsqueda y favoritos. La actualización sustituye las entradas precargadas anteriores.
- Ingredientes personalizados de etiqueta, estimados o pendientes, por 100 g/ml o peso por ración. Las variantes crudas y cocinadas tienen factores separados.
- Calculadora de ingrediente, platos con pesos editables y total inmediato, número de porciones y peso final opcional.
- Biblioteca de platos con búsqueda, favoritos, duplicación, edición y eliminación confirmada.
- Foto opcional tomada con la cámara, guardada localmente y visible en las tarjetas y selectores de platos.
- Comidas con platos e ingredientes; consumo por porciones del plato o por gramos del plato terminado, historial y plantillas.
- Repetición con los valores históricos o con los actuales y diferencias visibles. Los registros guardados conservan sus subtotales originales.
- Tema claro y oscuro.

Los datos nutricionales desconocidos se muestran como pendientes y propagan un total desconocido. No se convierten automáticamente pesos crudos en cocinados. Para calcular una cantidad por gramos del plato preparado, debe registrarse su peso final comestible.

## Catálogo de prueba

Las 66 entradas precargadas proceden de la lista facilitada por el usuario el 04/10/2026. Cada peso indicado equivale a una ración de 10 g de hidratos de carbono. La app conserva `g` como unidad, también para la leche, porque la lista se expresa como peso. Solo se indica el estado crudo o cocido cuando la lista lo especifica.

Al instalar esta versión sobre una anterior, se eliminan las entradas precargadas antiguas y se insertan las nuevas. Los alimentos personalizados, platos y comidas se conservan. Los platos y comidas creados con datos anteriores mantienen sus valores históricos; conviene revisar esos platos y sustituir sus ingredientes por los del catálogo nuevo antes de volver a usarlos.

## Límites conocidos

- No se ha ejecutado la interfaz en un dispositivo físico ni un emulador; sí se han compilado el APK y las pruebas locales.
- La foto requiere una aplicación de cámara en el móvil. No se ha probado físicamente la captura en un dispositivo real.
- La exportación/importación JSON, escaneo de etiquetas y códigos de barras, sincronización e integración con sensores quedan para una versión posterior. La base de datos separa las entidades y sus copias históricas para facilitar una exportación versionada.
- Las equivalencias facilitadas son genéricas: contrástalas con el alimento, la etiqueta comercial y su estado de preparación reales.
- No hay migraciones Room todavía: antes de cambiar el esquema hay que añadir y probar una migración para conservar los datos existentes.
