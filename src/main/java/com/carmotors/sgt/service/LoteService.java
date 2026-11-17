package com.carmotors.sgt.service;

import java.sql.Connection;
import java.sql.SQLException;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Lote;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.repository.LoteDAO;
import com.carmotors.sgt.repository.ProveedorDAO;
import com.carmotors.sgt.repository.ProveedorRepuestoDAO;
import com.carmotors.sgt.repository.impl.LoteDAOImpl;
import com.carmotors.sgt.repository.impl.ProveedorDAOImpl;
import com.carmotors.sgt.repository.impl.ProveedorRepuestoDAOImpl;
import com.carmotors.sgt.util.InputValidator;

/**
 * Reglas de negocio para el ingreso de inventario por lotes.
 *
 * <p>La validación de código único, proveedor activo y vínculo proveedor-repuesto se realiza
 * dentro de una única transacción JDBC para cumplir con RN-14 y RNF-08.</p>
 */
public class LoteService {

    private final LoteDAO loteDAO;
    private final ProveedorDAO proveedorDAO;
    private final ProveedorRepuestoDAO proveedorRepuestoDAO;

    public LoteService() {
        this(new LoteDAOImpl(), new ProveedorDAOImpl(), new ProveedorRepuestoDAOImpl());
    }

    public LoteService(LoteDAO loteDAO, ProveedorDAO proveedorDAO, ProveedorRepuestoDAO proveedorRepuestoDAO) {
        if (loteDAO == null) {
            throw new ValidacionException("El DAO de lotes es obligatorio.");
        }
        if (proveedorDAO == null) {
            throw new ValidacionException("El DAO de proveedores es obligatorio.");
        }
        if (proveedorRepuestoDAO == null) {
            throw new ValidacionException("El DAO del catálogo proveedor-repuesto es obligatorio.");
        }
        this.loteDAO = loteDAO;
        this.proveedorDAO = proveedorDAO;
        this.proveedorRepuestoDAO = proveedorRepuestoDAO;
    }

    public Lote registrarLote(Lote lote) {
        if (lote == null) {
            throw new ValidacionException("El lote no puede ser nulo.");
        }

        validarDatosBase(lote);

        if (!(loteDAO instanceof LoteDAOImpl) && !(proveedorDAO instanceof ProveedorDAOImpl)
                && !(proveedorRepuestoDAO instanceof ProveedorRepuestoDAOImpl)) {
            validarRelacionProveedorRepuesto(lote, proveedorDAO, proveedorRepuestoDAO);
            validarCodigoDuplicado(lote.getCodigoLote(), loteDAO);
            lote.setCantidadActual(lote.getCantidadInicial());
            int idGenerado = loteDAO.guardar(lote);
            lote.setId(idGenerado);
            return lote;
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                LoteDAO loteDaoTransaccional = new LoteDAOImpl(connection);
                ProveedorDAO proveedorDaoTransaccional = new ProveedorDAOImpl(connection);
                ProveedorRepuestoDAO proveedorRepuestoDaoTransaccional = new ProveedorRepuestoDAOImpl(connection);

                validarRelacionProveedorRepuesto(lote, proveedorDaoTransaccional, proveedorRepuestoDaoTransaccional);
                validarCodigoDuplicado(lote.getCodigoLote(), loteDaoTransaccional);

                lote.setCantidadActual(lote.getCantidadInicial());
                int idGenerado = loteDaoTransaccional.guardar(lote);
                lote.setId(idGenerado);
                connection.commit();
                return lote;
            } catch (SQLException e) {
                connection.rollback();
                throw new CarMotorsException("No se pudo registrar el lote en la base de datos: " + e.getMessage(), e);
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new CarMotorsException("No se pudo abrir la conexión para registrar el lote: " + e.getMessage(), e);
        }
    }

    private void validarDatosBase(Lote lote) {
        String codigoNormalizado = InputValidator.normalizarTexto(lote.getCodigoLote());
        if (codigoNormalizado.isEmpty()) {
            throw new ValidacionException("El código del lote es obligatorio.");
        }
        lote.setCodigoLote(codigoNormalizado);

        if (lote.getProveedorId() <= 0) {
            throw new ValidacionException("El ID del proveedor debe ser un número positivo.");
        }
        if (lote.getRepuestoId() <= 0) {
            throw new ValidacionException("El ID del repuesto debe ser un número positivo.");
        }
        if (lote.getCantidadInicial() <= 0) {
            throw new ValidacionException("La cantidad inicial del lote debe ser mayor que cero.");
        }
        if (lote.getFechaIngreso() == null) {
            throw new ValidacionException("La fecha de ingreso del lote es obligatoria.");
        }
        if (lote.getFechaCaducidad() != null && !lote.getFechaCaducidad().isAfter(lote.getFechaIngreso())) {
            throw new ValidacionException("La fecha de caducidad debe ser estrictamente posterior a la fecha de ingreso.");
        }
    }

    private void validarRelacionProveedorRepuesto(Lote lote, ProveedorDAO proveedorDao, ProveedorRepuestoDAO catalogoDao) {
        Proveedor proveedor = proveedorDao.buscarPorId(lote.getProveedorId());
        if (proveedor == null) {
            throw new EntidadNoEncontradaException("No existe un proveedor con el ID: " + lote.getProveedorId());
        }
        if (!proveedor.isActivo()) {
            throw new ValidacionException("El proveedor '" + proveedor.getNombre() + "' está inactivo y no puede registrar lotes.");
        }

        ProveedorRepuesto asociacion = catalogoDao.buscarPorProveedorYRepuesto(lote.getProveedorId(), lote.getRepuestoId());
        if (asociacion == null) {
            throw new ValidacionException("El proveedor no ofrece el repuesto indicado en el catálogo.");
        }
    }

    private void validarCodigoDuplicado(String codigoLote, LoteDAO loteDao) {
        Lote existente = loteDao.buscarPorCodigo(codigoLote);
        if (existente != null) {
            throw new DatoDuplicadoException("Ya existe un lote registrado con el código: " + codigoLote);
        }
    }
}
