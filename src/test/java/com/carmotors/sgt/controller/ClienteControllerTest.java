package com.carmotors.sgt.controller;

import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.repository.ClienteRepository;
import com.carmotors.sgt.repository.VehiculoRepository;
import com.carmotors.sgt.service.ClienteService;
import com.carmotors.sgt.service.VehiculoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas unitarias para {@link ClienteController}.
 */
class ClienteControllerTest {

    private ClienteController controllerConVehiculos;
    private ClienteController controllerSinVehiculos;
    private Cliente clienteActivo;

    @BeforeEach
    void setUp() {
        VehiculoRepoTest vehiculoRepo = new VehiculoRepoTest();
        ClienteRepoTest clienteRepo = new ClienteRepoTest();
        clienteActivo = new Cliente(1, "Pedro Pérez", "123456", "3001112233", "pedro@correo.com", true);
        clienteRepo.guardar(clienteActivo);

        ClienteService clienteService = new ClienteService(clienteRepo);
        VehiculoService vehiculoService = new VehiculoService(vehiculoRepo, clienteRepo);

        controllerConVehiculos = new ClienteController(clienteService, vehiculoService);
        controllerSinVehiculos = new ClienteController(clienteService);
    }

    @Test
    @DisplayName("Debe lanzar excepción si clienteService es nulo en el constructor")
    void testConstructorNulo() {
        assertThrows(IllegalArgumentException.class, () -> new ClienteController(null));
        assertThrows(IllegalArgumentException.class, () -> new ClienteController(null, null));
    }

    @Test
    @DisplayName("Debe registrar y listar vehículos a través del controlador")
    void testRegistrarYListarVehiculos() {
        Vehiculo vehiculo = new Vehiculo(1, "ABC123", "Renault", "Sandero");
        Vehiculo creado = controllerConVehiculos.registrarVehiculo(vehiculo);

        assertNotNull(creado);
        assertEquals(1, creado.getId());
        assertEquals("ABC123", creado.getPlaca());

        List<Vehiculo> porCliente = controllerConVehiculos.listarVehiculosPorCliente(1);
        assertEquals(1, porCliente.size());

        Vehiculo porPlaca = controllerConVehiculos.buscarVehiculoPorPlaca("ABC123");
        assertEquals("Renault", porPlaca.getMarca());

        Vehiculo porId = controllerConVehiculos.buscarVehiculoPorId(1);
        assertEquals("Sandero", porId.getModelo());

        List<Vehiculo> todos = controllerConVehiculos.listarTodosLosVehiculos();
        assertEquals(1, todos.size());
    }

    @Test
    @DisplayName("Debe lanzar ValidacionException si no se configuró VehiculoService al llamar operaciones de vehículos")
    void testOperacionesSinServicioVehiculos() {
        Vehiculo v = new Vehiculo(1, "ABC123", "Renault", "Sandero");
        assertThrows(ValidacionException.class, () -> controllerSinVehiculos.registrarVehiculo(v));
        assertThrows(ValidacionException.class, () -> controllerSinVehiculos.listarVehiculosPorCliente(1));
        assertThrows(ValidacionException.class, () -> controllerSinVehiculos.buscarVehiculoPorId(1));
        assertThrows(ValidacionException.class, () -> controllerSinVehiculos.buscarVehiculoPorPlaca("ABC123"));
        assertThrows(ValidacionException.class, () -> controllerSinVehiculos.listarTodosLosVehiculos());
    }

    private static class VehiculoRepoTest implements VehiculoRepository {
        private final List<Vehiculo> lista = new ArrayList<>();
        private int seq = 1;

        @Override
        public int guardar(Vehiculo vehiculo) {
            int id = seq++;
            vehiculo.setId(id);
            lista.add(vehiculo);
            return id;
        }

        @Override
        public Vehiculo buscarPorId(int id) {
            return lista.stream().filter(v -> v.getId() == id).findFirst().orElse(null);
        }

        @Override
        public Vehiculo buscarPorPlaca(String placa) {
            return lista.stream().filter(v -> v.getPlaca().equalsIgnoreCase(placa)).findFirst().orElse(null);
        }

        @Override
        public List<Vehiculo> listarPorCliente(int clienteId) {
            return lista.stream().filter(v -> v.getClienteId() == clienteId).toList();
        }

        @Override
        public List<Vehiculo> listarTodos() {
            return new ArrayList<>(lista);
        }
    }

    private static class ClienteRepoTest implements ClienteRepository {
        private final List<Cliente> lista = new ArrayList<>();

        @Override
        public List<Cliente> listarTodos() {
            return lista;
        }

        @Override
        public List<Cliente> listarActivos() {
            return lista.stream().filter(Cliente::isActivo).toList();
        }

        @Override
        public Cliente buscarPorId(int id) {
            return lista.stream().filter(c -> c.getId() == id).findFirst().orElse(null);
        }

        @Override
        public Cliente buscarPorDocumento(String documento) {
            return lista.stream().filter(c -> c.getDocumentoIdentidad().equals(documento)).findFirst().orElse(null);
        }

        @Override
        public Cliente buscarPorEmail(String email) {
            return lista.stream().filter(c -> c.getEmail().equalsIgnoreCase(email)).findFirst().orElse(null);
        }

        @Override
        public int guardar(Cliente cliente) {
            lista.add(cliente);
            return cliente.getId();
        }

        @Override
        public boolean actualizar(Cliente cliente) {
            return true;
        }

        @Override
        public boolean actualizarEstadoActivo(int id, boolean activo) {
            return true;
        }
    }
}
