package com.carmotors.sgt.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Lote;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.repository.LoteDAO;
import com.carmotors.sgt.repository.ProveedorDAO;
import com.carmotors.sgt.repository.ProveedorRepuestoDAO;
import com.carmotors.sgt.repository.RepuestoDAO;
import com.carmotors.sgt.repository.impl.LoteDAOImpl;
import com.carmotors.sgt.repository.impl.ProveedorDAOImpl;
import com.carmotors.sgt.repository.impl.ProveedorRepuestoDAOImpl;
import com.carmotors.sgt.repository.impl.RepuestoDAOImpl;
import com.carmotors.sgt.util.InputValidator;

/**
 * Reglas de negocio para el ingreso y consulta de inventario por lotes.
 */
public class LoteService {

    private static final int MAX_LONGITUD_CODIGO_LOTE = 40;

    private final LoteDAO loteDAO;
    private final ProveedorDAO proveedorDAO;
    private final ProveedorRepuestoDAO proveedorRepuestoDAO;
    private final RepuestoDAO repuestoDAO;
    private final boolean usarTransaccionJdbc;

    public LoteService() {
        this(new LoteDAOImpl(), new ProveedorDAOImpl(), new ProveedorRepuestoDAOImpl(), new RepuestoDAOImpl(), true);
    }

    public LoteService(LoteDAO loteDAO, ProveedorDAO proveedorDAO, ProveedorRepuestoDAO proveedorRepuestoDAO) {
        this(loteDAO, proveedorDAO, proveedorRepuestoDAO, new RepuestoDAOImpl(), true);
    }

    public LoteService(LoteDAO loteDAO, ProveedorDAO proveedorDAO, ProveedorRepuestoDAO proveedorRepuestoDAO,
                      RepuestoDAO repuestoDAO) {
        this(loteDAO, proveedorDAO, proveedorRepuestoDAO, repuestoDAO, false);
    }

    public LoteService(LoteDAO loteDAO, ProveedorDAO proveedorDAO, ProveedorRepuestoDAO proveedorRepuestoDAO,
                      RepuestoDAO repuestoDAO, boolean usarTransaccionJdbc) {
        if (loteDAO == null) {
            throw new ValidacionException("El DAO de lotes es obligatorio.");
        }
        if (proveedorDAO == null) {
            throw new ValidacionException("El DAO de proveedores es obligatorio.");
        }
        if (proveedorRepuestoDAO == null) {
            throw new ValidacionException("El DAO del catálogo proveedor-repuesto es obligatorio.");
        }
        if (repuestoDAO == null) {
            throw new ValidacionException("El DAO de repuestos es obligatorio.");
        }
        this.loteDAO = loteDAO;
        this.proveedorDAO = proveedorDAO;
        this.proveedorRepuestoDAO = proveedorRepuestoDAO;
        this.repuestoDAO = repuestoDAO;
        this.usarTransaccionJdbc = usarTransaccionJdbc;
    }

    public Lote buscarPorCodigo(String codigoLote) {
        if (codigoLote == null || codigoLote.trim().isEmpty()) {
            throw new ValidacionException("El código del lote es obligatorio para la búsqueda.");
        }
        return loteDAO.buscarPorCodigo(codigoLote.trim());
    }

    public List<Lote> listarPorRepuesto(int repuestoId) {
        if (repuestoId <= 0) {
            throw new ValidacionException("El ID del repuesto debe ser un número positivo.");
        }

        Repuesto repuesto = repuestoDAO.buscarPorId(repuestoId);
        if (repuesto == null) {
            throw new EntidadNoEncontradaException("No existe un repuesto con el ID: " + repuestoId);
        }

        List<Lote> lotes = loteDAO.listarPorRepuesto(repuestoId);
        List<Lote> resultado = new ArrayList<>(lotes);
        resultado.sort(Comparator
                .comparing((Lote lote) -> lote.getFechaCaducidad() == null)
                .thenComparing(Lote::getFechaCaducidad, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Lote::getFechaIngreso, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(Lote::getId));
        return resultado;
    }

    public Lote registrarLote(Lote lote) {
        if (lote == null) {
            throw new ValidacionException("El lote no puede ser nulo.");
        }

        validarDatosBase(lote);

        if (!usarTransaccionJdbc) {
            validarRelacionProveedorRepuesto(lote, proveedorDAO, proveedorRepuestoDAO, repuestoDAO);
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
                RepuestoDAO repuestoDaoTransaccional = new RepuestoDAOImpl(connection);

                validarRelacionProveedorRepuesto(lote, proveedorDaoTransaccional, proveedorRepuestoDaoTransaccional,
                        repuestoDaoTransaccional);
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
        if (codigoNormalizado.length() > MAX_LONGITUD_CODIGO_LOTE) {
            throw new ValidacionException("El código del lote no puede superar los " + MAX_LONGITUD_CODIGO_LOTE + " caracteres.");
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

    private void validarRelacionProveedorRepuesto(Lote lote, ProveedorDAO proveedorDao,
                                                 ProveedorRepuestoDAO catalogoDao, RepuestoDAO repuestoDao) {
        Proveedor proveedor = proveedorDao.buscarPorId(lote.getProveedorId());
        if (proveedor == null) {
            throw new EntidadNoEncontradaException("No existe un proveedor con el ID: " + lote.getProveedorId());
        }
        if (!proveedor.isActivo()) {
            throw new ValidacionException("El proveedor '" + proveedor.getNombre() + "' está inactivo y no puede registrar lotes.");
        }

        Repuesto repuesto = repuestoDao.buscarPorId(lote.getRepuestoId());
        if (repuesto == null) {
            throw new EntidadNoEncontradaException("No existe un repuesto con el ID: " + lote.getRepuestoId());
        }
        if (!repuesto.isActivo()) {
            throw new ValidacionException("El repuesto '" + repuesto.getNombre() + "' está inactivo y no puede recibir lotes nuevos.");
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
