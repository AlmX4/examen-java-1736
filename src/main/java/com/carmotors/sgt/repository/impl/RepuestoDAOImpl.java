package com.carmotors.sgt.repository.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.repository.RepuestoDAO;

public class RepuestoDAOImpl implements RepuestoDAO {

    private final Connection conexionCompartida;

    public RepuestoDAOImpl() {
        this.conexionCompartida = null;
    }

    public RepuestoDAOImpl(Connection conexionCompartida) {
        this.conexionCompartida = conexionCompartida;
    }

    @Override
    public List<Repuesto> listarTodos() {
        String sql = "SELECT id, repuesto_tipo_id, nombre, marca, precio_venta, cantidad_reservada, stock_minimo, activo " +
                "FROM repuestos ORDER BY nombre ASC";
        if (conexionCompartida != null) {
            return consultarLista(conexionCompartida, sql);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarLista(connection, sql);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar repuestos: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Repuesto> listarActivos() {
        String sql = "SELECT id, repuesto_tipo_id, nombre, marca, precio_venta, cantidad_reservada, stock_minimo, activo " +
                "FROM repuestos WHERE activo = true ORDER BY nombre ASC";
        if (conexionCompartida != null) {
            return consultarLista(conexionCompartida, sql);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarLista(connection, sql);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar repuestos activos: " + e.getMessage(), e);
        }
    }

    @Override
    public Repuesto buscarPorId(int id) {
        if (id <= 0) {
            throw new ValidacionException("El ID del repuesto debe ser un número positivo.");
        }

        String sql = "SELECT id, repuesto_tipo_id, nombre, marca, precio_venta, cantidad_reservada, stock_minimo, activo " +
                "FROM repuestos WHERE id = ?";
        if (conexionCompartida != null) {
            return consultarPorId(conexionCompartida, sql, id);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarPorId(connection, sql, id);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar repuesto por ID " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Repuesto buscarPorNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT id, repuesto_tipo_id, nombre, marca, precio_venta, cantidad_reservada, stock_minimo, activo " +
                "FROM repuestos WHERE LOWER(nombre) = LOWER(?)";
        if (conexionCompartida != null) {
            return consultarPorNombre(conexionCompartida, sql, nombre.trim());
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarPorNombre(connection, sql, nombre.trim());
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar repuesto por nombre: " + e.getMessage(), e);
        }
    }

    @Override
    public int guardar(Repuesto repuesto) {
        if (repuesto == null) {
            throw new ValidacionException("El repuesto no puede ser nulo.");
        }

        String sql = "INSERT INTO repuestos (repuesto_tipo_id, nombre, marca, precio_venta, cantidad_reservada, stock_minimo, activo) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        if (conexionCompartida != null) {
            return insertarRepuesto(conexionCompartida, sql, repuesto);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return insertarRepuesto(connection, sql, repuesto);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar el repuesto: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizar(Repuesto repuesto) {
        if (repuesto == null || repuesto.getId() <= 0) {
            throw new ValidacionException("El repuesto a actualizar es inválido.");
        }

        String sql = "UPDATE repuestos SET repuesto_tipo_id = ?, nombre = ?, marca = ?, precio_venta = ?, " +
                "stock_minimo = ?, activo = ? WHERE id = ?";
        if (conexionCompartida != null) {
            return ejecutarActualizar(conexionCompartida, sql, repuesto);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarActualizar(connection, sql, repuesto);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar el repuesto: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean desactivar(int id) {
        String sql = "UPDATE repuestos SET activo = false WHERE id = ?";
        if (conexionCompartida != null) {
            return ejecutarCambioEstado(conexionCompartida, sql, id);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarCambioEstado(connection, sql, id);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al desactivar el repuesto: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean reactivar(int id) {
        String sql = "UPDATE repuestos SET activo = true WHERE id = ?";
        if (conexionCompartida != null) {
            return ejecutarCambioEstado(conexionCompartida, sql, id);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarCambioEstado(connection, sql, id);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al reactivar el repuesto: " + e.getMessage(), e);
        }
    }

    private List<Repuesto> consultarLista(Connection connection, String sql) {
        List<Repuesto> lista = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                lista.add(mapearRepuesto(resultSet));
            }
            return lista;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar repuestos: " + e.getMessage(), e);
        }
    }

    private Repuesto consultarPorId(Connection connection, String sql, int id) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapearRepuesto(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar repuesto por ID " + id + ": " + e.getMessage(), e);
        }
    }

    private Repuesto consultarPorNombre(Connection connection, String sql, String nombre) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, nombre);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapearRepuesto(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar repuesto por nombre: " + e.getMessage(), e);
        }
    }

    private int insertarRepuesto(Connection connection, String sql, Repuesto repuesto) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, repuesto.getRepuestoTipoId());
            preparedStatement.setString(2, repuesto.getNombre());
            preparedStatement.setString(3, repuesto.getMarca());
            preparedStatement.setBigDecimal(4, repuesto.getPrecioVenta());
            preparedStatement.setInt(5, repuesto.getCantidadReservada());
            preparedStatement.setInt(6, repuesto.getStockMinimo());
            preparedStatement.setBoolean(7, repuesto.isActivo());

            int filas = preparedStatement.executeUpdate();
            if (filas == 0) {
                throw new CarMotorsException("No se insertó ningún repuesto en la base de datos.");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar el repuesto: " + e.getMessage(), e);
        }

        throw new CarMotorsException("No se obtuvo el ID generado al registrar el repuesto.");
    }

    private boolean ejecutarActualizar(Connection connection, String sql, Repuesto repuesto) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, repuesto.getRepuestoTipoId());
            preparedStatement.setString(2, repuesto.getNombre());
            preparedStatement.setString(3, repuesto.getMarca());
            preparedStatement.setBigDecimal(4, repuesto.getPrecioVenta());
            preparedStatement.setInt(5, repuesto.getStockMinimo());
            preparedStatement.setBoolean(6, repuesto.isActivo());
            preparedStatement.setInt(7, repuesto.getId());
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar el repuesto: " + e.getMessage(), e);
        }
    }

    private boolean ejecutarCambioEstado(Connection connection, String sql, int id) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al cambiar el estado del repuesto: " + e.getMessage(), e);
        }
    }

    private Repuesto mapearRepuesto(ResultSet resultSet) throws SQLException {
        Repuesto repuesto = new Repuesto();
        repuesto.setId(resultSet.getInt("id"));
        repuesto.setRepuestoTipoId(resultSet.getInt("repuesto_tipo_id"));
        repuesto.setNombre(resultSet.getString("nombre"));
        repuesto.setMarca(resultSet.getString("marca"));
        BigDecimal precioVenta = resultSet.getBigDecimal("precio_venta");
        repuesto.setPrecioVenta(precioVenta == null ? BigDecimal.ZERO : precioVenta);
        repuesto.setCantidadReservada(resultSet.getInt("cantidad_reservada"));
        repuesto.setStockMinimo(resultSet.getInt("stock_minimo"));
        repuesto.setActivo(resultSet.getBoolean("activo"));
        return repuesto;
    }
}
