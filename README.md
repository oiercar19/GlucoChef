# GlucoChef

Puedes descargar el APK desde [GlucoChef beta en GitHub Releases](https://github.com/oiercar19/GlucoChef/releases/tag/GlucoChef-beta).

Aplicación Android nativa para contar hidratos de carbono (HC). Una ración de HC equivale a 10 g HC. Los cálculos usan `BigDecimal` y solo redondean al mostrar. Permite anotar la insulina indicada por el usuario, pero no calcula dosis.

## Generar el APK en otro PC

Los APK, cachés, carpetas `build/` y la configuración local del SDK están excluidos mediante `.gitignore`. Después de clonar o actualizar el repositorio, cada PC debe configurar Java y su SDK; Gradle vuelve a generar los archivos de compilación.

### Requisitos

- **Git**, para clonar el repositorio o ejecutar `git pull`.
- **JDK 17**, instalado por separado y disponible para Gradle. El módulo `domain` requiere una toolchain de Java 17; no basta con tener únicamente un JDK de otra versión.
- **Android SDK Platform 35** (`platforms;android-35`).
- **Android SDK Build-Tools 34.0.0** (`build-tools;34.0.0`), versión predeterminada del Android Gradle Plugin 8.6.1 que usa el proyecto. Tener Build Tools 35.0.0 no sustituye este paquete.
- **Android SDK Platform-Tools**, para instalar el APK por USB con `adb`.
- **Conexión a Internet** durante la primera compilación para descargar Gradle y las dependencias.
- Para ejecutar la app: un móvil o emulador con **Android 8.0 (API 26) o posterior**. No se necesita un dispositivo ni un emulador para generar el APK.

Puedes instalar el SDK con [Android Studio](https://developer.android.com/studio) o con sus Command-line Tools. **No necesitas instalar Gradle ni Kotlin por separado**: el repositorio incluye `gradlew`, `gradlew.bat` y `gradle/wrapper/`, que descargan Gradle 8.7 y usan las versiones definidas en los archivos de compilación. Estos archivos del wrapper deben permanecer versionados.

La combinación de JDK, Gradle y Build Tools se basa en la [tabla oficial de compatibilidad de AGP 8.6](https://developer.android.com/build/releases/agp-8-6-0-release-notes).

### 1. Descargar o actualizar el proyecto

Primera descarga, tanto en PowerShell como en macOS/Linux:

```sh
git clone https://github.com/oiercar19/GlucoChef.git
cd GlucoChef
```

Si ya tienes el repositorio en ese PC, entra en su carpeta y actualízalo:

```sh
git pull
```

Ejecuta los siguientes comandos desde la raíz del proyecto, donde están `settings.gradle.kts` y `gradlew.bat`.

### 2. Configurar Java 17

En **Windows PowerShell**, adapta la ruta al directorio real de tu JDK 17 (sin añadir `bin` a `JAVA_HOME`):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

En **macOS**, con un JDK 17 instalado:

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

En **Linux**, adapta la ruta a tu instalación:

```sh
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

La salida debe indicar Java 17. Estos cambios se aplican a la terminal actual; para conservarlos, configura las variables de entorno de Windows o el archivo de inicio de tu shell en macOS/Linux.

Si compilas desde Android Studio, selecciona también JDK 17 en **Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK**. La selección del IDE y el `JAVA_HOME` de la terminal se configuran por separado.

### 3. Instalar y localizar el Android SDK

En Android Studio, abre **Tools > SDK Manager** e instala:

1. En **SDK Platforms**: Android 15, **API 35**.
2. En **SDK Tools**: **Android SDK Build-Tools 34.0.0** (activa **Show Package Details** para elegir la versión) y **Android SDK Platform-Tools**.
3. Para usar los comandos opcionales de abajo, instala también **Android SDK Command-line Tools (latest)**.

Acepta las licencias cuando se soliciten. Abre el proyecto en Android Studio y sincroniza Gradle; el IDE puede crear `local.properties` con la ruta del SDK.

Para compilar desde terminal, también puedes crear tú mismo **`local.properties` en la raíz del repositorio**, con una única línea como esta (sustituye la ruta):

```properties
# Windows: usa barras / para evitar tener que escapar las barras inversas.
sdk.dir=C:/Users/TU_USUARIO/AppData/Local/Android/Sdk
```

En macOS suele ser `sdk.dir=/Users/TU_USUARIO/Library/Android/sdk`; en Linux, `sdk.dir=/home/TU_USUARIO/Android/Sdk`. Comprueba la ruta real en SDK Manager. Este archivo es propio de cada PC y no debe subirse a Git.

Si prefieres instalar los paquetes desde terminal, con las Command-line Tools ya instaladas, usa [sdkmanager](https://developer.android.com/tools/sdkmanager). En **Windows PowerShell**, para la ruta habitual del SDK:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" 'platforms;android-35' 'build-tools;34.0.0' 'platform-tools'
```

En **macOS/Linux**, adapta la ruta del SDK (el ejemplo usa la habitual de Linux):

```sh
export ANDROID_HOME="$HOME/Android/Sdk"
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platforms;android-35" "build-tools;34.0.0" "platform-tools"
```

En macOS la ruta habitual es `$HOME/Library/Android/sdk`. `ANDROID_HOME` puede localizar el SDK sin `local.properties`; si configuras ambos, deben apuntar al mismo directorio.

### 4. Compilar el APK de desarrollo

En **Windows PowerShell**:

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug
```

En **macOS/Linux**:

```sh
chmod +x gradlew
./gradlew --version
./gradlew :app:assembleDebug
```

`--version` permite comprobar que se está usando Gradle 8.7 y Java 17. Cuando la compilación termine con `BUILD SUCCESSFUL`, el APK estará en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Este APK está firmado automáticamente para desarrollo y se puede instalar en el móvil. No se copia automáticamente a la raíz del repositorio. Si quieres una copia con un nombre fácil de compartir, puedes hacerla en PowerShell:

```powershell
Copy-Item .\app\build\outputs\apk\debug\app-debug.apk .\GlucoChef-debug.apk
```

Los APK generados siguen excluidos por `.gitignore`. Después de futuros `git pull`, normalmente basta con volver a ejecutar `:app:assembleDebug`; no hace falta borrar cachés ni ejecutar `clean` cada vez.

### 5. Ejecutar las pruebas e instalar en el móvil

Para ejecutar las pruebas locales y generar el APK en **Windows PowerShell**:

```powershell
.\gradlew.bat :domain:test :app:testDebugUnitTest :app:assembleDebug
```

En **macOS/Linux**:

```sh
./gradlew :domain:test :app:testDebugUnitTest :app:assembleDebug
```

Para instalar por USB, activa las opciones de desarrollador y la depuración USB en el móvil, conéctalo y acepta la autorización del PC. Con un único dispositivo o emulador conectado:

```powershell
# Windows PowerShell
.\gradlew.bat :app:installDebug
```

```sh
# macOS/Linux
./gradlew :app:installDebug
```

También puedes enviar `app-debug.apk` al móvil, abrirlo y permitir la instalación desde esa aplicación cuando Android lo solicite. Desde Android Studio, sincroniza Gradle, selecciona el dispositivo y ejecuta la configuración `app`.

### APK de distribución y firma entre distintos PCs

La clave de depuración suele generarse en el perfil de cada PC, en `.android/debug.keystore`. Un APK debug generado en otro PC puede tener una firma distinta: Android no permite actualizar una app instalada con otra firma. Para conservar la posibilidad de actualizarla y sus datos, usa la misma clave de firma en todos los equipos mediante una transferencia privada. Desinstalar la app para instalar otra firma elimina sus datos locales.

Para generar un APK de distribución firmado, usa **Build > Generate Signed Bundle / APK > APK** en Android Studio, selecciona o crea un keystore y elige la variante `release`. Conserva la clave, su alias y las contraseñas para futuras actualizaciones; los archivos `.jks`, `.keystore` y `keystore.properties` están ignorados y no llegan al otro PC con `git pull`.

El proyecto no incluye una configuración de firma release. Por tanto, ejecutar `.\gradlew.bat :app:assembleRelease` (o `./gradlew :app:assembleRelease`) genera `app/build/outputs/apk/release/app-release-unsigned.apk`, que necesita firmarse antes de instalarse. Consulta la [guía oficial de compilación y firma](https://developer.android.com/build/building-cmdline) para configurar una firma por terminal.

### Problemas frecuentes

- **`JAVA_HOME is not set`, ruta de Java inválida o toolchain 17 no encontrada**: instala JDK 17 y revisa `JAVA_HOME`, `PATH` y el Gradle JDK del IDE.
- **`SDK location not found`**: crea `local.properties` con la ruta de tu SDK o configura `ANDROID_HOME`.
- **Falta `android-35`, Build Tools o licencias**: instala los paquetes indicados en el paso 3 y acepta las licencias.
- **`gradlew.bat` no se reconoce en PowerShell**: usa `.\gradlew.bat`, con el prefijo `.\`, desde la raíz del proyecto.
- **`Permission denied` en macOS/Linux**: ejecuta `chmod +x gradlew`.
- **La descarga de Gradle o dependencias falla**: revisa la conexión, el proxy o el cortafuegos; vuelve a ejecutar el comando cuando se resuelva.
- **`INSTALL_FAILED_UPDATE_INCOMPATIBLE` al instalar**: el APK y la app instalada tienen firmas distintas; usa la misma clave de firma.

Los datos y las fotos de platos se guardan en el dispositivo; las preferencias de tema y tamaño del texto usan DataStore. No hay cuenta, red, publicidad ni analítica. La copia de seguridad automática de Android está desactivada.

## Uso sencillo

La app abre directamente el registro de una comida. Al añadir un plato guardado se incorpora al instante y puedes ajustar sus raciones HC. En platos y comidas, las raciones HC manuales opcionales sustituyen el cálculo automático. El historial muestra el detalle completo y permite editar las comidas guardadas. Cada comida admite observaciones e insulina anotada por el usuario. En Ajustes se puede activar «Texto más grande».

## Estructura

- `domain/`: funciones puras para calcular ingredientes, recetas, porciones, peso final y sumas; pruebas unitarias.
- `app/src/main/java/es/racionest1d/data/`: entidades Room, DAO, catálogo de prueba y repositorio.
- `app/src/main/java/es/racionest1d/`: ViewModel, navegación y pantallas Compose Material 3.
- `app/src/test/`: prueba de integración de Room para el historial y la repetición.

## Funciones incluidas

- Catálogo local de 66 alimentos y variantes con los pesos por ración facilitados por el usuario, búsqueda y favoritos. La actualización sustituye las entradas precargadas anteriores.
- Ingredientes personalizados de etiqueta, estimados o pendientes, por 100 g/ml o peso por ración. Las variantes crudas y cocinadas tienen factores separados.
- Calculadora de ingrediente y platos con pesos editables, cálculo automático de hidratos y raciones manuales opcionales para platos y comidas.
- Biblioteca de platos con búsqueda, favoritos, duplicación, edición y eliminación confirmada.
- Foto opcional tomada con la cámara, guardada localmente y visible en las tarjetas y selectores de platos.
- Comidas con platos e ingredientes; raciones HC ajustables y sobrescribibles, observaciones, registro manual de insulina e historial editable.
- Repetición con los valores históricos o con los actuales y diferencias visibles. Los registros guardados conservan sus subtotales originales.
- Tema claro y oscuro.

Los datos nutricionales desconocidos se muestran como pendientes y propagan un total desconocido. No se convierten automáticamente pesos crudos en cocinados.

## Catálogo de prueba

Las 66 entradas precargadas proceden de la lista facilitada por el usuario el 04/10/2026. Cada peso indicado equivale a una ración de 10 g de hidratos de carbono. La app conserva `g` como unidad, también para la leche, porque la lista se expresa como peso. Solo se indica el estado crudo o cocido cuando la lista lo especifica.

Al instalar esta versión sobre una anterior, se eliminan las entradas precargadas antiguas y se insertan las nuevas. Los alimentos personalizados, platos y comidas se conservan. Los platos y comidas creados con datos anteriores mantienen sus valores históricos; conviene revisar esos platos y sustituir sus ingredientes por los del catálogo nuevo antes de volver a usarlos.

## Límites conocidos

- No se ha ejecutado la interfaz en un dispositivo físico ni un emulador; sí se han compilado el APK y las pruebas locales.
- La foto requiere una aplicación de cámara en el móvil. No se ha probado físicamente la captura en un dispositivo real.
- La exportación/importación JSON, escaneo de etiquetas y códigos de barras, sincronización e integración con sensores quedan para una versión posterior. La base de datos separa las entidades y sus copias históricas para facilitar una exportación versionada.
- Las equivalencias facilitadas son genéricas: contrástalas con el alimento, la etiqueta comercial y su estado de preparación reales.
- La versión 0.8.0 migró la base de datos para añadir insulina por comida y convertir la cantidad guardada de los platos al plato completo. El historial conserva sus cálculos guardados.
- La versión 0.10.0 añade raciones HC manuales para platos y comidas; los campos son opcionales y sustituyen el total automático cuando se rellenan.
