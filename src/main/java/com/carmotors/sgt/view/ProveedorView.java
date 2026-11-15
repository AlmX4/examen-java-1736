package com.carmotors.sgt.view;

import java.util.List;

import com.carmotors.sgt.controller.ProveedorController;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.util.ConsoleReader;

public class ProveedorView implements SubmenuVista {

    private final ProveedorController proveedorController;

    public ProveedorView(ProveedorController proveedorController) {
        this.proveedorController = proveedorController;
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
            System.out.println("  0. Volver");
            System.out.println("==================================================");

            int opcion = ConsoleReader.leerEnteroEnRango("Seleccione una opción", 0, 6);
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
            List<Proveedor> proveedores = proveedorController.listarActivos();
            if (proveedores.isEmpty()) {
                System.out.println("\n  [i] No hay proveedores activos registrados.");
                ConsoleReader.pausar();
                return;
            }
            System.out.println("\n  [Lista de proveedores activos]");
            for (Proveedor proveedor : proveedores) {
                System.out.println("  - " + proveedor.getId() + " | " + proveedor.getNombre() + " | " + proveedor.getEmail() + " | " + proveedor.getTelefono());
            }
        } catch (Exception e) {
            System.out.println("\n  [!] " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void buscarProveedor() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("Ingrese el id del proveedor");
            Proveedor proveedor = proveedorController.buscarPorId(id);
            if (proveedor == null) {
                System.out.println("\n  [i] No se encontró un proveedor con ese id.");
            } else {
                System.out.println("\n  [Proveedor encontrado]");
                System.out.println(proveedor);
            }
        } catch (Exception e) {
            System.out.println("\n  [!] " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void crearProveedor() {
        try {
            Proveedor proveedor = new Proveedor();
            proveedor.setNombre(ConsoleReader.leerTextoObligatorio("Nombre del proveedor"));
            proveedor.setTelefono(ConsoleReader.leerTextoOpcional("Teléfono (opcional)"));
            proveedor.setEmail(ConsoleReader.leerEmail("Email del proveedor"));
            proveedor.setDireccion(ConsoleReader.leerTextoOpcional("Dirección (opcional)"));
            proveedor.setActivo(true);

            Proveedor creado = proveedorController.crear(proveedor);
            System.out.println("\n  [✓] Proveedor creado correctamente con id " + creado.getId());
        } catch (Exception e) {
            System.out.println("\n  [!] " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void actualizarProveedor() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("Ingrese el id del proveedor a actualizar");
            Proveedor actual = proveedorController.buscarPorId(id);
            if (actual == null) {
                System.out.println("\n  [i] No existe un proveedor con ese id.");
                ConsoleReader.pausar();
                return;
            }

            System.out.println("\n  Datos actuales: " + actual);
            Proveedor actualizado = new Proveedor();
            actualizado.setId(actual.getId());
            actualizado.setNombre(ConsoleReader.leerTextoObligatorio("Nuevo nombre"));
            actualizado.setTelefono(ConsoleReader.leerTextoOpcional("Nuevo teléfono (opcional)"));
            actualizado.setEmail(ConsoleReader.leerEmail("Nuevo email"));
            actualizado.setDireccion(ConsoleReader.leerTextoOpcional("Nueva dirección (opcional)"));
            actualizado.setActivo(actual.isActivo());

            Proveedor guardado = proveedorController.actualizar(actualizado);
            System.out.println("\n  [✓] Proveedor actualizado correctamente.");
            System.out.println(guardado);
        } catch (Exception e) {
            System.out.println("\n  [!] " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void desactivarProveedor() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("Ingrese el id del proveedor a desactivar");
            Proveedor proveedor = proveedorController.desactivar(id);
            System.out.println("\n  [✓] Proveedor desactivado correctamente.");
            System.out.println(proveedor);
        } catch (Exception e) {
            System.out.println("\n  [!] " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void reactivarProveedor() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("Ingrese el id del proveedor a reactivar");
            Proveedor proveedor = proveedorController.reactivar(id);
            System.out.println("\n  [✓] Proveedor reactivado correctamente.");
            System.out.println(proveedor);
        } catch (Exception e) {
            System.out.println("\n  [!] " + e.getMessage());
        }
        ConsoleReader.pausar();
    }
}
