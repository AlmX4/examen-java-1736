package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Especialidad;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.repository.EspecialidadRepository;
import com.carmotors.sgt.repository.TecnicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias en memoria para {@link TecnicoService} (RF-33, RN-17).
 */
class TecnicoServiceTest {

    private TecnicoRepositoryEnMemoria tecnicoRepository;
    private EspecialidadRepositoryEnMemoria especialidadRepository;
    private TecnicoService tecnicoService;

    @BeforeEach
    void configurar() {
        tecnicoRepository = new TecnicoRepositoryEnMemoria();
        especialidadRepository = new EspecialidadRepositoryEnMemoria();
        especialidadRepository.agregar(new Especialidad(1, "Mecánica general"));
        especialidadRepository.agregar(new Especialidad(3, "Frenos"));
        tecnicoService = new TecnicoService(tecnicoRepository, especialidadRepository);
    }

    @Test
    @DisplayName("Debe registrar un técnico válido con especialidad existente y activo en true")
    void registrarTecnicoValido() {
        Tecnico nuevo = new Tecnico(0, "  Pedro Ramírez  ", " 1090456123 ", 3, " 3205550001 ", true);
        Tecnico guardado = tecnicoService.registrarTecnico(nuevo);

        assertEquals(1, guardado.getId());
        assertEquals("Pedro Ramírez", guardado.getNombre());
        assertEquals("1090456123", guardado.getDocumentoIdentidad());
        assertEquals(3, guardado.getEspecialidadId());
        assertEquals("3205550001", guardado.getTelefono());
        assertTrue(guardado.isActivo());
    }

    @Test
    @DisplayName("Debe rechazar documento duplicado con DatoDuplicadoException")
    void rechazarDocumentoDuplicado() {
        tecnicoService.registrarTecnico(new Tecnico(0, "Pedro Ramírez", "1090456123", 3, null, true));

        Tecnico repetido = new Tecnico(0, "Otro Técnico", "1090456123", 1, "3001112233", true);
        assertThrows(DatoDuplicadoException.class, () -> tecnicoService.registrarTecnico(repetido));
    }

    @Test
    @DisplayName("Debe rechazar especialidad inexistente con EntidadNoEncontradaException")
    void rechazarEspecialidadInexistente() {
        Tecnico invalido = new Tecnico(0, "Juan Pérez", "1090456124", 99, null, true);
        assertThrows(EntidadNoEncontradaException.class, () -> tecnicoService.registrarTecnico(invalido));
    }

    @Test
    @DisplayName("Debe rechazar nombre o documento vacío con ValidacionException")
    void rechazarCamposObligatoriosVacios() {
        Tecnico sinNombre = new Tecnico(0, "   ", "1090456124", 1, null, true);
        assertThrows(ValidacionException.class, () -> tecnicoService.registrarTecnico(sinNombre));

        Tecnico sinDocumento = new Tecnico(0, "Juan Pérez", " ", 1, null, true);
        assertThrows(ValidacionException.class, () -> tecnicoService.registrarTecnico(sinDocumento));
    }

    @Test
    @DisplayName("Debe actualizar datos de un técnico existente")
    void actualizarTecnicoExistente() {
        Tecnico creado = tecnicoService.registrarTecnico(new Tecnico(0, "Laura Gómez", "1090456125", 1, null, true));
        creado.setNombre("Laura Gómez Updated");
        creado.setEspecialidadId(3);
        creado.setTelefono("3109998877");

        Tecnico actualizado = tecnicoService.actualizarTecnico(creado);
        assertEquals("Laura Gómez Updated", actualizado.getNombre());
        assertEquals(3, actualizado.getEspecialidadId());
        assertEquals("3109998877", actualizado.getTelefono());
    }

    @Test
    @DisplayName("Debe desactivar lógicamente sin eliminar el registro ni borrarlo de listarTodos")
    void desactivarYReactivarTecnicoLogicamente() {
        Tecnico creado = tecnicoService.registrarTecnico(new Tecnico(0, "Luis Ortega", "1090456128", 1, null, true));

        Tecnico desactivado = tecnicoService.desactivarTecnico(creado.getId());
        assertFalse(desactivado.isActivo());
        assertEquals(0, tecnicoService.listarActivos().size());
        assertEquals(1, tecnicoService.listarTodos().size());

        Tecnico reactivado = tecnicoService.reactivarTecnico(creado.getId());
        assertTrue(reactivado.isActivo());
        assertEquals(1, tecnicoService.listarActivos().size());
    }

    @Test
    @DisplayName("Debe lanzar EntidadNoEncontradaException al buscar un ID inexistente")
    void buscarIdInexistenteLanzaExcepcion() {
        assertThrows(EntidadNoEncontradaException.class, () -> tecnicoService.buscarPorId(999));
    }

    private static class EspecialidadRepositoryEnMemoria implements EspecialidadRepository {

        private final List<Especialidad> datos = new ArrayList<>();

        void agregar(Especialidad especialidad) {
            datos.add(especialidad);
        }

        @Override
        public List<Especialidad> listarTodos() {
            return new ArrayList<>(datos);
        }

        @Override
        public Especialidad buscarPorId(int id) {
            for (Especialidad esp : datos) {
                if (esp.getId() == id) {
                    return esp;
                }
            }
            return null;
        }
    }

    private static class TecnicoRepositoryEnMemoria implements TecnicoRepository {

        private final List<Tecnico> datos = new ArrayList<>();
        private int secuenciaId = 1;

        @Override
        public List<Tecnico> listarTodos() {
            return new ArrayList<>(datos);
        }

        @Override
        public List<Tecnico> listarActivos() {
            List<Tecnico> activos = new ArrayList<>();
            for (Tecnico tecnico : datos) {
                if (tecnico.isActivo()) {
                    activos.add(tecnico);
                }
            }
            return activos;
        }

        @Override
        public Tecnico buscarPorId(int id) {
            for (Tecnico tecnico : datos) {
                if (tecnico.getId() == id) {
                    return tecnico;
                }
            }
            return null;
        }

        @Override
        public Tecnico buscarPorDocumento(String documento) {
            for (Tecnico tecnico : datos) {
                if (tecnico.getDocumentoIdentidad().equals(documento)) {
                    return tecnico;
                }
            }
            return null;
        }

        @Override
        public int guardar(Tecnico tecnico) {
            int nuevoId = secuenciaId++;
            Tecnico copia = new Tecnico(
                    nuevoId,
                    tecnico.getNombre(),
                    tecnico.getDocumentoIdentidad(),
                    tecnico.getEspecialidadId(),
                    tecnico.getTelefono(),
                    tecnico.isActivo()
            );
            datos.add(copia);
            return nuevoId;
        }

        @Override
        public boolean actualizar(Tecnico tecnico) {
            for (int i = 0; i < datos.size(); i++) {
                if (datos.get(i).getId() == tecnico.getId()) {
                    datos.set(i, new Tecnico(
                            tecnico.getId(),
                            tecnico.getNombre(),
                            tecnico.getDocumentoIdentidad(),
                            tecnico.getEspecialidadId(),
                            tecnico.getTelefono(),
                            tecnico.isActivo()
                    ));
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean actualizarEstadoActivo(int id, boolean activo) {
            for (Tecnico tecnico : datos) {
                if (tecnico.getId() == id) {
                    tecnico.setActivo(activo);
                    return true;
                }
            }
            return false;
        }
    }
}
