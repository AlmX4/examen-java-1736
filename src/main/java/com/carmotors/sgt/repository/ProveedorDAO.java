package com.carmotors.sgt.repository;

import java.util.List;
import java.util.Optional;

import com.carmotors.sgt.model.Proveedor;

public interface ProveedorDAO {

    List<Proveedor> listarTodos();

    List<Proveedor> listarActivos();

    Optional<Proveedor> buscarPorId(int id);

    Optional<Proveedor> buscarPorEmail(String email);

    int guardar(Proveedor proveedor);

    boolean actualizar(Proveedor proveedor);

    boolean desactivar(int id);

    boolean reactivar(int id);
}
