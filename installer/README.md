# Cómo generar el instalador (.exe)

El instalador empaqueta la app + un runtime de Java embebido en un solo `.exe`
(el cliente **no** necesita tener Java). Incluye una página para ingresar la
clave de licencia Pro (opcional) durante la instalación.

## Requisitos (una sola vez)
- JDK 17 (para `jpackage`).
- [Inno Setup 6](https://jrsoftware.org/isdl.php) (para el instalador).
  Instalación rápida: `winget install JRSoftware.InnoSetup`

## Pasos

1. Generar la app-image con jpackage (desde la raíz del proyecto):
   ```bash
   mvn clean package -DskipTests
   mvn dependency:copy-dependencies -DoutputDirectory=target/app-input
   cp target/Comercio-ControlV1-1.0-SNAPSHOT.jar target/app-input/
   jpackage --type app-image --name ComercioControl ^
     --input target/app-input ^
     --main-jar Comercio-ControlV1-1.0-SNAPSHOT.jar ^
     --main-class app.Launcher ^
     --icon src/main/resources/img/icono.ico ^
     --app-version 1.0 --dest target/dist
   ```

2. Compilar el instalador con Inno Setup:
   ```bash
   ISCC installer/ComercioControl.iss
   ```

3. El instalador queda en `installer/Output/ComercioControl-Setup-1.0.exe`.

## Notas
- Los datos del negocio (base de datos, recibos, logo, licencia) se guardan en
  `%APPDATA%\ComercioControl`, así la app funciona aunque se instale en
  `C:\Program Files`.
- Si el cliente escribe el titular y la clave en la página de activación del
  instalador, se crea `%APPDATA%\ComercioControl\activacion.properties` y la app
  activa la versión Pro automáticamente en el primer arranque.
