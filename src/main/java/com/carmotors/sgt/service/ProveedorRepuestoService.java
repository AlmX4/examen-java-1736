package com.carmotors.sgt.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.repository.ProveedorDAO;
import com.carmotors.sgt.repository.ProveedorRepuestoDAO;
import com.carmotors.sgt.repository.RepuestoDAO;
import com.carmotors.sgt.repository.impl.ProveedorDAOImpl;
import com.carmotors.sgt.repository.impl.ProveedorRepuestoDAOImpl;
import com.carmotors.sgt.repository.impl.RepuestoDAOImpl;

/**
 * Reglas del catálogo proveedor-repuesto (RF-29, RN-18).
 *
 * <p>Las validaciones usan los DAO inyectados, por eso se pueden probar en memoria.
 * El cambio de proveedor habitual (desmarcar el anterior y marcar el nuevo) se ejecuta
 * en {@link #ejecutarCambioHabitual(int, int)} dentro de una única transacción JDBC.</p>
 */
public class ProveedorRepuestoService {

    private static final int ESCALA_COSTO = 2;

    private final ProveedorRepuestoDAO proveedorRepuestoDAO;
    private final ProveedorDAO proveedorDAO;
    private final RepuestoDAO repuestoDAO;

    public ProveedorRepuestoService() {
        this(new ProveedorRepuestoDAOImpl(), new ProveedorDAOImpl(), new RepuestoDAOImpl());
    }

    public ProveedorRepuestoService(ProveedorRepuestoDAO proveedorRepuestoDAO, ProveedorDAO proveedorDAO,
                                    RepuestoDAO repuestoDAO) {
        if (proveedorRepuestoDAO == null) {
            throw new ValidacionException("El DAO del catálogo proveedor-repuesto es obligatorio.");
        }
        if (proveedorDAO == null) {
            throw new ValidacionException("El DAO de proveedores es obligatorio.");
        }
        if (repuestoDAO == null) {
            throw new ValidacionException("El DAO de repuestos es obligatorio.");
        }
        this.proveedorRepuestoDAO = proveedorRepuestoDAO;
        this.proveedorDAO = proveedorDAO;
        this.repuestoDAO = repuestoDAO;
    }

    public List<ProveedorRepuesto> listarPorProveedor(int proveedorId) {
        buscarProveedor(proveedorId);
        return proveedorRepuestoDAO.listarPorProveedor(proveedorId);
    }

    public List<ProveedorRepuesto> listarPorRepuesto(int repuestoId) {
        buscarRepuesto(repuestoId);
        return proveedorRepuestoDAO.listarPorRepuesto(repuestoId);
    }

    /**
     * Asocia un repuesto a un proveedor activo. La asociación se crea sin marca de habitual;
     * para marcarla se usa {@link #marcarHabitual(int, int)}, que aplica la transacción de RN-18.
     */
    public ProveedorRepuesto asociar(ProveedorRepuesto proveedorRepuesto) {
        if (proveedorRepuesto == null) {
            throw new ValidacionException("La asociación proveedor-repuesto no puede ser nula.");
        }
        Proveedor proveedor = buscarProveedor(proveedorRepuesto.getProveedorId());
        buscarRepuesto(proveedorRepuesto.getRepuestoId());
        if (!proveedor.isActivo()) {
            throw new ValidacionException("El proveedor '" + proveedor.getNombre()
                    + "' está inactivo y no puede asociarse a repuestos.");
        }

        proveedorRepuesto.setCosto(validarCosto(proveedorRepuesto.getCosto()));
        validarTiempoEntrega(proveedorRepuesto.getTiempoEntregaDias());

        ProveedorRepuesto existente = proveedorRepuestoDAO.buscarPorProveedorYRepuesto(
                proveedorRepuesto.getProveedorId(), proveedorRepuesto.getRepuestoId());
        if (existente != null) {
            throw new DatoDuplicadoException("El proveedor ya ofrece ese repuesto en su catálogo.");
        }

        proveedorRepuesto.setEsHabitual(false);
        if (!proveedorRepuestoDAO.guardar(proveedorRepuesto)) {
            throw new CarMotorsException("No se pudo registrar la asociación proveedor-repuesto.");
        }
        return proveedorRepuesto;
    }

    public ProveedorRepuesto actualizarCondiciones(int proveedorId, int repuestoId, BigDecimal costo,
                                                   int tiempoEntregaDias) {
        ProveedorRepuesto actual = buscarAsociacion(proveedorId, repuestoId);
        BigDecimal costoValidado = validarCosto(costo);
        validarTiempoEntrega(tiempoEntregaDias);

        actual.setCosto(costoValidado);
        actual.setTiempoEntregaDias(tiempoEntregaDias);

        if (!proveedorRepuestoDAO.actualizarCondiciones(actual)) {
            throw new CarMotorsException("No se pudo actualizar la asociación proveedor-repuesto.");
        }
        return actual;
    }

    public void quitarAsociacion(int proveedorId, int repuestoId) {
        buscarAsociacion(proveedorId, repuestoId);
        if (!proveedorRepuestoDAO.eliminar(proveedorId, repuestoId)) {
            throw new CarMotorsException("No se pudo quitar la asociación proveedor-repuesto.");
        }
    }

    /**
     * Marca al proveedor como habitual del repuesto (RF-29, RN-18). El habitual anterior,
     * si existe, queda desmarcado en la misma transacción.
     */
    public ProveedorRepuesto marcarHabitual(int proveedorId, int repuestoId) {
        Proveedor proveedor = buscarProveedor(proveedorId);
        buscarRepuesto(repuestoId);
        if (!proveedor.isActivo()) {
            throw new ValidacionException("El proveedor '" + proveedor.getNombre()
                    + "' está inactivo y no puede ser el proveedor habitual.");
        }

        ProveedorRepuesto asociacion = buscarAsociacion(proveedorId, repuestoId);
        if (asociacion.isEsHabitual()) {
            throw new ValidacionException("El proveedor '" + proveedor.getNombre()
                    + "' ya es el proveedor habitual de este repuesto.");
        }

        ejecutarCambioHabitual(proveedorId, repuestoId);
        asociacion.setEsHabitual(true);
        return asociacion;
    }

    /**
     * Unidad de trabajo JDBC (RNF-08, sección 5.6): una sola {@link Connection} con
     * {@code autoCommit=false} compartida por el DAO; {@code commit} si todo sale bien,
     * {@code rollback} ante cualquier error y cierre con try-with-resources.
     */
    protected void ejecutarCambioHabitual(int proveedorId, int repuestoId) {
        try (Connection conexion = ConnectionFactory.getInstance().getConnection()) {
            conexion.setAutoCommit(false);
            try {
                ProveedorRepuestoDAO daoTransaccional = new ProveedorRepuestoDAOImpl(conexion);
                aplicarCambioHabitual(daoTransaccional, proveedorId, repuestoId);
                conexion.commit();
            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            } catch (RuntimeException e) {
                // Cualquier error (incluidas las CarMotorsException del DAO) deshace la transacción;
                // si no, el setAutoCommit(true) del finally confirmaría un cambio a medias.
                conexion.rollback();
                throw e;
            } finally {
                conexion.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new CarMotorsException("No se pudo cambiar el proveedor habitual; no se aplicó ningún cambio: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Pasos del cambio de habitual sobre el DAO recibido: desmarca el anterior y marca el nuevo.
     * Si el marcado no afecta ninguna fila lanza una excepción para provocar el rollback.
     */
    protected void aplicarCambioHabitual(ProveedorRepuestoDAO dao, int proveedorId, int repuestoId) {
        dao.desmarcarHabitual(repuestoId);
        if (!dao.marcarHabitual(proveedorId, repuestoId)) {
            throw new CarMotorsException("No se pudo marcar el proveedor habitual del repuesto.");
        }
    }

    private BigDecimal validarCosto(BigDecimal costo) {
        if (costo == null) {
            throw new ValidacionException("El costo es obligatorio.");
        }
        BigDecimal costoNormalizado = costo.setScale(ESCALA_COSTO, RoundingMode.HALF_UP);
        if (costoNormalizado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionException("El costo debe ser mayor que cero.");
        }
        return costoNormalizado;
    }

    private void validarTiempoEntrega(int tiempoEntregaDias) {
        if (tiempoEntregaDias < 0) {
            throw new ValidacionException("El tiempo de entrega no puede ser negativo.");
        }
    }

    private Proveedor buscarProveedor(int proveedorId) {
        if (proveedorId <= 0) {
            throw new ValidacionException("El ID del proveedor debe ser un número positivo.");
        }
        Proveedor proveedor = proveedorDAO.buscarPorId(proveedorId);
        if (proveedor == null) {
            throw new EntidadNoEncontradaException("No existe un proveedor con el ID: " + proveedorId);
        }
        return proveedor;
    }

    private Repuesto buscarRepuesto(int repuestoId) {
        if (repuestoId <= 0) {
            throw new ValidacionException("El ID del repuesto debe ser un número positivo.");
        }
        Repuesto repuesto = repuestoDAO.buscarPorId(repuestoId);
        if (repuesto == null) {
            throw new EntidadNoEncontradaException("No existe un repuesto con el ID: " + repuestoId);
        }
        return repuesto;
    }

    private ProveedorRepuesto buscarAsociacion(int proveedorId, int repuestoId) {
        buscarProveedor(proveedorId);
        buscarRepuesto(repuestoId);
        ProveedorRepuesto asociacion = proveedorRepuestoDAO.buscarPorProveedorYRepuesto(proveedorId, repuestoId);
        if (asociacion == null) {
            throw new EntidadNoEncontradaException("El proveedor " + proveedorId
                    + " no ofrece el repuesto " + repuestoId + " en su catálogo.");
        }
        return asociacion;
    }
}
