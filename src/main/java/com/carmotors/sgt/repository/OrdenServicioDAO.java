package com.carmotors.sgt.repository;

import com.carmotors.sgt.model.OrdenServicio;

/**
 * Contrato de persistencia para la cabecera de las órdenes de servicio ({@code ordenes_servicio}).
 */
public interface OrdenServicioDAO {

    /**
     * Inserta la cabecera de una orden y retorna el ID autogenerado.
     *
     * @param orden entidad a persistir
     * @return ID asignado en base de datos
     */
    int guardar(OrdenServicio orden);

    /**
     * Busca una orden por su identificador.
     *
     * @param id identificador de la orden
     * @return la orden encontrada o {@code null} si no existe
     */
    OrdenServicio buscarPorId(int id);

    /**
     * Busca y bloquea una orden dentro de una transacción JDBC activa ({@code FOR UPDATE}).
     *
     * @param id identificador de la orden
     * @return la orden encontrada o {@code null} si no existe
     */
    OrdenServicio buscarPorIdParaActualizar(int id);

    /**
     * Actualiza el estado de una orden existente (RF-12, RN-06).
     *
     * @param ordenId       identificador de la orden
     * @param nuevoEstadoId identificador del nuevo estado en {@code servicio_estados}
     * @return {@code true} si se actualizó al menos una fila
     */
    boolean actualizarEstado(int ordenId, int nuevoEstadoId);
}
