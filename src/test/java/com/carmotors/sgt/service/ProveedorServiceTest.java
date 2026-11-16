package com.carmotors.sgt.service;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.repository.ProveedorDAO;

class ProveedorServiceTest {

    @Test
    void crearProveedorConNombreVacioDebeLanzarExcepcion() {
        ProveedorService service = new ProveedorService(new ProveedorDAOMock());
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(" ");
        proveedor.setEmail("proveedor@dominio.com");

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crearProveedor(proveedor));

        assertTrue(ex.getMessage().toLowerCase().contains("nombre"));
    }

    @Test
    void crearProveedorConEmailInvalidoDebeLanzarExcepcion() {
        ProveedorService service = new ProveedorService(new ProveedorDAOMock());
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre("Distribuidora Uno");
        proveedor.setEmail("correo-no-valido");

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crearProveedor(proveedor));

        assertTrue(ex.getMessage().toLowerCase().contains("email"));
    }

    @Test
    void desactivarProveedorDebeMarcarActivoComoFalse() {
        ProveedorDAOMock dao = new ProveedorDAOMock();
        ProveedorService service = new ProveedorService(dao);
        Proveedor proveedor = new Proveedor(1, "Distribuidora Uno", "3001234567", "ventas@dominio.com", "Calle 1", true);
        dao.getDatos().add(proveedor);

        service.desactivarProveedor(1);

        assertFalse(dao.getDatos().get(0).isActivo());
    }

    private static class ProveedorDAOMock implements ProveedorDAO {
        private final List<Proveedor> datos = new ArrayList<>();

        @Override
        public List<Proveedor> listarTodos() {
            return datos;
        }

        @Override
        public List<Proveedor> listarActivos() {
            List<Proveedor> activos = new ArrayList<>();
            for (Proveedor proveedor : datos) {
                if (proveedor.isActivo()) {
                    activos.add(proveedor);
                }
            }
            return activos;
        }

        @Override
        public Proveedor buscarPorId(int id) {
            for (Proveedor proveedor : datos) {
                if (proveedor.getId() == id) {
                    return proveedor;
                }
            }
            return null;
        }

        @Override
        public Proveedor buscarPorEmail(String email) {
            for (Proveedor proveedor : datos) {
                if (proveedor.getEmail() != null && proveedor.getEmail().equalsIgnoreCase(email)) {
                    return proveedor;
                }
            }
            return null;
        }

        @Override
        public int guardar(Proveedor proveedor) {
            if (proveedor.getId() == 0) {
                proveedor.setId(datos.size() + 1);
            }
            for (int i = 0; i < datos.size(); i++) {
                if (datos.get(i).getId() == proveedor.getId()) {
                    datos.remove(i);
                    break;
                }
            }
            datos.add(proveedor);
            return proveedor.getId();
        }

        @Override
        public boolean actualizar(Proveedor proveedor) {
            return guardar(proveedor) > 0;
        }

        @Override
        public boolean desactivar(int id) {
            Proveedor proveedor = buscarPorId(id);
            if (proveedor == null) {
                return false;
            }
            proveedor.setActivo(false);
            return true;
        }

        @Override
        public boolean reactivar(int id) {
            Proveedor proveedor = buscarPorId(id);
            if (proveedor == null) {
                return false;
            }
            proveedor.setActivo(true);
            return true;
        }

        public List<Proveedor> getDatos() {
            return datos;
        }
    }
}
