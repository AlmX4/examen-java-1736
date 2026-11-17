package com.carmotors.sgt.controller;

import com.carmotors.sgt.model.OrdenServicio;
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

    public String obtenerNombreEstado(int estadoId) {
        return ordenServicioService.obtenerNombreEstado(estadoId);
    }
}
