package com.carmotors.sgt;

import com.carmotors.sgt.controller.ProveedorController;
import com.carmotors.sgt.repository.ProveedorDAO;
import com.carmotors.sgt.repository.impl.ProveedorDAOImpl;
import com.carmotors.sgt.service.ProveedorService;
import com.carmotors.sgt.view.ConsolaMenuView;
import com.carmotors.sgt.view.ProveedorView;

/**
 * Punto de entrada de la aplicación CarMotors SGT.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("CarMotors - Sistema de Gestión de Taller");

        ConsolaMenuView menuPrincipal = new ConsolaMenuView();

        ProveedorDAO proveedorDAO = new ProveedorDAOImpl();
        ProveedorService proveedorService = new ProveedorService(proveedorDAO);
        ProveedorController proveedorController = new ProveedorController(proveedorService);
        menuPrincipal.setSubmenuProveedores(new ProveedorView(proveedorController));

        // Conexión de submenús por módulo (implementar SubmenuVista en com.carmotors.sgt.view):
        // menuPrincipal.setSubmenuClientes(new ClienteView(clienteController));
        // menuPrincipal.setSubmenuInventario(new InventarioView(inventarioController));
        // menuPrincipal.setSubmenuTecnicos(new TecnicoView(tecnicoController));
        // menuPrincipal.setSubmenuServicios(new OrdenServicioView(ordenServicioController));
        // menuPrincipal.setSubmenuFacturacion(new FacturacionView(facturacionController));
        // menuPrincipal.setSubmenuReportes(new ReportesView(reportesController));

        menuPrincipal.mostrarMenuPrincipal();
    }
}
