package app.db;

import app.dao.UsuarioDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class InicializarDB {

    public static void inicializar() {
        try (Connection conn = ConexionSQLite.conectar();
             Statement stmt = conn.createStatement()) {
            crearTablas(stmt);
            aplicarMigraciones(stmt);
            insertarUsuariosDefault(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Error al inicializar la base de datos: " + e.getMessage(), e);
        }
    }

    private static void aplicarMigraciones(Statement stmt) {
        try { stmt.executeUpdate("ALTER TABLE ventas ADD COLUMN cliente_id INTEGER REFERENCES clientes(id) ON DELETE SET NULL"); }
        catch (SQLException ignored) {}
        try { stmt.executeUpdate("ALTER TABLE usuarios ADD COLUMN cedula TEXT"); }
        catch (SQLException ignored) {}
        try { stmt.executeUpdate("ALTER TABLE usuarios ADD COLUMN fecha_nacimiento TEXT"); }
        catch (SQLException ignored) {}
    }

    private static void crearTablas(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS clientes (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre    TEXT NOT NULL,
                cedula    TEXT UNIQUE,
                telefono  TEXT,
                email     TEXT,
                direccion TEXT,
                activo    INTEGER NOT NULL DEFAULT 1
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS proveedores (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                nit          TEXT NOT NULL UNIQUE,
                razon_social TEXT NOT NULL,
                contacto     TEXT,
                telefono     TEXT,
                email        TEXT,
                activo       INTEGER NOT NULL DEFAULT 1
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS usuarios (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre        TEXT NOT NULL,
                usuario       TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                rol           TEXT NOT NULL CHECK(rol IN ('administrador','vendedor','bodeguero')),
                activo        INTEGER NOT NULL DEFAULT 1
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS productos (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                codigo       TEXT NOT NULL UNIQUE,
                nombre       TEXT NOT NULL,
                categoria    TEXT,
                precio_costo REAL NOT NULL DEFAULT 0,
                precio_venta REAL NOT NULL DEFAULT 0,
                stock_actual INTEGER NOT NULL DEFAULT 0,
                stock_minimo INTEGER NOT NULL DEFAULT 0,
                proveedor_id INTEGER,
                FOREIGN KEY (proveedor_id) REFERENCES proveedores(id)
                    ON DELETE SET NULL ON UPDATE CASCADE
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS ventas (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha      TEXT NOT NULL DEFAULT (datetime('now','localtime')),
                total      REAL NOT NULL DEFAULT 0,
                descuento  REAL NOT NULL DEFAULT 0,
                iva        REAL NOT NULL DEFAULT 0,
                usuario_id INTEGER NOT NULL,
                cliente_id INTEGER,
                FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
                FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE SET NULL ON UPDATE CASCADE
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS detalle_ventas (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                venta_id        INTEGER NOT NULL,
                producto_id     INTEGER NOT NULL,
                cantidad        INTEGER NOT NULL,
                precio_unitario REAL NOT NULL,
                FOREIGN KEY (venta_id)    REFERENCES ventas(id)    ON DELETE CASCADE ON UPDATE CASCADE,
                FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE RESTRICT ON UPDATE CASCADE
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS movimientos (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                producto_id      INTEGER NOT NULL,
                tipo             TEXT NOT NULL CHECK(tipo IN ('entrada','salida','ajuste')),
                cantidad         INTEGER NOT NULL,
                stock_resultante INTEGER NOT NULL,
                fecha            TEXT NOT NULL DEFAULT (datetime('now','localtime')),
                usuario_id       INTEGER NOT NULL,
                observacion      TEXT,
                FOREIGN KEY (producto_id) REFERENCES productos(id)
                    ON DELETE RESTRICT ON UPDATE CASCADE,
                FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
                    ON DELETE RESTRICT ON UPDATE CASCADE
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS caja (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha      TEXT NOT NULL,
                apertura   REAL NOT NULL DEFAULT 0,
                cierre     REAL NOT NULL DEFAULT 0,
                ventas_dia REAL NOT NULL DEFAULT 0,
                diferencia REAL NOT NULL DEFAULT 0,
                observacion TEXT,
                usuario_id INTEGER NOT NULL,
                estado     TEXT NOT NULL DEFAULT 'abierta' CHECK(estado IN ('abierta','cerrada')),
                FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS auditoria (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id     INTEGER NOT NULL,
                accion         TEXT NOT NULL,
                tabla_affected TEXT,
                fecha          TEXT NOT NULL DEFAULT (datetime('now','localtime')),
                detalle        TEXT,
                FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
                    ON DELETE RESTRICT ON UPDATE CASCADE
            )
        """);
    }

    private static void insertarUsuariosDefault(Statement stmt) throws SQLException {
        // Sin usuarios por defecto. El primer usuario se crea desde el formulario de registro.
    }
}
