package com.carmotors.sgt.repository;

import java.util.List;

import com.carmotors.sgt.model.EvaluacionProveedor;

public interface EvaluacionProveedorDAO {

    List<EvaluacionProveedor> listarTodos();

    List<EvaluacionProveedor> listarPorProveedor(int proveedorId);

    EvaluacionProveedor buscarPorId(int id);

    int guardar(EvaluacionProveedor evaluacion);
}
