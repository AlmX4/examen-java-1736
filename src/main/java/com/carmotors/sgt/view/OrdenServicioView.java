package com.carmotors.sgt.view;

import com.carmotors.sgt.controller.ClienteController;
import com.carmotors.sgt.controller.OrdenServicioController;
import com.carmotors.sgt.controller.ServicioTipoController;
import com.carmotors.sgt.controller.TecnicoController;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.OrdenServicio;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.util.ConsoleReader;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Submenú de consola para las órdenes de servicio (RF-08, RNF-03, RNF-24).
 * Solo captura datos e imprime resultados; las reglas residen en el servicio.
 */
public class OrdenServicioView implements SubmenuVista {

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrdenServicioController ordenServicioController;
    private final ClienteController clienteController;
    private final ServicioTipoController servicioTipoController;

    private final TecnicoController tecnicoController;

    public OrdenServicioView(OrdenServicioController ordenServicioController,
                             ClienteController clienteController,
                             ServicioTipoController servicioTipoController,
                             TecnicoController tecnicoController) {
        if (ordenServicioController == null) {
            throw new IllegalArgumentException("El controlador de órdenes de servicio es obligatorio.");
        }
        if (clienteController == null) {
            throw new IllegalArgumentException("El controlador de clientes y vehículos es obligatorio.");
        }
        if (servicioTipoController == null) {
            throw new IllegalArgumentException("El controlador de tipos de servicio es obligatorio.");
        }
        if (tecnicoController == null) {
            throw new IllegalArgumentException("El controlador de técnicos es obligatorio.");
        }
        this.ordenServicioController = ordenServicioController;
        this.clienteController = clienteController;
        this.servicioTipoController = servicioTipoController;
        this.tecnicoController = tecnicoController;
    }

    @Override
    public String obtenerTitulo() {
        return "Órdenes de Servicio";
    }

    @Override
    public void mostrarMenu() {
        boolean ejecutando = true;
        while (ejecutando) {
            System.out.println("\n==================================================");
            System.out.println("              ÓRDENES DE SERVICIO                 ");
            System.out.println("==================================================");
            System.out.println("  1. Crear orden de servicio");
            System.out.println("  2. Agregar línea de servicio");
            System.out.println("  3. Retirar línea de servicio");
            System.out.println("  0. Volver");
            System.out.println("==================================================");

            int opcion = ConsoleReader.leerEnteroEnRango("Seleccione una opción", 0, 3);
            switch (opcion) {
                case 1:
                    crearOrden();
                    break;
                case 2:
                    agregarLinea();
                    break;
                case 3:
                    quitarLinea();
                    break;
                case 0:
                    ejecutando = false;
                    break;
                default:
                    break;
            }
        }
    }

    private void crearOrden() {
        try {
            String placa = ConsoleReader.leerPlaca("Placa del vehículo");
            Vehiculo vehiculo = clienteController.buscarVehiculoPorPlaca(placa);
            System.out.println("  Vehículo: " + vehiculo.getPlaca() + " | " + vehiculo.getMarca()
                    + " " + vehiculo.getModelo());

            String descripcion = ConsoleReader.leerTextoObligatorio("Descripción del problema");

            List<ServicioTipo> tipos = servicioTipoController.listarTodos();
            if (tipos.isEmpty()) {
                System.out.println("\n  [!] No hay tipos de servicio registrados; no se puede crear la orden.");
                ConsoleReader.pausar();
                return;
            }

            List<OrdenServicioDetalle> lineas = new ArrayList<>();
            boolean agregarOtra = true;
            while (agregarOtra) {
                System.out.println("\n  --- Línea de servicio #" + (lineas.size() + 1) + " ---");
                ServicioTipo tipo = seleccionarTipoServicio(tipos);
                String observaciones = ConsoleReader.leerTextoOpcional("Observaciones (Enter para omitir)");
                lineas.add(new OrdenServicioDetalle(tipo.getId(), observaciones));
                agregarOtra = ConsoleReader.leerConfirmacion("¿Desea agregar otra línea de servicio?");
            }

            OrdenServicio creada = ordenServicioController.crearOrden(vehiculo.getId(), descripcion, lineas);
            System.out.println("\n  [✓] Orden de servicio creada exitosamente.");
            imprimirResumen(creada, vehiculo);
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error al crear la orden de servicio: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void agregarLinea() {
        try {
            int ordenId = ConsoleReader.leerEnteroPositivo("ID de la orden");
            boolean requiereTecnico = ordenServicioController.requiereTecnicoParaAgregar(ordenId);
            List<ServicioTipo> tipos = servicioTipoController.listarTodos();
            if (tipos.isEmpty()) {
                System.out.println("  [!] No hay tipos de servicio registrados.");
                return;
            }

            ServicioTipo tipo = seleccionarTipoServicio(tipos);
            String observaciones = ConsoleReader.leerTextoOpcional("Observaciones (Enter para omitir)");
            Integer tecnicoId = null;
            boolean confirmarOtraEspecialidad = false;
            if (requiereTecnico) {
                List<Tecnico> tecnicos = tecnicoController.listarActivos();
                if (tecnicos.isEmpty()) {
                    System.out.println("  [!] No hay técnicos activos para asignar.");
                    return;
                }
                for (Tecnico tecnico : tecnicos) {
                    System.out.println("    " + tecnico.getId() + ". " + tecnico.getNombre()
                            + " | Especialidad ID: " + tecnico.getEspecialidadId());
                }
                int seleccion = ConsoleReader.leerEnteroEnRango("ID del técnico (0 para cancelar)", 0, Integer.MAX_VALUE);
                if (seleccion == 0) {
                    return;
                }
                Tecnico tecnico = tecnicoController.buscarPorId(seleccion);
                if (!ordenServicioController.especialidadCoincide(tipo, tecnico)) {
                    confirmarOtraEspecialidad = ConsoleReader.leerConfirmacion(
                            "La especialidad del técnico difiere de la requerida. ¿Desea asignarlo?");
                    if (!confirmarOtraEspecialidad) {
                        return;
                    }
                }
                tecnicoId = seleccion;
            }
            OrdenServicioDetalle linea = new OrdenServicioDetalle(tipo.getId(), observaciones);
            System.out.println("  " + ordenServicioController.agregarLinea(
                    ordenId, linea, tecnicoId, confirmarOtraEspecialidad));
        } catch (CarMotorsException e) {
            System.out.println("  [!] " + e.getMessage());
        } finally {
            ConsoleReader.pausar();
        }
    }

    private void quitarLinea() {
        try {
            int ordenId = ConsoleReader.leerEnteroPositivo("ID de la orden");
            imprimirLineas(ordenServicioController.listarLineas(ordenId));
            int detalleId = ConsoleReader.leerEnteroEnRango("ID de la línea (0 para cancelar)", 0, Integer.MAX_VALUE);
            if (detalleId != 0) {
                System.out.println("  " + ordenServicioController.quitarLinea(ordenId, detalleId));
            }
        } catch (CarMotorsException e) {
            System.out.println("  [!] " + e.getMessage());
        } finally {
            ConsoleReader.pausar();
        }
    }

    private ServicioTipo seleccionarTipoServicio(List<ServicioTipo> tipos) {
        System.out.println("  Tipos de servicio disponibles:");
        for (ServicioTipo tipo : tipos) {
            System.out.println("    " + tipo.getId() + ". " + tipo.getNombre()
                    + " | Mano de obra base: $" + tipo.getPrecioManoObraBase());
        }
        while (true) {
            int idElegido = ConsoleReader.leerEnteroPositivo("ID del tipo de servicio");
            for (ServicioTipo tipo : tipos) {
                if (tipo.getId() == idElegido) {
                    return tipo;
                }
            }
            System.out.println("  [!] Error: El ID no pertenece al catálogo de tipos de servicio.");
        }
    }

    private void imprimirResumen(OrdenServicio orden, Vehiculo vehiculo) {
        System.out.println("\n--- Resumen de la Orden ---");
        System.out.println("  Orden ID:          " + orden.getId());
        System.out.println("  Placa:             " + vehiculo.getPlaca());
        System.out.println("  Estado:            " + ordenServicioController.obtenerNombreEstado(orden.getEstadoId()));
        System.out.println("  Fecha recepción:   " + orden.getFechaRecepcion().format(FORMATO_FECHA_HORA));
        System.out.println("  Problema:          " + orden.getDescripcionProblema());
        System.out.println("  Líneas de servicio:");
        imprimirLineas(ordenServicioController.listarLineas(orden.getId()));
    }

    private void imprimirLineas(List<OrdenServicioDetalle> lineas) {
        for (OrdenServicioDetalle linea : lineas) {
            String observaciones = linea.getObservaciones() == null ? "N/A" : linea.getObservaciones();
            System.out.println("    - Línea " + linea.getId()
                    + " | " + linea.getNombreServicioSnapshot()
                    + " | Mano de obra: $" + linea.getCostoManoObra()
                    + " | Observaciones: " + observaciones);
        }
    }
}
