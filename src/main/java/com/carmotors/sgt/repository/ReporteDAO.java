package com.carmotors.sgt.repository;

import com.carmotors.sgt.model.RegistroConsumoRepuesto;
import com.carmotors.sgt.model.RegistroServicioSolicitado;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrato de consultas de solo lectura para los reportes gerenciales (RF-21, RF-25, RF-26, RF-27).
 * Las reglas de agregación, filtrado de negocio y ordenamiento con Stream API residen en {@code service}.
 */
public interface ReporteDAO {

    /**
     * Consulta los registros históricos de repuestos asignados en órdenes completadas o posteriores
     * ({@code Completado}, {@code Facturado}, {@code Entregado}) acotados por {@code fecha_completado}
     * (RF-27, §4.2).
     *
     * @param fechaDesde límite inferior inclusivo de {@code fecha_completado} (opcional, puede ser {@code null})
     * @param fechaHasta límite superior inclusivo de {@code fecha_completado} (opcional, puede ser {@code null})
     * @return lista de registros históricos con snapshots de nombre y precio unitario de venta
     */
    List<RegistroConsumoRepuesto> listarConsumoRepuestos(LocalDate fechaDesde, LocalDate fechaHasta);

    /** Recupera las líneas finalizadas y sus repuestos en un rango inclusivo de fechas. */
    List<RegistroServicioSolicitado> listarServiciosSolicitados(LocalDate fechaDesde, LocalDate fechaHasta);
}
