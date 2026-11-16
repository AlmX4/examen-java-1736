package com.carmotors.sgt.controller;

import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.service.ClienteService;

import java.util.List;

/**
 * Controlador de clientes: recibe peticiones de la vista y coordina con {@link ClienteService}.
 * No accede directamente a repositorios ni abre conexiones JDBC (RNF-04).
 */
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        if (clienteService == null) {
            throw new IllegalArgumentException("El servicio de clientes es obligatorio.");
        }
        this.clienteService = clienteService;
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
}
