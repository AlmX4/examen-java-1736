package com.carmotors.sgt.controller;

import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.DetalleTecnico;
import com.carmotors.sgt.model.OrdenServicio;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.service.OrdenServicioService;
import com.carmotors.sgt.service.chain.SugerenciaTecnico;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controlador de órdenes de servicio (RNF-04): delega en {@link OrdenServicioService}.
 */
public class OrdenServicioController {

    private final OrdenServicioService ordenServicioService;

    public OrdenServicioController(OrdenServicioService ordenServicioService) {
        if (ordenServicioService == null) {
            throw new IllegalArgumentException("El servicio de órdenes de servicio es obligatorio.");
        }
        this.ordenServicioService = ordenServicioService;
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
}
