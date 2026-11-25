package com.carmotors.sgt.model;

import java.time.Duration;
import java.util.Optional;

public class TiempoPromedioReparacion {
    private final String tipoMantenimiento;
    private final Optional<Duration> duracionPromedio;

    public TiempoPromedioReparacion(String tipoMantenimiento, Optional<Duration> duracionPromedio) {
        this.tipoMantenimiento = tipoMantenimiento;
        this.duracionPromedio = duracionPromedio;
    }

    public String getTipoMantenimiento() {
        return tipoMantenimiento;
    }

    public Optional<Duration> getDuracionPromedio() {
        return duracionPromedio;
    }
}
