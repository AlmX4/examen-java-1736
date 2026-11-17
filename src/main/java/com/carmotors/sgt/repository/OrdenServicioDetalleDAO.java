package com.carmotors.sgt.repository;

import com.carmotors.sgt.model.OrdenServicioDetalle;

import java.util.List;

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
}
