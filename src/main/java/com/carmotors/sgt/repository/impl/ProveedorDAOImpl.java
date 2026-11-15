package com.carmotors.sgt.repository.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.repository.ProveedorDAO;

public class ProveedorDAOImpl implements ProveedorDAO {

    @Override
    public List<Proveedor> listarTodos() {
        String sql = "SELECT id, nombre, telefono, email, direccion, activo FROM proveedores ORDER BY nombre ASC";
        List<Proveedor> proveedores = new ArrayList<>();
        try (Connection connection = ConnectionFactory.getInstance().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                proveedores.add(mapearProveedor(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudieron listar los proveedores.", e);
        }
        return proveedores;
    }

    @Override
    public List<Proveedor> listarActivos() {
        String sql = "SELECT id, nombre, telefono, email, direccion, activo FROM proveedores WHERE activo = true ORDER BY nombre ASC";
        List<Proveedor> proveedores = new ArrayList<>();
        try (Connection connection = ConnectionFactory.getInstance().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                proveedores.add(mapearProveedor(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudieron listar los proveedores activos.", e);
        }
        return proveedores;
    }

    @Override
    public Optional<Proveedor> buscarPorId(int id) {
        String sql = "SELECT id, nombre, telefono, email, direccion, activo FROM proveedores WHERE id = ?";
        try (Connection connection = ConnectionFactory.getInstance().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapearProveedor(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar el proveedor por id.", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Proveedor> buscarPorEmail(String email) {
        String sql = "SELECT id, nombre, telefono, email, direccion, activo FROM proveedores WHERE LOWER(email) = LOWER(?)";
        try (Connection connection = ConnectionFactory.getInstance().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, email);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapearProveedor(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar el proveedor por email.", e);
        }
        return Optional.empty();
    }

    @Override
    public int guardar(Proveedor proveedor) {
        if (proveedor == null) {
            throw new IllegalArgumentException("El proveedor no puede ser nulo.");
        }

        String sql = "INSERT INTO proveedores (nombre, telefono, email, direccion, activo) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = ConnectionFactory.getInstance().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setString(1, proveedor.getNombre());
            preparedStatement.setString(2, proveedor.getTelefono());
            preparedStatement.setString(3, proveedor.getEmail());
            preparedStatement.setString(4, proveedor.getDireccion());
            preparedStatement.setBoolean(5, proveedor.isActivo());

            int filas = preparedStatement.executeUpdate();
            if (filas == 0) {
                throw new RuntimeException("No se insertó ningún proveedor.");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo guardar el proveedor.", e);
        }
        throw new RuntimeException("No se obtuvo el id generado del proveedor.");
    }

    @Override
    public boolean actualizar(Proveedor proveedor) {
        if (proveedor == null || proveedor.getId() <= 0) {
            throw new IllegalArgumentException("El proveedor a actualizar es inválido.");
        }

        String sql = "UPDATE proveedores SET nombre = ?, telefono = ?, email = ?, direccion = ?, activo = ? WHERE id = ?";
        try (Connection connection = ConnectionFactory.getInstance().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, proveedor.getNombre());
            preparedStatement.setString(2, proveedor.getTelefono());
            preparedStatement.setString(3, proveedor.getEmail());
            preparedStatement.setString(4, proveedor.getDireccion());
            preparedStatement.setBoolean(5, proveedor.isActivo());
            preparedStatement.setInt(6, proveedor.getId());

            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo actualizar el proveedor.", e);
        }
    }

    @Override
    public boolean desactivar(int id) {
        String sql = "UPDATE proveedores SET activo = false WHERE id = ?";
        try (Connection connection = ConnectionFactory.getInstance().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo desactivar el proveedor.", e);
        }
    }

    @Override
    public boolean reactivar(int id) {
        String sql = "UPDATE proveedores SET activo = true WHERE id = ?";
        try (Connection connection = ConnectionFactory.getInstance().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo reactivar el proveedor.", e);
        }
    }

    private Proveedor mapearProveedor(ResultSet resultSet) throws SQLException {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(resultSet.getInt("id"));
        proveedor.setNombre(resultSet.getString("nombre"));
        proveedor.setTelefono(resultSet.getString("telefono"));
        proveedor.setEmail(resultSet.getString("email"));
        proveedor.setDireccion(resultSet.getString("direccion"));
        proveedor.setActivo(resultSet.getBoolean("activo"));
        return proveedor;
    }
}
