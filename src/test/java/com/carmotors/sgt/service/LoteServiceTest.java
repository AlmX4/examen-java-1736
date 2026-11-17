package com.carmotors.sgt.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Lote;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.repository.LoteDAO;
import com.carmotors.sgt.repository.ProveedorDAO;
import com.carmotors.sgt.repository.ProveedorRepuestoDAO;
import com.carmotors.sgt.repository.RepuestoDAO;

class LoteServiceTest {

    private LoteService service;
    private LoteDAOMock loteDAO;
    private ProveedorDAOMock proveedorDAO;
    private ProveedorRepuestoDAOMock proveedorRepuestoDAO;
    private RepuestoDAOMock repuestoDAO;

    @BeforeEach
    void setUp() {
        loteDAO = new LoteDAOMock();
        proveedorDAO = new ProveedorDAOMock();
        proveedorDAO.agregar(new Proveedor(1, "Importadora Sur", "5712345678", "ventas@importadora.com", "Calle 5", true));
        proveedorDAO.agregar(new Proveedor(2, "Baterías Centro", "5712345679", "baterias@centro.com", "Calle 7", false));

        proveedorRepuestoDAO = new ProveedorRepuestoDAOMock();
        proveedorRepuestoDAO.agregar(new ProveedorRepuesto(1, 10, java.math.BigDecimal.valueOf(55000), 4, true));

        repuestoDAO = new RepuestoDAOMock();
        repuestoDAO.agregar(new Repuesto(10, 1, "Filtro de aire", "Bosch", java.math.BigDecimal.valueOf(45000), 0, 5, true));
        repuestoDAO.agregar(new Repuesto(11, 1, "Bujía", "NGK", java.math.BigDecimal.valueOf(15000), 0, 2, false));

        service = new LoteService(loteDAO, proveedorDAO, proveedorRepuestoDAO, repuestoDAO);
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

    @Test
    @DisplayName("Debe rechazar un repuesto inexistente")
    void rechazarRepuestoInexistente() {
        Lote lote = new Lote();
        lote.setCodigoLote("LOT-TEST-006");
        lote.setProveedorId(1);
        lote.setRepuestoId(99);
        lote.setFechaIngreso(LocalDate.of(2026, 11, 1));
        lote.setCantidadInicial(8);

        assertThrows(EntidadNoEncontradaException.class, () -> service.registrarLote(lote));
    }

    @Test
    @DisplayName("Debe rechazar un repuesto inactivo")
    void rechazarRepuestoInactivo() {
        proveedorRepuestoDAO.agregar(new ProveedorRepuesto(1, 11, java.math.BigDecimal.valueOf(14000), 3, true));

        Lote lote = new Lote();
        lote.setCodigoLote("LOT-TEST-007");
        lote.setProveedorId(1);
        lote.setRepuestoId(11);
        lote.setFechaIngreso(LocalDate.of(2026, 11, 2));
        lote.setCantidadInicial(5);

        assertThrows(ValidacionException.class, () -> service.registrarLote(lote));
    }

    @Test
    @DisplayName("Debe consultar trazabilidad por código sin distinguir mayúsculas")
    void buscarPorCodigo() {
        loteDAO.agregar(new Lote(1, "LOT-TRACE-001", 1, 10,
                LocalDate.of(2026, 11, 1), LocalDate.of(2027, 11, 1), 20, 14));

        Lote encontrado = service.buscarPorCodigo("lot-trace-001");

        assertNotNull(encontrado);
        assertEquals("LOT-TRACE-001", encontrado.getCodigoLote());
        assertEquals(20, encontrado.getCantidadInicial());
        assertEquals(14, encontrado.getCantidadActual());
        assertNull(service.buscarPorCodigo("LOT-INEXISTENTE"));
    }

    @Test
    @DisplayName("Debe listar lotes por caducidad, ingreso e identificador")
    void listarPorRepuestoOrdenado() {
        loteDAO.agregar(new Lote(1, "LOT-SIN-CADUCIDAD", 1, 10,
                LocalDate.of(2026, 1, 1), null, 30, 30));
        loteDAO.agregar(new Lote(2, "LOT-CADUCIDAD-TARDIA", 1, 10,
                LocalDate.of(2026, 2, 1), LocalDate.of(2027, 5, 1), 20, 20));
        loteDAO.agregar(new Lote(3, "LOT-CADUCIDAD-CERCANA", 1, 10,
                LocalDate.of(2026, 3, 1), LocalDate.of(2027, 3, 1), 15, 10));

        List<Lote> lotes = service.listarPorRepuesto(10);

        assertEquals("LOT-CADUCIDAD-CERCANA", lotes.get(0).getCodigoLote());
        assertEquals("LOT-CADUCIDAD-TARDIA", lotes.get(1).getCodigoLote());
        assertEquals("LOT-SIN-CADUCIDAD", lotes.get(2).getCodigoLote());
    }

    @Test
    @DisplayName("Debe rechazar trazabilidad de un repuesto inexistente")
    void rechazarListadoRepuestoInexistente() {
        assertThrows(EntidadNoEncontradaException.class, () -> service.listarPorRepuesto(99));
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
        public List<Lote> listarPorRepuesto(int repuestoId) {
            List<Lote> resultado = new ArrayList<>();
            for (Lote lote : datos) {
                if (lote.getRepuestoId() == repuestoId) {
                    resultado.add(lote);
                }
            }
            return resultado;
        }

        @Override
        public int guardar(Lote lote) {
            lote.setId(nextId++);
            datos.add(lote);
            return lote.getId();
        }

        @Override
        public int sumarStockFisico(int repuestoId) {
            int total = 0;
            for (Lote lote : datos) {
                if (lote.getRepuestoId() == repuestoId) {
                    total += lote.getCantidadActual();
                }
            }
            return total;
        }

        @Override
        public int sumarStockOperativo(int repuestoId, LocalDate fechaReferencia) {
            int total = 0;
            for (Lote lote : datos) {
                boolean vigente = lote.getFechaCaducidad() == null || !lote.getFechaCaducidad().isBefore(fechaReferencia);
                if (lote.getRepuestoId() == repuestoId && vigente) {
                    total += lote.getCantidadActual();
                }
            }
            return total;
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

    private static class RepuestoDAOMock implements RepuestoDAO {
        private final List<Repuesto> datos = new ArrayList<>();

        void agregar(Repuesto repuesto) {
            datos.add(repuesto);
        }

        @Override
        public List<Repuesto> listarTodos() {
            return new ArrayList<>(datos);
        }

        @Override
        public List<Repuesto> listarActivos() {
            List<Repuesto> activos = new ArrayList<>();
            for (Repuesto repuesto : datos) {
                if (repuesto.isActivo()) {
                    activos.add(repuesto);
                }
            }
            return activos;
        }

        @Override
        public Repuesto buscarPorId(int id) {
            for (Repuesto repuesto : datos) {
                if (repuesto.getId() == id) {
                    return repuesto;
                }
            }
            return null;
        }

        @Override
        public Repuesto buscarPorNombre(String nombre) {
            return null;
        }

        @Override
        public int guardar(Repuesto repuesto) {
            repuesto.setId(datos.size() + 1);
            datos.add(repuesto);
            return repuesto.getId();
        }

        @Override
        public boolean actualizar(Repuesto repuesto) {
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
