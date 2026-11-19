package com.carmotors.sgt.view;

import com.carmotors.sgt.controller.ReportesController;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.ReporteConsumoRepuestos;
import com.carmotors.sgt.model.ReporteConsumoRepuestos.DetalleConsumo;
import com.carmotors.sgt.model.ReporteConsumoRepuestos.ResumenConsumo;
import com.carmotors.sgt.util.ConsoleReader;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Submenú de consola para los reportes gerenciales de CarMotors SGT (RF-21, RF-25, RF-26, RF-27).
 * Solo captura parámetros e imprime resultados; las reglas residen en {@code service} (RNF-03).
 */
public class ReportesView implements SubmenuVista {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReportesController reportesController;

    public ReportesView(ReportesController reportesController) {
        if (reportesController == null) {
            throw new IllegalArgumentException("El controlador de reportes es obligatorio.");
        }
        this.reportesController = reportesController;
    }

    @Override
    public String obtenerTitulo() {
        return "Reportes Gerenciales";
    }

    @Override
    public void mostrarMenu() {
        boolean ejecutando = true;
        while (ejecutando) {
            System.out.println("\n==================================================");
            System.out.println("               REPORTES GERENCIALES               ");
            System.out.println("==================================================");
            System.out.println("  1. Consumo de repuestos (RF-27)");
            System.out.println("  0. Volver");
            System.out.println("==================================================");

            int opcion = ConsoleReader.leerEnteroEnRango("Seleccione una opción", 0, 1);
            switch (opcion) {
                case 1:
                    mostrarReporteConsumoRepuestos();
                    break;
                case 0:
                    ejecutando = false;
                    break;
                default:
                    break;
            }
        }
    }

    private void mostrarReporteConsumoRepuestos() {
        try {
            System.out.println("\n--- Reporte de Consumo de Repuestos (RF-27) ---");
            System.out.println("  Criterio de rango: fecha de completado de órdenes Completadas, Facturadas o Entregadas.");
            LocalDate desde = ConsoleReader.leerFechaOpcional("Completado desde (yyyy-MM-dd, Enter para sin límite)");
            LocalDate hasta = ConsoleReader.leerFechaOpcional("Completado hasta (yyyy-MM-dd, Enter para sin límite)");

            ReporteConsumoRepuestos reporte = reportesController.generarReporteConsumoRepuestos(desde, hasta);
            imprimirReporteConsumoRepuestos(reporte);
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] No se pudo generar el reporte: " + e.getMessage());
        } finally {
            ConsoleReader.pausar();
        }
    }

    private void imprimirReporteConsumoRepuestos(ReporteConsumoRepuestos reporte) {
        System.out.println("\n==================================================");
        System.out.println("         CONSUMO DE REPUESTOS (VALOR COMERCIAL)   ");
        System.out.println("==================================================");
        System.out.println("  Rango fecha_completado: " + formatearRango(reporte.getFechaDesde(), reporte.getFechaHasta()));
        System.out.println("  Nota: Excluye reservas de órdenes Pendientes/En Proceso y órdenes Canceladas.");
        System.out.println("        Valorizado al precio de venta histórico (valor comercial, no costo ni utilidad).");

        if (reporte.estaVacio()) {
            System.out.println("\n  [i] No se encontraron consumos de repuestos en el rango de fechas indicado.");
            return;
        }

        System.out.println("\n  1) Consumo total por repuesto (ordenado por valor comercial descendente):");
        for (ResumenConsumo item : reporte.getPorRepuesto()) {
            System.out.println("    - Repuesto " + item.getId() + " | " + item.getNombre()
                    + " | Unidades: " + item.getUnidades()
                    + " | Valor comercial: $" + item.getValorComercial().toPlainString());
        }

        System.out.println("\n  2) Consumo total por tipo de servicio (ordenado por valor comercial descendente):");
        for (ResumenConsumo item : reporte.getPorTipoServicio()) {
            System.out.println("    - Servicio " + item.getId() + " | " + item.getNombre()
                    + " | Unidades: " + item.getUnidades()
                    + " | Valor comercial: $" + item.getValorComercial().toPlainString());
        }

        System.out.println("\n  3) Desglose por repuesto y tipo de servicio:");
        for (DetalleConsumo detalle : reporte.getDetallePorRepuestoYServicio()) {
            System.out.println("    - " + detalle.getNombreRepuesto() + " (ID " + detalle.getRepuestoId() + ")"
                    + " | Servicio: " + detalle.getNombreServicio() + " (ID " + detalle.getServicioTipoId() + ")"
                    + " | Unidades: " + detalle.getUnidades()
                    + " | Valor comercial: $" + detalle.getValorComercial().toPlainString());
        }

        System.out.println("\n  --------------------------------------------------");
        System.out.println("  Total unidades consumidas: " + reporte.getTotalUnidades());
        System.out.println("  Valor comercial total:     $" + reporte.getValorComercialTotal().toPlainString());
        System.out.println("  --------------------------------------------------");
    }

    private String formatearRango(LocalDate desde, LocalDate hasta) {
        String textoDesde = desde != null ? desde.format(FORMATO_FECHA) : "Inicio";
        String textoHasta = hasta != null ? hasta.format(FORMATO_FECHA) : "Actualidad";
        return textoDesde + " a " + textoHasta;
    }
}
