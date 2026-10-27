# KHome

Launcher (pantalla de inicio) para Android TV / Google TV, nativo en Kotlin + Jetpack Compose for TV. Diseño limpio: tiempo y reloj arriba, carrusel de apps en el centro y botones redondos abajo.

## Funciones

- **Carrusel de apps** con tarjetas de icono (3 a 6 por pantalla), borde dorado al enfocar y nombre solo en la tarjeta enfocada. Puntos de página.
- **Elegir qué apps se ven** en el escritorio: Ajustes → Apps del escritorio.
- **Pulsación larga** (OK mantenido) en una app: mover, ocultar, cerrar, información y desinstalar. El orden se guarda.
- **Tiempo** (Open-Meteo, sin clave): temperatura, estado, máx/mín, lluvia, viento, humedad y los próximos días. Elige tu ciudad en Ajustes → Tiempo.
- **Novedades por plataforma** (Netflix, Prime Video, Disney+, Max, Apple TV+, Movistar Plus+, SkyShowtime): una fila de carteles por cada plataforma que tengas instalada. Al elegir un cartel se abre la app de la plataforma (no el título). Usa TMDB y necesita una clave (ver abajo).
- Reloj grande con fecha (12/24 h).
- Botones inferiores: entradas HDMI, estado de red (Ethernet/Wi-Fi/VPN) y ajustes. Estilo plano o con relieve.
- Temas claro / oscuro / sistema, 10 fondos y 6 colores de acento; tarjetas con icono o banner.
- **Actualizador integrado:** detecta nuevas releases de GitHub y se instala desde la propia app.
- El botón Atrás no cierra el launcher; pulsar Home estando en KHome vuelve al inicio.

Nota: el botón de entradas depende de que la tele exponga sus entradas HDMI al sistema; en algunos modelos puede no funcionar.

## Novedades: clave de TMDB

Las filas de novedades usan la API gratuita de [TMDB](https://www.themoviedb.org). Crea una clave en themoviedb.org (Ajustes → API) y guárdala en el repositorio como secreto de Actions con el nombre `TMDB_API_KEY` (Settings → Secrets and variables → Actions). Los workflows la pasan al compilar. Sin clave, KHome funciona igual pero sin esas filas.

Este producto usa la API de TMDB pero no está respaldado ni certificado por TMDB.

## Compilar

Con Android Studio: abre la carpeta del proyecto y ejecuta `app`.

Por línea de comandos (JDK 17 y Android SDK 34):

```
gradle assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. Además, el workflow de GitHub Actions (`.github/workflows/build.yml`) lo compila en cada push y lo deja como artefacto descargable.

## Instalar en la tele (Haier Google TV)

1. En la tele: Ajustes → Sistema → Acerca de → pulsa varias veces sobre "Compilación" para activar opciones de desarrollador, y activa **Depuración por USB/ADB** (o "Depuración inalámbrica").
2. Desde el PC:

```
adb connect IP_DE_LA_TELE:5555
adb install -r app-debug.apk
```

KHome aparecerá como app. Ábrela manualmente la primera vez para probarla.

## Hacerlo el Home real

En Google TV el sistema suele abrir siempre su launcher, así que hay que desactivarlo. Instala KHome **antes** de hacerlo:

```
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
```

En versiones antiguas de Android TV el paquete es `com.google.android.tvlauncher`.

Para volver al launcher de Google:

```
adb shell pm enable com.google.android.apps.tv.launcherx
```

Alternativa sin desactivar nada: abre KHome a mano o remapea un botón del mando con una app como TVQuickActions.

## Publicar una release

El workflow `Release` compila el APK firmado y lo adjunta a una Release de GitHub. Hay dos formas de lanzarlo:

- **Desde la web de GitHub, sin tocar nada en local:** Actions → *Release* → *Run workflow* → escribe la versión (por ejemplo `0.2.0`).
- **Con un tag:** al subir un tag con formato `vX.Y.Z`.

```
git tag v0.2.0
git push origin v0.2.0
```

La versión interna se calcula a partir del número (`0.2.0` → código `200`), así que cada release debe ser mayor que la anterior.

## Actualizaciones dentro de la app

- Al abrir KHome consulta en segundo plano la última Release del repo. Si hay una versión más nueva, aparece un botón **Actualización X.Y.Z** junto al reloj.
- En **Ajustes → Actualizaciones** puedes buscar a mano, ver la versión instalada y pulsar **Descargar e instalar**.
- La descarga es solo por HTTPS desde GitHub y se verifica con el SHA-256 de la release cuando GitHub lo facilita. Después, el instalador del sistema pide confirmación y actualiza conservando tus datos.
- La primera vez, Android te pedirá permitir a KHome instalar apps desconocidas.

### Firma

Para que una actualización se instale sobre la anterior, todos los APK deben ir firmados con la misma clave. Por defecto se usa `app/debug.keystore`, incluida en el repo. Es cómoda, pero al ser pública cualquiera podría firmar un APK con la misma clave. Si quieres evitarlo, crea tu propia keystore y guarda estos secrets en el repo (Settings → Secrets and variables → Actions): `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` y `KEY_PASSWORD`. El workflow de release los usará solos. Ojo: cambiar la clave una vez publicada una versión obliga a desinstalar e instalar de nuevo.

## Estructura

- `update/`: comprobación de versión, descarga con verificación e instalación.
- `data/`: lectura de apps instaladas y preferencias (DataStore).
- `HomeViewModel.kt`: estado, favoritas, ocultas y refresco al instalar/desinstalar.
- `ui/Theme.kt`: paleta y utilidades de cristal (`Modifier.glass`, `GlassBackground`).
- `ui/Components.kt`: tarjeta de app, botones, menú contextual y barra superior.
- `ui/HomeScreen.kt` y `ui/SettingsScreen.kt`: pantallas.

## Ideas siguientes

- Fondos de pantalla personalizados y presentación de fotos.
- Fila de MyIPTV (canales o últimos vistos) en el Home.
- Carpetas de apps, packs de iconos y colores personalizables.
- Copia y restauración de la configuración.
