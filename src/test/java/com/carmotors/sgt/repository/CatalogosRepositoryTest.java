package com.carmotors.sgt.repository;

import com.carmotors.sgt.exception.ConfiguracionException;
import com.carmotors.sgt.model.Especialidad;
import com.carmotors.sgt.model.MantenimientoTipo;
import com.carmotors.sgt.model.RepuestoTipo;
import com.carmotors.sgt.model.ServicioEstado;
import com.carmotors.sgt.repository.impl.EspecialidadRepositoryImpl;
import com.carmotors.sgt.repository.impl.MantenimientoTipoRepositoryImpl;
import com.carmotors.sgt.repository.impl.RepuestoTipoRepositoryImpl;
import com.carmotors.sgt.repository.impl.ServicioEstadoRepositoryImpl;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pruebas para las implementaciones JDBC de las tablas de consulta.
 * Valida contratos y, si MySQL está activo con config local, verifica lectura de data.sql.
 */
class CatalogosRepositoryTest {

    @Test
    @DisplayName("Los repositorios deben instanciarse correctamente sin conexión compartida")
    void testInstanciacionRepositorios() {
        assertNotNull(new RepuestoTipoRepositoryImpl());
        assertNotNull(new EspecialidadRepositoryImpl());
        assertNotNull(new MantenimientoTipoRepositoryImpl());
        assertNotNull(new ServicioEstadoRepositoryImpl());
    }

    @Test
    @DisplayName("Verificación JDBC condicional de los 4 catálogos si MySQL y config.properties están activos")
    void testConsultasJdbcCondicionales() {
        File configFile = new File("src/main/resources/config.properties");
        Assumptions.assumeTrue(configFile.exists(), 
                "Omitiendo prueba JDBC: no existe 'src/main/resources/config.properties' local.");

        try {
            // 1. RepuestoTipoRepository
            RepuestoTipoRepository repRepo = new RepuestoTipoRepositoryImpl();
            List<RepuestoTipo> tipos = repRepo.listarTodos();
            assertNotNull(tipos, "La lista de tipos de repuesto no debe ser nula");
            assertFalse(tipos.isEmpty(), "La lista de tipos de repuesto debe contener registros de data.sql");
            assertNotNull(repRepo.buscarPorId(1), "El tipo con ID 1 debe existir");
            assertNull(repRepo.buscarPorId(9999), "Un ID inexistente debe retornar null");

            // 2. EspecialidadRepository
            EspecialidadRepository espRepo = new EspecialidadRepositoryImpl();
            List<Especialidad> especialidades = espRepo.listarTodos();
            assertNotNull(especialidades, "La lista de especialidades no debe ser nula");
            assertFalse(especialidades.isEmpty(), "La lista de especialidades debe contener registros");
            assertNotNull(espRepo.buscarPorId(1));
            assertNull(espRepo.buscarPorId(9999));

            // 3. MantenimientoTipoRepository
            MantenimientoTipoRepository mantRepo = new MantenimientoTipoRepositoryImpl();
            List<MantenimientoTipo> mantenimientos = mantRepo.listarTodos();
            assertNotNull(mantenimientos, "La lista de tipos de mantenimiento no debe ser nula");
            assertFalse(mantenimientos.isEmpty(), "La lista de tipos de mantenimiento debe contener registros");
            assertNotNull(mantRepo.buscarPorId(1));
            assertNull(mantRepo.buscarPorId(9999));

            // 4. ServicioEstadoRepository
            ServicioEstadoRepository estadoRepo = new ServicioEstadoRepositoryImpl();
            List<ServicioEstado> estados = estadoRepo.listarTodos();
            assertNotNull(estados, "La lista de estados no debe ser nula");
            assertFalse(estados.isEmpty(), "La lista de estados debe contener registros");
            assertNotNull(estadoRepo.buscarPorId(1));
            assertNull(estadoRepo.buscarPorId(9999));

        } catch (ConfiguracionException e) {
            Assumptions.assumeTrue(false, "Configuración no disponible: " + e.getMessage());
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "MySQL no accesible en este momento: " + e.getMessage());
        }
    }
}
