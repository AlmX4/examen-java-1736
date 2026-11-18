package com.carmotors.sgt.view;

import java.util.List;

import com.carmotors.sgt.controller.ProveedorController;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.util.ConsoleReader;

public class ProveedorView implements SubmenuVista {

    private final ProveedorController proveedorController;
    private final EvaluacionProveedorView evaluacionProveedorView;
    private final CatalogoProveedorView catalogoProveedorView;

    public ProveedorView(ProveedorController proveedorController) {
        this(proveedorController, null);
    }

    public ProveedorView(ProveedorController proveedorController, EvaluacionProveedorView evaluacionProveedorView) {
        this(proveedorController, evaluacionProveedorView, null);
    }

    public ProveedorView(ProveedorController proveedorController, EvaluacionProveedorView evaluacionProveedorView,
                         CatalogoProveedorView catalogoProveedorView) {
        if (proveedorController == null) {
            throw new IllegalArgumentException("El controlador de proveedores es obligatorio.");
        }
        this.proveedorController = proveedorController;
        this.evaluacionProveedorView = evaluacionProveedorView;
        this.catalogoProveedorView = catalogoProveedorView;
    }

    @Override
    public String obtenerTitulo() {
        return "Proveedores";
    }

    @Override
    public void mostrarMenu() {
        boolean ejecutando = true;
        while (ejecutando) {
            System.out.println("\n==================================================");
            System.out.println("             GESTIÓN DE PROVEEDORES");
            System.out.println("==================================================");
            System.out.println("  1. Listar proveedores");
            System.out.println("  2. Buscar proveedor por id");
            System.out.println("  3. Crear proveedor");
            System.out.println("  4. Actualizar proveedor");
            System.out.println("  5. Desactivar proveedor");
            System.out.println("  6. Reactivar proveedor");
            System.out.println("  7. Evaluaciones de proveedores");
            System.out.println("  8. Catálogo de repuestos del proveedor");
            System.out.println("  9. Comparar proveedores de un repuesto");
            System.out.println("  0. Volver");
            System.out.println("==================================================");

            int opcion = ConsoleReader.leerEnteroEnRango("Seleccione una opción", 0, 9);
            switch (opcion) {
                case 1:
                    listarProveedores();
                    break;
                case 2:
                    buscarProveedor();
                    break;
                case 3:
                    crearProveedor();
                    break;
                case 4:
                    actualizarProveedor();
                    break;
                case 5:
                    desactivarProveedor();
                    break;
                case 6:
                    reactivarProveedor();
                    break;
                case 7:
                    if (evaluacionProveedorView != null) {
                        evaluacionProveedorView.mostrarMenu();
                    } else {
                        System.out.println("\n  [i] El módulo de evaluaciones aún no está conectado.");
                        ConsoleReader.pausar();
                    }
                    break;
                case 8:
                    if (catalogoProveedorView != null) {
                        catalogoProveedorView.gestionarCatalogoProveedor();
                    } else {
                        System.out.println("\n  [i] El catálogo proveedor-repuesto aún no está conectado.");
                        ConsoleReader.pausar();
                    }
                    break;
                case 9:
                    if (catalogoProveedorView != null) {
                        catalogoProveedorView.compararProveedoresDeRepuesto();
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

    private void listarProveedores() {
        try {
            List<Proveedor> proveedores = proveedorController.listarTodos();
            if (proveedores.isEmpty()) {
                System.out.println("\n  [i] No hay proveedores registrados.");
            } else {
                System.out.println("\n  [Lista de proveedores]");
                for (Proveedor proveedor : proveedores) {
                    imprimirDetalleProveedor(proveedor);
                }
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void buscarProveedor() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("Ingrese el id del proveedor");
            Proveedor proveedor = proveedorController.buscarPorId(id);
            System.out.println("\n  [Proveedor encontrado]");
            imprimirDetalleProveedor(proveedor);
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void crearProveedor() {
        try {
            Proveedor proveedor = new Proveedor();
            proveedor.setNombre(ConsoleReader.leerTextoObligatorio("Nombre del proveedor"));
            proveedor.setTelefono(ConsoleReader.leerTextoOpcional("Teléfono (opcional)"));
            proveedor.setEmail(ConsoleReader.leerEmailOpcional("Email del proveedor (opcional)"));
            proveedor.setDireccion(ConsoleReader.leerTextoOpcional("Dirección (opcional)"));
            proveedor.setActivo(true);

            Proveedor creado = proveedorController.crear(proveedor);
            System.out.println("\n  [✓] Proveedor creado correctamente con id " + creado.getId());
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void actualizarProveedor() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("Ingrese el id del proveedor a actualizar");
            Proveedor actual = proveedorController.buscarPorId(id);
            System.out.println("\n  Datos actuales:");
            imprimirDetalleProveedor(actual);

            Proveedor actualizado = new Proveedor();
            actualizado.setId(actual.getId());
            actualizado.setNombre(ConsoleReader.leerTextoObligatorio("Nuevo nombre"));
            actualizado.setTelefono(ConsoleReader.leerTextoOpcional("Nuevo teléfono (opcional)"));
            actualizado.setEmail(ConsoleReader.leerEmailOpcional("Nuevo email (opcional)"));
            actualizado.setDireccion(ConsoleReader.leerTextoOpcional("Nueva dirección (opcional)"));
            actualizado.setActivo(actual.isActivo());

            Proveedor guardado = proveedorController.actualizar(actualizado);
            System.out.println("\n  [✓] Proveedor actualizado correctamente.");
            imprimirDetalleProveedor(guardado);
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void desactivarProveedor() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("Ingrese el id del proveedor a desactivar");
            Proveedor actual = proveedorController.buscarPorId(id);
            if (!actual.isActivo()) {
                System.out.println("\n  [i] El proveedor ya se encuentra inactivo.");
                ConsoleReader.pausar();
                return;
            }

            if (!ConsoleReader.leerConfirmacion("¿Confirma desactivar al proveedor '" + actual.getNombre() + "'?")) {
                System.out.println("\n  [i] Operación cancelada.");
                ConsoleReader.pausar();
                return;
            }

            Proveedor proveedor = proveedorController.desactivar(id);
            System.out.println("\n  [✓] Proveedor desactivado correctamente.");
            imprimirDetalleProveedor(proveedor);
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void reactivarProveedor() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("Ingrese el id del proveedor a reactivar");
            Proveedor actual = proveedorController.buscarPorId(id);
            if (actual.isActivo()) {
                System.out.println("\n  [i] El proveedor ya se encuentra activo.");
                ConsoleReader.pausar();
                return;
            }

            Proveedor proveedor = proveedorController.reactivar(id);
            System.out.println("\n  [✓] Proveedor reactivado correctamente.");
            imprimirDetalleProveedor(proveedor);
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void imprimirDetalleProveedor(Proveedor proveedor) {
        String estado = proveedor.isActivo() ? "ACTIVO" : "INACTIVO";
        String email = (proveedor.getEmail() == null || proveedor.getEmail().isEmpty())
                ? "Sin email"
                : proveedor.getEmail();
        String telefono = (proveedor.getTelefono() == null || proveedor.getTelefono().isEmpty())
                ? "Sin teléfono"
                : proveedor.getTelefono();
        String direccion = (proveedor.getDireccion() == null || proveedor.getDireccion().isEmpty())
                ? "Sin dirección"
                : proveedor.getDireccion();
        System.out.println("  - ID " + proveedor.getId()
                + " | " + proveedor.getNombre()
                + " | Estado: " + estado
                + " | Email: " + email
                + " | Tel: " + telefono
                + " | Dir: " + direccion);
    }
}
