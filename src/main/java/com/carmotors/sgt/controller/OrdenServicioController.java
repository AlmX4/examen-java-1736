package com.carmotors.sgt.controller;

import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.ConsultaOrdenServicio;
import com.carmotors.sgt.model.FiltroOrdenServicio;
import com.carmotors.sgt.model.ResumenOrdenServicio;
import com.carmotors.sgt.model.DetalleRepuesto;
import com.carmotors.sgt.model.DetalleTecnico;
import com.carmotors.sgt.model.OrdenServicio;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.service.OrdenServicioService;
import com.carmotors.sgt.service.chain.SugerenciaTecnico;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Controlador de órdenes de servicio (RNF-04): delega en {@link OrdenServicioService}.
 */
public class OrdenServicioController {

    private static final Logger LOGGER = Logger.getLogger(OrdenServicioController.class.getName());

    private final OrdenServicioService ordenServicioService;

    public OrdenServicioController(OrdenServicioService ordenServicioService) {
        if (ordenServicioService == null) {
            throw new IllegalArgumentException("El servicio de órdenes de servicio es obligatorio.");
        }
        this.ordenServicioService = ordenServicioService;
    }

    public Consulta<List<ResumenOrdenServicio>> consultarOrdenes(FiltroOrdenServicio filtro) {
        return consultar(() -> ordenServicioService.consultarOrdenes(filtro));
    }

    public Consulta<ConsultaOrdenServicio> consultarDetalle(int ordenId) {
        return consultar(() -> ordenServicioService.consultarDetalle(ordenId));
    }

    private <T> Consulta<T> consultar(Supplier<T> operacion) {
        try {
            return new Consulta<>(operacion.get(), null);
        } catch (CarMotorsException e) {
            if (e.getCause() instanceof SQLException) {
                SQLException error = (SQLException) e.getCause();
                LOGGER.warning("Falló una consulta de órdenes (código JDBC " + error.getErrorCode() + ").");
                return new Consulta<>(null, "No se pudo consultar la información de órdenes.");
            }
            return new Consulta<>(null, e.getMessage());
        }
    }

    public static class Consulta<T> {
        private final T datos;
        private final String error;

        private Consulta(T datos, String error) {
            this.datos = datos;
            this.error = error;
        }

        public T getDatos() { return datos; }
        public String getError() { return error; }
    }

    public OrdenServicio crearOrden(int vehiculoId, String descripcionProblema, List<OrdenServicioDetalle> lineas) {
        return ordenServicioService.crearOrden(vehiculoId, descripcionProblema, lineas);
    }

    public List<OrdenServicioDetalle> listarLineas(int ordenId) {
        return ordenServicioService.listarLineas(ordenId);
    }

    public boolean requiereTecnicoParaAgregar(int ordenId) {
        return ordenServicioService.requiereTecnicoParaAgregar(ordenId);
    }

    public boolean especialidadCoincide(ServicioTipo tipo, Tecnico tecnico) {
        return ordenServicioService.especialidadCoincide(tipo, tecnico);
    }

    public String agregarLinea(int ordenId, OrdenServicioDetalle linea, Integer tecnicoId,
                               boolean confirmarOtraEspecialidad) {
        try {
            int detalleId = ordenServicioService.agregarLinea(ordenId, linea, tecnicoId, confirmarOtraEspecialidad);
            return "Línea de servicio agregada. ID: " + detalleId;
        } catch (CarMotorsException e) {
            return "No se pudo agregar la línea: " + e.getMessage();
        }
    }

    public String quitarLinea(int ordenId, int detalleId) {
        try {
            ordenServicioService.quitarLinea(ordenId, detalleId);
            return "Línea de servicio retirada.";
        } catch (CarMotorsException e) {
            return "No se pudo retirar la línea: " + e.getMessage();
        }
    }

    public String obtenerNombreEstado(int estadoId) {
        return ordenServicioService.obtenerNombreEstado(estadoId);
    }

    public void validarOrdenEditable(int ordenId) {
        ordenServicioService.validarOrdenEditable(ordenId);
    }

    public SugerenciaTecnico sugerirTecnicoParaLinea(int ordenId, int detalleId) {
        return ordenServicioService.sugerirTecnicoParaLinea(ordenId, detalleId);
    }

    public boolean requiereConfirmacionEspecialidad(int ordenId, int detalleId, int tecnicoId) {
        return ordenServicioService.requiereConfirmacionEspecialidad(ordenId, detalleId, tecnicoId);
    }

    public List<DetalleTecnico> listarTecnicosDeLinea(int ordenId, int detalleId) {
        return ordenServicioService.listarTecnicosDeLinea(ordenId, detalleId);
    }

    public String asignarTecnicoALinea(
            int ordenId,
            int detalleId,
            int tecnicoId,
            BigDecimal horasTrabajadas,
            boolean confirmarOtraEspecialidad) {
        try {
            DetalleTecnico asignacion = ordenServicioService.asignarTecnicoALinea(
                    ordenId, detalleId, tecnicoId, horasTrabajadas, confirmarOtraEspecialidad);
            return "Técnico asignado a la línea " + detalleId
                    + " (horas registradas: " + asignacion.getHorasTrabajadas() + " h).";
        } catch (CarMotorsException e) {
            return "No se pudo asignar el técnico: " + e.getMessage();
        }
    }

    public String registrarHorasTrabajadas(
            int ordenId,
            int detalleId,
            int tecnicoId,
            BigDecimal horasTrabajadas) {
        try {
            DetalleTecnico actualizada = ordenServicioService.registrarHorasTrabajadas(
                    ordenId, detalleId, tecnicoId, horasTrabajadas);
            return "Horas trabajadas registradas: " + actualizada.getHorasTrabajadas() + " h.";
        } catch (CarMotorsException e) {
            return "No se pudieron registrar las horas: " + e.getMessage();
        }
    }

    public DetalleRepuesto reservarRepuesto(int detalleId, int repuestoId, int cantidad) {
        return ordenServicioService.reservarRepuesto(detalleId, repuestoId, cantidad);
    }

    public int consultarDisponible(int repuestoId) {
        return ordenServicioService.consultarDisponible(repuestoId);
    }
}
