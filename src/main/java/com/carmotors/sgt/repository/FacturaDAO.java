package com.carmotors.sgt.repository;

import com.carmotors.sgt.model.Factura;

/**
 * Contrato de persistencia para la cabecera de las facturas ({@code facturas}).
 */
public interface FacturaDAO {

    /**
     * Inserta la cabecera de una factura y retorna el ID autogenerado.
     *
     * @param factura cabecera a persistir
     * @return ID asignado en base de datos
     */
    int guardar(Factura factura);

    /**
     * Busca una factura por su identificador.
     *
     * @return la factura encontrada o {@code null} si no existe
     */
    Factura buscarPorId(int id);

    /**
     * Busca la factura asociada a una orden (relación 1:1).
     *
     * @return la factura de la orden o {@code null} si la orden no ha sido facturada
     */
    Factura buscarPorOrden(int ordenId);

    /**
     * Obtiene el número de factura más alto emitido en un año (formato {@code F-<año>-<consecutivo>}).
     *
     * @param anio año de emisión
     * @return el último número del año o {@code null} si no hay facturas en ese año
     */
    String obtenerUltimoNumeroDelAnio(int anio);
}
