package com.carmotors.sgt.repository.impl;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.RegistroConsumoRepuesto;
import com.carmotors.sgt.repository.ReporteDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de las consultas de reportes gerenciales (RNF-05, RNF-06).
 * Todas las consultas parametrizables utilizan {@link PreparedStatement}.
 */
public class ReporteDAOImpl implements ReporteDAO {

    private final Connection conexionCompartida;

    public ReporteDAOImpl() {
        this(null);
    }

    public ReporteDAOImpl(Connection conexionCompartida) {
        this.conexionCompartida = conexionCompartida;
    }

    @Override
    public List<RegistroConsumoRepuesto> listarConsumoRepuestos(LocalDate fechaDesde, LocalDate fechaHasta) {
        StringBuilder sql = new StringBuilder(
                "SELECT o.id AS orden_id, "
                        + "o.estado_id, "
                        + "o.fecha_completado, "
                        + "dr.repuesto_id, "
                        + "dr.nombre_repuesto_snapshot, "
                        + "osd.servicio_tipo_id, "
                        + "osd.nombre_servicio_snapshot, "
                        + "dr.cantidad_asignada, "
                        + "dr.precio_unitario_venta "
                        + "FROM ordenes_servicio o "
                        + "INNER JOIN orden_servicio_detalle osd ON osd.orden_id = o.id "
                        + "INNER JOIN detalle_repuestos dr ON dr.detalle_id = osd.id "
                        + "WHERE o.estado_id IN (3, 4, 5) "
                        + "AND o.fecha_completado IS NOT NULL");

        if (fechaDesde != null) {
            sql.append(" AND o.fecha_completado >= ?");
        }
        if (fechaHasta != null) {
            sql.append(" AND o.fecha_completado < ?");
        }
        sql.append(" ORDER BY o.fecha_completado ASC, o.id ASC, osd.id ASC, dr.repuesto_id ASC");

        if (conexionCompartida != null) {
            return ejecutarConsultaConsumo(conexionCompartida, sql.toString(), fechaDesde, fechaHasta);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarConsultaConsumo(connection, sql.toString(), fechaDesde, fechaHasta);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el consumo de repuestos: " + e.getMessage(), e);
        }
    }

    private List<RegistroConsumoRepuesto> ejecutarConsultaConsumo(Connection connection, String sql,
                                                                  LocalDate fechaDesde, LocalDate fechaHasta) {
        List<RegistroConsumoRepuesto> registros = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            int indice = 1;
            if (fechaDesde != null) {
                preparedStatement.setTimestamp(indice++, Timestamp.valueOf(fechaDesde.atStartOfDay()));
            }
            if (fechaHasta != null) {
                preparedStatement.setTimestamp(indice, Timestamp.valueOf(fechaHasta.plusDays(1).atStartOfDay()));
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    Timestamp completado = resultSet.getTimestamp("fecha_completado");
                    RegistroConsumoRepuesto registro = new RegistroConsumoRepuesto(
                            resultSet.getInt("orden_id"),
                            resultSet.getInt("estado_id"),
                            completado != null ? completado.toLocalDateTime() : null,
                            resultSet.getInt("repuesto_id"),
                            resultSet.getString("nombre_repuesto_snapshot"),
                            resultSet.getInt("servicio_tipo_id"),
                            resultSet.getString("nombre_servicio_snapshot"),
                            resultSet.getInt("cantidad_asignada"),
                            resultSet.getBigDecimal("precio_unitario_venta")
                    );
                    registros.add(registro);
                }
            }
            return registros;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el consumo de repuestos: " + e.getMessage(), e);
        }
    }
}
