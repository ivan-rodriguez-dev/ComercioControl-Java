<div align="center">

<img src="src/main/resources/img/icono.png" alt="ComercioControl" width="120"/>

# ComercioControl

Sistema de **punto de venta e inventario** para comercio minorista.

![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-17-1f8acb)
![SQLite](https://img.shields.io/badge/SQLite-3-003B57?logo=sqlite&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-build-C71A36?logo=apachemaven&logoColor=white)

</div>

---

Aplicación de escritorio para gestionar productos, ventas, clientes, proveedores,
movimientos de stock, caja diaria, reportes y usuarios con **control de acceso por rol**.
Construida con **JavaFX 17** y **SQLite** (sin servidor: la base de datos es un archivo local).

## ✨ Funcionalidades

- 🔐 **Autenticación** con registro del primer usuario (administrador) e inicio de sesión.
- 👥 **Roles** con módulos restringidos:
  | Rol | Acceso |
  |-----|--------|
  | `administrador` | Todo + gestión de usuarios y auditoría |
  | `vendedor` | Dashboard, Ventas (POS), Clientes y Caja |
  | `bodeguero` | Dashboard, Inventario, Movimientos y Proveedores |
- 📊 **Dashboard** con indicadores (KPIs) y gráficos.
- 📦 **Inventario**: productos, categorías, stock mínimo y alertas de stock crítico.
- 🛒 **Punto de venta (POS)**: carrito, descuento, IVA (19 %), selección de cliente y
  **recibo en PDF**. Exige una caja abierta para registrar ventas.
- 🧾 **Clientes** y **Proveedores**: gestión CRUD.
- 🔄 **Movimientos** de stock (entradas, salidas, ajustes).
- 💵 **Caja**: apertura y cierre diario con cálculo de diferencia
  `efectivo contado − (apertura + ventas del día)`.
- 📈 **Reportes** (ventas, más vendidos, inventario, stock crítico) con exportación a
  **PDF** y **Excel**.
- 🕵️ **Auditoría** de acciones (solo administrador).

## 🛠️ Tecnologías

| Componente | Uso |
|-----------|-----|
| JavaFX 17 | Interfaz de usuario (FXML + CSS) |
| SQLite (JDBC) | Persistencia local en archivo |
| iText 5 | Exportación a PDF (recibos y reportes) |
| Apache POI | Exportación a Excel |
| Maven | Construcción y dependencias |

## 🚀 Requisitos

- **Java JDK 17** o superior
- **Maven 3.8+**

Las dependencias se descargan automáticamente vía Maven.

## ▶️ Cómo ejecutar

```bash
mvn clean javafx:run
```

La base de datos `comerciocontrol.db` se crea sola en el directorio de ejecución la primera
vez. Al no existir usuarios, la app abre el formulario de **registro** para crear el primer
administrador.

### Desde IntelliJ IDEA

- **Opción rápida:** ejecutar `app.Launcher` (clic derecho → *Run 'Launcher.main()'*).
  `Launcher` evita el error *"JavaFX runtime components are missing"* que aparece al
  ejecutar una clase `Application` directamente.
- **Opción Maven:** panel *Maven* → *Plugins* → `javafx` → `javafx:run`.

## 📁 Estructura

```
src/main/java/app/
├── Launcher.java             # Entrada para IDE (no extiende Application)
├── Main.java                 # Aplicación JavaFX
├── controller/               # Controladores de cada vista
├── dao/                      # Acceso a datos (JDBC / SQLite)
├── db/                       # Conexión e inicialización de la BD
├── model/                    # Entidades del dominio
└── util/                     # Sesión, formato, generación de recibos PDF

src/main/resources/
├── fxml/                     # Vistas
├── css/styles.css            # Estilos
└── img/                      # Íconos de la app (icono.png + tamaños + icono.ico)
```

## 📦 Empaquetado como ejecutable (.exe)

Para distribuir la app en Windows con **jpackage** (incluido en el JDK):

1. Copiar las dependencias y generar el JAR:
   ```bash
   mvn clean package dependency:copy-dependencies -DoutputDirectory=target/lib
   ```
2. Crear la imagen/instalador apuntando al ícono `.ico` incluido:
   ```bash
   jpackage --type app-image ^
     --name ComercioControl ^
     --input target ^
     --main-jar Comercio-ControlV1-1.0-SNAPSHOT.jar ^
     --main-class app.Launcher ^
     --icon src/main/resources/img/icono.ico
   ```
   > Usar `--type msi` o `--type exe` (requiere WiX Toolset) para generar un instalador.
   > Asegúrate de que las dependencias estén disponibles para el JAR (p. ej. en `target/lib`).

## ⚠️ Notas

- Las contraseñas se almacenan con hash **MD5** (entorno local de tienda). Para mayor
  seguridad se recomienda migrar a SHA-256 con sal o BCrypt.
- Los recibos PDF generados se guardan en `recibos/` (ignorado por git).
