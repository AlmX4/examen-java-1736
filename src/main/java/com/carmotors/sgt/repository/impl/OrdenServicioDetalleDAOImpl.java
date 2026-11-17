package com.carmotors.sgt.repository.impl;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.repository.OrdenServicioDetalleDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de las líneas de servicio ({@code orden_servicio_detalle}).
 * Admite una {@link Connection} compartida para participar en transacciones del servicio.
 */
public class OrdenServicioDetalleDAOImpl implements OrdenServicioDetalleDAO {

    private final Connection conexionCompartida;

    public OrdenServicioDetalleDAOImpl() {
        this(null);
    }

    public OrdenServicioDetalleDAOImpl(Connection conexionCompartida) {
        this.conexionCompartida = conexionCompartida;
    }

    @Override
    public int guardar(OrdenServicioDetalle detalle) {
        if (detalle == null) {
            throw new ValidacionException("La línea de servicio no puede ser nula.");
        }

        String sql = "INSERT INTO orden_servicio_detalle (orden_id, servicio_tipo_id, nombre_servicio_snapshot, "
                + "costo_mano_obra, observaciones) VALUES (?, ?, ?, ?, ?)";

        if (conexionCompartida != null) {
            return insertarDetalle(conexionCompartida, sql, detalle);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return insertarDetalle(connection, sql, detalle);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar la línea de servicio: " + e.getMessage(), e);
        }
    }

    @Override
    public List<OrdenServicioDetalle> listarPorOrden(int ordenId) {
        String sql = "SELECT id, orden_id, servicio_tipo_id, nombre_servicio_snapshot, costo_mano_obra, observaciones "
                + "FROM orden_servicio_detalle WHERE orden_id = ? ORDER BY id ASC";

        if (conexionCompartida != null) {
            return consultarPorOrden(conexionCompartida, sql, ordenId);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarPorOrden(connection, sql, ordenId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar las líneas de la orden " + ordenId + ": " + e.getMessage(), e);
        }
    }

    private int insertarDetalle(Connection connection, String sql, OrdenServicioDetalle detalle) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, detalle.getOrdenId());
            preparedStatement.setInt(2, detalle.getServicioTipoId());
            preparedStatement.setString(3, detalle.getNombreServicioSnapshot());
            preparedStatement.setBigDecimal(4, detalle.getCostoManoObra());
            if (detalle.getObservaciones() != null) {
                preparedStatement.setString(5, detalle.getObservaciones());
            } else {
                preparedStatement.setNull(5, Types.VARCHAR);
            }

            int filas = preparedStatement.executeUpdate();
            if (filas == 0) {
                throw new CarMotorsException("No se insertó ninguna fila para la línea de servicio.");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar la línea de servicio: " + e.getMessage(), e);
        }

        throw new CarMotorsException("No se obtuvo el identificador generado para la línea de servicio.");
    }

    private List<OrdenServicioDetalle> consultarPorOrden(Connection connection, String sql, int ordenId) {
        List<OrdenServicioDetalle> lineas = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, ordenId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    lineas.add(mapearDetalle(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar las líneas de la orden " + ordenId + ": " + e.getMessage(), e);
        }
        return lineas;
    }

    private OrdenServicioDetalle mapearDetalle(ResultSet resultSet) throws SQLException {
        OrdenServicioDetalle detalle = new OrdenServicioDetalle();
        detalle.setId(resultSet.getInt("id"));
        detalle.setOrdenId(resultSet.getInt("orden_id"));
        detalle.setServicioTipoId(resultSet.getInt("servicio_tipo_id"));
        detalle.setNombreServicioSnapshot(resultSet.getString("nombre_servicio_snapshot"));
        detalle.setCostoManoObra(resultSet.getBigDecimal("costo_mano_obra"));
        detalle.setObservaciones(resultSet.getString("observaciones"));
        return detalle;
    }
}
