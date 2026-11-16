package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Especialidad;
import com.carmotors.sgt.model.MantenimientoTipo;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.repository.EspecialidadRepository;
import com.carmotors.sgt.repository.MantenimientoTipoRepository;
import com.carmotors.sgt.repository.ServicioTipoRepository;
import com.carmotors.sgt.util.InputValidator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Capa de lógica de negocio para el catálogo de tipos de servicio ({@code servicio_tipos}).
 * Aplica validación de precio no negativo con {@link BigDecimal} (HALF_UP, escala 2)
 * y la regla de intervalo en meses según tipo Preventivo o Correctivo.
 */
public class ServicioTipoService {

    private final ServicioTipoRepository servicioTipoRepository;
    private final MantenimientoTipoRepository mantenimientoTipoRepository;
    private final EspecialidadRepository especialidadRepository;

    public ServicioTipoService(
            ServicioTipoRepository servicioTipoRepository,
            MantenimientoTipoRepository mantenimientoTipoRepository,
            EspecialidadRepository especialidadRepository) {
        if (servicioTipoRepository == null) {
            throw new IllegalArgumentException("El repositorio de tipos de servicio es obligatorio.");
        }
        if (mantenimientoTipoRepository == null) {
            throw new IllegalArgumentException("El repositorio de tipos de mantenimiento es obligatorio.");
        }
        if (especialidadRepository == null) {
            throw new IllegalArgumentException("El repositorio de especialidades es obligatorio.");
        }
        this.servicioTipoRepository = servicioTipoRepository;
        this.mantenimientoTipoRepository = mantenimientoTipoRepository;
        this.especialidadRepository = especialidadRepository;
    }

    public List<ServicioTipo> listarTodos() {
        return servicioTipoRepository.listarTodos();
    }

    public List<MantenimientoTipo> listarTiposMantenimiento() {
        return mantenimientoTipoRepository.listarTodos();
    }

    public List<Especialidad> listarEspecialidades() {
        return especialidadRepository.listarTodos();
    }

    public MantenimientoTipo buscarTipoMantenimientoPorId(int mantenimientoTipoId) {
        return mantenimientoTipoRepository.buscarPorId(mantenimientoTipoId);
    }

    public Especialidad buscarEspecialidadPorId(int especialidadId) {
        return especialidadRepository.buscarPorId(especialidadId);
    }

    public ServicioTipo buscarPorId(int id) {
        if (id <= 0) {
            throw new ValidacionException("El ID del tipo de servicio debe ser un número positivo.");
        }
        ServicioTipo servicioTipo = servicioTipoRepository.buscarPorId(id);
        if (servicioTipo == null) {
            throw new EntidadNoEncontradaException("No se encontró un tipo de servicio con el ID: " + id);
        }
        return servicioTipo;
    }

    public ServicioTipo registrarServicioTipo(ServicioTipo servicioTipo) {
        validarYNormalizar(servicioTipo);

        ServicioTipo existente = servicioTipoRepository.buscarPorNombre(servicioTipo.getNombre());
        if (existente != null) {
            throw new DatoDuplicadoException(
                    "Ya existe un tipo de servicio registrado con el nombre: " + servicioTipo.getNombre());
        }

        int idGenerado = servicioTipoRepository.guardar(servicioTipo);
        servicioTipo.setId(idGenerado);
        return servicioTipo;
    }

    public ServicioTipo actualizarServicioTipo(ServicioTipo servicioTipo) {
        if (servicioTipo == null || servicioTipo.getId() <= 0) {
            throw new ValidacionException("Debe indicar un tipo de servicio válido con ID mayor que cero.");
        }
        ServicioTipo actual = servicioTipoRepository.buscarPorId(servicioTipo.getId());
        if (actual == null) {
            throw new EntidadNoEncontradaException("No existe un tipo de servicio con el ID: " + servicioTipo.getId());
        }

        validarYNormalizar(servicioTipo);

        ServicioTipo conMismoNombre = servicioTipoRepository.buscarPorNombre(servicioTipo.getNombre());
        if (conMismoNombre != null && conMismoNombre.getId() != servicioTipo.getId()) {
            throw new DatoDuplicadoException(
                    "Ya existe otro tipo de servicio con el nombre: " + servicioTipo.getNombre());
        }

        boolean actualizado = servicioTipoRepository.actualizar(servicioTipo);
        if (!actualizado) {
            throw new ValidacionException("No se pudo actualizar el tipo de servicio con ID: " + servicioTipo.getId());
        }
        return servicioTipo;
    }

    public void eliminarServicioTipo(int id) {
        buscarPorId(id);
        boolean eliminado = servicioTipoRepository.eliminar(id);
        if (!eliminado) {
            throw new ValidacionException("No se pudo eliminar el tipo de servicio con ID: " + id);
        }
    }

    private void validarYNormalizar(ServicioTipo servicioTipo) {
        if (servicioTipo == null) {
            throw new ValidacionException("Los datos del tipo de servicio no pueden ser nulos.");
        }

        String nombre = InputValidator.normalizarTexto(servicioTipo.getNombre());
        if (nombre.isEmpty()) {
            throw new ValidacionException("El nombre del tipo de servicio es obligatorio.");
        }
        servicioTipo.setNombre(nombre);

        if (servicioTipo.getMantenimientoTipoId() <= 0) {
            throw new ValidacionException("Debe seleccionar un tipo de mantenimiento válido.");
        }
        MantenimientoTipo mantenimientoTipo =
                mantenimientoTipoRepository.buscarPorId(servicioTipo.getMantenimientoTipoId());
        if (mantenimientoTipo == null) {
            throw new EntidadNoEncontradaException(
                    "El tipo de mantenimiento seleccionado (ID " + servicioTipo.getMantenimientoTipoId() + ") no existe.");
        }

        if (servicioTipo.getEspecialidadId() <= 0) {
            throw new ValidacionException("Debe seleccionar una especialidad válida.");
        }
        Especialidad especialidad = especialidadRepository.buscarPorId(servicioTipo.getEspecialidadId());
        if (especialidad == null) {
            throw new EntidadNoEncontradaException(
                    "La especialidad seleccionada (ID " + servicioTipo.getEspecialidadId() + ") no existe.");
        }

        BigDecimal precio = servicioTipo.getPrecioManoObraBase();
        if (!InputValidator.esBigDecimalNoNegativo(precio)) {
            throw new ValidacionException("El precio base de mano de obra debe ser mayor o igual a 0.00.");
        }
        servicioTipo.setPrecioManoObraBase(precio.setScale(2, RoundingMode.HALF_UP));

        validarIntervaloSegunMantenimiento(mantenimientoTipo, servicioTipo.getIntervaloMeses());
    }

    private void validarIntervaloSegunMantenimiento(MantenimientoTipo tipo, Integer intervaloMeses) {
        String nombreTipo = InputValidator.normalizarTexto(tipo.getNombre());
        if (nombreTipo.equalsIgnoreCase("Preventivo")) {
            if (intervaloMeses == null || intervaloMeses <= 0) {
                throw new ValidacionException(
                        "Un servicio de mantenimiento Preventivo exige un intervalo en meses mayor que cero.");
            }
            return;
        }
        if (nombreTipo.equalsIgnoreCase("Correctivo") && intervaloMeses != null) {
            throw new ValidacionException(
                    "Un servicio de mantenimiento Correctivo no debe tener intervalo en meses (debe ser nulo).");
        }
    }
}
