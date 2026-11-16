package com.carmotors.sgt.controller;

import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.service.ClienteService;
import com.carmotors.sgt.service.VehiculoService;

import java.util.List;

/**
 * Controlador del módulo de clientes y vehículos: recibe peticiones de la vista
 * y coordina con {@link ClienteService} y {@link VehiculoService}.
 * No accede directamente a repositorios ni abre conexiones JDBC (RNF-04).
 */
public class ClienteController {

    private final ClienteService clienteService;
    private final VehiculoService vehiculoService;

    public ClienteController(ClienteService clienteService) {
        this(clienteService, null);
    }

    public ClienteController(ClienteService clienteService, VehiculoService vehiculoService) {
        if (clienteService == null) {
            throw new IllegalArgumentException("El servicio de clientes es obligatorio.");
        }
        this.clienteService = clienteService;
        this.vehiculoService = vehiculoService;
    }

    public List<Cliente> listarTodos() {
        return clienteService.listarTodos();
    }

    public List<Cliente> listarActivos() {
        return clienteService.listarActivos();
    }

    public Cliente buscarPorId(int id) {
        return clienteService.buscarPorId(id);
    }

    public Cliente buscarPorDocumento(String documento) {
        return clienteService.buscarPorDocumento(documento);
    }

    public Cliente registrar(Cliente cliente) {
        return clienteService.registrarCliente(cliente);
    }

    public Cliente actualizar(Cliente cliente) {
        return clienteService.actualizarCliente(cliente);
    }

    public Cliente desactivar(int id) {
        return clienteService.desactivarCliente(id);
    }

    public Cliente reactivar(int id) {
        return clienteService.reactivarCliente(id);
    }

    // --- Métodos de Vehículos (RF-18, RN-13, RN-17, HU-09) ---

    public Vehiculo registrarVehiculo(Vehiculo vehiculo) {
        verificarServicioVehiculos();
        return vehiculoService.registrarVehiculo(vehiculo);
    }

    public List<Vehiculo> listarVehiculosPorCliente(int clienteId) {
        verificarServicioVehiculos();
        return vehiculoService.listarPorCliente(clienteId);
    }

    public Vehiculo buscarVehiculoPorId(int id) {
        verificarServicioVehiculos();
        return vehiculoService.buscarPorId(id);
    }

    public Vehiculo buscarVehiculoPorPlaca(String placa) {
        verificarServicioVehiculos();
        return vehiculoService.buscarPorPlaca(placa);
    }

    public List<Vehiculo> listarTodosLosVehiculos() {
        verificarServicioVehiculos();
        return vehiculoService.listarTodos();
    }

    private void verificarServicioVehiculos() {
        if (vehiculoService == null) {
            throw new ValidacionException("El servicio de vehículos no ha sido configurado.");
        }
    }
}
