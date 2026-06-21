# ComercioControl

Sistema de gestión para comercio minorista (punto de venta e inventario) construido con
**JavaFX 17** y **SQLite**. Permite administrar productos, ventas, clientes, proveedores,
movimientos de stock, caja diaria, reportes y usuarios con control de acceso por rol.

---

## Funcionalidades

- **Autenticación** con registro del primer usuario (administrador) y login.
- **Roles de usuario** con módulos restringidos:
  - `administrador`: acceso total + gestión de usuarios y auditoría.
  - `vendedor`: Dashboard, Ventas (POS), Clientes y Caja.
  - `bodeguero`: Dashboard, Inventario, Movimientos y Proveedores.
- **Dashboard** con indicadores (KPIs) y gráficos.
- **Inventario**: alta/edición de productos, categorías, stock mínimo y alertas.
- **Punto de venta (POS)**: carrito, descuento, IVA (19 %), selección de cliente y
  **generación de recibo en PDF**. Requiere una caja abierta para registrar ventas.
- **Clientes** y **Proveedores**: gestión CRUD.
- **Movimientos** de stock (entradas, salidas, ajustes).
- **Caja**: apertura y cierre diario con cálculo de diferencia
  (`efectivo contado − (apertura + ventas del día)`).
- **Reportes** (ventas, más vendidos, inventario, stock crítico) con exportación a
  **PDF** e **Excel**.
- **Auditoría** de acciones (solo administrador).

---

## Requisitos

- **Java JDK 17** o superior.
- **Maven 3.8+**.

Las dependencias (JavaFX, SQLite JDBC, iText, Apache POI) se descargan automáticamente
vía Maven.

---

## Cómo ejecutar

Desde la raíz del proyecto:

```bash
mvn clean javafx:run
```

La base de datos `comerciocontrol.db` se crea automáticamente en el directorio de
ejecución la primera vez. Al no existir usuarios, la app abre el formulario de **registro**
para crear el primer administrador.

### Compilar sin ejecutar

```bash
mvn clean compile
```

---

## Estructura del proyecto

```
src/main/java/app/
├── Main.java                 # Punto de entrada (JavaFX Application)
├── controller/               # Controladores de cada vista (FXML)
├── dao/                      # Acceso a datos (JDBC sobre SQLite)
├── db/                       # Conexión e inicialización de la BD
├── model/                    # Entidades (Producto, Venta, Cliente, ...)
└── util/                     # Utilidades (sesión, formato, recibo PDF)

src/main/resources/
├── fxml/                     # Vistas (interfaz)
├── css/styles.css            # Estilos
└── img/                      # Ícono de la aplicación (icono.png)
```

---

## Ícono de la aplicación

Coloca el ícono en `src/main/resources/img/icono.png` (PNG cuadrado, 256×256 o 512×512).
La app lo usa automáticamente como ícono de ventana y barra de tareas.

---

## Empaquetado como ejecutable (.exe) — opcional

Para distribuir la app como instalador en Windows se puede usar **jpackage** (incluido en
el JDK). A grandes rasgos:

1. Generar el JAR con dependencias (configurar `maven-shade-plugin` o `maven-assembly-plugin`).
2. Ejecutar `jpackage` apuntando al JAR, con `--type exe` (o `msi`) y `--icon icono.ico`.

> Para el instalador se requiere un archivo `.ico` (además del `.png` de la ventana).

---

## Notas técnicas

- Las contraseñas se almacenan con hash **MD5** (suficiente para un entorno local de
  tienda; para mayor seguridad se recomienda migrar a SHA-256 con sal o BCrypt).
- Los recibos PDF generados se guardan en la carpeta `recibos/` (ignorada por git).
