# KHome

Launcher (pantalla de inicio) para Android TV / Google TV con estética **glassmorphism**: paneles de cristal translúcidos, bordes de luz y fondo con halos de color. Nativo en Kotlin + Jetpack Compose for TV.

## Funciones (v0.1)

- Rejilla con todas tus apps (banner de TV si lo tienen; si no, el icono), ordenadas por nombre.
- **Dock de favoritas** en la parte inferior.
- **Menú con pulsación larga** (OK mantenido): abrir, añadir/quitar de favoritas, mover favoritas, ocultar, información de la app y desinstalar.
- Reloj y fecha, indicador de Wi-Fi / Ethernet y botón de ajustes.
- Ajustes: apps por fila (5–8), reloj 12/24 h, gestión de apps ocultas, acceso a los ajustes del sistema y al selector de pantalla de inicio.
- Se actualiza solo al instalar o desinstalar apps.
- El botón Atrás no cierra el launcher; pulsar Home estando en KHome vuelve arriba.

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

## Estructura

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
