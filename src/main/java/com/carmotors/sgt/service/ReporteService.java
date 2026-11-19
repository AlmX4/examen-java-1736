package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.RegistroConsumoRepuesto;
import com.carmotors.sgt.model.RegistroServicioSolicitado;
import com.carmotors.sgt.model.ResumenServicioSolicitado;
import com.carmotors.sgt.model.ReporteConsumoRepuestos;
import com.carmotors.sgt.model.ReporteConsumoRepuestos.DetalleConsumo;
import com.carmotors.sgt.model.ReporteConsumoRepuestos.ResumenConsumo;
import com.carmotors.sgt.repository.ReporteDAO;
import com.carmotors.sgt.repository.impl.ReporteDAOImpl;
import com.carmotors.sgt.service.state.EstadoCompletado;
import com.carmotors.sgt.service.state.EstadoEntregado;
import com.carmotors.sgt.service.state.EstadoFacturado;
import com.carmotors.sgt.util.InputValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Capa de negocio para los reportes gerenciales de CarMotors SGT (RF-21, RF-25, RF-26, RF-27).
 * Procesa, agrupa y ordena los datos históricos mediante Lambdas y Stream API (RNF-11),
 * utilizando {@link BigDecimal} con escala 2 y redondeo {@code HALF_UP} (RNF-15).
 */
public class ReporteService {

    private static final BigDecimal CERO_MONETARIO =
            BigDecimal.ZERO.setScale(InputValidator.ESCALA_MONETARIA, InputValidator.REDONDEO_MONETARIO);
    private static final int ESTADO_COMPLETADO = new EstadoCompletado().getEstadoId();
    private static final int ESTADO_FACTURADO = new EstadoFacturado().getEstadoId();
    private static final int ESTADO_ENTREGADO = new EstadoEntregado().getEstadoId();

    private final ReporteDAO reporteDAO;

    public ReporteService() {
        this(new ReporteDAOImpl());
    }

    public ReporteService(ReporteDAO reporteDAO) {
        if (reporteDAO == null) {
            throw new ValidacionException("El DAO de reportes es obligatorio.");
        }
        this.reporteDAO = reporteDAO;
    }

    /**
     * Genera el reporte de consumo de repuestos por repuesto y por tipo de servicio en un rango
     * de {@code fecha_completado} (RF-27, §4.2, §4.3, HU-13).
     *
     * <p>Solo contabiliza órdenes en estado {@code Completado}, {@code Facturado} o
     * {@code Entregado}; excluye estrictamente reservas de órdenes {@code Pendiente},
     * {@code En Proceso} y órdenes {@code Cancelada}. El valor comercial se calcula con el
     * {@code precio_unitario_venta} histórico guardado en {@code detalle_repuestos} (RN-11) y
     * todos los resultados se ordenan por valor comercial descendente.</p>
     *
     * @param fechaDesde límite inferior inclusivo de {@code fecha_completado} (puede ser {@code null})
     * @param fechaHasta límite superior inclusivo de {@code fecha_completado} (puede ser {@code null})
     * @return reporte consolidado con totales por repuesto, por tipo de servicio y desglose combinado
     */
    public ReporteConsumoRepuestos generarReporteConsumoRepuestos(LocalDate fechaDesde, LocalDate fechaHasta) {
        validarRangoFechas(fechaDesde, fechaHasta);

        List<RegistroConsumoRepuesto> registrosCrudos = reporteDAO.listarConsumoRepuestos(fechaDesde, fechaHasta);
        List<RegistroConsumoRepuesto> validos = registrosCrudos.stream()
                .filter(registro -> registro != null)
                .filter(registro -> esEstadoCompletadoOPosterior(registro.getEstadoId()))
                .filter(registro -> registro.getFechaCompletado() != null)
                .filter(registro -> estaEnRango(registro.getFechaCompletado().toLocalDate(), fechaDesde, fechaHasta))
                .filter(registro -> registro.getCantidadAsignada() > 0 && registro.getPrecioUnitarioVenta() != null)
                .collect(Collectors.toList());

        List<ResumenConsumo> porRepuesto = agruparPorRepuesto(validos);
        List<ResumenConsumo> porTipoServicio = agruparPorTipoServicio(validos);
        List<DetalleConsumo> detalleCombinado = agruparPorRepuestoYServicio(validos);

        int totalUnidades = validos.stream()
                .mapToInt(RegistroConsumoRepuesto::getCantidadAsignada)
                .sum();
        BigDecimal valorTotal = validos.stream()
                .map(this::calcularValorComercial)
                .reduce(CERO_MONETARIO, BigDecimal::add)
                .setScale(InputValidator.ESCALA_MONETARIA, InputValidator.REDONDEO_MONETARIO);

        return new ReporteConsumoRepuestos(
                fechaDesde, fechaHasta, porRepuesto, porTipoServicio, detalleCombinado, totalUnidades, valorTotal);
    }

    public List<ResumenServicioSolicitado> generarReporteServiciosSolicitados(LocalDate fechaDesde, LocalDate fechaHasta) {
        if (fechaDesde == null || fechaHasta == null) {
            throw new ValidacionException("Indique ambas fechas del rango de finalización.");
        }
        validarRangoFechas(fechaDesde, fechaHasta);

        return reporteDAO.listarServiciosSolicitados(fechaDesde, fechaHasta).stream()
                .filter(registro -> esEstadoCompletadoOPosterior(registro.getEstadoId()))
                .filter(registro -> registro.getFechaCompletado() != null)
                .filter(registro -> estaEnRango(registro.getFechaCompletado().toLocalDate(), fechaDesde, fechaHasta))
                .collect(Collectors.groupingBy(RegistroServicioSolicitado::getServicioTipoId)).values().stream()
                .map(this::resumirTipoServicio)
                .sorted(Comparator.comparingInt(ResumenServicioSolicitado::getCantidadLineas).reversed()
                        .thenComparingInt(ResumenServicioSolicitado::getServicioTipoId))
                .collect(Collectors.toList());
    }

    private ResumenServicioSolicitado resumirTipoServicio(List<RegistroServicioSolicitado> registros) {
        RegistroServicioSolicitado servicio = registros.get(0);
        Map<Integer, List<RegistroServicioSolicitado>> porLinea = registros.stream()
                .collect(Collectors.groupingBy(RegistroServicioSolicitado::getDetalleId));
        BigDecimal ingresos = porLinea.values().stream()
                .map(this::calcularIngresoLinea)
                .reduce(CERO_MONETARIO, BigDecimal::add)
                .setScale(InputValidator.ESCALA_MONETARIA, InputValidator.REDONDEO_MONETARIO);

        return new ResumenServicioSolicitado(servicio.getServicioTipoId(), servicio.getNombreServicio(),
                porLinea.size(), ingresos);
    }

    private BigDecimal calcularIngresoLinea(List<RegistroServicioSolicitado> registros) {
        return registros.stream()
                .filter(registro -> registro.getPrecioUnitarioVenta() != null)
                .map(registro -> registro.getPrecioUnitarioVenta()
                        .multiply(BigDecimal.valueOf(registro.getCantidadRepuesto())))
                .reduce(registros.get(0).getManoDeObra(), BigDecimal::add);
    }

    private void validarRangoFechas(LocalDate fechaDesde, LocalDate fechaHasta) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new ValidacionException("La fecha inicial debe ser anterior o igual a la fecha final.");
        }
    }

    private boolean esEstadoCompletadoOPosterior(int estadoId) {
        return estadoId == ESTADO_COMPLETADO
                || estadoId == ESTADO_FACTURADO
                || estadoId == ESTADO_ENTREGADO;
    }

    private boolean estaEnRango(LocalDate fecha, LocalDate desde, LocalDate hasta) {
        if (desde != null && fecha.isBefore(desde)) {
            return false;
        }
        return hasta == null || !fecha.isAfter(hasta);
    }

    private BigDecimal calcularValorComercial(RegistroConsumoRepuesto registro) {
        return registro.getPrecioUnitarioVenta()
                .multiply(BigDecimal.valueOf(registro.getCantidadAsignada()))
                .setScale(InputValidator.ESCALA_MONETARIA, InputValidator.REDONDEO_MONETARIO);
    }

    private List<ResumenConsumo> agruparPorRepuesto(List<RegistroConsumoRepuesto> registros) {
        Map<Integer, List<RegistroConsumoRepuesto>> grupos = registros.stream()
                .collect(Collectors.groupingBy(RegistroConsumoRepuesto::getRepuestoId));

        return grupos.entrySet().stream()
                .map(entry -> {
                    int repuestoId = entry.getKey();
                    List<RegistroConsumoRepuesto> items = entry.getValue();
                    String nombre = seleccionarNombreMasReciente(items, true);
                    int unidades = items.stream().mapToInt(RegistroConsumoRepuesto::getCantidadAsignada).sum();
                    BigDecimal valor = sumarValorComercial(items);
                    return new ResumenConsumo(repuestoId, nombre, unidades, valor);
                })
                .sorted(comparadorResumenConsumo())
                .collect(Collectors.toList());
    }

    private List<ResumenConsumo> agruparPorTipoServicio(List<RegistroConsumoRepuesto> registros) {
        Map<Integer, List<RegistroConsumoRepuesto>> grupos = registros.stream()
                .collect(Collectors.groupingBy(RegistroConsumoRepuesto::getServicioTipoId));

        return grupos.entrySet().stream()
                .map(entry -> {
                    int servicioTipoId = entry.getKey();
                    List<RegistroConsumoRepuesto> items = entry.getValue();
                    String nombre = seleccionarNombreMasReciente(items, false);
                    int unidades = items.stream().mapToInt(RegistroConsumoRepuesto::getCantidadAsignada).sum();
                    BigDecimal valor = sumarValorComercial(items);
                    return new ResumenConsumo(servicioTipoId, nombre, unidades, valor);
                })
                .sorted(comparadorResumenConsumo())
                .collect(Collectors.toList());
    }

    private List<DetalleConsumo> agruparPorRepuestoYServicio(List<RegistroConsumoRepuesto> registros) {
        Map<String, List<RegistroConsumoRepuesto>> grupos = registros.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getRepuestoId() + ":" + r.getServicioTipoId()));

        return grupos.values().stream()
                .map(items -> {
                    RegistroConsumoRepuesto primero = items.get(0);
                    String nombreRepuesto = seleccionarNombreMasReciente(items, true);
                    String nombreServicio = seleccionarNombreMasReciente(items, false);
                    int unidades = items.stream().mapToInt(RegistroConsumoRepuesto::getCantidadAsignada).sum();
                    BigDecimal valor = sumarValorComercial(items);
                    return new DetalleConsumo(
                            primero.getRepuestoId(), nombreRepuesto,
                            primero.getServicioTipoId(), nombreServicio,
                            unidades, valor);
                })
                .sorted(comparadorDetalleConsumo())
                .collect(Collectors.toList());
    }

    private BigDecimal sumarValorComercial(List<RegistroConsumoRepuesto> items) {
        return items.stream()
                .map(this::calcularValorComercial)
                .reduce(CERO_MONETARIO, BigDecimal::add)
                .setScale(InputValidator.ESCALA_MONETARIA, InputValidator.REDONDEO_MONETARIO);
    }

    private String seleccionarNombreMasReciente(List<RegistroConsumoRepuesto> items, boolean esRepuesto) {
        RegistroConsumoRepuesto seleccionado = items.get(0);
        for (RegistroConsumoRepuesto actual : items) {
            if (actual.getFechaCompletado().isAfter(seleccionado.getFechaCompletado())
                    || (actual.getFechaCompletado().isEqual(seleccionado.getFechaCompletado())
                    && actual.getOrdenId() > seleccionado.getOrdenId())) {
                seleccionado = actual;
            }
        }
        String nombre = esRepuesto ? seleccionado.getNombreRepuesto() : seleccionado.getNombreServicio();
        return nombre == null || nombre.isBlank() ? "Sin nombre" : nombre.trim();
    }

    private Comparator<ResumenConsumo> comparadorResumenConsumo() {
        return (a, b) -> {
            int porValor = b.getValorComercial().compareTo(a.getValorComercial());
            if (porValor != 0) {
                return porValor;
            }
            int porUnidades = Integer.compare(b.getUnidades(), a.getUnidades());
            if (porUnidades != 0) {
                return porUnidades;
            }
            int porNombre = a.getNombre().compareToIgnoreCase(b.getNombre());
            if (porNombre != 0) {
                return porNombre;
            }
            return Integer.compare(a.getId(), b.getId());
        };
    }

    private Comparator<DetalleConsumo> comparadorDetalleConsumo() {
        return (a, b) -> {
            int porValor = b.getValorComercial().compareTo(a.getValorComercial());
            if (porValor != 0) {
                return porValor;
            }
            int porUnidades = Integer.compare(b.getUnidades(), a.getUnidades());
            if (porUnidades != 0) {
                return porUnidades;
            }
            int porRepuesto = a.getNombreRepuesto().compareToIgnoreCase(b.getNombreRepuesto());
            if (porRepuesto != 0) {
                return porRepuesto;
            }
            int porServicio = a.getNombreServicio().compareToIgnoreCase(b.getNombreServicio());
            if (porServicio != 0) {
                return porServicio;
            }
            int porRepuestoId = Integer.compare(a.getRepuestoId(), b.getRepuestoId());
            if (porRepuestoId != 0) {
                return porRepuestoId;
            }
            return Integer.compare(a.getServicioTipoId(), b.getServicioTipoId());
        };
    }
}
