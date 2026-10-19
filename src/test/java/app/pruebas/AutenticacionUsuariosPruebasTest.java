package app.pruebas;

import app.dao.UsuarioDAO;
import app.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RF-01 / RF-02 – Autenticación y usuarios")
class AutenticacionUsuariosPruebasTest extends BaseDatosPrueba {

    @Test @DisplayName("CP-001 Inicio de sesión con credenciales válidas")
    void cp001() {
        admin();
        Usuario u = usuarioDAO.autenticar("admin", "Admin123*");
        assertNotNull(u);
        assertEquals("Administrador Prueba", u.getNombre());
        assertEquals(Usuario.Rol.administrador, u.getRol());
    }

    @Test @DisplayName("CP-002 Inicio de sesión con contraseña incorrecta")
    void cp002() {
        admin();
        assertNull(usuarioDAO.autenticar("admin", "Incorrecta1"));
    }

    @Test @DisplayName("CP-003 Inicio de sesión con usuario inexistente")
    void cp003() {
        admin();
        assertNull(usuarioDAO.autenticar("noexiste", "Cualquiera1"));
    }

    @Test @DisplayName("CP-004 Inicio de sesión de usuario inactivo")
    void cp004() {
        admin();
        Usuario v = crearUsuario("Vendedor Uno", "vendedor1", "Vend123*", Usuario.Rol.vendedor, true);
        v.setActivo(false);
        usuarioDAO.actualizar(v);
        assertNull(usuarioDAO.autenticar("vendedor1", "Vend123*"));
    }

    @Test @DisplayName("CP-005 Campos vacíos en el login (capa de datos)")
    void cp005() {
        admin();
        assertNull(usuarioDAO.autenticar("", ""));
    }

    @Test @DisplayName("CP-006 Registro del primer administrador con BD vacía")
    void cp006() {
        assertTrue(usuarioDAO.listarTodos().isEmpty(), "La BD nueva debe iniciar sin usuarios");
        admin();
        assertEquals(1, usuarioDAO.listarTodos().size());
        assertEquals(Usuario.Rol.administrador, usuarioDAO.listarTodos().get(0).getRol());
        assertNotNull(usuarioDAO.autenticar("admin", "Admin123*"));
    }

    @Test @DisplayName("CP-008 Crear usuario con nombre de usuario duplicado")
    void cp008() {
        admin();
        assertTrue(usuarioDAO.existeUsuario("admin"));
        assertThrows(RuntimeException.class,
                () -> crearUsuario("Otro", "admin", "Otra123*", Usuario.Rol.vendedor, true));
        assertEquals(1, usuarioDAO.listarTodos().size());
    }

    @Test @DisplayName("CP-009 Crear usuario con rol vendedor")
    void cp009() {
        admin();
        crearUsuario("Vendedor Uno", "vendedor1", "Vend123*", Usuario.Rol.vendedor, true);
        Usuario v = usuarioDAO.autenticar("vendedor1", "Vend123*");
        assertNotNull(v);
        assertEquals(Usuario.Rol.vendedor, v.getRol());
    }

    @Test @DisplayName("CP-010 Cambiar la contraseña de un usuario")
    void cp010() {
        admin();
        Usuario v = crearUsuario("Vendedor Uno", "vendedor1", "Vend123*", Usuario.Rol.vendedor, true);
        usuarioDAO.actualizarPassword(v.getId(), UsuarioDAO.md5("Vend456*"));
        assertNotNull(usuarioDAO.autenticar("vendedor1", "Vend456*"));
        assertNull(usuarioDAO.autenticar("vendedor1", "Vend123*"));
    }

    @Test @DisplayName("CP-044 Contraseña almacenada como hash")
    void cp044() {
        admin();
        String hash = usuarioDAO.listarTodos().get(0).getPasswordHash();
        assertNotEquals("Admin123*", hash);
        assertEquals(32, hash.length(), "Hash hexadecimal de 32 caracteres (MD5)");
        assertTrue(hash.matches("[0-9a-f]{32}"));
    }
}
