package com.carmotors.sgt;

import com.carmotors.sgt.controller.ServicioTipoController;
import com.carmotors.sgt.controller.TecnicoController;
import com.carmotors.sgt.repository.EspecialidadRepository;
import com.carmotors.sgt.repository.MantenimientoTipoRepository;
import com.carmotors.sgt.repository.ServicioTipoRepository;
import com.carmotors.sgt.repository.TecnicoRepository;
import com.carmotors.sgt.repository.impl.EspecialidadRepositoryImpl;
import com.carmotors.sgt.repository.impl.MantenimientoTipoRepositoryImpl;
import com.carmotors.sgt.repository.impl.ServicioTipoRepositoryImpl;
import com.carmotors.sgt.repository.impl.TecnicoRepositoryImpl;
import com.carmotors.sgt.service.ServicioTipoService;
import com.carmotors.sgt.service.TecnicoService;
import com.carmotors.sgt.view.ConsolaMenuView;
import com.carmotors.sgt.view.ServicioTipoView;
import com.carmotors.sgt.view.TecnicoView;

/**
 * Punto de entrada de la aplicación CarMotors SGT.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("CarMotors - Sistema de Gestión de Taller");

        ConsolaMenuView menuPrincipal = new ConsolaMenuView();

        EspecialidadRepository especialidadRepository = new EspecialidadRepositoryImpl();
        MantenimientoTipoRepository mantenimientoTipoRepository = new MantenimientoTipoRepositoryImpl();

        TecnicoRepository tecnicoRepository = new TecnicoRepositoryImpl();
        TecnicoService tecnicoService = new TecnicoService(tecnicoRepository, especialidadRepository);
        TecnicoController tecnicoController = new TecnicoController(tecnicoService);
        menuPrincipal.setSubmenuTecnicos(new TecnicoView(tecnicoController));

        ServicioTipoRepository servicioTipoRepository = new ServicioTipoRepositoryImpl();
        ServicioTipoService servicioTipoService = new ServicioTipoService(
                servicioTipoRepository, mantenimientoTipoRepository, especialidadRepository);
        ServicioTipoController servicioTipoController = new ServicioTipoController(servicioTipoService);
        menuPrincipal.setSubmenuServicios(new ServicioTipoView(servicioTipoController));

        // Conexión de submenús por módulo (implementar SubmenuVista en com.carmotors.sgt.view):
        // menuPrincipal.setSubmenuClientes(new ClienteView(clienteController));
        // menuPrincipal.setSubmenuProveedores(new ProveedorView(proveedorController));
        // menuPrincipal.setSubmenuInventario(new InventarioView(inventarioController));
        // menuPrincipal.setSubmenuFacturacion(new FacturacionView(facturacionController));
        // menuPrincipal.setSubmenuReportes(new ReportesView(reportesController));

        menuPrincipal.mostrarMenuPrincipal();
    }
}
