package com.carmotors.sgt.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.EvaluacionProveedor;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.repository.EvaluacionProveedorDAO;
import com.carmotors.sgt.repository.ProveedorDAO;
import com.carmotors.sgt.repository.impl.EvaluacionProveedorDAOImpl;
import com.carmotors.sgt.repository.impl.ProveedorDAOImpl;
import com.carmotors.sgt.util.InputValidator;

public class EvaluacionProveedorService {

    private static final int MIN_CALIFICACION = 1;
    private static final int MAX_CALIFICACION = 5;

    private final EvaluacionProveedorDAO evaluacionDAO;
    private final ProveedorDAO proveedorDAO;

    public EvaluacionProveedorService() {
        this(new EvaluacionProveedorDAOImpl(), new ProveedorDAOImpl());
    }

    public EvaluacionProveedorService(EvaluacionProveedorDAO evaluacionDAO, ProveedorDAO proveedorDAO) {
        if (evaluacionDAO == null) {
            throw new ValidacionException("El DAO de evaluaciones es obligatorio.");
        }
        if (proveedorDAO == null) {
            throw new ValidacionException("El DAO de proveedores es obligatorio.");
        }
        this.evaluacionDAO = evaluacionDAO;
        this.proveedorDAO = proveedorDAO;
    }

    public List<EvaluacionProveedor> listarTodas() {
        return evaluacionDAO.listarTodos();
    }

    public List<EvaluacionProveedor> listarPorProveedor(int proveedorId) {
        validarProveedorExistente(proveedorId);
        return evaluacionDAO.listarPorProveedor(proveedorId);
    }

    public EvaluacionProveedor registrarEvaluacion(EvaluacionProveedor evaluacion) {
        validarEvaluacion(evaluacion);

        Proveedor proveedor = validarProveedorExistente(evaluacion.getProveedorId());
        if (!proveedor.isActivo()) {
            throw new ValidacionException("El proveedor está inactivo y no puede recibir evaluaciones.");
        }

        if (evaluacion.getFecha() == null) {
            evaluacion.setFecha(LocalDate.now());
        }

        String comentario = normalizarComentario(evaluacion.getComentario());
        evaluacion.setComentario(comentario);

        int idGenerado = evaluacionDAO.guardar(evaluacion);
        evaluacion.setId(idGenerado);
        return evaluacion;
    }

    public BigDecimal obtenerPromedioProveedor(int proveedorId) {
        validarProveedorExistente(proveedorId);
        List<EvaluacionProveedor> historial = evaluacionDAO.listarPorProveedor(proveedorId);
        if (historial == null || historial.isEmpty()) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        BigDecimal acumulado = BigDecimal.ZERO;
        for (EvaluacionProveedor evaluacion : historial) {
            acumulado = acumulado.add(evaluacion.getPuntaje());
        }
        BigDecimal promedio = acumulado.divide(BigDecimal.valueOf(historial.size()), 1, RoundingMode.HALF_UP);
        return promedio;
    }

    private void validarEvaluacion(EvaluacionProveedor evaluacion) {
        if (evaluacion == null) {
            throw new ValidacionException("La evaluación no puede ser nula.");
        }

        if (evaluacion.getProveedorId() <= 0) {
            throw new ValidacionException("Debe indicar un proveedor válido para la evaluación.");
        }

        validarRango(evaluacion.getCalidad(), "calidad");
        validarRango(evaluacion.getPuntualidad(), "puntualidad");
    }

    private void validarRango(int valor, String campo) {
        if (valor < MIN_CALIFICACION || valor > MAX_CALIFICACION) {
            throw new ValidacionException("La calificación de " + campo + " debe estar entre " + MIN_CALIFICACION + " y " + MAX_CALIFICACION + ".");
        }
    }

    private Proveedor validarProveedorExistente(int proveedorId) {
        Proveedor proveedor = proveedorDAO.buscarPorId(proveedorId);
        if (proveedor == null) {
            throw new EntidadNoEncontradaException("No existe un proveedor con el ID: " + proveedorId);
        }
        return proveedor;
    }

    private String normalizarComentario(String comentario) {
        String limpio = InputValidator.normalizarTexto(comentario);
        return limpio.isEmpty() ? null : limpio;
    }
}
