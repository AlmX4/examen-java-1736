package com.carmotors.sgt;

import com.carmotors.sgt.controller.ClienteController;
import com.carmotors.sgt.controller.ProveedorController;
import com.carmotors.sgt.controller.RepuestoController;
import com.carmotors.sgt.controller.ServicioTipoController;
import com.carmotors.sgt.controller.TecnicoController;
import com.carmotors.sgt.repository.ClienteRepository;
import com.carmotors.sgt.repository.EspecialidadRepository;
import com.carmotors.sgt.repository.MantenimientoTipoRepository;
import com.carmotors.sgt.repository.ProveedorDAO;
import com.carmotors.sgt.repository.RepuestoDAO;
import com.carmotors.sgt.repository.ServicioTipoRepository;
import com.carmotors.sgt.repository.TecnicoRepository;
import com.carmotors.sgt.repository.impl.ClienteRepositoryImpl;
import com.carmotors.sgt.repository.impl.EspecialidadRepositoryImpl;
import com.carmotors.sgt.repository.impl.MantenimientoTipoRepositoryImpl;
import com.carmotors.sgt.repository.impl.ProveedorDAOImpl;
import com.carmotors.sgt.repository.impl.RepuestoDAOImpl;
import com.carmotors.sgt.repository.impl.ServicioTipoRepositoryImpl;
import com.carmotors.sgt.repository.impl.TecnicoRepositoryImpl;
import com.carmotors.sgt.service.ClienteService;
import com.carmotors.sgt.service.ProveedorService;
import com.carmotors.sgt.service.RepuestoService;
import com.carmotors.sgt.service.ServicioTipoService;
import com.carmotors.sgt.service.TecnicoService;
import com.carmotors.sgt.view.ClienteView;
import com.carmotors.sgt.view.ConsolaMenuView;
import com.carmotors.sgt.view.ProveedorView;
import com.carmotors.sgt.view.RepuestoView;
import com.carmotors.sgt.view.ServicioTipoView;
import com.carmotors.sgt.view.TecnicoView;

/**
 * Punto de entrada de la aplicación CarMotors SGT.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("CarMotors - Sistema de Gestión de Taller");

        ConsolaMenuView menuPrincipal = new ConsolaMenuView();

        ClienteRepository clienteRepository = new ClienteRepositoryImpl();
        ClienteService clienteService = new ClienteService(clienteRepository);
        ClienteController clienteController = new ClienteController(clienteService);
        menuPrincipal.setSubmenuClientes(new ClienteView(clienteController));

        ProveedorDAO proveedorDAO = new ProveedorDAOImpl();
        ProveedorService proveedorService = new ProveedorService(proveedorDAO);
        ProveedorController proveedorController = new ProveedorController(proveedorService);
        menuPrincipal.setSubmenuProveedores(new ProveedorView(proveedorController));

        RepuestoDAO repuestoDAO = new RepuestoDAOImpl();
        RepuestoService repuestoService = new RepuestoService(repuestoDAO);
        RepuestoController repuestoController = new RepuestoController(repuestoService);
        menuPrincipal.setSubmenuInventario(new RepuestoView(repuestoController));

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
        // menuPrincipal.setSubmenuFacturacion(new FacturacionView(facturacionController));
        // menuPrincipal.setSubmenuReportes(new ReportesView(reportesController));

        menuPrincipal.mostrarMenuPrincipal();
    }
}
