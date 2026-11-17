package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Especialidad;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.repository.EspecialidadDAO;
import com.carmotors.sgt.repository.TecnicoDAO;
import com.carmotors.sgt.util.InputValidator;

import java.util.List;

/**
 * Capa de lógica de negocio para la gestión de técnicos y su especialidad (RF-33, RN-17).
 */
public class TecnicoService {

    private final TecnicoDAO tecnicoDAO;
    private final EspecialidadDAO especialidadDAO;

    public TecnicoService(TecnicoDAO tecnicoDAO, EspecialidadDAO especialidadDAO) {
        if (tecnicoDAO == null) {
            throw new IllegalArgumentException("El repositorio de técnicos es obligatorio.");
        }
        if (especialidadDAO == null) {
            throw new IllegalArgumentException("El repositorio de especialidades es obligatorio.");
        }
        this.tecnicoDAO = tecnicoDAO;
        this.especialidadDAO = especialidadDAO;
    }

    public List<Tecnico> listarTodos() {
        return tecnicoDAO.listarTodos();
    }

    public List<Tecnico> listarActivos() {
        return tecnicoDAO.listarActivos();
    }

    public List<Especialidad> listarEspecialidades() {
        return especialidadDAO.listarTodos();
    }

    public Especialidad buscarEspecialidadPorId(int especialidadId) {
        return especialidadDAO.buscarPorId(especialidadId);
    }

    public Tecnico buscarPorId(int id) {
        if (id <= 0) {
            throw new ValidacionException("El ID del técnico debe ser un número positivo.");
        }
        Tecnico tecnico = tecnicoDAO.buscarPorId(id);
        if (tecnico == null) {
            throw new EntidadNoEncontradaException("No se encontró un técnico con el ID: " + id);
        }
        return tecnico;
    }

    public Tecnico buscarPorDocumento(String documento) {
        String docNormalizado = InputValidator.normalizarTexto(documento);
        if (docNormalizado.isEmpty()) {
            throw new ValidacionException("El documento de identidad no puede estar vacío.");
        }
        Tecnico tecnico = tecnicoDAO.buscarPorDocumento(docNormalizado);
        if (tecnico == null) {
            throw new EntidadNoEncontradaException("No se encontró un técnico con el documento: " + docNormalizado);
        }
        return tecnico;
    }

    public Tecnico registrarTecnico(Tecnico tecnico) {
        validarYNormalizarDatos(tecnico);

        Tecnico existente = tecnicoDAO.buscarPorDocumento(tecnico.getDocumentoIdentidad());
        if (existente != null) {
            throw new DatoDuplicadoException(
                    "Ya existe un técnico registrado con el documento: " + tecnico.getDocumentoIdentidad());
        }

        tecnico.setActivo(true);
        int idGenerado = tecnicoDAO.guardar(tecnico);
        tecnico.setId(idGenerado);
        return tecnico;
    }

    public Tecnico actualizarTecnico(Tecnico tecnico) {
        if (tecnico == null || tecnico.getId() <= 0) {
            throw new ValidacionException("Debe indicar un técnico válido con ID mayor que cero.");
        }
        Tecnico actual = tecnicoDAO.buscarPorId(tecnico.getId());
        if (actual == null) {
            throw new EntidadNoEncontradaException("No existe un técnico con el ID: " + tecnico.getId());
        }

        validarYNormalizarDatos(tecnico);

        Tecnico conMismoDoc = tecnicoDAO.buscarPorDocumento(tecnico.getDocumentoIdentidad());
        if (conMismoDoc != null && conMismoDoc.getId() != tecnico.getId()) {
            throw new DatoDuplicadoException(
                    "Ya existe otro técnico registrado con el documento: " + tecnico.getDocumentoIdentidad());
        }

        boolean actualizado = tecnicoDAO.actualizar(tecnico);
        if (!actualizado) {
            throw new ValidacionException("No se pudo actualizar el técnico con ID: " + tecnico.getId());
        }
        return tecnico;
    }

    /**
     * Desactiva lógicamente a un técnico para impedir nuevas asignaciones sin borrar su historial (RN-17).
     */
    public Tecnico desactivarTecnico(int id) {
        Tecnico tecnico = buscarPorId(id);
        if (!tecnico.isActivo()) {
            return tecnico;
        }
        boolean actualizado = tecnicoDAO.actualizarEstadoActivo(id, false);
        if (!actualizado) {
            throw new ValidacionException("No se pudo desactivar el técnico con ID: " + id);
        }
        tecnico.setActivo(false);
        return tecnico;
    }

    public Tecnico reactivarTecnico(int id) {
        Tecnico tecnico = buscarPorId(id);
        if (tecnico.isActivo()) {
            return tecnico;
        }
        boolean actualizado = tecnicoDAO.actualizarEstadoActivo(id, true);
        if (!actualizado) {
            throw new ValidacionException("No se pudo reactivar el técnico con ID: " + id);
        }
        tecnico.setActivo(true);
        return tecnico;
    }

    private void validarYNormalizarDatos(Tecnico tecnico) {
        if (tecnico == null) {
            throw new ValidacionException("Los datos del técnico no pueden ser nulos.");
        }

        String nombre = InputValidator.normalizarTexto(tecnico.getNombre());
        if (nombre.isEmpty()) {
            throw new ValidacionException("El nombre del técnico es obligatorio.");
        }
        tecnico.setNombre(nombre);

        String documento = InputValidator.normalizarTexto(tecnico.getDocumentoIdentidad());
        if (documento.isEmpty()) {
            throw new ValidacionException("El documento de identidad del técnico es obligatorio.");
        }
        tecnico.setDocumentoIdentidad(documento);

        if (tecnico.getEspecialidadId() <= 0) {
            throw new ValidacionException("Debe seleccionar una especialidad válida.");
        }
        Especialidad especialidad = especialidadDAO.buscarPorId(tecnico.getEspecialidadId());
        if (especialidad == null) {
            throw new EntidadNoEncontradaException(
                    "La especialidad seleccionada (ID " + tecnico.getEspecialidadId() + ") no existe.");
        }

        String telefono = InputValidator.normalizarTexto(tecnico.getTelefono());
        tecnico.setTelefono(telefono.isEmpty() ? null : telefono);
    }
}
