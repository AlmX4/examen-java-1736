package com.carmotors.sgt.repository;

import java.time.LocalDate;
import java.util.List;

import com.carmotors.sgt.model.Lote;

/**
 * Acceso a datos del lote de inventario.
 */
public interface LoteDAO {

    Lote buscarPorCodigo(String codigoLote);

    List<Lote> listarPorRepuesto(int repuestoId);

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

    /**
     * Lista los lotes candidatos para el reporte de vencimiento (RF-06):
     * con saldo restante mayor a cero, fecha de caducidad no nula y caducidad menor o igual
     * a la fecha límite indicada (incluye lotes ya vencidos y lotes por vencer hasta la fecha límite).
     */
    List<Lote> listarPorVencer(LocalDate fechaLimite);
}
