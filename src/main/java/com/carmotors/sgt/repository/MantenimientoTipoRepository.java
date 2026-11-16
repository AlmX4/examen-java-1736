package com.carmotors.sgt.repository;

import com.carmotors.sgt.model.MantenimientoTipo;

import java.util.List;

/**
 * Contrato de persistencia de solo lectura para los tipos de mantenimiento.
 */
public interface MantenimientoTipoRepository {

    /**
     * Retorna todos los tipos de mantenimiento ordenados por ID ascendente.
     *
     * @return lista de tipos de mantenimiento
     */
    List<MantenimientoTipo> listarTodos();

    /**
     * Busca un tipo de mantenimiento por su identificador único.
     *
     * @param id identificador del tipo de mantenimiento
     * @return la entidad encontrada o {@code null} si no existe
     */
    MantenimientoTipo buscarPorId(int id);
}
