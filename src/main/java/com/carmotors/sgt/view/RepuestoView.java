package com.carmotors.sgt.view;

import java.util.List;

import com.carmotors.sgt.controller.InventarioController;
import com.carmotors.sgt.controller.RepuestoController;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.model.RepuestoTipo;
import com.carmotors.sgt.model.StockRepuesto;
import com.carmotors.sgt.util.ConsoleReader;

public class RepuestoView implements SubmenuVista {

    private final RepuestoController repuestoController;
    private final CatalogoProveedorView catalogoProveedorView;
    private final LoteView loteView;
    private final InventarioController inventarioController;

    public RepuestoView(RepuestoController repuestoController) {
        this(repuestoController, null, null);
    }

    public RepuestoView(RepuestoController repuestoController, CatalogoProveedorView catalogoProveedorView) {
        this(repuestoController, catalogoProveedorView, null);
    }

    public RepuestoView(RepuestoController repuestoController, CatalogoProveedorView catalogoProveedorView,
                       LoteView loteView) {
        this(repuestoController, catalogoProveedorView, loteView, null);
    }

    public RepuestoView(RepuestoController repuestoController, CatalogoProveedorView catalogoProveedorView,
                       LoteView loteView, InventarioController inventarioController) {
        if (repuestoController == null) {
            throw new IllegalArgumentException("El controlador de repuestos es obligatorio.");
        }
        this.repuestoController = repuestoController;
        this.catalogoProveedorView = catalogoProveedorView;
        this.loteView = loteView;
        this.inventarioController = inventarioController;
    }

    @Override
    public String obtenerTitulo() {
        return "Inventario y Lotes";
    }

    @Override
    public void mostrarMenu() {
        boolean ejecutando = true;
        while (ejecutando) {
            System.out.println("\n==================================================");
            System.out.println("           GESTIÓN DE INVENTARIO Y LOTES          ");
            System.out.println("==================================================");
            System.out.println("  -- Catálogo de Repuestos --");
            System.out.println("  1. Listar repuestos");
            System.out.println("  2. Buscar repuesto por ID");
            System.out.println("  3. Crear repuesto");
            System.out.println("  4. Actualizar repuesto");
            System.out.println("  5. Desactivar repuesto");
            System.out.println("  6. Reactivar repuesto");
            System.out.println("  7. Ver proveedores del repuesto");
            System.out.println("  -- Lotes, Stock y Alertas --");
            System.out.println("  8. Gestión e ingreso de lotes");
            System.out.println("  9. Ver stock del repuesto");
            System.out.println(" 10. Buscar y filtrar repuestos");
            System.out.println(" 11. Reporte de stock bajo");
            System.out.println("  0. Volver al menú principal");
            System.out.println("==================================================");

            int opcion = ConsoleReader.leerEnteroEnRango("Seleccione una opción", 0, 11);
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
                case 8:
                    if (loteView != null) {
                        loteView.mostrarMenu();
                    } else {
                        System.out.println("\n  [i] El ingreso de lotes aún no está conectado.");
                        ConsoleReader.pausar();
                    }
                    break;
                case 9:
                    verStockRepuesto();
                    break;
                case 10:
                    buscarPorFiltros();
                    break;
                case 11:
                    generarReporteStockBajo();
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
            int id = ConsoleReader.leerEnteroPositivo("Ingrese el ID del repuesto");
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
            int tipoId = seleccionarTipo(tipos);
            if (tipoId == 0) {
                System.out.println("\n  [i] Registro de repuesto cancelado.");
                ConsoleReader.pausar();
                return;
            }
            repuesto.setRepuestoTipoId(tipoId);
            repuesto.setMarca(ConsoleReader.leerTextoObligatorio("Marca del repuesto"));
            repuesto.setPrecioVenta(ConsoleReader.leerBigDecimalPositivo("Precio de venta"));
            repuesto.setStockMinimo(ConsoleReader.leerEnteroNoNegativo("Stock mínimo"));
            repuesto.setCantidadReservada(0);
            repuesto.setActivo(true);

            Repuesto creado = repuestoController.crear(repuesto);
            System.out.println("\n  [✓] Repuesto creado correctamente con ID " + creado.getId());
            imprimirDetalleRepuesto(creado);
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void actualizarRepuesto() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("Ingrese el ID del repuesto a actualizar");
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
            int tipoId = seleccionarTipo(tipos);
            if (tipoId == 0) {
                System.out.println("\n  [i] Actualización de repuesto cancelada.");
                ConsoleReader.pausar();
                return;
            }
            actualizado.setRepuestoTipoId(tipoId);
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
            int id = ConsoleReader.leerEnteroPositivo("Ingrese el ID del repuesto a desactivar");
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
            int id = ConsoleReader.leerEnteroPositivo("Ingrese el ID del repuesto a reactivar");
            Repuesto actual = repuestoController.buscarPorId(id);
            imprimirDetalleRepuesto(actual);

            if (actual.isActivo()) {
                System.out.println("\n  [i] El repuesto ya se encuentra activo.");
                ConsoleReader.pausar();
                return;
            }

            if (!ConsoleReader.leerConfirmacion("¿Confirma reactivar el repuesto '" + actual.getNombre() + "'?")) {
                System.out.println("\n  [i] Operación cancelada.");
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

    private void verStockRepuesto() {
        if (inventarioController == null) {
            System.out.println("\n  [i] La consulta de stock aún no está conectada.");
            ConsoleReader.pausar();
            return;
        }

        try {
            int id = ConsoleReader.leerEnteroPositivo("Ingrese el ID del repuesto");
            StockRepuesto stock = inventarioController.consultarStock(id);
            Repuesto repuesto = stock.getRepuesto();

            System.out.println("\n  [Stock del repuesto]");
            System.out.println("  - ID " + repuesto.getId() + " | " + repuesto.getNombre());
            System.out.println("    Stock físico     : " + stock.getStockFisico());
            System.out.println("    Stock operativo  : " + stock.getStockOperativo());
            System.out.println("    Reservado        : " + stock.getReservado());
            System.out.println("    Disponible       : " + stock.getDisponible());
            System.out.println("    Stock mínimo     : " + stock.getStockMinimo());

            if (stock.getDeficit() > 0) {
                System.out.println("\n  [!] ALERTA: faltan " + stock.getDeficit()
                        + " unidad(es) para respaldar las reservas.");
                System.out.println("      Hay reservas sin respaldo porque hay lotes vencidos.");
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void buscarPorFiltros() {
        try {
            System.out.println("\n  [Buscar y filtrar repuestos] (deje vacío cualquier criterio para omitirlo)");
            mostrarGuiaTipos();
            String nombre = ConsoleReader.leerTextoOpcional("Nombre");
            String tipo = ConsoleReader.leerTextoOpcional("Tipo");
            String marca = ConsoleReader.leerTextoOpcional("Marca");
            String proveedor = ConsoleReader.leerTextoOpcional("Proveedor");

            List<Repuesto> resultados = repuestoController.buscarPorFiltros(nombre, tipo, marca, proveedor);
            if (resultados.isEmpty()) {
                System.out.println("\n  [i] No se encontraron repuestos que coincidan con los filtros indicados.");
            } else {
                System.out.println("\n  [Resultados encontrados: " + resultados.size() + "]");
                for (Repuesto repuesto : resultados) {
                    imprimirDetalleRepuesto(repuesto);
                }
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void mostrarGuiaTipos() {
        List<RepuestoTipo> tipos = repuestoController.listarTipos();
        if (tipos.isEmpty()) {
            return;
        }
        StringBuilder guia = new StringBuilder("  Tipos disponibles: ");
        for (int i = 0; i < tipos.size(); i++) {
            if (i > 0) {
                guia.append(", ");
            }
            guia.append(tipos.get(i).getNombre());
        }
        System.out.println(guia);
    }

    private void generarReporteStockBajo() {
        if (inventarioController == null) {
            System.out.println("\n  [i] El reporte de stock bajo aún no está conectado.");
            ConsoleReader.pausar();
            return;
        }

        try {
            List<StockRepuesto> bajoMinimo = inventarioController.generarReporteStockBajo();
            if (bajoMinimo.isEmpty()) {
                System.out.println("\n  [i] No hay repuestos activos con stock bajo mínimo.");
            } else {
                System.out.println("\n  [Reporte de stock bajo: " + bajoMinimo.size() + " repuesto(s)]");
                for (StockRepuesto stock : bajoMinimo) {
                    Repuesto repuesto = stock.getRepuesto();
                    System.out.println("  - ID " + repuesto.getId()
                            + " | " + repuesto.getNombre()
                            + " | Físico: " + stock.getStockFisico()
                            + " | Operativo: " + stock.getStockOperativo()
                            + " | Reservado: " + stock.getReservado()
                            + " | Disponible: " + stock.getDisponible()
                            + " | Mínimo: " + stock.getStockMinimo()
                            + " | Proveedor habitual: " + stock.getProveedorHabitual());
                    if (stock.getDeficit() > 0) {
                        System.out.println("      [!] ALERTA: déficit de " + stock.getDeficit()
                                + " unidad(es) para cubrir reservas vigentes.");
                    }
                }
            }
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
            int idElegido = ConsoleReader.leerEnteroEnRango(
                    "Seleccione el ID del tipo de repuesto (0 para cancelar)", 0, Integer.MAX_VALUE);
            if (idElegido == 0) {
                return 0;
            }
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
