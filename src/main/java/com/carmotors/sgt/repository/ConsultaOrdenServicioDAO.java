package com.carmotors.sgt.repository;

import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.FiltroOrdenServicio;
import com.carmotors.sgt.model.ResumenOrdenServicio;
import com.carmotors.sgt.model.Vehiculo;

import java.sql.SQLException;
import java.util.List;

public interface ConsultaOrdenServicioDAO {
    List<ResumenOrdenServicio> listar(FiltroOrdenServicio filtro) throws SQLException;

    void cargarDatosFacturados(int ordenId, Cliente cliente, Vehiculo vehiculo) throws SQLException;
}
