package com.carmotors.sgt.repository;

import java.time.LocalDate;

import com.carmotors.sgt.model.Lote;

/**
 * Acceso a datos del lote de inventario.
 */
public interface LoteDAO {

    Lote buscarPorCodigo(String codigoLote);

    int guardar(Lote lote);

    /**
     * Stock físico: suma de {@code cantidad_actual} de todos los lotes del repuesto (0 si no tiene lotes).
     */
    int sumarStockFisico(int repuestoId);

    /**
     * Stock operativo: suma de {@code cantidad_actual} de los lotes del repuesto sin caducidad
     * o con caducidad igual o posterior a la fecha indicada (0 si no hay lotes vigentes).
     */
    int sumarStockOperativo(int repuestoId, LocalDate fechaReferencia);
}
