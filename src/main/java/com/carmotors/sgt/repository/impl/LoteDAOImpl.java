package com.carmotors.sgt.repository.impl;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Lote;
import com.carmotors.sgt.repository.LoteDAO;

public class LoteDAOImpl implements LoteDAO {

    private final Connection conexionCompartida;

    public LoteDAOImpl() {
        this(null);
    }

    public LoteDAOImpl(Connection conexionCompartida) {
        this.conexionCompartida = conexionCompartida;
    }

    @Override
    public Lote buscarPorCodigo(String codigoLote) {
        if (codigoLote == null || codigoLote.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT id, codigo_lote, proveedor_id, repuesto_id, fecha_ingreso, fecha_caducidad, cantidad_inicial, cantidad_actual "
                + "FROM lotes WHERE LOWER(codigo_lote) = LOWER(?)";

        if (conexionCompartida != null) {
            return consultarPorCodigo(conexionCompartida, sql, codigoLote.trim());
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarPorCodigo(connection, sql, codigoLote.trim());
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar el lote por código: " + e.getMessage(), e);
        }
    }

    @Override
    public int guardar(Lote lote) {
        if (lote == null) {
            throw new ValidacionException("El lote no puede ser nulo.");
        }

        String sql = "INSERT INTO lotes (codigo_lote, proveedor_id, repuesto_id, fecha_ingreso, fecha_caducidad, cantidad_inicial, cantidad_actual) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        if (conexionCompartida != null) {
            return insertarLote(conexionCompartida, sql, lote);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return insertarLote(connection, sql, lote);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar el lote: " + e.getMessage(), e);
        }
    }

    private Lote consultarPorCodigo(Connection connection, String sql, String codigoLote) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, codigoLote);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapearLote(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el lote por código: " + e.getMessage(), e);
        }
    }

    private int insertarLote(Connection connection, String sql, Lote lote) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, lote.getCodigoLote());
            preparedStatement.setInt(2, lote.getProveedorId());
            preparedStatement.setInt(3, lote.getRepuestoId());
            preparedStatement.setDate(4, Date.valueOf(lote.getFechaIngreso()));
            if (lote.getFechaCaducidad() != null) {
                preparedStatement.setDate(5, Date.valueOf(lote.getFechaCaducidad()));
            } else {
                preparedStatement.setNull(5, java.sql.Types.DATE);
            }
            preparedStatement.setInt(6, lote.getCantidadInicial());
            preparedStatement.setInt(7, lote.getCantidadActual());

            int filas = preparedStatement.executeUpdate();
            if (filas == 0) {
                throw new CarMotorsException("No se insertó ninguna fila para el lote indicado.");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar el lote: " + e.getMessage(), e);
        }

        throw new CarMotorsException("No se obtuvo el identificador generado para el lote.");
    }

    private Lote mapearLote(ResultSet resultSet) throws SQLException {
        Lote lote = new Lote();
        lote.setId(resultSet.getInt("id"));
        lote.setCodigoLote(resultSet.getString("codigo_lote"));
        lote.setProveedorId(resultSet.getInt("proveedor_id"));
        lote.setRepuestoId(resultSet.getInt("repuesto_id"));
        Date fechaIngreso = resultSet.getDate("fecha_ingreso");
        lote.setFechaIngreso(fechaIngreso != null ? fechaIngreso.toLocalDate() : null);
        Date fechaCaducidad = resultSet.getDate("fecha_caducidad");
        lote.setFechaCaducidad(fechaCaducidad != null ? fechaCaducidad.toLocalDate() : null);
        lote.setCantidadInicial(resultSet.getInt("cantidad_inicial"));
        lote.setCantidadActual(resultSet.getInt("cantidad_actual"));
        return lote;
    }
}
