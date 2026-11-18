package com.carmotors.sgt.repository;

import com.carmotors.sgt.model.OrdenServicioDetalle;

import java.util.List;
import java.util.Map;

/**
 * Contrato de persistencia para las líneas de servicio de una orden ({@code orden_servicio_detalle}).
 */
public interface OrdenServicioDetalleDAO {

    /**
     * Inserta una línea de servicio y retorna el ID autogenerado.
     *
     * @param detalle línea a persistir (con {@code ordenId} ya asignado)
     * @return ID asignado en base de datos
     */
    int guardar(OrdenServicioDetalle detalle);

    /**
     * Retorna las líneas de una orden ordenadas por ID ascendente.
     *
     * @param ordenId identificador de la orden
     * @return lista de líneas (vacía si la orden no tiene líneas)
     */
    List<OrdenServicioDetalle> listarPorOrden(int ordenId);

    /**
     * Lista y bloquea las líneas de una orden dentro de una transacción ({@code FOR UPDATE}).
     *
     * @param ordenId identificador de la orden
     * @return lista de líneas de la orden
     */
    List<OrdenServicioDetalle> listarPorOrdenParaActualizar(int ordenId);

    /**
     * Asocia un técnico a una línea de servicio recién creada.
     *
     * @param detalleId identificador de la línea
     * @param tecnicoId identificador del técnico activo
     */
    void asignarTecnico(int detalleId, int tecnicoId);

    /**
     * Indica si una línea tiene horas trabajadas mayores a cero registradas.
     *
     * @param detalleId identificador de la línea
     * @return {@code true} si existe algún técnico con horas mayores a cero en la línea
     */
    boolean tieneHorasTrabajadas(int detalleId);

    /**
     * Obtiene y bloquea las reservas de repuestos asociadas a una línea.
     *
     * @param detalleId identificador de la línea
     * @return mapa {@code repuestoId -> cantidadAsignada}
     */
    Map<Integer, Integer> listarReservasParaActualizar(int detalleId);

    /**
     * Elimina una línea de servicio perteneciente a la orden indicada.
     *
     * @param ordenId   identificador de la orden
     * @param detalleId identificador de la línea
     */
    void eliminar(int ordenId, int detalleId);
}
