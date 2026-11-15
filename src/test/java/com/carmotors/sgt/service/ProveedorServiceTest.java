package com.carmotors.sgt.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.repository.ProveedorDAO;

class ProveedorServiceTest {

    @Test
    void crearProveedorConNombreVacioDebeLanzarExcepcion() {
        ProveedorService service = new ProveedorService(new ProveedorDAOMock());
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(" ");
        proveedor.setEmail("proveedor@dominio.com");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.crearProveedor(proveedor));

        assertTrue(ex.getMessage().toLowerCase().contains("nombre"));
    }

    @Test
    void crearProveedorConEmailInvalidoDebeLanzarExcepcion() {
        ProveedorService service = new ProveedorService(new ProveedorDAOMock());
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre("Distribuidora Uno");
        proveedor.setEmail("correo-no-valido");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
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
            return datos.stream().filter(Proveedor::isActivo).toList();
        }

        @Override
        public Optional<Proveedor> buscarPorId(int id) {
            return datos.stream().filter(p -> p.getId() == id).findFirst();
        }

        @Override
        public Optional<Proveedor> buscarPorEmail(String email) {
            return datos.stream().filter(p -> p.getEmail().equalsIgnoreCase(email)).findFirst();
        }

        @Override
        public int guardar(Proveedor proveedor) {
            if (proveedor.getId() == 0) {
                proveedor.setId(datos.size() + 1);
            }
            datos.removeIf(p -> p.getId() == proveedor.getId());
            datos.add(proveedor);
            return proveedor.getId();
        }

        @Override
        public boolean actualizar(Proveedor proveedor) {
            return guardar(proveedor) > 0;
        }

        @Override
        public boolean desactivar(int id) {
            return datos.stream().filter(p -> p.getId() == id).findFirst().map(p -> {
                p.setActivo(false);
                return true;
            }).orElse(false);
        }

        @Override
        public boolean reactivar(int id) {
            return datos.stream().filter(p -> p.getId() == id).findFirst().map(p -> {
                p.setActivo(true);
                return true;
            }).orElse(false);
        }

        public List<Proveedor> getDatos() {
            return datos;
        }
    }
}
