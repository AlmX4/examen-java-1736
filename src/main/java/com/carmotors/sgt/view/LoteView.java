package com.carmotors.sgt.view;

import com.carmotors.sgt.controller.LoteController;
import com.carmotors.sgt.controller.ProveedorController;
import com.carmotors.sgt.controller.RepuestoController;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.Lote;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.util.ConsoleReader;

public class LoteView implements SubmenuVista {

    private final LoteController loteController;
    private final ProveedorController proveedorController;
    private final RepuestoController repuestoController;

    public LoteView(LoteController loteController, ProveedorController proveedorController,
                    RepuestoController repuestoController) {
        if (loteController == null) {
            throw new IllegalArgumentException("El controlador de lotes es obligatorio.");
        }
        if (proveedorController == null) {
            throw new IllegalArgumentException("El controlador de proveedores es obligatorio.");
        }
        if (repuestoController == null) {
            throw new IllegalArgumentException("El controlador de repuestos es obligatorio.");
        }
        this.loteController = loteController;
        this.proveedorController = proveedorController;
        this.repuestoController = repuestoController;
    }

    @Override
    public String obtenerTitulo() {
        return "Ingreso de lotes";
    }

    @Override
    public void mostrarMenu() {
        boolean ejecutando = true;
        while (ejecutando) {
            System.out.println("\n==================================================");
            System.out.println("                  INGRESO DE LOTES");
            System.out.println("==================================================");
            System.out.println("  1. Registrar lote");
            System.out.println("  2. Consultar lote por código");
            System.out.println("  0. Volver");
            System.out.println("==================================================");

            int opcion = ConsoleReader.leerEnteroEnRango("Seleccione una opción", 0, 2);
            switch (opcion) {
                case 1:
                    registrarLote();
                    break;
                case 2:
                    consultarLote();
                    break;
                case 0:
                    ejecutando = false;
                    break;
                default:
                    break;
            }
        }
    }

    private void registrarLote() {
        try {
            int proveedorId = ConsoleReader.leerEnteroPositivo("Ingrese el id del proveedor");
            Proveedor proveedor = proveedorController.buscarPorId(proveedorId);
            if (!proveedor.isActivo()) {
                System.out.println("\n  [!] Error: El proveedor está inactivo y no puede registrar lotes.");
                ConsoleReader.pausar();
                return;
            }

            int repuestoId = ConsoleReader.leerEnteroPositivo("Ingrese el id del repuesto");
            Repuesto repuesto = repuestoController.buscarPorId(repuestoId);
            if (!repuesto.isActivo()) {
                System.out.println("\n  [!] Error: El repuesto está inactivo y no puede recibir lotes nuevos.");
                ConsoleReader.pausar();
                return;
            }

            Lote lote = new Lote();
            lote.setCodigoLote(ConsoleReader.leerTextoObligatorio("Código del lote"));
            lote.setProveedorId(proveedorId);
            lote.setRepuestoId(repuestoId);
            lote.setFechaIngreso(ConsoleReader.leerFecha("Fecha de ingreso (yyyy-MM-dd)"));
            lote.setFechaCaducidad(ConsoleReader.leerFechaOpcional("Fecha de caducidad (opcional, yyyy-MM-dd)"));
            lote.setCantidadInicial(ConsoleReader.leerEnteroPositivo("Cantidad inicial del lote"));

            Lote guardado = loteController.registrar(lote);
            System.out.println("\n  [✓] Lote registrado correctamente con id " + guardado.getId());
            System.out.println("  Código: " + guardado.getCodigoLote()
                    + " | Proveedor: " + proveedorId
                    + " | Repuesto: " + repuestoId
                    + " | Cantidad: " + guardado.getCantidadActual());
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void consultarLote() {
        try {
            String codigo = ConsoleReader.leerTextoObligatorio("Ingrese el código del lote");
            Lote lote = loteController.buscarPorCodigo(codigo);
            if (lote == null) {
                System.out.println("\n  [i] No existe un lote con ese código.");
            } else {
                System.out.println("\n  [Lote encontrado]");
                System.out.println("  - ID: " + lote.getId());
                System.out.println("  - Código: " + lote.getCodigoLote());
                System.out.println("  - Proveedor: " + lote.getProveedorId());
                System.out.println("  - Repuesto: " + lote.getRepuestoId());
                System.out.println("  - Fecha ingreso: " + lote.getFechaIngreso());
                System.out.println("  - Fecha caducidad: " + (lote.getFechaCaducidad() == null ? "Sin caducidad" : lote.getFechaCaducidad()));
                System.out.println("  - Cantidad inicial: " + lote.getCantidadInicial());
                System.out.println("  - Cantidad actual: " + lote.getCantidadActual());
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }
}
