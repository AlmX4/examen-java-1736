package com.carmotors.sgt.view;

import java.math.BigDecimal;
import java.util.List;

import com.carmotors.sgt.controller.ProveedorController;
import com.carmotors.sgt.controller.ProveedorRepuestoController;
import com.carmotors.sgt.controller.RepuestoController;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.ComparacionProveedor;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.util.ConsoleReader;

/**
 * Vista de consola del catálogo proveedor-repuesto (RF-29). Se usa desde el submenú
 * de proveedores (gestión del catálogo) y desde el de inventario (proveedores de un repuesto).
 */
public class CatalogoProveedorView {

    private static final String FORMATO_REPUESTOS = "  %-6s %-34s %-14s %-12s %-8s%n";
    private static final String FORMATO_PROVEEDORES = "  %-6s %-26s %-9s %-14s %-12s %-8s%n";
    private static final String FORMATO_COMPARACION = "  %-6s %-24s %-12s %-10s %-18s %-8s%n";
    private static final String SEPARADOR = "  ------------------------------------------------------------------------------";
    private static final String SEPARADOR_COMPARACION = "  -----------------------------------------------------------------------------------------";

    private final ProveedorRepuestoController proveedorRepuestoController;
    private final ProveedorController proveedorController;
    private final RepuestoController repuestoController;

    public CatalogoProveedorView(ProveedorRepuestoController proveedorRepuestoController,
                                 ProveedorController proveedorController,
                                 RepuestoController repuestoController) {
        if (proveedorRepuestoController == null) {
            throw new IllegalArgumentException("El controlador del catálogo proveedor-repuesto es obligatorio.");
        }
        if (proveedorController == null) {
            throw new IllegalArgumentException("El controlador de proveedores es obligatorio.");
        }
        if (repuestoController == null) {
            throw new IllegalArgumentException("El controlador de repuestos es obligatorio.");
        }
        this.proveedorRepuestoController = proveedorRepuestoController;
        this.proveedorController = proveedorController;
        this.repuestoController = repuestoController;
    }

    /**
     * Submenú del catálogo de un proveedor: listar, asociar, actualizar, quitar y marcar habitual.
     */
    public void gestionarCatalogoProveedor() {
        Proveedor proveedor;
        try {
            int proveedorId = ConsoleReader.leerEnteroPositivo("Ingrese el id del proveedor");
            proveedor = proveedorController.buscarPorId(proveedorId);
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
            ConsoleReader.pausar();
            return;
        }

        boolean ejecutando = true;
        while (ejecutando) {
            System.out.println("\n==================================================");
            System.out.println("  CATÁLOGO DE REPUESTOS: " + proveedor.getNombre());
            System.out.println("==================================================");
            System.out.println("  1. Listar repuestos ofrecidos");
            System.out.println("  2. Asociar repuesto");
            System.out.println("  3. Actualizar costo y tiempo de entrega");
            System.out.println("  4. Quitar asociación");
            System.out.println("  5. Marcar como proveedor habitual");
            System.out.println("  0. Volver");
            System.out.println("==================================================");

            int opcion = ConsoleReader.leerEnteroEnRango("Seleccione una opción", 0, 5);
            switch (opcion) {
                case 1:
                    listarRepuestosDelProveedor(proveedor);
                    break;
                case 2:
                    asociarRepuesto(proveedor);
                    break;
                case 3:
                    actualizarCondiciones(proveedor);
                    break;
                case 4:
                    quitarAsociacion(proveedor);
                    break;
                case 5:
                    marcarHabitual(proveedor);
                    break;
                case 0:
                    ejecutando = false;
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Muestra los proveedores que ofrecen un repuesto, con costo, tiempo de entrega y habitual.
     */
    public void verProveedoresDeRepuesto() {
        try {
            int repuestoId = ConsoleReader.leerEnteroPositivo("Ingrese el id del repuesto");
            Repuesto repuesto = repuestoController.buscarPorId(repuestoId);
            List<ProveedorRepuesto> catalogo = proveedorRepuestoController.listarPorRepuesto(repuestoId);
            if (catalogo.isEmpty()) {
                System.out.println("\n  [i] Ningún proveedor ofrece el repuesto '" + repuesto.getNombre() + "'.");
            } else {
                System.out.println("\n  [Proveedores de " + repuesto.getNombre() + "]");
                System.out.printf(FORMATO_PROVEEDORES, "ID", "Proveedor", "Estado", "Costo", "Entrega", "Habitual");
                System.out.println(SEPARADOR);
                for (ProveedorRepuesto item : catalogo) {
                    Proveedor proveedor = proveedorController.buscarPorId(item.getProveedorId());
                    System.out.printf(FORMATO_PROVEEDORES,
                            proveedor.getId(),
                            proveedor.getNombre(),
                            proveedor.isActivo() ? "ACTIVO" : "INACTIVO",
                            "$" + item.getCosto(),
                            item.getTiempoEntregaDias() + " días",
                            item.isEsHabitual() ? "Sí" : "-");
                }
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    /**
     * Compara los proveedores activos que ofrecen un repuesto ordenados por costo (RF-30, RN-16, RNF-11).
     */
    public void compararProveedoresDeRepuesto() {
        try {
            int repuestoId = ConsoleReader.leerEnteroPositivo("Ingrese el id del repuesto a comparar");
            Repuesto repuesto = repuestoController.buscarPorId(repuestoId);
            List<ComparacionProveedor> comparacion = proveedorRepuestoController.compararProveedoresPorRepuesto(repuestoId);
            if (comparacion.isEmpty()) {
                System.out.println("\n  [i] Ningún proveedor activo ofrece el repuesto '" + repuesto.getNombre() + "'.");
            } else {
                System.out.println("\n  [Comparación de proveedores activos para: " + repuesto.getNombre()
                        + " (" + repuesto.getMarca() + ")]");
                System.out.printf(FORMATO_COMPARACION, "ID", "Proveedor", "Costo", "Entrega", "Calificación", "Habitual");
                System.out.println(SEPARADOR_COMPARACION);
                for (ComparacionProveedor item : comparacion) {
                    String calificacion = (item.getPromedioEvaluacion() != null)
                            ? item.getPromedioEvaluacion() + " (" + item.getCantidadEvaluaciones() + " eval)"
                            : "Sin evaluar";
                    System.out.printf(FORMATO_COMPARACION,
                            item.getProveedorId(),
                            item.getNombreProveedor(),
                            "$" + item.getCosto(),
                            item.getTiempoEntregaDias() + " días",
                            calificacion,
                            item.isEsHabitual() ? "Sí" : "-");
                }
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void listarRepuestosDelProveedor(Proveedor proveedor) {
        try {
            List<ProveedorRepuesto> catalogo = proveedorRepuestoController.listarPorProveedor(proveedor.getId());
            if (catalogo.isEmpty()) {
                System.out.println("\n  [i] El proveedor '" + proveedor.getNombre() + "' aún no ofrece repuestos.");
            } else {
                System.out.println("\n  [Repuestos ofrecidos por " + proveedor.getNombre() + "]");
                System.out.printf(FORMATO_REPUESTOS, "ID", "Repuesto", "Costo", "Entrega", "Habitual");
                System.out.println(SEPARADOR);
                for (ProveedorRepuesto item : catalogo) {
                    Repuesto repuesto = repuestoController.buscarPorId(item.getRepuestoId());
                    System.out.printf(FORMATO_REPUESTOS,
                            repuesto.getId(),
                            repuesto.getNombre(),
                            "$" + item.getCosto(),
                            item.getTiempoEntregaDias() + " días",
                            item.isEsHabitual() ? "Sí" : "-");
                }
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void asociarRepuesto(Proveedor proveedor) {
        try {
            int repuestoId = ConsoleReader.leerEnteroPositivo("Ingrese el id del repuesto a asociar");
            Repuesto repuesto = repuestoController.buscarPorId(repuestoId);
            System.out.println("  Repuesto: " + repuesto.getNombre() + " (" + repuesto.getMarca() + ")");

            ProveedorRepuesto asociacion = new ProveedorRepuesto();
            asociacion.setProveedorId(proveedor.getId());
            asociacion.setRepuestoId(repuestoId);
            asociacion.setCosto(ConsoleReader.leerBigDecimalPositivo("Costo de compra"));
            asociacion.setTiempoEntregaDias(ConsoleReader.leerEnteroNoNegativo("Tiempo de entrega (días)"));

            ProveedorRepuesto creada = proveedorRepuestoController.asociar(asociacion);
            System.out.println("\n  [✓] Repuesto '" + repuesto.getNombre() + "' asociado con costo $"
                    + creada.getCosto() + " y entrega en " + creada.getTiempoEntregaDias() + " días.");

            if (ConsoleReader.leerConfirmacion("¿Marcar a este proveedor como habitual del repuesto?")) {
                proveedorRepuestoController.marcarHabitual(proveedor.getId(), repuestoId);
                System.out.println("  [✓] Proveedor marcado como habitual. Si había otro habitual, quedó desmarcado.");
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void actualizarCondiciones(Proveedor proveedor) {
        try {
            int repuestoId = ConsoleReader.leerEnteroPositivo("Ingrese el id del repuesto");
            BigDecimal costo = ConsoleReader.leerBigDecimalPositivo("Nuevo costo de compra");
            int tiempoEntrega = ConsoleReader.leerEnteroNoNegativo("Nuevo tiempo de entrega (días)");

            ProveedorRepuesto actualizada = proveedorRepuestoController.actualizarCondiciones(
                    proveedor.getId(), repuestoId, costo, tiempoEntrega);
            System.out.println("\n  [✓] Condiciones actualizadas: costo $" + actualizada.getCosto()
                    + ", entrega en " + actualizada.getTiempoEntregaDias() + " días.");
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void quitarAsociacion(Proveedor proveedor) {
        try {
            int repuestoId = ConsoleReader.leerEnteroPositivo("Ingrese el id del repuesto a quitar");
            Repuesto repuesto = repuestoController.buscarPorId(repuestoId);
            if (!ConsoleReader.leerConfirmacion("¿Confirma quitar '" + repuesto.getNombre()
                    + "' del catálogo de '" + proveedor.getNombre() + "'?")) {
                System.out.println("\n  [i] Operación cancelada.");
                ConsoleReader.pausar();
                return;
            }
            proveedorRepuestoController.quitarAsociacion(proveedor.getId(), repuestoId);
            System.out.println("\n  [✓] Asociación eliminada del catálogo.");
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void marcarHabitual(Proveedor proveedor) {
        try {
            int repuestoId = ConsoleReader.leerEnteroPositivo("Ingrese el id del repuesto");
            proveedorRepuestoController.marcarHabitual(proveedor.getId(), repuestoId);
            System.out.println("\n  [✓] '" + proveedor.getNombre() + "' es ahora el proveedor habitual del repuesto "
                    + repuestoId + ". Si había otro habitual, quedó desmarcado.");
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }
}
