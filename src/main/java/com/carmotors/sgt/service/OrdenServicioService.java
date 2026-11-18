package com.carmotors.sgt.service;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.OrdenServicio;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.repository.ClienteDAO;
import com.carmotors.sgt.repository.OrdenServicioDAO;
import com.carmotors.sgt.repository.OrdenServicioDetalleDAO;
import com.carmotors.sgt.repository.RepuestoDAO;
import com.carmotors.sgt.repository.ServicioTipoDAO;
import com.carmotors.sgt.repository.TecnicoDAO;
import com.carmotors.sgt.repository.VehiculoDAO;
import com.carmotors.sgt.repository.impl.OrdenServicioDAOImpl;
import com.carmotors.sgt.repository.impl.OrdenServicioDetalleDAOImpl;
import com.carmotors.sgt.repository.impl.RepuestoDAOImpl;
import com.carmotors.sgt.repository.impl.ServicioTipoDAOImpl;
import com.carmotors.sgt.repository.impl.TecnicoDAOImpl;
import com.carmotors.sgt.service.state.EstadoEnProceso;
import com.carmotors.sgt.service.state.EstadoOrden;
import com.carmotors.sgt.service.state.EstadoOrdenFactory;
import com.carmotors.sgt.service.state.EstadoPendiente;
import com.carmotors.sgt.util.InputValidator;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Reglas de negocio para crear órdenes y editar sus líneas (RF-08 y RF-35). */
public class OrdenServicioService {

    private static final int MAX_LONGITUD_DESCRIPCION = 255;
    private static final int MAX_LONGITUD_OBSERVACIONES = 255;

    private final OrdenServicioDAO ordenServicioDAO;
    private final OrdenServicioDetalleDAO ordenServicioDetalleDAO;
    private final VehiculoDAO vehiculoDAO;
    private final ClienteDAO clienteDAO;
    private final ServicioTipoDAO servicioTipoDAO;
    private final TecnicoDAO tecnicoDAO;
    private final RepuestoDAO repuestoDAO;
    private final boolean usarTransaccionJdbc;

    /**
     * Constructor de producción: usa DAOs JDBC y transacción con una única {@link Connection}.
     */
    public OrdenServicioService(VehiculoDAO vehiculoDAO, ClienteDAO clienteDAO, ServicioTipoDAO servicioTipoDAO) {
        this(new OrdenServicioDAOImpl(), new OrdenServicioDetalleDAOImpl(), vehiculoDAO, clienteDAO,
                servicioTipoDAO, new TecnicoDAOImpl(), new RepuestoDAOImpl(), true);
    }

    /**
     * Constructor para pruebas con DAOs en memoria: guarda directamente con los DAOs recibidos.
     */
    public OrdenServicioService(OrdenServicioDAO ordenServicioDAO, OrdenServicioDetalleDAO ordenServicioDetalleDAO,
                                VehiculoDAO vehiculoDAO, ClienteDAO clienteDAO, ServicioTipoDAO servicioTipoDAO) {
        this(ordenServicioDAO, ordenServicioDetalleDAO, vehiculoDAO, clienteDAO,
                servicioTipoDAO, new TecnicoDAOImpl(), new RepuestoDAOImpl(), false);
    }

    public OrdenServicioService(OrdenServicioDAO ordenServicioDAO, OrdenServicioDetalleDAO ordenServicioDetalleDAO,
                                VehiculoDAO vehiculoDAO, ClienteDAO clienteDAO, ServicioTipoDAO servicioTipoDAO,
                                boolean usarTransaccionJdbc) {
        this(ordenServicioDAO, ordenServicioDetalleDAO, vehiculoDAO, clienteDAO,
                servicioTipoDAO, new TecnicoDAOImpl(), new RepuestoDAOImpl(), usarTransaccionJdbc);
    }

    public OrdenServicioService(OrdenServicioDAO ordenServicioDAO, OrdenServicioDetalleDAO ordenServicioDetalleDAO,
                                VehiculoDAO vehiculoDAO, ClienteDAO clienteDAO, ServicioTipoDAO servicioTipoDAO,
                                TecnicoDAO tecnicoDAO, RepuestoDAO repuestoDAO, boolean usarTransaccionJdbc) {
        if (ordenServicioDAO == null) {
            throw new ValidacionException("El DAO de órdenes de servicio es obligatorio.");
        }
        if (ordenServicioDetalleDAO == null) {
            throw new ValidacionException("El DAO de líneas de servicio es obligatorio.");
        }
        if (vehiculoDAO == null) {
            throw new ValidacionException("El DAO de vehículos es obligatorio.");
        }
        if (clienteDAO == null) {
            throw new ValidacionException("El DAO de clientes es obligatorio.");
        }
        if (servicioTipoDAO == null) {
            throw new ValidacionException("El DAO de tipos de servicio es obligatorio.");
        }
        if (tecnicoDAO == null) {
            throw new ValidacionException("El DAO de técnicos es obligatorio.");
        }
        if (repuestoDAO == null) {
            throw new ValidacionException("El DAO de repuestos es obligatorio.");
        }
        this.ordenServicioDAO = ordenServicioDAO;
        this.ordenServicioDetalleDAO = ordenServicioDetalleDAO;
        this.vehiculoDAO = vehiculoDAO;
        this.clienteDAO = clienteDAO;
        this.servicioTipoDAO = servicioTipoDAO;
        this.tecnicoDAO = tecnicoDAO;
        this.repuestoDAO = repuestoDAO;
        this.usarTransaccionJdbc = usarTransaccionJdbc;
    }

    /**
     * Crea una orden en estado Pendiente con sus líneas iniciales (RF-08, RN-18).
     *
     * @param vehiculoId          vehículo que ingresa al taller
     * @param descripcionProblema descripción del problema reportado (obligatoria, máx. 255)
     * @param lineas              líneas solicitadas; cada una con {@code servicioTipoId} y observaciones opcionales
     * @return la orden creada con su ID generado
     */
    public OrdenServicio crearOrden(int vehiculoId, String descripcionProblema, List<OrdenServicioDetalle> lineas) {
        validarVehiculoYCliente(vehiculoId);
        String descripcion = validarDescripcion(descripcionProblema);
        List<OrdenServicioDetalle> lineasPreparadas = prepararLineas(lineas);

        OrdenServicio orden = new OrdenServicio();
        orden.setVehiculoId(vehiculoId);
        orden.setEstadoId(new EstadoPendiente().getEstadoId());
        orden.setFechaRecepcion(LocalDateTime.now().withNano(0));
        orden.setDescripcionProblema(descripcion);

        if (!usarTransaccionJdbc) {
            guardarOrdenYLineas(orden, lineasPreparadas, ordenServicioDAO, ordenServicioDetalleDAO);
            return orden;
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                OrdenServicioDAO ordenDaoTransaccional = new OrdenServicioDAOImpl(connection);
                OrdenServicioDetalleDAO detalleDaoTransaccional = new OrdenServicioDetalleDAOImpl(connection);

                guardarOrdenYLineas(orden, lineasPreparadas, ordenDaoTransaccional, detalleDaoTransaccional);
                connection.commit();
                return orden;
            } catch (CarMotorsException e) {
                connection.rollback();
                throw e;
            } catch (SQLException e) {
                connection.rollback();
                throw new CarMotorsException("No se pudo registrar la orden de servicio: " + e.getMessage(), e);
            } catch (RuntimeException e) {
                connection.rollback();
                throw new CarMotorsException("Error inesperado al registrar la orden de servicio: " + e.getMessage(), e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new CarMotorsException("No se pudo completar la transacción de la orden de servicio: "
                    + e.getMessage(), e);
        }
    }

    public boolean requiereTecnicoParaAgregar(int ordenId) {
        if (ordenId <= 0) {
            throw new ValidacionException("El ID de la orden debe ser un número positivo.");
        }
        return validarEstadoEditable(ordenServicioDAO.buscarPorId(ordenId), ordenId) instanceof EstadoEnProceso;
    }

    public boolean especialidadCoincide(ServicioTipo tipo, Tecnico tecnico) {
        if (tipo == null || tecnico == null) {
            return false;
        }
        return tipo.getEspecialidadId() == tecnico.getEspecialidadId();
    }

    public int agregarLinea(int ordenId, OrdenServicioDetalle solicitada, Integer tecnicoId,
                            boolean confirmarOtraEspecialidad) {
        if (ordenId <= 0) {
            throw new ValidacionException("El ID de la orden debe ser un número positivo.");
        }
        if (solicitada == null) {
            throw new ValidacionException("La línea de servicio es obligatoria.");
        }
        return editarLineas(ordenId, (detallesDao, tiposDao, tecnicosDao, repuestosDao, estado) -> {
            ServicioTipo tipo = tiposDao.buscarPorId(solicitada.getServicioTipoId());
            OrdenServicioDetalle linea = prepararLinea(solicitada, tipo, 1);
            Tecnico tecnico = null;
            if (estado instanceof EstadoEnProceso) {
                if (tecnicoId == null || tecnicoId <= 0) {
                    throw new ValidacionException("Una orden En Proceso exige asignar un técnico activo a la nueva línea.");
                }
                tecnico = tecnicosDao.buscarPorIdParaActualizar(tecnicoId);
                if (tecnico == null || !tecnico.isActivo()) {
                    throw new ValidacionException("Seleccione un técnico existente y activo.");
                }
                if (!especialidadCoincide(tipo, tecnico) && !confirmarOtraEspecialidad) {
                    throw new ValidacionException("Confirme la asignación del técnico de otra especialidad.");
                }
            }

            linea.setOrdenId(ordenId);
            int detalleId = detallesDao.guardar(linea);
            linea.setId(detalleId);
            if (tecnico != null) {
                detallesDao.asignarTecnico(detalleId, tecnico.getId());
            }
            return detalleId;
        });
    }

    public void quitarLinea(int ordenId, int detalleId) {
        if (ordenId <= 0) {
            throw new ValidacionException("El ID de la orden debe ser un número positivo.");
        }
        if (detalleId <= 0) {
            throw new ValidacionException("El ID de la línea debe ser un número positivo.");
        }
        editarLineas(ordenId, (detallesDao, tiposDao, tecnicosDao, repuestosDao, estado) -> {
            List<OrdenServicioDetalle> lineas = detallesDao.listarPorOrdenParaActualizar(ordenId);
            boolean pertenece = false;
            for (OrdenServicioDetalle linea : lineas) {
                if (linea.getId() == detalleId) {
                    pertenece = true;
                    break;
                }
            }
            if (!pertenece) {
                throw new EntidadNoEncontradaException("La línea indicada no pertenece a la orden.");
            }
            if (lineas.size() == 1) {
                throw new ValidacionException("La orden debe conservar al menos una línea de servicio.");
            }
            if (estado instanceof EstadoEnProceso && detallesDao.tieneHorasTrabajadas(detalleId)) {
                throw new ValidacionException("La línea tiene horas trabajadas registradas y no se puede retirar.");
            }

            for (Map.Entry<Integer, Integer> reserva : detallesDao.listarReservasParaActualizar(detalleId).entrySet()) {
                if (!repuestosDao.descontarReserva(reserva.getKey(), reserva.getValue())) {
                    throw new ValidacionException("La reserva del repuesto " + reserva.getKey() + " es inconsistente.");
                }
            }
            detallesDao.eliminar(ordenId, detalleId);
            return detalleId;
        });
    }

    private EstadoOrden validarEstadoEditable(OrdenServicio orden, int ordenId) {
        if (orden == null) {
            throw new EntidadNoEncontradaException("No se encontró una orden de servicio con el ID: " + ordenId);
        }
        EstadoOrden estado = EstadoOrdenFactory.desdeId(orden.getEstadoId());
        if (!estado.permiteEdicion()) {
            throw new ValidacionException("No se pueden editar las líneas de una orden en estado " + estado.getNombre() + ".");
        }
        return estado;
    }

    private int editarLineas(int ordenId, EdicionLineas edicion) {
        if (!usarTransaccionJdbc) {
            OrdenServicio orden = ordenServicioDAO.buscarPorIdParaActualizar(ordenId);
            EstadoOrden estado = validarEstadoEditable(orden, ordenId);
            return edicion.ejecutar(ordenServicioDetalleDAO, servicioTipoDAO, tecnicoDAO, repuestoDAO, estado);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                OrdenServicioDAO ordenDao = new OrdenServicioDAOImpl(connection);
                OrdenServicioDetalleDAO detalleDao = new OrdenServicioDetalleDAOImpl(connection);
                ServicioTipoDAO tipoDao = new ServicioTipoDAOImpl(connection);
                TecnicoDAO tecnicoDaoTransaccional = new TecnicoDAOImpl(connection);
                RepuestoDAO repuestoDaoTransaccional = new RepuestoDAOImpl(connection);

                OrdenServicio orden = ordenDao.buscarPorIdParaActualizar(ordenId);
                EstadoOrden estado = validarEstadoEditable(orden, ordenId);
                int detalleId = edicion.ejecutar(
                        detalleDao, tipoDao, tecnicoDaoTransaccional, repuestoDaoTransaccional, estado);
                connection.commit();
                return detalleId;
            } catch (RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("No se pudo editar la orden de servicio: " + e.getMessage(), e);
        }
    }

    private interface EdicionLineas {
        int ejecutar(OrdenServicioDetalleDAO detalleDao,
                     ServicioTipoDAO servicioTipoDao,
                     TecnicoDAO tecnicoDao,
                     RepuestoDAO repuestoDao,
                     EstadoOrden estado);
    }

    /**
     * Retorna las líneas de una orden existente.
     *
     * @param ordenId identificador de la orden
     * @return líneas de la orden ordenadas por ID
     */
    public List<OrdenServicioDetalle> listarLineas(int ordenId) {
        if (ordenId <= 0) {
            throw new ValidacionException("El ID de la orden debe ser un número positivo.");
        }
        OrdenServicio orden = ordenServicioDAO.buscarPorId(ordenId);
        if (orden == null) {
            throw new EntidadNoEncontradaException("No se encontró una orden de servicio con el ID: " + ordenId);
        }
        return ordenServicioDetalleDAO.listarPorOrden(ordenId);
    }

    /**
     * Traduce el ID de estado de una orden a su nombre usando el patrón State.
     *
     * @param estadoId identificador de {@code servicio_estados}
     * @return nombre del estado
     */
    public String obtenerNombreEstado(int estadoId) {
        return EstadoOrdenFactory.desdeId(estadoId).getNombre();
    }

    private void validarVehiculoYCliente(int vehiculoId) {
        if (vehiculoId <= 0) {
            throw new ValidacionException("El ID del vehículo debe ser un número positivo.");
        }
        Vehiculo vehiculo = vehiculoDAO.buscarPorId(vehiculoId);
        if (vehiculo == null) {
            throw new EntidadNoEncontradaException(
                    "No se encontró un vehículo con el ID: " + vehiculoId + ". Primero debe registrarse el vehículo.");
        }
        Cliente cliente = clienteDAO.buscarPorId(vehiculo.getClienteId());
        if (cliente == null) {
            throw new EntidadNoEncontradaException(
                    "No se encontró el cliente dueño del vehículo con placa " + vehiculo.getPlaca() + ".");
        }
        if (!cliente.isActivo()) {
            throw new ValidacionException("El cliente '" + cliente.getNombre()
                    + "' dueño del vehículo está inactivo; no se pueden crear órdenes para su vehículo.");
        }
    }

    private String validarDescripcion(String descripcionProblema) {
        String descripcion = InputValidator.normalizarTexto(descripcionProblema);
        if (descripcion.isEmpty()) {
            throw new ValidacionException("La descripción del problema es obligatoria.");
        }
        if (descripcion.length() > MAX_LONGITUD_DESCRIPCION) {
            throw new ValidacionException("La descripción del problema no puede superar los "
                    + MAX_LONGITUD_DESCRIPCION + " caracteres.");
        }
        return descripcion;
    }

    private List<OrdenServicioDetalle> prepararLineas(List<OrdenServicioDetalle> lineas) {
        if (lineas == null || lineas.isEmpty()) {
            throw new ValidacionException("La orden debe tener al menos una línea de servicio (RN-18).");
        }

        List<OrdenServicioDetalle> preparadas = new ArrayList<>();
        int numero = 1;
        for (OrdenServicioDetalle solicitada : lineas) {
            if (solicitada == null) {
                throw new ValidacionException("La línea " + numero + " de la orden no puede ser nula.");
            }
            ServicioTipo tipo = servicioTipoDAO.buscarPorId(solicitada.getServicioTipoId());
            preparadas.add(prepararLinea(solicitada, tipo, numero));
            numero++;
        }
        return preparadas;
    }

    private OrdenServicioDetalle prepararLinea(OrdenServicioDetalle solicitada, ServicioTipo tipo, int numero) {
        if (tipo == null) {
            throw new EntidadNoEncontradaException("Línea " + numero + ": no existe un tipo de servicio con el ID: "
                    + solicitada.getServicioTipoId());
        }
        if (tipo.getPrecioManoObraBase() == null) {
            throw new ValidacionException("El tipo de servicio '" + tipo.getNombre()
                    + "' no tiene precio base de mano de obra.");
        }

        String observaciones = InputValidator.normalizarTexto(solicitada.getObservaciones());
        if (observaciones.length() > MAX_LONGITUD_OBSERVACIONES) {
            throw new ValidacionException("Línea " + numero + ": las observaciones no pueden superar los "
                    + MAX_LONGITUD_OBSERVACIONES + " caracteres.");
        }

        OrdenServicioDetalle linea = new OrdenServicioDetalle();
        linea.setServicioTipoId(tipo.getId());
        linea.setNombreServicioSnapshot(tipo.getNombre());
        linea.setCostoManoObra(tipo.getPrecioManoObraBase()
                .setScale(InputValidator.ESCALA_MONETARIA, InputValidator.REDONDEO_MONETARIO));
        linea.setObservaciones(observaciones.isEmpty() ? null : observaciones);
        return linea;
    }

    private void guardarOrdenYLineas(OrdenServicio orden, List<OrdenServicioDetalle> lineas,
                                     OrdenServicioDAO ordenDao, OrdenServicioDetalleDAO detalleDao) {
        int ordenId = ordenDao.guardar(orden);
        orden.setId(ordenId);
        for (OrdenServicioDetalle linea : lineas) {
            linea.setOrdenId(ordenId);
            int detalleId = detalleDao.guardar(linea);
            linea.setId(detalleId);
        }
    }
}
