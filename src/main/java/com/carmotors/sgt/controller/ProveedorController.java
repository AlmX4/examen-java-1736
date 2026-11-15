package com.carmotors.sgt.controller;

import java.util.List;
import java.util.Objects;

import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.service.ProveedorService;

public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController() {
        this(new ProveedorService());
    }

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = Objects.requireNonNull(proveedorService, "El servicio de proveedores es obligatorio.");
    }

    public List<Proveedor> listarTodos() {
        return proveedorService.listarTodos();
    }

    public List<Proveedor> listarActivos() {
        return proveedorService.listarActivos();
    }

    public Proveedor buscarPorId(int id) {
        return proveedorService.buscarPorId(id);
    }

    public Proveedor guardar(Proveedor proveedor) {
        return proveedor.getId() > 0 ? proveedorService.actualizarProveedor(proveedor) : proveedorService.crearProveedor(proveedor);
    }

    public Proveedor crear(Proveedor proveedor) {
        return proveedorService.crearProveedor(proveedor);
    }

    public Proveedor actualizar(Proveedor proveedor) {
        return proveedorService.actualizarProveedor(proveedor);
    }

    public Proveedor desactivar(int id) {
        return proveedorService.desactivarProveedor(id);
    }

    public Proveedor reactivar(int id) {
        return proveedorService.reactivarProveedor(id);
    }
}
