package com.carmotors.sgt.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.EvaluacionProveedor;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.repository.ProveedorDAO;
import com.carmotors.sgt.repository.EvaluacionProveedorDAO;

class EvaluacionProveedorServiceTest {

    private EvaluacionProveedorService service;
    private ProveedorDAOMock proveedorDAO;
    private EvaluacionProveedorDAOMock evaluacionDAO;

    @BeforeEach
    void setUp() {
        proveedorDAO = new ProveedorDAOMock();
        proveedorDAO.agregar(new Proveedor(1, "AutoPartes del Norte", "5775550101", "ventas@autopartesnorte.com", "Calle 1", true));
        proveedorDAO.agregar(new Proveedor(2, "Frenos y Más", "5775550102", "pedidos@frenosymas.com", "Calle 2", false));
        evaluacionDAO = new EvaluacionProveedorDAOMock();
        service = new EvaluacionProveedorService(evaluacionDAO, proveedorDAO);
    }

    @Test
    @DisplayName("Debe registrar una evaluación válida con fecha actual y texto opcional")
    void registrarEvaluacionValida() {
        EvaluacionProveedor evaluacion = new EvaluacionProveedor();
        evaluacion.setProveedorId(1);
        evaluacion.setCalidad(5);
        evaluacion.setPuntualidad(4);
        evaluacion.setComentario("  Buena atención y entrega a tiempo  ");

        EvaluacionProveedor guardada = service.registrarEvaluacion(evaluacion);

        assertEquals(1, guardada.getId());
        assertEquals(1, guardada.getProveedorId());
        assertEquals(LocalDate.now(), guardada.getFecha());
        assertEquals("Buena atención y entrega a tiempo", guardada.getComentario());
    }

    @Test
    @DisplayName("Debe rechazar calidad y puntualidad fuera del rango 1 a 5")
    void rechazarCalificacionesFueraDeRango() {
        EvaluacionProveedor baja = new EvaluacionProveedor();
        baja.setProveedorId(1);
        baja.setCalidad(0);
        baja.setPuntualidad(3);
        assertThrows(ValidacionException.class, () -> service.registrarEvaluacion(baja));

        EvaluacionProveedor alta = new EvaluacionProveedor();
        alta.setProveedorId(1);
        alta.setCalidad(3);
        alta.setPuntualidad(6);
        assertThrows(ValidacionException.class, () -> service.registrarEvaluacion(alta));
    }

    @Test
    @DisplayName("Debe rechazar proveedores inexistentes e inactivos")
    void rechazarProveedorInvalido() {
        EvaluacionProveedor inexistente = new EvaluacionProveedor();
        inexistente.setProveedorId(999);
        inexistente.setCalidad(4);
        inexistente.setPuntualidad(4);
        assertThrows(EntidadNoEncontradaException.class, () -> service.registrarEvaluacion(inexistente));

        EvaluacionProveedor inactivo = new EvaluacionProveedor();
        inactivo.setProveedorId(2);
        inactivo.setCalidad(4);
        inactivo.setPuntualidad(4);
        assertThrows(ValidacionException.class, () -> service.registrarEvaluacion(inactivo));
    }

    @Test
    @DisplayName("Debe listar el historial de una evaluación por proveedor")
    void listarHistorialPorProveedor() {
        EvaluacionProveedor primera = new EvaluacionProveedor();
        primera.setProveedorId(1);
        primera.setCalidad(4);
        primera.setPuntualidad(5);
        primera.setFecha(LocalDate.of(2026, 6, 20));
        service.registrarEvaluacion(primera);

        EvaluacionProveedor segunda = new EvaluacionProveedor();
        segunda.setProveedorId(1);
        segunda.setCalidad(5);
        segunda.setPuntualidad(4);
        service.registrarEvaluacion(segunda);

        List<EvaluacionProveedor> historial = service.listarPorProveedor(1);
        assertEquals(2, historial.size());
        assertTrue(historial.get(0).getFecha().compareTo(historial.get(1).getFecha()) >= 0);
    }

    private static class ProveedorDAOMock implements ProveedorDAO {
        private final List<Proveedor> datos = new ArrayList<>();

        void agregar(Proveedor proveedor) {
            datos.add(proveedor);
        }

        @Override
        public List<Proveedor> listarTodos() { return new ArrayList<>(datos); }

        @Override
        public List<Proveedor> listarActivos() {
            List<Proveedor> activos = new ArrayList<>();
            for (Proveedor proveedor : datos) {
                if (proveedor.isActivo()) {
                    activos.add(proveedor);
                }
            }
            return activos;
        }

        @Override
        public Proveedor buscarPorId(int id) {
            for (Proveedor proveedor : datos) {
                if (proveedor.getId() == id) {
                    return proveedor;
                }
            }
            return null;
        }

        @Override
        public Proveedor buscarPorEmail(String email) { return null; }

        @Override
        public int guardar(Proveedor proveedor) { return 0; }

        @Override
        public boolean actualizar(Proveedor proveedor) { return false; }

        @Override
        public boolean desactivar(int id) { return false; }

        @Override
        public boolean reactivar(int id) { return false; }
    }

    private static class EvaluacionProveedorDAOMock implements EvaluacionProveedorDAO {
        private final List<EvaluacionProveedor> datos = new ArrayList<>();
        private int secuencia = 1;

        @Override
        public List<EvaluacionProveedor> listarTodos() {
            return new ArrayList<>(datos);
        }

        @Override
        public List<EvaluacionProveedor> listarPorProveedor(int proveedorId) {
            List<EvaluacionProveedor> resultado = new ArrayList<>();
            for (EvaluacionProveedor item : datos) {
                if (item.getProveedorId() == proveedorId) {
                    resultado.add(item);
                }
            }
            resultado.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));
            return resultado;
        }

        @Override
        public int guardar(EvaluacionProveedor evaluacion) {
            int id = secuencia++;
            EvaluacionProveedor copia = new EvaluacionProveedor();
            copia.setId(id);
            copia.setProveedorId(evaluacion.getProveedorId());
            copia.setFecha(evaluacion.getFecha());
            copia.setCalidad(evaluacion.getCalidad());
            copia.setPuntualidad(evaluacion.getPuntualidad());
            copia.setComentario(evaluacion.getComentario());
            datos.add(copia);
            return id;
        }

        @Override
        public EvaluacionProveedor buscarPorId(int id) { return null; }
    }
}
