package com.carmotors.sgt.repository;

import com.carmotors.sgt.model.Tecnico;

import java.util.List;

/**
 * Contrato de persistencia para la gestión de técnicos del taller (RF-33, RN-17).
 */
public interface TecnicoDAO {

    /**
     * Retorna todos los técnicos registrados ordenados por ID ascendente.
     *
     * @return lista completa de técnicos (activos e inactivos)
     */
    List<Tecnico> listarTodos();

    /**
     * Retorna únicamente los técnicos activos ordenados por ID ascendente.
     *
     * @return lista de técnicos activos
     */
    List<Tecnico> listarActivos();

    /**
     * Busca un técnico por su identificador único.
     *
     * @param id identificador del técnico
     * @return el técnico encontrado o {@code null} si no existe
     */
    Tecnico buscarPorId(int id);

    /**
     * Busca un técnico por su documento de identidad exacto.
     *
     * @param documento documento de identidad
     * @return el técnico encontrado o {@code null} si no existe
     */
    Tecnico buscarPorDocumento(String documento);

    /**
     * Inserta un nuevo técnico y retorna el ID autogenerado.
     *
     * @param tecnico entidad a registrar
     * @return ID generado en base de datos
     */
    int guardar(Tecnico tecnico);

    /**
     * Actualiza los datos de un técnico existente.
     *
     * @param tecnico entidad con los datos actualizados
     * @return {@code true} si se actualizó al menos una fila
     */
    boolean actualizar(Tecnico tecnico);

    /**
     * Cambia el estado de activación lógica de un técnico (RN-17).
     *
     * @param id identificador del técnico
     * @param activo {@code true} para reactivar, {@code false} para desactivar
     * @return {@code true} si el registro fue actualizado
     */
    boolean actualizarEstadoActivo(int id, boolean activo);
}
