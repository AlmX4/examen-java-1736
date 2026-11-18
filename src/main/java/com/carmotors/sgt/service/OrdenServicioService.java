package com.carmotors.sgt.service;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.StockInsuficienteException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.DetalleRepuesto;
import com.carmotors.sgt.model.DetalleTecnico;
import com.carmotors.sgt.model.OrdenServicio;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.repository.ClienteDAO;
import com.carmotors.sgt.repository.DetalleRepuestoDAO;
import com.carmotors.sgt.repository.OrdenServicioDAO;
import com.carmotors.sgt.repository.OrdenServicioDetalleDAO;
import com.carmotors.sgt.repository.RepuestoDAO;
import com.carmotors.sgt.repository.ServicioTipoDAO;
import com.carmotors.sgt.repository.TecnicoDAO;
import com.carmotors.sgt.repository.VehiculoDAO;
import com.carmotors.sgt.repository.impl.DetalleRepuestoDAOImpl;
import com.carmotors.sgt.repository.impl.OrdenServicioDAOImpl;
import com.carmotors.sgt.repository.impl.OrdenServicioDetalleDAOImpl;
import com.carmotors.sgt.repository.impl.RepuestoDAOImpl;
import com.carmotors.sgt.repository.impl.ServicioTipoDAOImpl;
import com.carmotors.sgt.repository.impl.TecnicoDAOImpl;
import com.carmotors.sgt.service.chain.SugerenciaTecnico;
import com.carmotors.sgt.service.chain.SugerenciaTecnicoChain;
import com.carmotors.sgt.service.state.EstadoEnProceso;
import com.carmotors.sgt.service.state.EstadoOrden;
import com.carmotors.sgt.service.state.EstadoOrdenFactory;
import com.carmotors.sgt.service.state.EstadoPendiente;
import com.carmotors.sgt.util.InputValidator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reglas de negocio para crear órdenes, editar sus líneas y reservar repuestos en ellas
 * (RF-08, RF-10, RF-35, RN-01, RN-02, RN-11).
 */
public class OrdenServicioService {

    private static final int MAX_LONGITUD_DESCRIPCION = 255;
    private static final int MAX_LONGITUD_OBSERVACIONES = 255;
    private static final BigDecimal MAX_HORAS_TRABAJADAS = new BigDecimal("999.99");

    private final OrdenServicioDAO ordenServicioDAO;
    private final OrdenServicioDetalleDAO ordenServicioDetalleDAO;
    private final VehiculoDAO vehiculoDAO;
    private final ClienteDAO clienteDAO;
    private final ServicioTipoDAO servicioTipoDAO;
    private final TecnicoDAO tecnicoDAO;
    private final RepuestoDAO repuestoDAO;
    private final SugerenciaTecnicoChain sugerenciaTecnicoChain;
    private final DetalleRepuestoDAO detalleRepuestoDAO;
    private final InventarioService inventarioService;
    private final boolean usarTransaccionJdbc;

    /**
     * Constructor de producción: usa DAOs JDBC y transacción con una única {@link Connection}.
     * Las dependencias de la reserva de repuestos se crean con sus implementaciones por defecto.
     */
    public OrdenServicioService(VehiculoDAO vehiculoDAO, ClienteDAO clienteDAO, ServicioTipoDAO servicioTipoDAO) {
        this(vehiculoDAO, clienteDAO, servicioTipoDAO, new RepuestoDAOImpl(), new DetalleRepuestoDAOImpl(),
                new InventarioService());
    }

    /**
     * Constructor de producción con las dependencias de la reserva de repuestos (RF-10) inyectadas.
     */
    public OrdenServicioService(VehiculoDAO vehiculoDAO, ClienteDAO clienteDAO, ServicioTipoDAO servicioTipoDAO,
                                RepuestoDAO repuestoDAO, DetalleRepuestoDAO detalleRepuestoDAO,
                                InventarioService inventarioService) {
        this(new OrdenServicioDAOImpl(), new OrdenServicioDetalleDAOImpl(), vehiculoDAO, clienteDAO,
                servicioTipoDAO, new TecnicoDAOImpl(), repuestoDAO, detalleRepuestoDAO, inventarioService,
                true, true);
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
        this(ordenServicioDAO, ordenServicioDetalleDAO, vehiculoDAO, clienteDAO, servicioTipoDAO,
                tecnicoDAO, repuestoDAO, null, null, usarTransaccionJdbc, false);
    }

    /**
     * Constructor completo: con {@code usarTransaccionJdbc = false} permite probar la reserva
     * de repuestos con DAOs en memoria.
     */
    public OrdenServicioService(OrdenServicioDAO ordenServicioDAO, OrdenServicioDetalleDAO ordenServicioDetalleDAO,
                                VehiculoDAO vehiculoDAO, ClienteDAO clienteDAO, ServicioTipoDAO servicioTipoDAO,
                                RepuestoDAO repuestoDAO, DetalleRepuestoDAO detalleRepuestoDAO,
                                InventarioService inventarioService, boolean usarTransaccionJdbc) {
        this(ordenServicioDAO, ordenServicioDetalleDAO, vehiculoDAO, clienteDAO, servicioTipoDAO,
                new TecnicoDAOImpl(), repuestoDAO, detalleRepuestoDAO, inventarioService, usarTransaccionJdbc, true);
    }

    private OrdenServicioService(OrdenServicioDAO ordenServicioDAO, OrdenServicioDetalleDAO ordenServicioDetalleDAO,
                                 VehiculoDAO vehiculoDAO, ClienteDAO clienteDAO, ServicioTipoDAO servicioTipoDAO,
                                 TecnicoDAO tecnicoDAO, RepuestoDAO repuestoDAO,
                                 DetalleRepuestoDAO detalleRepuestoDAO, InventarioService inventarioService,
                                 boolean usarTransaccionJdbc, boolean exigirDependenciasReserva) {
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
        if (exigirDependenciasReserva) {
            if (detalleRepuestoDAO == null) {
                throw new ValidacionException("El DAO de repuestos por línea es obligatorio.");
            }
            if (inventarioService == null) {
                throw new ValidacionException("El servicio de inventario es obligatorio.");
            }
        }
        this.repuestoDAO = repuestoDAO;
        this.sugerenciaTecnicoChain = new SugerenciaTecnicoChain();
        this.detalleRepuestoDAO = detalleRepuestoDAO;
        this.inventarioService = inventarioService;
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

    /**
     * Verifica que una orden exista y se encuentre en estado editable ({@code Pendiente} o {@code En Proceso}).
     *
     * @param ordenId identificador de la orden
     */
    public void validarOrdenEditable(int ordenId) {
        validarOrdenId(ordenId);
        validarEstadoEditable(ordenServicioDAO.buscarPorId(ordenId), ordenId);
    }

    /**
     * Retorna las asignaciones de técnicos de una línea de orden (RF-09).
     *
     * @param ordenId   identificador de la orden
     * @param detalleId identificador de la línea perteneciente a la orden
     * @return lista de asignaciones con sus horas trabajadas
     */
    public List<DetalleTecnico> listarTecnicosDeLinea(int ordenId, int detalleId) {
        validarOrdenId(ordenId);
        validarDetalleId(detalleId);
        OrdenServicio orden = ordenServicioDAO.buscarPorId(ordenId);
        if (orden == null) {
            throw new EntidadNoEncontradaException("No se encontró una orden de servicio con el ID: " + ordenId);
        }
        buscarLineaDeOrden(ordenServicioDetalleDAO, ordenId, detalleId, false);
        return ordenServicioDetalleDAO.listarTecnicosAsignados(detalleId);
    }

    /**
     * Sugiere el técnico óptimo para una línea usando la cadena {@code RN-20} / {@code RN-21},
     * excluyendo a los técnicos ya asignados a la misma línea (RF-09).
     *
     * @param ordenId   identificador de la orden editable
     * @param detalleId identificador de la línea
     * @return sugerencia encontrada o {@code null} si no quedan técnicos activos elegibles
     */
    public SugerenciaTecnico sugerirTecnicoParaLinea(int ordenId, int detalleId) {
        validarOrdenId(ordenId);
        validarDetalleId(detalleId);
        validarEstadoEditable(ordenServicioDAO.buscarPorId(ordenId), ordenId);
        OrdenServicioDetalle linea = buscarLineaDeOrden(ordenServicioDetalleDAO, ordenId, detalleId, false);
        ServicioTipo tipo = obtenerServicioTipoObligatorio(servicioTipoDAO, linea.getServicioTipoId());

        List<Tecnico> activos = tecnicoDAO.listarActivos();
        Map<Integer, Integer> cargas = tecnicoDAO.contarCargaActivaPorTecnico();
        List<DetalleTecnico> yaAsignados = ordenServicioDetalleDAO.listarTecnicosAsignados(detalleId);

        Set<Integer> idsAsignados = new HashSet<>();
        for (DetalleTecnico asignacion : yaAsignados) {
            idsAsignados.add(asignacion.getTecnicoId());
        }

        List<Tecnico> candidatos = new ArrayList<>();
        if (activos != null) {
            for (Tecnico tecnico : activos) {
                if (tecnico != null && !idsAsignados.contains(tecnico.getId())) {
                    candidatos.add(tecnico);
                }
            }
        }

        return sugerenciaTecnicoChain.sugerir(tipo.getEspecialidadId(), candidatos, cargas);
    }

    /**
     * Valida precondiciones de asignación e indica si se requiere confirmación explícita porque
     * la especialidad del técnico no coincide con la requerida por la línea (RF-09, RN-19).
     *
     * @param ordenId   identificador de la orden editable
     * @param detalleId identificador de la línea
     * @param tecnicoId identificador del técnico candidato
     * @return {@code true} si la especialidad no coincide y debe pedirse confirmación
     */
    public boolean requiereConfirmacionEspecialidad(int ordenId, int detalleId, int tecnicoId) {
        validarOrdenId(ordenId);
        validarDetalleId(detalleId);
        validarTecnicoId(tecnicoId);
        validarEstadoEditable(ordenServicioDAO.buscarPorId(ordenId), ordenId);
        OrdenServicioDetalle linea = buscarLineaDeOrden(ordenServicioDetalleDAO, ordenId, detalleId, false);
        ServicioTipo tipo = obtenerServicioTipoObligatorio(servicioTipoDAO, linea.getServicioTipoId());
        Tecnico tecnico = validarTecnicoDisponible(tecnicoDAO, tecnicoId, false);

        if (ordenServicioDetalleDAO.existeTecnicoAsignado(detalleId, tecnicoId)) {
            throw new DatoDuplicadoException("El técnico '" + tecnico.getNombre()
                    + "' ya está asignado a la línea " + detalleId + ".");
        }
        return !especialidadCoincide(tipo, tecnico);
    }

    /**
     * Asigna un técnico activo a una línea de orden con sus horas trabajadas iniciales (RF-09, RN-07, RN-19).
     *
     * @param ordenId                   identificador de la orden editable
     * @param detalleId                 identificador de la línea
     * @param tecnicoId                 identificador del técnico activo
     * @param horasTrabajadas           horas iniciales (si es {@code null} se normaliza a {@code 0.00})
     * @param confirmarOtraEspecialidad confirmación explícita cuando la especialidad no coincide (RN-19)
     * @return registro {@link DetalleTecnico} persistido
     */
    public DetalleTecnico asignarTecnicoALinea(
            int ordenId,
            int detalleId,
            int tecnicoId,
            BigDecimal horasTrabajadas,
            boolean confirmarOtraEspecialidad) {
        validarOrdenId(ordenId);
        validarDetalleId(detalleId);
        validarTecnicoId(tecnicoId);
        BigDecimal horasNormalizadas = validarHorasTrabajadas(horasTrabajadas, true);

        editarLineas(ordenId, (detallesDao, tiposDao, tecnicosDao, repuestosDao, estado) -> {
            OrdenServicioDetalle linea = buscarLineaDeOrden(detallesDao, ordenId, detalleId, true);
            Tecnico tecnico = validarTecnicoDisponible(tecnicosDao, tecnicoId, true);
            if (detallesDao.existeTecnicoAsignado(detalleId, tecnicoId)) {
                throw new DatoDuplicadoException("El técnico '" + tecnico.getNombre()
                        + "' ya está asignado a la línea " + detalleId + ".");
            }
            ServicioTipo tipo = obtenerServicioTipoObligatorio(tiposDao, linea.getServicioTipoId());
            if (!especialidadCoincide(tipo, tecnico) && !confirmarOtraEspecialidad) {
                throw new ValidacionException(
                        "La especialidad del técnico no coincide con la requerida por el servicio; "
                                + "se requiere confirmación explícita (RN-19).");
            }
            detallesDao.asignarTecnico(detalleId, tecnicoId, horasNormalizadas);
            return detalleId;
        });

        return new DetalleTecnico(detalleId, tecnicoId, horasNormalizadas);
    }

    /**
     * Registra o actualiza las horas trabajadas de un técnico ya asignado a una línea (RF-09).
     *
     * @param ordenId         identificador de la orden editable
     * @param detalleId       identificador de la línea
     * @param tecnicoId       identificador del técnico asignado
     * @param horasTrabajadas total de horas trabajadas (obligatorio, no negativo)
     * @return registro {@link DetalleTecnico} actualizado
     */
    public DetalleTecnico registrarHorasTrabajadas(
            int ordenId,
            int detalleId,
            int tecnicoId,
            BigDecimal horasTrabajadas) {
        validarOrdenId(ordenId);
        validarDetalleId(detalleId);
        validarTecnicoId(tecnicoId);
        BigDecimal horasNormalizadas = validarHorasTrabajadas(horasTrabajadas, false);

        editarLineas(ordenId, (detallesDao, tiposDao, tecnicosDao, repuestosDao, estado) -> {
            buscarLineaDeOrden(detallesDao, ordenId, detalleId, true);
            if (!detallesDao.existeTecnicoAsignado(detalleId, tecnicoId)) {
                throw new EntidadNoEncontradaException("El técnico " + tecnicoId
                        + " no está asignado a la línea " + detalleId + ".");
            }
            if (!detallesDao.actualizarHorasTrabajadas(detalleId, tecnicoId, horasNormalizadas)) {
                throw new CarMotorsException("No se pudieron actualizar las horas trabajadas del técnico "
                        + tecnicoId + " en la línea " + detalleId + ".");
            }
            return detalleId;
        });

        return new DetalleTecnico(detalleId, tecnicoId, horasNormalizadas);
    }

    private void validarOrdenId(int ordenId) {
        if (ordenId <= 0) {
            throw new ValidacionException("El ID de la orden debe ser un número positivo.");
        }
    }

    private void validarDetalleId(int detalleId) {
        if (detalleId <= 0) {
            throw new ValidacionException("El ID de la línea debe ser un número positivo.");
        }
    }

    private void validarTecnicoId(int tecnicoId) {
        if (tecnicoId <= 0) {
            throw new ValidacionException("El ID del técnico debe ser un número positivo.");
        }
    }

    private OrdenServicioDetalle buscarLineaDeOrden(
            OrdenServicioDetalleDAO detalleDao,
            int ordenId,
            int detalleId,
            boolean bloquear) {
        List<OrdenServicioDetalle> lineas = bloquear
                ? detalleDao.listarPorOrdenParaActualizar(ordenId)
                : detalleDao.listarPorOrden(ordenId);
        for (OrdenServicioDetalle linea : lineas) {
            if (linea.getId() == detalleId) {
                return linea;
            }
        }
        throw new EntidadNoEncontradaException(
                "La línea " + detalleId + " no pertenece a la orden " + ordenId + ".");
    }

    private ServicioTipo obtenerServicioTipoObligatorio(ServicioTipoDAO tipoDao, int servicioTipoId) {
        ServicioTipo tipo = tipoDao.buscarPorId(servicioTipoId);
        if (tipo == null) {
            throw new EntidadNoEncontradaException(
                    "No se encontró el tipo de servicio con ID: " + servicioTipoId + ".");
        }
        return tipo;
    }

    private Tecnico validarTecnicoDisponible(TecnicoDAO daoTecnico, int tecnicoId, boolean bloquear) {
        Tecnico tecnico = bloquear
                ? daoTecnico.buscarPorIdParaActualizar(tecnicoId)
                : daoTecnico.buscarPorId(tecnicoId);
        if (tecnico == null) {
            throw new EntidadNoEncontradaException("No se encontró un técnico con el ID: " + tecnicoId + ".");
        }
        if (!tecnico.isActivo()) {
            throw new ValidacionException("El técnico '" + tecnico.getNombre()
                    + "' está inactivo y no puede ser asignado.");
        }
        return tecnico;
    }

    private BigDecimal validarHorasTrabajadas(BigDecimal horasTrabajadas, boolean permitirNulo) {
        if (horasTrabajadas == null) {
            if (permitirNulo) {
                return BigDecimal.ZERO.setScale(InputValidator.ESCALA_MONETARIA, InputValidator.REDONDEO_MONETARIO);
            }
            throw new ValidacionException("Las horas trabajadas son obligatorias.");
        }
        if (horasTrabajadas.signum() < 0) {
            throw new ValidacionException("Las horas trabajadas no pueden ser negativas.");
        }
        BigDecimal normalizadas = horasTrabajadas.setScale(
                InputValidator.ESCALA_MONETARIA, InputValidator.REDONDEO_MONETARIO);
        if (normalizadas.compareTo(MAX_HORAS_TRABAJADAS) > 0) {
            throw new ValidacionException(
                    "Las horas trabajadas no pueden superar " + MAX_HORAS_TRABAJADAS + ".");
        }
        return normalizadas;
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
     * Reserva unidades de un repuesto en una línea de servicio (RF-10, RN-01, RN-02, RN-11).
     *
     * <p>Valida todo antes de abrir la transacción. En una única unidad de trabajo JDBC aumenta
     * {@code repuestos.cantidad_reservada} y guarda la reserva en {@code detalle_repuestos} con el
     * nombre y el precio de venta vigentes como snapshot. El stock físico de los lotes no cambia.</p>
     *
     * @param detalleId  línea de servicio que recibe el repuesto
     * @param repuestoId repuesto a reservar
     * @param cantidad   unidades a reservar (mayor que cero)
     * @return la reserva guardada
     */
    public DetalleRepuesto reservarRepuesto(int detalleId, int repuestoId, int cantidad) {
        verificarDependenciasReserva();
        if (cantidad <= 0) {
            throw new ValidacionException("La cantidad a reservar debe ser mayor que cero.");
        }
        if (detalleId <= 0) {
            throw new ValidacionException("El ID de la línea de servicio debe ser un número positivo.");
        }
        if (repuestoId <= 0) {
            throw new ValidacionException("El ID del repuesto debe ser un número positivo.");
        }

        OrdenServicioDetalle linea = ordenServicioDetalleDAO.buscarPorId(detalleId);
        if (linea == null) {
            throw new EntidadNoEncontradaException("No se encontró una línea de servicio con el ID: " + detalleId);
        }
        OrdenServicio orden = ordenServicioDAO.buscarPorId(linea.getOrdenId());
        if (orden == null) {
            throw new EntidadNoEncontradaException("No se encontró la orden de servicio con el ID: "
                    + linea.getOrdenId());
        }
        EstadoOrden estado = EstadoOrdenFactory.desdeId(orden.getEstadoId());
        if (!estado.permiteEdicion()) {
            throw new ValidacionException("La orden " + orden.getId() + " está en estado '" + estado.getNombre()
                    + "' y no permite cambios; no se pueden reservar repuestos.");
        }

        Repuesto repuesto = repuestoDAO.buscarPorId(repuestoId);
        if (repuesto == null) {
            throw new EntidadNoEncontradaException("No se encontró un repuesto con el ID: " + repuestoId);
        }
        if (!repuesto.isActivo()) {
            throw new ValidacionException("El repuesto '" + repuesto.getNombre()
                    + "' está inactivo; no se puede reservar.");
        }
        if (detalleRepuestoDAO.buscar(detalleId, repuestoId) != null) {
            throw new ValidacionException("El repuesto '" + repuesto.getNombre()
                    + "' ya está asignado a la línea " + detalleId + ".");
        }

        int disponible = inventarioService.calcularStock(repuestoId).getDisponible();
        if (cantidad > disponible) {
            throw new StockInsuficienteException("Stock insuficiente para '" + repuesto.getNombre()
                    + "': solicitado " + cantidad + ", disponible " + disponible + ".");
        }

        DetalleRepuesto reserva = new DetalleRepuesto();
        reserva.setDetalleId(detalleId);
        reserva.setRepuestoId(repuestoId);
        reserva.setCantidadAsignada(cantidad);
        reserva.setNombreRepuestoSnapshot(repuesto.getNombre());
        reserva.setPrecioUnitarioVenta(repuesto.getPrecioVenta()
                .setScale(InputValidator.ESCALA_MONETARIA, InputValidator.REDONDEO_MONETARIO));

        if (!usarTransaccionJdbc) {
            guardarReserva(reserva, repuestoDAO, detalleRepuestoDAO);
            return reserva;
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                RepuestoDAO repuestoDaoTransaccional = new RepuestoDAOImpl(connection);
                DetalleRepuestoDAO detalleRepuestoDaoTransaccional = new DetalleRepuestoDAOImpl(connection);

                guardarReserva(reserva, repuestoDaoTransaccional, detalleRepuestoDaoTransaccional);
                connection.commit();
                return reserva;
            } catch (CarMotorsException e) {
                connection.rollback();
                throw e;
            } catch (SQLException e) {
                connection.rollback();
                throw new CarMotorsException("No se pudo reservar el repuesto: " + e.getMessage(), e);
            } catch (RuntimeException e) {
                connection.rollback();
                throw new CarMotorsException("Error inesperado al reservar el repuesto: " + e.getMessage(), e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new CarMotorsException("No se pudo completar la transacción de la reserva del repuesto: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Retorna el disponible para nuevas reservas de un repuesto (sección 2.5).
     *
     * @param repuestoId identificador del repuesto
     * @return unidades disponibles
     */
    public int consultarDisponible(int repuestoId) {
        verificarDependenciasReserva();
        return inventarioService.calcularStock(repuestoId).getDisponible();
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

    private void verificarDependenciasReserva() {
        if (repuestoDAO == null || detalleRepuestoDAO == null || inventarioService == null) {
            throw new CarMotorsException("La reserva de repuestos no está configurada en este servicio.");
        }
    }

    private void guardarReserva(DetalleRepuesto reserva, RepuestoDAO repuestoDao, DetalleRepuestoDAO detalleDao) {
        boolean actualizado = repuestoDao.aumentarReservado(reserva.getRepuestoId(), reserva.getCantidadAsignada());
        if (!actualizado) {
            throw new CarMotorsException("No se pudo actualizar el reservado del repuesto "
                    + reserva.getRepuestoId() + ".");
        }
        boolean guardado = detalleDao.guardar(reserva);
        if (!guardado) {
            throw new CarMotorsException("No se pudo registrar la reserva del repuesto en la línea "
                    + reserva.getDetalleId() + ".");
        }
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
