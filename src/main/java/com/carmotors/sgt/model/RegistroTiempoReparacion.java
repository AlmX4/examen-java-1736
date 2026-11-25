package com.carmotors.sgt.model;

import java.time.LocalDateTime;

public class RegistroTiempoReparacion {
    private final String tipoMantenimiento;
    private final LocalDateTime fechaRecepcion;
    private final LocalDateTime fechaEntrega;

    public RegistroTiempoReparacion(String tipoMantenimiento, LocalDateTime fechaRecepcion, LocalDateTime fechaEntrega) {
        this.tipoMantenimiento = tipoMantenimiento;
        this.fechaRecepcion = fechaRecepcion;
        this.fechaEntrega = fechaEntrega;
    }

    public String getTipoMantenimiento() {
        return tipoMantenimiento;
    }

    public LocalDateTime getFechaRecepcion() {
        return fechaRecepcion;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }
}
