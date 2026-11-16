package com.carmotors.sgt.repository.impl;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.repository.TecnicoRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC para la persistencia de técnicos (RF-33, RNF-07, RNF-08).
 */
public class TecnicoRepositoryImpl implements TecnicoRepository {

    private static final String COLUMNAS_SELECT =
            "SELECT id, nombre, documento_identidad, especialidad_id, telefono, activo FROM tecnicos";

    private final Connection conexionCompartida;

    /**
     * Constructor por defecto que obtiene conexiones desde {@link ConnectionFactory}.
     */
    public TecnicoRepositoryImpl() {
        this.conexionCompartida = null;
    }

    /**
     * Constructor que recibe una conexión compartida para participar en transacciones atómicas.
     *
     * @param conexion conexión JDBC compartida
     */
    public TecnicoRepositoryImpl(Connection conexion) {
        this.conexionCompartida = conexion;
    }

    @Override
    public List<Tecnico> listarTodos() {
        String sql = COLUMNAS_SELECT + " ORDER BY id ASC";
        if (conexionCompartida != null) {
            return ejecutarListado(conexionCompartida, sql);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarListado(conn, sql);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar técnicos: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Tecnico> listarActivos() {
        String sql = COLUMNAS_SELECT + " WHERE activo = TRUE ORDER BY id ASC";
        if (conexionCompartida != null) {
            return ejecutarListado(conexionCompartida, sql);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarListado(conn, sql);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar técnicos activos: " + e.getMessage(), e);
        }
    }

    @Override
    public Tecnico buscarPorId(int id) {
        String sql = COLUMNAS_SELECT + " WHERE id = ?";
        if (conexionCompartida != null) {
            return ejecutarBusquedaPorEntero(conexionCompartida, sql, id);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarBusquedaPorEntero(conn, sql, id);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar técnico por ID " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Tecnico buscarPorDocumento(String documento) {
        String sql = COLUMNAS_SELECT + " WHERE documento_identidad = ?";
        if (conexionCompartida != null) {
            return ejecutarBusquedaPorTexto(conexionCompartida, sql, documento);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarBusquedaPorTexto(conn, sql, documento);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar técnico por documento: " + e.getMessage(), e);
        }
    }

    @Override
    public int guardar(Tecnico tecnico) {
        if (tecnico == null) {
            throw new IllegalArgumentException("El técnico no puede ser nulo.");
        }
        String sql = "INSERT INTO tecnicos (nombre, documento_identidad, especialidad_id, telefono, activo) "
                + "VALUES (?, ?, ?, ?, ?)";
        if (conexionCompartida != null) {
            return ejecutarInsercion(conexionCompartida, sql, tecnico);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarInsercion(conn, sql, tecnico);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar el técnico: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizar(Tecnico tecnico) {
        if (tecnico == null || tecnico.getId() <= 0) {
            throw new IllegalArgumentException("El técnico a actualizar es inválido.");
        }
        String sql = "UPDATE tecnicos SET nombre = ?, documento_identidad = ?, especialidad_id = ?, "
                + "telefono = ?, activo = ? WHERE id = ?";
        if (conexionCompartida != null) {
            return ejecutarActualizacion(conexionCompartida, sql, tecnico);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarActualizacion(conn, sql, tecnico);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar el técnico: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizarEstadoActivo(int id, boolean activo) {
        String sql = "UPDATE tecnicos SET activo = ? WHERE id = ?";
        if (conexionCompartida != null) {
            return ejecutarCambioEstado(conexionCompartida, sql, id, activo);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarCambioEstado(conn, sql, id, activo);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al cambiar el estado del técnico: " + e.getMessage(), e);
        }
    }

    private List<Tecnico> ejecutarListado(Connection conn, String sql) {
        List<Tecnico> tecnicos = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tecnicos.add(mapearTecnico(rs));
            }
            return tecnicos;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el listado de técnicos: " + e.getMessage(), e);
        }
    }

    private Tecnico ejecutarBusquedaPorEntero(Connection conn, String sql, int valor) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearTecnico(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el técnico por identificador: " + e.getMessage(), e);
        }
    }

    private Tecnico ejecutarBusquedaPorTexto(Connection conn, String sql, String valor) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearTecnico(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el técnico por documento: " + e.getMessage(), e);
        }
    }

    private int ejecutarInsercion(Connection conn, String sql, Tecnico tecnico) {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tecnico.getNombre());
            ps.setString(2, tecnico.getDocumentoIdentidad());
            ps.setInt(3, tecnico.getEspecialidadId());
            if (tecnico.getTelefono() == null || tecnico.getTelefono().isEmpty()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, tecnico.getTelefono());
            }
            ps.setBoolean(5, tecnico.isActivo());

            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new CarMotorsException("No se pudo insertar el técnico en la base de datos.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new CarMotorsException("No se obtuvo el ID generado al registrar el técnico.");
        } catch (SQLException e) {
            throw new CarMotorsException("Error al insertar el técnico: " + e.getMessage(), e);
        }
    }

    private boolean ejecutarActualizacion(Connection conn, String sql, Tecnico tecnico) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tecnico.getNombre());
            ps.setString(2, tecnico.getDocumentoIdentidad());
            ps.setInt(3, tecnico.getEspecialidadId());
            if (tecnico.getTelefono() == null || tecnico.getTelefono().isEmpty()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, tecnico.getTelefono());
            }
            ps.setBoolean(5, tecnico.isActivo());
            ps.setInt(6, tecnico.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar el técnico: " + e.getMessage(), e);
        }
    }

    private boolean ejecutarCambioEstado(Connection conn, String sql, int id, boolean activo) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar el estado del técnico: " + e.getMessage(), e);
        }
    }

    private Tecnico mapearTecnico(ResultSet rs) throws SQLException {
        return new Tecnico(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("documento_identidad"),
                rs.getInt("especialidad_id"),
                rs.getString("telefono"),
                rs.getBoolean("activo")
        );
    }
}
