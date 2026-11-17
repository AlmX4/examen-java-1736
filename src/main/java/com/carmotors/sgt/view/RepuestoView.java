package com.carmotors.sgt.view;

import java.util.List;

import com.carmotors.sgt.controller.RepuestoController;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.model.RepuestoTipo;
import com.carmotors.sgt.util.ConsoleReader;

public class RepuestoView implements SubmenuVista {

    private final RepuestoController repuestoController;
    private final CatalogoProveedorView catalogoProveedorView;

    public RepuestoView(RepuestoController repuestoController) {
        this(repuestoController, null);
    }

    public RepuestoView(RepuestoController repuestoController, CatalogoProveedorView catalogoProveedorView) {
        if (repuestoController == null) {
            throw new IllegalArgumentException("El controlador de repuestos es obligatorio.");
        }
        this.repuestoController = repuestoController;
        this.catalogoProveedorView = catalogoProveedorView;
    }

    @Override
    public String obtenerTitulo() {
        return "Inventario - Repuestos";
    }

    @Override
    public void mostrarMenu() {
        boolean ejecutando = true;
        while (ejecutando) {
            System.out.println("\n==================================================");
            System.out.println("                 GESTIÓN DE REPUESTOS");
            System.out.println("==================================================");
            System.out.println("  1. Listar repuestos");
            System.out.println("  2. Buscar repuesto por id");
            System.out.println("  3. Crear repuesto");
            System.out.println("  4. Actualizar repuesto");
            System.out.println("  5. Desactivar repuesto");
            System.out.println("  6. Reactivar repuesto");
            System.out.println("  7. Ver proveedores del repuesto");
            System.out.println("  0. Volver");
            System.out.println("==================================================");

            int opcion = ConsoleReader.leerEnteroEnRango("Seleccione una opción", 0, 7);
            switch (opcion) {
                case 1:
                    listarRepuestos();
                    break;
                case 2:
                    buscarRepuesto();
                    break;
                case 3:
                    crearRepuesto();
                    break;
                case 4:
                    actualizarRepuesto();
                    break;
                case 5:
                    desactivarRepuesto();
                    break;
                case 6:
                    reactivarRepuesto();
                    break;
                case 7:
                    if (catalogoProveedorView != null) {
                        catalogoProveedorView.verProveedoresDeRepuesto();
                    } else {
                        System.out.println("\n  [i] El catálogo proveedor-repuesto aún no está conectado.");
                        ConsoleReader.pausar();
                    }
                    break;
                case 0:
                    ejecutando = false;
                    break;
                default:
                    break;
            }
        }
    }

    private void listarRepuestos() {
        try {
            List<Repuesto> repuestos = repuestoController.listarTodos();
            if (repuestos.isEmpty()) {
                System.out.println("\n  [i] No hay repuestos registrados.");
            } else {
                System.out.println("\n  [Lista de repuestos]");
                for (Repuesto repuesto : repuestos) {
                    imprimirDetalleRepuesto(repuesto);
                }
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void buscarRepuesto() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("Ingrese el id del repuesto");
            Repuesto repuesto = repuestoController.buscarPorId(id);
            System.out.println("\n  [Repuesto encontrado]");
            imprimirDetalleRepuesto(repuesto);
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void crearRepuesto() {
        try {
            List<RepuestoTipo> tipos = repuestoController.listarTipos();
            if (tipos.isEmpty()) {
                System.out.println("\n  [!] No hay tipos de repuesto registrados en la base de datos.");
                ConsoleReader.pausar();
                return;
            }

            Repuesto repuesto = new Repuesto();
            repuesto.setNombre(ConsoleReader.leerTextoObligatorio("Nombre del repuesto"));
            repuesto.setRepuestoTipoId(seleccionarTipo(tipos));
            repuesto.setMarca(ConsoleReader.leerTextoObligatorio("Marca del repuesto"));
            repuesto.setPrecioVenta(ConsoleReader.leerBigDecimalPositivo("Precio de venta"));
            repuesto.setStockMinimo(ConsoleReader.leerEnteroNoNegativo("Stock mínimo"));
            repuesto.setCantidadReservada(0);
            repuesto.setActivo(true);

            Repuesto creado = repuestoController.crear(repuesto);
            System.out.println("\n  [✓] Repuesto creado correctamente con id " + creado.getId());
            imprimirDetalleRepuesto(creado);
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void actualizarRepuesto() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("Ingrese el id del repuesto a actualizar");
            Repuesto actual = repuestoController.buscarPorId(id);
            System.out.println("\n  Datos actuales:");
            imprimirDetalleRepuesto(actual);

            List<RepuestoTipo> tipos = repuestoController.listarTipos();
            if (tipos.isEmpty()) {
                System.out.println("\n  [!] No hay tipos de repuesto registrados en la base de datos.");
                ConsoleReader.pausar();
                return;
            }

            Repuesto actualizado = new Repuesto();
            actualizado.setId(actual.getId());
            actualizado.setNombre(ConsoleReader.leerTextoObligatorio("Nuevo nombre"));
            actualizado.setRepuestoTipoId(seleccionarTipo(tipos));
            actualizado.setMarca(ConsoleReader.leerTextoObligatorio("Nueva marca"));
            actualizado.setPrecioVenta(ConsoleReader.leerBigDecimalPositivo("Nuevo precio de venta"));
            actualizado.setStockMinimo(ConsoleReader.leerEnteroNoNegativo("Nuevo stock mínimo"));
            actualizado.setCantidadReservada(actual.getCantidadReservada());
            actualizado.setActivo(actual.isActivo());

            Repuesto guardado = repuestoController.actualizar(actualizado);
            System.out.println("\n  [✓] Repuesto actualizado correctamente.");
            imprimirDetalleRepuesto(guardado);
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void desactivarRepuesto() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("Ingrese el id del repuesto a desactivar");
            Repuesto actual = repuestoController.buscarPorId(id);
            if (!actual.isActivo()) {
                System.out.println("\n  [i] El repuesto ya se encuentra inactivo.");
                ConsoleReader.pausar();
                return;
            }

            if (!ConsoleReader.leerConfirmacion("¿Confirma desactivar el repuesto '" + actual.getNombre() + "'?")) {
                System.out.println("\n  [i] Operación cancelada.");
                ConsoleReader.pausar();
                return;
            }

            Repuesto repuesto = repuestoController.desactivar(id);
            System.out.println("\n  [✓] Repuesto desactivado correctamente.");
            imprimirDetalleRepuesto(repuesto);
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void reactivarRepuesto() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("Ingrese el id del repuesto a reactivar");
            Repuesto actual = repuestoController.buscarPorId(id);
            if (actual.isActivo()) {
                System.out.println("\n  [i] El repuesto ya se encuentra activo.");
                ConsoleReader.pausar();
                return;
            }

            Repuesto repuesto = repuestoController.reactivar(id);
            System.out.println("\n  [✓] Repuesto reactivado correctamente.");
            imprimirDetalleRepuesto(repuesto);
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private int seleccionarTipo(List<RepuestoTipo> tipos) {
        System.out.println("\n  Tipos de repuesto disponibles:");
        for (RepuestoTipo tipo : tipos) {
            System.out.println("    " + tipo.getId() + ". " + tipo.getNombre());
        }

        while (true) {
            int idElegido = ConsoleReader.leerEnteroPositivo("Seleccione el ID del tipo de repuesto");
            for (RepuestoTipo tipo : tipos) {
                if (tipo.getId() == idElegido) {
                    return idElegido;
                }
            }
            System.out.println("  [!] Error: El ID del tipo de repuesto no pertenece al catálogo.");
        }
    }

    private void imprimirDetalleRepuesto(Repuesto repuesto) {
        String estado = repuesto.isActivo() ? "ACTIVO" : "INACTIVO";
        System.out.println("  - ID " + repuesto.getId()
                + " | " + repuesto.getNombre()
                + " | Tipo: " + repuestoController.obtenerNombreTipo(repuesto.getRepuestoTipoId())
                + " | Marca: " + repuesto.getMarca()
                + " | Precio: $" + repuesto.getPrecioVenta()
                + " | Stock mínimo: " + repuesto.getStockMinimo()
                + " | Reservada: " + repuesto.getCantidadReservada()
                + " | Estado: " + estado);
    }
}
