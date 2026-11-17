package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.OrdenServicio;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.repository.ClienteDAO;
import com.carmotors.sgt.repository.OrdenServicioDAO;
import com.carmotors.sgt.repository.OrdenServicioDetalleDAO;
import com.carmotors.sgt.repository.ServicioTipoDAO;
import com.carmotors.sgt.repository.VehiculoDAO;
import com.carmotors.sgt.service.state.EstadoPendiente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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

    @BeforeEach
    void setUp() {
        ordenDAO = new OrdenServicioDAOMock();
        detalleDAO = new OrdenServicioDetalleDAOMock();

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

        service = new OrdenServicioService(ordenDAO, detalleDAO, vehiculoDAO, clienteDAO, servicioTipoDAO);
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
    }

    private static class OrdenServicioDetalleDAOMock implements OrdenServicioDetalleDAO {
        private final List<OrdenServicioDetalle> detalles = new ArrayList<>();

        @Override
        public int guardar(OrdenServicioDetalle detalle) {
            detalles.add(detalle);
            return detalles.size();
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
}
