package com.carmotors.sgt.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Lote;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.repository.LoteDAO;
import com.carmotors.sgt.repository.ProveedorDAO;
import com.carmotors.sgt.repository.ProveedorRepuestoDAO;

class LoteServiceTest {

    private LoteService service;
    private LoteDAOMock loteDAO;
    private ProveedorDAOMock proveedorDAO;
    private ProveedorRepuestoDAOMock proveedorRepuestoDAO;

    @BeforeEach
    void setUp() {
        loteDAO = new LoteDAOMock();
        proveedorDAO = new ProveedorDAOMock();
        proveedorDAO.agregar(new Proveedor(1, "Importadora Sur", "5712345678", "ventas@importadora.com", "Calle 5", true));
        proveedorDAO.agregar(new Proveedor(2, "Baterías Centro", "5712345679", "baterias@centro.com", "Calle 7", false));

        proveedorRepuestoDAO = new ProveedorRepuestoDAOMock();
        proveedorRepuestoDAO.agregar(new ProveedorRepuesto(1, 10, java.math.BigDecimal.valueOf(55000), 4, true));

        service = new LoteService(loteDAO, proveedorDAO, proveedorRepuestoDAO);
    }

    @Test
    @DisplayName("Debe registrar un lote válido con cantidad inicial igual a actual")
    void registrarLoteValido() {
        Lote lote = new Lote();
        lote.setCodigoLote("LOT-TEST-001");
        lote.setProveedorId(1);
        lote.setRepuestoId(10);
        lote.setFechaIngreso(LocalDate.of(2026, 11, 1));
        lote.setFechaCaducidad(LocalDate.of(2027, 11, 1));
        lote.setCantidadInicial(25);

        Lote guardado = service.registrarLote(lote);

        assertNotNull(guardado);
        assertEquals(1, guardado.getId());
        assertEquals(25, guardado.getCantidadInicial());
        assertEquals(25, guardado.getCantidadActual());
    }

    @Test
    @DisplayName("Debe rechazar un código de lote duplicado")
    void rechazarCodigoDuplicado() {
        loteDAO.agregar(new Lote(1, "LOT-TEST-001", 1, 10, LocalDate.of(2026, 10, 1), LocalDate.of(2027, 10, 1), 10, 10));

        Lote lote = new Lote();
        lote.setCodigoLote("LOT-TEST-001");
        lote.setProveedorId(1);
        lote.setRepuestoId(10);
        lote.setFechaIngreso(LocalDate.of(2026, 11, 1));
        lote.setFechaCaducidad(LocalDate.of(2027, 11, 1));
        lote.setCantidadInicial(12);

        assertThrows(DatoDuplicadoException.class, () -> service.registrarLote(lote));
    }

    @Test
    @DisplayName("Debe rechazar una cantidad inicial menor o igual a cero")
    void rechazarCantidadInicialInvalida() {
        Lote lote = new Lote();
        lote.setCodigoLote("LOT-TEST-002");
        lote.setProveedorId(1);
        lote.setRepuestoId(10);
        lote.setFechaIngreso(LocalDate.of(2026, 11, 1));
        lote.setFechaCaducidad(LocalDate.of(2027, 11, 1));
        lote.setCantidadInicial(0);

        assertThrows(ValidacionException.class, () -> service.registrarLote(lote));
    }

    @Test
    @DisplayName("Debe rechazar caducidad que no sea estrictamente posterior a la fecha de ingreso")
    void rechazarCaducidadInvalida() {
        Lote lote = new Lote();
        lote.setCodigoLote("LOT-TEST-003");
        lote.setProveedorId(1);
        lote.setRepuestoId(10);
        lote.setFechaIngreso(LocalDate.of(2026, 11, 1));
        lote.setFechaCaducidad(LocalDate.of(2026, 11, 1));
        lote.setCantidadInicial(8);

        assertThrows(ValidacionException.class, () -> service.registrarLote(lote));
    }

    @Test
    @DisplayName("Debe rechazar proveedor no asociado al repuesto o inactivo")
    void rechazarProveedorInvalido() {
        Lote lote = new Lote();
        lote.setCodigoLote("LOT-TEST-004");
        lote.setProveedorId(2);
        lote.setRepuestoId(10);
        lote.setFechaIngreso(LocalDate.of(2026, 11, 1));
        lote.setCantidadInicial(8);

        assertThrows(ValidacionException.class, () -> service.registrarLote(lote));
    }

    @Test
    @DisplayName("Debe rechazar un proveedor inexistente")
    void rechazarProveedorInexistente() {
        Lote lote = new Lote();
        lote.setCodigoLote("LOT-TEST-005");
        lote.setProveedorId(99);
        lote.setRepuestoId(10);
        lote.setFechaIngreso(LocalDate.of(2026, 11, 1));
        lote.setCantidadInicial(8);

        assertThrows(EntidadNoEncontradaException.class, () -> service.registrarLote(lote));
    }

    private static class LoteDAOMock implements LoteDAO {
        private final List<Lote> datos = new ArrayList<>();
        private int nextId = 1;

        void agregar(Lote lote) {
            lote.setId(nextId++);
            datos.add(lote);
        }

        @Override
        public Lote buscarPorCodigo(String codigo) {
            for (Lote lote : datos) {
                if (lote.getCodigoLote().equalsIgnoreCase(codigo)) {
                    return lote;
                }
            }
            return null;
        }

        @Override
        public int guardar(Lote lote) {
            lote.setId(nextId++);
            datos.add(lote);
            return lote.getId();
        }
    }

    private static class ProveedorDAOMock implements ProveedorDAO {
        private final List<Proveedor> datos = new ArrayList<>();

        void agregar(Proveedor proveedor) {
            datos.add(proveedor);
        }

        @Override
        public List<Proveedor> listarTodos() {
            return new ArrayList<>(datos);
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
            return null;
        }

        @Override
        public int guardar(Proveedor proveedor) {
            return proveedor.getId();
        }

        @Override
        public boolean actualizar(Proveedor proveedor) {
            return true;
        }

        @Override
        public boolean desactivar(int id) {
            return true;
        }

        @Override
        public boolean reactivar(int id) {
            return true;
        }
    }

    private static class ProveedorRepuestoDAOMock implements ProveedorRepuestoDAO {
        private final List<ProveedorRepuesto> datos = new ArrayList<>();

        void agregar(ProveedorRepuesto proveedorRepuesto) {
            datos.add(proveedorRepuesto);
        }

        @Override
        public List<ProveedorRepuesto> listarPorProveedor(int proveedorId) {
            return new ArrayList<>(datos);
        }

        @Override
        public List<ProveedorRepuesto> listarPorRepuesto(int repuestoId) {
            return new ArrayList<>(datos);
        }

        @Override
        public ProveedorRepuesto buscarPorProveedorYRepuesto(int proveedorId, int repuestoId) {
            for (ProveedorRepuesto item : datos) {
                if (item.getProveedorId() == proveedorId && item.getRepuestoId() == repuestoId) {
                    return item;
                }
            }
            return null;
        }

        @Override
        public boolean guardar(ProveedorRepuesto proveedorRepuesto) {
            return datos.add(proveedorRepuesto);
        }

        @Override
        public boolean actualizarCondiciones(ProveedorRepuesto proveedorRepuesto) {
            return true;
        }

        @Override
        public boolean eliminar(int proveedorId, int repuestoId) {
            return true;
        }

        @Override
        public int desmarcarHabitual(int repuestoId) {
            return 0;
        }

        @Override
        public boolean marcarHabitual(int proveedorId, int repuestoId) {
            return true;
        }
    }
}
