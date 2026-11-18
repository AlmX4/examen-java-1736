package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.CargaLineaTecnico;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.OrdenServicio;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.repository.ClienteDAO;
import com.carmotors.sgt.repository.OrdenServicioDAO;
import com.carmotors.sgt.repository.OrdenServicioDetalleDAO;
import com.carmotors.sgt.repository.RepuestoDAO;
import com.carmotors.sgt.repository.ServicioTipoDAO;
import com.carmotors.sgt.repository.TecnicoDAO;
import com.carmotors.sgt.repository.VehiculoDAO;
import com.carmotors.sgt.service.state.EstadoCancelada;
import com.carmotors.sgt.service.state.EstadoCompletado;
import com.carmotors.sgt.service.state.EstadoEnProceso;
import com.carmotors.sgt.service.state.EstadoEntregado;
import com.carmotors.sgt.service.state.EstadoFacturado;
import com.carmotors.sgt.service.state.EstadoPendiente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrdenServicioServiceTest {

    private OrdenServicioService service;
    private OrdenServicioDAOMock ordenDAO;
    private OrdenServicioDetalleDAOMock detalleDAO;
    private TecnicoDAOMock tecnicoDAO;
    private RepuestoDAOMock repuestoDAO;

    @BeforeEach
    void setUp() {
        ordenDAO = new OrdenServicioDAOMock();
        detalleDAO = new OrdenServicioDetalleDAOMock();
        tecnicoDAO = new TecnicoDAOMock();
        repuestoDAO = new RepuestoDAOMock();

        ClienteDAOMock clienteDAO = new ClienteDAOMock();
        clienteDAO.agregar(new Cliente(1, "María Rojas", "1094567801", "3105551101", "maria@correo.com", true));
        clienteDAO.agregar(new Cliente(7, "Pablo Ortiz", "1091234507", "3165551107", "pablo@correo.com", false));

        VehiculoDAOMock vehiculoDAO = new VehiculoDAOMock();
        vehiculoDAO.agregar(new Vehiculo(1, 1, "ABC123", "Chevrolet", "Spark GT"));
        vehiculoDAO.agregar(new Vehiculo(9, 7, "ZZZ999", "Mazda", "2"));

        ServicioTipoDAOMock servicioTipoDAO = new ServicioTipoDAOMock();
        servicioTipoDAO.agregar(new ServicioTipo(1, "Cambio de aceite y filtro", 1, 1, new BigDecimal("60000"), 6));
        servicioTipoDAO.agregar(new ServicioTipo(2, "Revisión y cambio de pastillas de freno", 2, 3,
                new BigDecimal("90000.005"), null));

        tecnicoDAO.agregar(new Tecnico(1, "Pedro Ramírez", "1090111222", 1, "3101112233", true));
        tecnicoDAO.agregar(new Tecnico(2, "Laura Gómez", "1090333444", 3, "3104445566", true));
        tecnicoDAO.agregar(new Tecnico(3, "Luis Ortega", "1090555666", 1, "3107778899", false));

        repuestoDAO.agregar(new Repuesto(1, 1, "Aceite sintético", "Mobil", new BigDecimal("38000.00"), 4, 2, true));
        repuestoDAO.agregar(new Repuesto(2, 1, "Filtro de aceite", "Bosch", new BigDecimal("25000.00"), 1, 2, true));

        service = new OrdenServicioService(
                ordenDAO, detalleDAO, vehiculoDAO, clienteDAO, servicioTipoDAO,
                tecnicoDAO, repuestoDAO, false);
    }

    private List<OrdenServicioDetalle> lineas(int... tipoIds) {
        List<OrdenServicioDetalle> lista = new ArrayList<>();
        for (int tipoId : tipoIds) {
            lista.add(new OrdenServicioDetalle(tipoId, null));
        }
        return lista;
    }

    @Test
    @DisplayName("Crea una orden Pendiente con una línea que copia nombre y mano de obra del tipo")
    void crearOrdenConUnaLinea() {
        List<OrdenServicioDetalle> solicitadas = new ArrayList<>();
        solicitadas.add(new OrdenServicioDetalle(1, "  Usar aceite sintético  "));
        LocalDateTime antes = LocalDateTime.now().withNano(0);

        OrdenServicio orden = service.crearOrden(1, "  Cambio de aceite por kilometraje  ", solicitadas);

        assertEquals(1, orden.getId());
        assertEquals(1, orden.getVehiculoId());
        assertEquals(new EstadoPendiente().getEstadoId(), orden.getEstadoId());
        assertEquals("Cambio de aceite por kilometraje", orden.getDescripcionProblema());
        assertNotNull(orden.getFechaRecepcion());
        assertFalse(orden.getFechaRecepcion().isBefore(antes));
        assertFalse(orden.getFechaRecepcion().isAfter(LocalDateTime.now()));
        assertNull(orden.getFechaCompletado());
        assertNull(orden.getFechaEntrega());
        assertNull(orden.getFechaCancelacion());

        List<OrdenServicioDetalle> guardadas = service.listarLineas(orden.getId());
        assertEquals(1, guardadas.size());
        OrdenServicioDetalle linea = guardadas.get(0);
        assertEquals(1, linea.getOrdenId());
        assertEquals(1, linea.getServicioTipoId());
        assertEquals("Cambio de aceite y filtro", linea.getNombreServicioSnapshot());
        assertEquals(new BigDecimal("60000.00"), linea.getCostoManoObra());
        assertEquals("Usar aceite sintético", linea.getObservaciones());
        assertEquals("Pendiente", service.obtenerNombreEstado(orden.getEstadoId()));
    }

    @Test
    @DisplayName("Crea una orden con varias líneas, todas asociadas al ID generado")
    void crearOrdenConVariasLineas() {
        OrdenServicio orden = service.crearOrden(1, "Mantenimiento general", lineas(1, 2, 1));

        List<OrdenServicioDetalle> guardadas = service.listarLineas(orden.getId());
        assertEquals(3, guardadas.size());
        assertEquals("Cambio de aceite y filtro", guardadas.get(0).getNombreServicioSnapshot());
        assertEquals("Revisión y cambio de pastillas de freno", guardadas.get(1).getNombreServicioSnapshot());
        assertEquals(new BigDecimal("90000.01"), guardadas.get(1).getCostoManoObra());
        assertEquals(new BigDecimal("60000.00"), guardadas.get(2).getCostoManoObra());
        for (OrdenServicioDetalle linea : guardadas) {
            assertEquals(orden.getId(), linea.getOrdenId());
            assertTrue(linea.getId() > 0);
            assertNull(linea.getObservaciones());
        }
        assertEquals(1, ordenDAO.ordenes.size());
    }

    @Test
    @DisplayName("Rechaza un vehículo inexistente sin guardar nada")
    void rechazarVehiculoInexistente() {
        assertThrows(EntidadNoEncontradaException.class,
                () -> service.crearOrden(99, "Ruido en el motor", lineas(1)));
        assertNadaGuardado();
    }

    @Test
    @DisplayName("Rechaza un vehículo cuyo cliente dueño está inactivo")
    void rechazarClienteInactivo() {
        assertThrows(ValidacionException.class,
                () -> service.crearOrden(9, "Ruido en el motor", lineas(1)));
        assertNadaGuardado();
    }

    @Test
    @DisplayName("Rechaza una descripción vacía o en blanco")
    void rechazarDescripcionVacia() {
        assertThrows(ValidacionException.class, () -> service.crearOrden(1, "   ", lineas(1)));
        assertThrows(ValidacionException.class, () -> service.crearOrden(1, null, lineas(1)));
        assertNadaGuardado();
    }

    @Test
    @DisplayName("Rechaza una descripción de más de 255 caracteres y acepta exactamente 255")
    void rechazarDescripcionDemasiadoLarga() {
        String larga = "a".repeat(256);
        assertThrows(ValidacionException.class, () -> service.crearOrden(1, larga, lineas(1)));
        assertNadaGuardado();

        OrdenServicio orden = service.crearOrden(1, "b".repeat(255), lineas(1));
        assertEquals(255, orden.getDescripcionProblema().length());
    }

    @Test
    @DisplayName("Rechaza una orden sin líneas de servicio (RN-18)")
    void rechazarOrdenSinLineas() {
        assertThrows(ValidacionException.class, () -> service.crearOrden(1, "Revisión", new ArrayList<>()));
        assertThrows(ValidacionException.class, () -> service.crearOrden(1, "Revisión", null));
        assertNadaGuardado();
    }

    @Test
    @DisplayName("Rechaza una línea con un tipo de servicio inexistente sin guardar la cabecera")
    void rechazarTipoServicioInexistente() {
        assertThrows(EntidadNoEncontradaException.class,
                () -> service.crearOrden(1, "Revisión", lineas(1, 42)));
        assertNadaGuardado();
    }

    @Test
    @DisplayName("T4.3: Agrega línea a orden Pendiente congelando snapshot y costo de mano de obra")
    void agregarLineaEnOrdenPendiente() {
        OrdenServicio orden = service.crearOrden(1, "Revisión general", lineas(1));

        int nuevoDetalleId = service.agregarLinea(
                orden.getId(), new OrdenServicioDetalle(2, "  Revisar discos  "), null, false);

        assertTrue(nuevoDetalleId > 0);
        List<OrdenServicioDetalle> guardadas = service.listarLineas(orden.getId());
        assertEquals(2, guardadas.size());
        OrdenServicioDetalle nueva = guardadas.get(1);
        assertEquals("Revisión y cambio de pastillas de freno", nueva.getNombreServicioSnapshot());
        assertEquals(new BigDecimal("90000.01"), nueva.getCostoManoObra());
        assertEquals("Revisar discos", nueva.getObservaciones());
        assertFalse(service.requiereTecnicoParaAgregar(orden.getId()));
    }

    @Test
    @DisplayName("T4.3: En Proceso exige técnico activo, asigna en la misma operación y respeta RN-19")
    void agregarLineaEnOrdenEnProcesoConValidacionesTecnicoYEspecialidad() {
        OrdenServicio orden = service.crearOrden(1, "Falla frenos", lineas(1));
        orden.setEstadoId(new EstadoEnProceso().getEstadoId());
        assertTrue(service.requiereTecnicoParaAgregar(orden.getId()));

        // Sin técnico -> rechaza
        assertThrows(ValidacionException.class,
                () -> service.agregarLinea(orden.getId(), new OrdenServicioDetalle(1, null), null, false));

        // Técnico inactivo (ID 3) -> rechaza
        assertThrows(ValidacionException.class,
                () -> service.agregarLinea(orden.getId(), new OrdenServicioDetalle(1, null), 3, false));

        // Especialidad distinta sin confirmar (Servicio 2 requiere esp. 3, Técnico 1 es esp. 1) -> rechaza
        assertThrows(ValidacionException.class,
                () -> service.agregarLinea(orden.getId(), new OrdenServicioDetalle(2, null), 1, false));

        // Especialidad distinta confirmada -> permite y asigna
        int detalleConfirmado = service.agregarLinea(
                orden.getId(), new OrdenServicioDetalle(2, null), 1, true);
        assertEquals(Integer.valueOf(1), detalleDAO.tecnicosAsignados.get(detalleConfirmado));

        // Especialidad coincidente (Servicio 2 esp. 3, Técnico 2 esp. 3) -> permite sin confirmación extra
        int detalleEspecialista = service.agregarLinea(
                orden.getId(), new OrdenServicioDetalle(2, null), 2, false);
        assertEquals(Integer.valueOf(2), detalleDAO.tecnicosAsignados.get(detalleEspecialista));
    }

    @Test
    @DisplayName("T4.3: Bloquea agregar o retirar líneas en estados Completado, Facturado, Entregado y Cancelada")
    void rechazarEdicionEnEstadosNoEditables() {
        int[] estadosBloqueados = new int[] {
                new EstadoCompletado().getEstadoId(),
                new EstadoFacturado().getEstadoId(),
                new EstadoEntregado().getEstadoId(),
                new EstadoCancelada().getEstadoId()
        };

        for (int estadoId : estadosBloqueados) {
            OrdenServicio orden = service.crearOrden(1, "Orden bloqueada", lineas(1, 2));
            int detalleId = service.listarLineas(orden.getId()).get(0).getId();
            orden.setEstadoId(estadoId);

            assertThrows(ValidacionException.class,
                    () -> service.requiereTecnicoParaAgregar(orden.getId()));
            assertThrows(ValidacionException.class,
                    () -> service.agregarLinea(orden.getId(), new OrdenServicioDetalle(1, null), 1, true));
            assertThrows(ValidacionException.class,
                    () -> service.quitarLinea(orden.getId(), detalleId));
        }
    }

    @Test
    @DisplayName("T4.3: Retirar línea libera exactamente sus reservas en repuestos y protege última línea (RN-05, RN-18)")
    void quitarLineaLiberaReservasYProtegeUltimaLinea() {
        OrdenServicio orden = service.crearOrden(1, "Cambio aceite y alineación", lineas(1, 2));
        List<OrdenServicioDetalle> guardadas = service.listarLineas(orden.getId());
        int detalleConReservas = guardadas.get(0).getId();
        int detalleSinReservas = guardadas.get(1).getId();

        detalleDAO.agregarReserva(detalleConReservas, 1, 4);
        detalleDAO.agregarReserva(detalleConReservas, 2, 1);

        service.quitarLinea(orden.getId(), detalleConReservas);

        assertEquals(0, repuestoDAO.buscarPorId(1).getCantidadReservada());
        assertEquals(0, repuestoDAO.buscarPorId(2).getCantidadReservada());
        assertEquals(1, service.listarLineas(orden.getId()).size());

        // No permite retirar la última línea restante
        assertThrows(ValidacionException.class,
                () -> service.quitarLinea(orden.getId(), detalleSinReservas));
    }

    @Test
    @DisplayName("T4.3: En Proceso rechaza retirar línea con horas trabajadas > 0 y permite si tiene 0.00h (RF-35)")
    void quitarLineaEnProcesoValidaHorasTrabajadas() {
        OrdenServicio orden = service.crearOrden(1, "Trabajo en progreso", lineas(1, 2, 1));
        List<OrdenServicioDetalle> guardadas = service.listarLineas(orden.getId());
        int detalleConHoras = guardadas.get(0).getId();
        int detalleSinHoras = guardadas.get(1).getId();

        detalleDAO.fijarHoras(detalleConHoras, new BigDecimal("1.50"));
        detalleDAO.fijarHoras(detalleSinHoras, BigDecimal.ZERO);
        orden.setEstadoId(new EstadoEnProceso().getEstadoId());

        assertThrows(ValidacionException.class,
                () -> service.quitarLinea(orden.getId(), detalleConHoras));
        assertEquals(3, service.listarLineas(orden.getId()).size());

        service.quitarLinea(orden.getId(), detalleSinHoras);
        assertEquals(2, service.listarLineas(orden.getId()).size());
    }

    @Test
    @DisplayName("T4.3: especialidadCoincide maneja nulos de forma segura")
    void especialidadCoincideConNulos() {
        ServicioTipo tipo = new ServicioTipo(1, "Aceite", 1, 2, new BigDecimal("50000"), 6);
        Tecnico tecnicoIgual = new Tecnico(1, "Ana", "100", 2, null, true);
        Tecnico tecnicoDistinto = new Tecnico(2, "Luis", "200", 3, null, true);

        assertFalse(service.especialidadCoincide(null, tecnicoIgual));
        assertFalse(service.especialidadCoincide(tipo, null));
        assertFalse(service.especialidadCoincide(tipo, tecnicoDistinto));
        assertTrue(service.especialidadCoincide(tipo, tecnicoIgual));
    }

    private void assertNadaGuardado() {
        assertTrue(ordenDAO.ordenes.isEmpty());
        assertTrue(detalleDAO.detalles.isEmpty());
    }

    // ------------------------------------------------------------------
    // DAOs en memoria
    // ------------------------------------------------------------------

    private static class OrdenServicioDAOMock implements OrdenServicioDAO {
        private final Map<Integer, OrdenServicio> ordenes = new HashMap<>();
        private int secuencia = 0;

        @Override
        public int guardar(OrdenServicio orden) {
            secuencia++;
            ordenes.put(secuencia, orden);
            return secuencia;
        }

        @Override
        public OrdenServicio buscarPorId(int id) {
            return ordenes.get(id);
        }

        @Override
        public OrdenServicio buscarPorIdParaActualizar(int id) {
            return ordenes.get(id);
        }
    }

    private static class OrdenServicioDetalleDAOMock implements OrdenServicioDetalleDAO {
        private final List<OrdenServicioDetalle> detalles = new ArrayList<>();
        private final Map<Integer, Integer> tecnicosAsignados = new HashMap<>();
        private final Map<Integer, BigDecimal> horasPorDetalle = new HashMap<>();
        private final Map<Integer, Map<Integer, Integer>> reservasPorDetalle = new HashMap<>();
        private int secuencia = 0;

        void agregarReserva(int detalleId, int repuestoId, int cantidad) {
            Map<Integer, Integer> mapa = reservasPorDetalle.get(detalleId);
            if (mapa == null) {
                mapa = new LinkedHashMap<>();
                reservasPorDetalle.put(detalleId, mapa);
            }
            mapa.put(repuestoId, cantidad);
        }

        void fijarHoras(int detalleId, BigDecimal horas) {
            horasPorDetalle.put(detalleId, horas);
        }

        @Override
        public int guardar(OrdenServicioDetalle detalle) {
            secuencia++;
            detalles.add(detalle);
            return secuencia;
        }

        @Override
        public List<OrdenServicioDetalle> listarPorOrden(int ordenId) {
            List<OrdenServicioDetalle> resultado = new ArrayList<>();
            for (OrdenServicioDetalle detalle : detalles) {
                if (detalle.getOrdenId() == ordenId) {
                    resultado.add(detalle);
                }
            }
            return resultado;
        }

        @Override
        public List<OrdenServicioDetalle> listarPorOrdenParaActualizar(int ordenId) {
            return listarPorOrden(ordenId);
        }

        @Override
        public void asignarTecnico(int detalleId, int tecnicoId) {
            tecnicosAsignados.put(detalleId, tecnicoId);
        }

        @Override
        public boolean tieneHorasTrabajadas(int detalleId) {
            BigDecimal horas = horasPorDetalle.get(detalleId);
            return horas != null && horas.signum() > 0;
        }

        @Override
        public Map<Integer, Integer> listarReservasParaActualizar(int detalleId) {
            Map<Integer, Integer> mapa = reservasPorDetalle.get(detalleId);
            return mapa != null ? new LinkedHashMap<>(mapa) : new LinkedHashMap<>();
        }

        @Override
        public void eliminar(int ordenId, int detalleId) {
            for (int i = 0; i < detalles.size(); i++) {
                OrdenServicioDetalle d = detalles.get(i);
                if (d.getOrdenId() == ordenId && d.getId() == detalleId) {
                    detalles.remove(i);
                    return;
                }
            }
        }
    }

    private static class VehiculoDAOMock implements VehiculoDAO {
        private final Map<Integer, Vehiculo> vehiculos = new HashMap<>();

        void agregar(Vehiculo vehiculo) {
            vehiculos.put(vehiculo.getId(), vehiculo);
        }

        @Override
        public int guardar(Vehiculo vehiculo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vehiculo buscarPorId(int id) {
            return vehiculos.get(id);
        }

        @Override
        public Vehiculo buscarPorPlaca(String placa) {
            for (Vehiculo vehiculo : vehiculos.values()) {
                if (vehiculo.getPlaca().equals(placa)) {
                    return vehiculo;
                }
            }
            return null;
        }

        @Override
        public List<Vehiculo> listarPorCliente(int clienteId) {
            return new ArrayList<>();
        }

        @Override
        public List<Vehiculo> listarTodos() {
            return new ArrayList<>(vehiculos.values());
        }
    }

    private static class ClienteDAOMock implements ClienteDAO {
        private final Map<Integer, Cliente> clientes = new HashMap<>();

        void agregar(Cliente cliente) {
            clientes.put(cliente.getId(), cliente);
        }

        @Override
        public List<Cliente> listarTodos() {
            return new ArrayList<>(clientes.values());
        }

        @Override
        public List<Cliente> listarActivos() {
            return new ArrayList<>();
        }

        @Override
        public Cliente buscarPorId(int id) {
            return clientes.get(id);
        }

        @Override
        public Cliente buscarPorDocumento(String documento) {
            return null;
        }

        @Override
        public Cliente buscarPorEmail(String email) {
            return null;
        }

        @Override
        public int guardar(Cliente cliente) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean actualizar(Cliente cliente) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean actualizarEstadoActivo(int id, boolean activo) {
            throw new UnsupportedOperationException();
        }
    }

    private static class ServicioTipoDAOMock implements ServicioTipoDAO {
        private final Map<Integer, ServicioTipo> tipos = new HashMap<>();

        void agregar(ServicioTipo tipo) {
            tipos.put(tipo.getId(), tipo);
        }

        @Override
        public List<ServicioTipo> listarTodos() {
            return new ArrayList<>(tipos.values());
        }

        @Override
        public ServicioTipo buscarPorId(int id) {
            return tipos.get(id);
        }

        @Override
        public ServicioTipo buscarPorNombre(String nombre) {
            return null;
        }

        @Override
        public int guardar(ServicioTipo servicioTipo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean actualizar(ServicioTipo servicioTipo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean eliminar(int id) {
            throw new UnsupportedOperationException();
        }
    }

    private static class TecnicoDAOMock implements TecnicoDAO {
        private final Map<Integer, Tecnico> tecnicos = new HashMap<>();

        void agregar(Tecnico tecnico) {
            tecnicos.put(tecnico.getId(), tecnico);
        }

        @Override
        public List<Tecnico> listarTodos() {
            return new ArrayList<>(tecnicos.values());
        }

        @Override
        public List<Tecnico> listarActivos() {
            List<Tecnico> activos = new ArrayList<>();
            for (Tecnico t : tecnicos.values()) {
                if (t.isActivo()) {
                    activos.add(t);
                }
            }
            return activos;
        }

        @Override
        public Tecnico buscarPorId(int id) {
            return tecnicos.get(id);
        }

        @Override
        public Tecnico buscarPorIdParaActualizar(int id) {
            return tecnicos.get(id);
        }

        @Override
        public Tecnico buscarPorDocumento(String documento) {
            return null;
        }

        @Override
        public int guardar(Tecnico tecnico) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean actualizar(Tecnico tecnico) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean actualizarEstadoActivo(int id, boolean activo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<CargaLineaTecnico> listarCargaPorTecnico(int tecnicoId) {
            return new ArrayList<>();
        }

        @Override
        public Map<Integer, Integer> contarCargaActivaPorTecnico() {
            return new HashMap<>();
        }
    }

    private static class RepuestoDAOMock implements RepuestoDAO {
        private final Map<Integer, Repuesto> repuestos = new HashMap<>();

        void agregar(Repuesto repuesto) {
            repuestos.put(repuesto.getId(), repuesto);
        }

        @Override
        public List<Repuesto> listarTodos() {
            return new ArrayList<>(repuestos.values());
        }

        @Override
        public List<Repuesto> listarActivos() {
            return new ArrayList<>(repuestos.values());
        }

        @Override
        public Repuesto buscarPorId(int id) {
            return repuestos.get(id);
        }

        @Override
        public Repuesto buscarPorNombre(String nombre) {
            return null;
        }

        @Override
        public int guardar(Repuesto repuesto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean actualizar(Repuesto repuesto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean desactivar(int id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean reactivar(int id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean descontarReserva(int repuestoId, int cantidad) {
            Repuesto r = repuestos.get(repuestoId);
            if (r == null || r.getCantidadReservada() < cantidad) {
                return false;
            }
            r.setCantidadReservada(r.getCantidadReservada() - cantidad);
            return true;
        }
    }
}
