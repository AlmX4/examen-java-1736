package com.carmotors.sgt.view;

import com.carmotors.sgt.controller.FacturaController;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.ConsultaFactura;
import com.carmotors.sgt.model.Factura;
import com.carmotors.sgt.model.FacturaDetalle;
import com.carmotors.sgt.model.FacturaSnapshot;
import com.carmotors.sgt.model.TipoConcepto;
import com.carmotors.sgt.util.ConsoleReader;

import java.time.format.DateTimeFormatter;

/**
 * Submenú de consola de facturación (RF-22, RF-23, RNF-03, RNF-24).
 * Solo captura datos e imprime resultados; las reglas y los cálculos residen en el servicio.
 */
public class FacturacionView implements SubmenuVista {

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FacturaController facturaController;

    public FacturacionView(FacturaController facturaController) {
        if (facturaController == null) {
            throw new IllegalArgumentException("El controlador de facturación es obligatorio.");
        }
        this.facturaController = facturaController;
    }

    @Override
    public String obtenerTitulo() {
        return "Facturación";
    }

    @Override
    public void mostrarMenu() {
        boolean ejecutando = true;
        while (ejecutando) {
            System.out.println("\n==================================================");
            System.out.println("                  FACTURACIÓN                     ");
            System.out.println("==================================================");
            System.out.println("  1. Generar factura de una orden");
            System.out.println("  2. Reintentar generación del TXT");
            System.out.println("  0. Volver");
            System.out.println("==================================================");

            int opcion = ConsoleReader.leerEnteroEnRango("Seleccione una opción", 0, 2);
            switch (opcion) {
                case 1:
                    generarFactura();
                    break;
                case 2:
                    reintentarArchivo();
                    break;
                case 0:
                    ejecutando = false;
                    break;
                default:
                    break;
            }
        }
    }

    private void generarFactura() {
        try {
            int ordenId = ConsoleReader.leerEnteroPositivo("ID de la orden de servicio");
            boolean confirmar = ConsoleReader.leerConfirmacion("Facturar la orden " + ordenId
                    + " la cambia a Facturado y no se puede deshacer. ¿Desea continuar?");
            if (!confirmar) {
                System.out.println("\n  [i] Operación cancelada; no se generó ninguna factura.");
                return;
            }

            ConsultaFactura generada = facturaController.generarFactura(ordenId);
            Factura factura = generada.getFactura();
            System.out.println("\n  [✓] Factura registrada en BD (ID " + factura.getId() + ").");
            if (factura.isArchivoGenerado()) {
                System.out.println("  TXT generado: factura_" + factura.getNumeroFactura() + ".txt");
            } else {
                System.out.println("  [!] El TXT quedó pendiente. Use la opción 2 con este ID para reintentar.");
            }
            imprimirFactura(generada);
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] No se pudo generar la factura: " + e.getMessage());
        } finally {
            ConsoleReader.pausar();
        }
    }

    private void reintentarArchivo() {
        try {
            int facturaId = ConsoleReader.leerEnteroPositivo("ID de la factura");
            System.out.println("\n  [✓] TXT generado: " + facturaController.reintentarArchivo(facturaId));
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] No se pudo generar el TXT: " + e.getMessage());
        } finally {
            ConsoleReader.pausar();
        }
    }

    private void imprimirFactura(ConsultaFactura consulta) {
        Factura factura = consulta.getFactura();
        FacturaSnapshot snapshot = consulta.getSnapshot();

        System.out.println("\n==================================================");
        System.out.println("  FACTURA " + factura.getNumeroFactura());
        System.out.println("==================================================");
        System.out.println("  Fecha:     " + factura.getFechaEmision().format(FORMATO_FECHA_HORA));
        System.out.println("  Orden:     " + factura.getOrdenId());
        System.out.println("  Cliente:   " + snapshot.getClienteNombre() + " | Documento: " + snapshot.getClienteDocumento());
        System.out.println("             Teléfono: " + textoOpcional(snapshot.getClienteTelefono())
                + " | Email: " + textoOpcional(snapshot.getClienteEmail()));
        System.out.println("  Vehículo:  " + snapshot.getVehiculoPlaca() + " | " + snapshot.getVehiculoMarca()
                + " " + snapshot.getVehiculoModelo());
        System.out.println("--------------------------------------------------");
        System.out.println("  Conceptos:");
        for (FacturaDetalle concepto : consulta.getDetalles()) {
            String tipo = concepto.getTipoConcepto() == TipoConcepto.MANO_OBRA ? "Mano de obra" : "Repuesto";
            System.out.println("    - [" + tipo + "] " + concepto.getDescripcionSnapshot());
            System.out.println("        " + concepto.getCantidad() + " x $" + concepto.getPrecioUnitario()
                    + " = $" + concepto.getSubtotal());
        }
        System.out.println("--------------------------------------------------");
        System.out.println("  Total repuestos:     $" + factura.getTotalRepuestos());
        System.out.println("  Total mano de obra:  $" + factura.getTotalManoObra());
        System.out.println("  Subtotal:            $" + factura.getSubtotal());
        System.out.println("  IVA:                 $" + factura.getImpuestos());
        System.out.println("  TOTAL:               $" + factura.getTotalFinal());
        System.out.println("==================================================");
    }

    private String textoOpcional(String valor) {
        return valor == null || valor.trim().isEmpty() ? "N/A" : valor;
    }
}
