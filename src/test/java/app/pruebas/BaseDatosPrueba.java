package app.pruebas;

import app.dao.ProductoDAO;
import app.dao.ProveedorDAO;
import app.dao.UsuarioDAO;
import app.db.InicializarDB;
import app.model.Producto;
import app.model.Proveedor;
import app.model.Usuario;
import app.util.Rutas;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.math.BigDecimal;

/**
 * Base común de las pruebas: antes de cada caso deja una base de datos nueva y vacía
 * en la carpeta de datos de prueba (target/datos-prueba), tal como define el plan de pruebas.
 */
public abstract class BaseDatosPrueba {

    protected final UsuarioDAO usuarioDAO = new UsuarioDAO();
    protected final ProductoDAO productoDAO = new ProductoDAO();
    protected final ProveedorDAO proveedorDAO = new ProveedorDAO();

    @BeforeEach
    void baseNueva() {
        File db = Rutas.archivo("comerciocontrol.db");
        if (db.exists() && !db.delete()) throw new IllegalStateException("No se pudo reiniciar la BD de prueba");
        InicializarDB.inicializar();
    }

    protected Usuario crearUsuario(String nombre, String usuario, String clave, Usuario.Rol rol, boolean activo) {
        Usuario u = new Usuario(0, nombre, usuario, UsuarioDAO.md5(clave), rol, activo);
        usuarioDAO.guardar(u);
        return u;
    }

    protected Usuario admin() {
        return crearUsuario("Administrador Prueba", "admin", "Admin123*", Usuario.Rol.administrador, true);
    }

    protected Producto crearProducto(String codigo, String nombre, String precio, int stock, int minimo, Integer proveedorId) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setCategoria("Abarrotes");
        p.setPrecioCosto(new BigDecimal(precio).multiply(new BigDecimal("0.7")));
        p.setPrecioVenta(new BigDecimal(precio));
        p.setStockActual(stock);
        p.setStockMinimo(minimo);
        p.setProveedorId(proveedorId);
        productoDAO.guardar(p);
        return p;
    }

    protected Proveedor crearProveedor(String nit, String razonSocial) {
        Proveedor pr = new Proveedor();
        pr.setNit(nit);
        pr.setRazonSocial(razonSocial);
        pr.setActivo(true);
        proveedorDAO.guardar(pr);
        return pr;
    }
}
