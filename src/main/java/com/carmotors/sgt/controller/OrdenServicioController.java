package com.carmotors.sgt.controller;

import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.OrdenServicio;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.service.OrdenServicioService;

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
}
