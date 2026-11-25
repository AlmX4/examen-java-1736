package com.carmotors.sgt.service;

import com.carmotors.sgt.dao.impl.ReporteDAOImpl;
import com.carmotors.sgt.model.RegistroTiempoReparacion;
import com.carmotors.sgt.model.TiempoPromedioReparacion;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReporteTiemposPromedioTest {

    @Test
    void promediaPorTipoConPrecisionDeSegundosYOmiteFechasInvertidas() {
        LocalDateTime recepcion = LocalDateTime.of(2026, 11, 20, 8, 0);
        List<RegistroTiempoReparacion> registros = List.of(
                new RegistroTiempoReparacion("Preventivo", recepcion, recepcion.plusHours(24).plusSeconds(30)),
                new RegistroTiempoReparacion("Preventivo", recepcion, recepcion.plusHours(26).plusSeconds(90)),
                new RegistroTiempoReparacion("Preventivo", recepcion, recepcion.minusHours(1)));

        ReporteService servicio = new ReporteService(new ReporteDAOImpl() {
            @Override
            public List<RegistroTiempoReparacion> listarTiemposReparacion() {
                return registros;
            }
        });

        List<TiempoPromedioReparacion> resultados = servicio.generarReporteTiemposPromedio();

        assertEquals(2, resultados.size());
        assertEquals("Preventivo", resultados.get(0).getTipoMantenimiento());
        assertEquals(Optional.of(Duration.ofHours(25).plusMinutes(1)), resultados.get(0).getDuracionPromedio());
        assertEquals("Correctivo", resultados.get(1).getTipoMantenimiento());
        assertEquals(Optional.empty(), resultados.get(1).getDuracionPromedio());
    }
}
