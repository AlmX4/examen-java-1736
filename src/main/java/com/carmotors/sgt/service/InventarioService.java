package com.carmotors.sgt.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.model.StockRepuesto;
import com.carmotors.sgt.repository.LoteDAO;
import com.carmotors.sgt.repository.RepuestoDAO;
import com.carmotors.sgt.repository.impl.LoteDAOImpl;
import com.carmotors.sgt.repository.impl.RepuestoDAOImpl;

/**
 * Cálculo de stock por repuesto según la sección 2.5 del documento maestro (RF-07).
 *
 * <p>Los lotes son la fuente de verdad: el stock físico y el operativo se obtienen con consultas
 * de agregación sobre {@code lotes}. El reservado proviene de {@code repuestos.cantidad_reservada}.</p>
 */
public class InventarioService {

    private final RepuestoDAO repuestoDAO;
    private final LoteDAO loteDAO;

    public InventarioService() {
        this(new RepuestoDAOImpl(), new LoteDAOImpl());
    }

    public InventarioService(RepuestoDAO repuestoDAO, LoteDAO loteDAO) {
        if (repuestoDAO == null) {
            throw new ValidacionException("El DAO de repuestos es obligatorio.");
        }
        if (loteDAO == null) {
            throw new ValidacionException("El DAO de lotes es obligatorio.");
        }
        this.repuestoDAO = repuestoDAO;
        this.loteDAO = loteDAO;
    }

    /**
     * Calcula las métricas de stock del repuesto indicado.
     */
    public StockRepuesto calcularStock(int repuestoId) {
        if (repuestoId <= 0) {
            throw new ValidacionException("El ID del repuesto debe ser un número positivo.");
        }

        Repuesto repuesto = repuestoDAO.buscarPorId(repuestoId);
        if (repuesto == null) {
            throw new EntidadNoEncontradaException("No se encontró un repuesto con el ID: " + repuestoId);
        }
        return calcularStock(repuesto, LocalDate.now());
    }

    /**
     * Calcula las métricas de stock de todos los repuestos activos, para reportes como stock bajo (T3.7).
     */
    public List<StockRepuesto> calcularStockActivos() {
        LocalDate hoy = LocalDate.now();
        return repuestoDAO.listarActivos().stream()
                .map(repuesto -> calcularStock(repuesto, hoy))
                .collect(Collectors.toList());
    }

    private StockRepuesto calcularStock(Repuesto repuesto, LocalDate fechaReferencia) {
        int stockFisico = loteDAO.sumarStockFisico(repuesto.getId());
        int stockOperativo = loteDAO.sumarStockOperativo(repuesto.getId(), fechaReferencia);
        int reservado = repuesto.getCantidadReservada();
        int disponible = Math.max(0, stockOperativo - reservado);
        int deficit = Math.max(0, reservado - stockOperativo);

        return new StockRepuesto(repuesto, stockFisico, stockOperativo, reservado,
                disponible, deficit, repuesto.getStockMinimo());
    }
}
