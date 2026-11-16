package com.carmotors.sgt.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.model.RepuestoTipo;
import com.carmotors.sgt.repository.RepuestoDAO;
import com.carmotors.sgt.repository.RepuestoTipoRepository;

class RepuestoServiceTest {

    private RepuestoDAOMock repuestoDAO;
    private RepuestoTipoRepositoryEnMemoria tipoRepository;
    private RepuestoService service;

    @BeforeEach
    void setUp() {
        repuestoDAO = new RepuestoDAOMock();
        tipoRepository = new RepuestoTipoRepositoryEnMemoria();
        tipoRepository.agregar(new RepuestoTipo(1, "Filtro"));
        tipoRepository.agregar(new RepuestoTipo(2, "Batería"));
        service = new RepuestoService(repuestoDAO, tipoRepository);
    }

    @Test
    @DisplayName("Debe crear un repuesto válido normalizando precio a escala 2 y valores por defecto")
    void crearRepuestoValidoNormalizaPrecioYValoresPorDefecto() {
        Repuesto repuesto = new Repuesto();
        repuesto.setRepuestoTipoId(1);
        repuesto.setNombre("  Filtro de aceite  ");
        repuesto.setMarca("  Bosch  ");
        repuesto.setPrecioVenta(new BigDecimal("15000.556"));
        repuesto.setStockMinimo(5);

        Repuesto creado = service.crearRepuesto(repuesto);

        assertEquals(1, creado.getId());
        assertEquals("Filtro de aceite", creado.getNombre());
        assertEquals("Bosch", creado.getMarca());
        assertEquals(new BigDecimal("15000.56"), creado.getPrecioVenta());
        assertEquals(2, creado.getPrecioVenta().scale());
        assertEquals(0, creado.getCantidadReservada());
        assertTrue(creado.isActivo());
    }

    @Test
    @DisplayName("Debe rechazar precios negativos, stock mínimo negativo, nombre o marca vacíos y tipo inexistente")
    void rechazaValidacionesDeDominio() {
        Repuesto precioInvalido = new Repuesto();
        precioInvalido.setRepuestoTipoId(1);
        precioInvalido.setNombre("Filtro");
        precioInvalido.setMarca("Bosch");
        precioInvalido.setPrecioVenta(new BigDecimal("0.00"));
        assertThrows(ValidacionException.class, () -> service.crearRepuesto(precioInvalido));

        Repuesto stockInvalido = new Repuesto();
        stockInvalido.setRepuestoTipoId(1);
        stockInvalido.setNombre("Filtro");
        stockInvalido.setMarca("Bosch");
        stockInvalido.setPrecioVenta(new BigDecimal("1000.00"));
        stockInvalido.setStockMinimo(-1);
        assertThrows(ValidacionException.class, () -> service.crearRepuesto(stockInvalido));

        Repuesto nombreVacio = new Repuesto();
        nombreVacio.setRepuestoTipoId(1);
        nombreVacio.setNombre("   ");
        nombreVacio.setMarca("Bosch");
        nombreVacio.setPrecioVenta(new BigDecimal("1000.00"));
        assertThrows(ValidacionException.class, () -> service.crearRepuesto(nombreVacio));

        Repuesto marcaVacia = new Repuesto();
        marcaVacia.setRepuestoTipoId(1);
        marcaVacia.setNombre("Filtro");
        marcaVacia.setMarca("   ");
        marcaVacia.setPrecioVenta(new BigDecimal("1000.00"));
        assertThrows(ValidacionException.class, () -> service.crearRepuesto(marcaVacia));

        Repuesto tipoInvalido = new Repuesto();
        tipoInvalido.setRepuestoTipoId(99);
        tipoInvalido.setNombre("Filtro");
        tipoInvalido.setMarca("Bosch");
        tipoInvalido.setPrecioVenta(new BigDecimal("1000.00"));
        assertThrows(ValidacionException.class, () -> service.crearRepuesto(tipoInvalido));
    }

    @Test
    @DisplayName("Debe rechazar nombres duplicados al crear y al actualizar")
    void rechazaRepuestosDuplicados() {
        Repuesto original = new Repuesto();
        original.setRepuestoTipoId(1);
        original.setNombre("Filtro de aceite");
        original.setMarca("Bosch");
        original.setPrecioVenta(new BigDecimal("5000.00"));
        original.setStockMinimo(2);
        service.crearRepuesto(original);

        Repuesto duplicado = new Repuesto();
        duplicado.setRepuestoTipoId(2);
        duplicado.setNombre("  filtro de aceite  ");
        duplicado.setMarca("Atlas");
        duplicado.setPrecioVenta(new BigDecimal("5200.00"));
        duplicado.setStockMinimo(2);
        assertThrows(DatoDuplicadoException.class, () -> service.crearRepuesto(duplicado));

        Repuesto otro = new Repuesto();
        otro.setRepuestoTipoId(2);
        otro.setNombre("Batería de prueba");
        otro.setMarca("Moura");
        otro.setPrecioVenta(new BigDecimal("3200.00"));
        otro.setStockMinimo(1);
        Repuesto segundo = service.crearRepuesto(otro);

        Repuesto actualizado = new Repuesto();
        actualizado.setId(segundo.getId());
        actualizado.setRepuestoTipoId(2);
        actualizado.setNombre("Filtro de aceite");
        actualizado.setMarca("Bosch");
        actualizado.setPrecioVenta(new BigDecimal("6000.00"));
        actualizado.setStockMinimo(4);
        assertThrows(DatoDuplicadoException.class, () -> service.actualizarRepuesto(actualizado));
    }

    @Test
    @DisplayName("Debe preservar cantidadReservada y activo al actualizar y lanzar entidad no encontrada si no existe")
    void actualizarConservaValoresYBuscaInexistentes() {
        Repuesto original = new Repuesto();
        original.setRepuestoTipoId(1);
        original.setNombre("Filtro de aceite");
        original.setMarca("Bosch");
        original.setPrecioVenta(new BigDecimal("5000.00"));
        original.setCantidadReservada(3);
        original.setStockMinimo(2);
        original.setActivo(true);
        Repuesto creado = service.crearRepuesto(original);

        Repuesto cambios = new Repuesto();
        cambios.setId(creado.getId());
        cambios.setRepuestoTipoId(2);
        cambios.setNombre("Filtro de aire");
        cambios.setMarca("Mann");
        cambios.setPrecioVenta(new BigDecimal("6000.50"));
        cambios.setStockMinimo(10);
        cambios.setCantidadReservada(77);
        cambios.setActivo(false);

        Repuesto actualizado = service.actualizarRepuesto(cambios);

        assertEquals(0, actualizado.getCantidadReservada());
        assertTrue(actualizado.isActivo());
        assertEquals("Filtro de aire", actualizado.getNombre());

        assertThrows(EntidadNoEncontradaException.class, () -> service.buscarPorId(999));
        assertThrows(EntidadNoEncontradaException.class, () -> service.actualizarRepuesto(new Repuesto(999, 1, "X", "Y", new BigDecimal("10.00"), 0, 1, true)));
        assertThrows(EntidadNoEncontradaException.class, () -> service.desactivarRepuesto(999));
        assertThrows(EntidadNoEncontradaException.class, () -> service.reactivarRepuesto(999));
    }

    @Test
    @DisplayName("Debe desactivar y reactivar un repuesto sin borrar su registro")
    void desactivarYReactivarRepuesto() {
        Repuesto repuesto = new Repuesto();
        repuesto.setRepuestoTipoId(1);
        repuesto.setNombre("Filtro de aceite");
        repuesto.setMarca("Bosch");
        repuesto.setPrecioVenta(new BigDecimal("5000.00"));
        repuesto.setStockMinimo(2);
        Repuesto creado = service.crearRepuesto(repuesto);

        Repuesto desactivado = service.desactivarRepuesto(creado.getId());
        assertTrue(!desactivado.isActivo());

        Repuesto reactivado = service.reactivarRepuesto(creado.getId());
        assertTrue(reactivado.isActivo());
    }

    private static class RepuestoTipoRepositoryEnMemoria implements RepuestoTipoRepository {
        private final List<RepuestoTipo> datos = new ArrayList<>();

        void agregar(RepuestoTipo tipo) {
            datos.add(tipo);
        }

        @Override
        public List<RepuestoTipo> listarTodos() {
            return new ArrayList<>(datos);
        }

        @Override
        public RepuestoTipo buscarPorId(int id) {
            for (RepuestoTipo tipo : datos) {
                if (tipo.getId() == id) {
                    return tipo;
                }
            }
            return null;
        }
    }

    private static class RepuestoDAOMock implements RepuestoDAO {
        private final List<Repuesto> datos = new ArrayList<>();
        private int siguienteId = 1;

        @Override
        public List<Repuesto> listarTodos() {
            return new ArrayList<>(datos);
        }

        @Override
        public List<Repuesto> listarActivos() {
            List<Repuesto> activos = new ArrayList<>();
            for (Repuesto repuesto : datos) {
                if (repuesto.isActivo()) {
                    activos.add(repuesto);
                }
            }
            return activos;
        }

        @Override
        public Repuesto buscarPorId(int id) {
            for (Repuesto repuesto : datos) {
                if (repuesto.getId() == id) {
                    return repuesto;
                }
            }
            return null;
        }

        @Override
        public Repuesto buscarPorNombre(String nombre) {
            if (nombre == null) {
                return null;
            }
            for (Repuesto repuesto : datos) {
                if (repuesto.getNombre() != null && repuesto.getNombre().equalsIgnoreCase(nombre)) {
                    return repuesto;
                }
            }
            return null;
        }

        @Override
        public int guardar(Repuesto repuesto) {
            int id = siguienteId++;
            Repuesto copia = new Repuesto(
                    id,
                    repuesto.getRepuestoTipoId(),
                    repuesto.getNombre(),
                    repuesto.getMarca(),
                    repuesto.getPrecioVenta(),
                    repuesto.getCantidadReservada(),
                    repuesto.getStockMinimo(),
                    repuesto.isActivo());
            datos.add(copia);
            return id;
        }

        @Override
        public boolean actualizar(Repuesto repuesto) {
            for (int i = 0; i < datos.size(); i++) {
                if (datos.get(i).getId() == repuesto.getId()) {
                    datos.set(i, new Repuesto(
                            repuesto.getId(),
                            repuesto.getRepuestoTipoId(),
                            repuesto.getNombre(),
                            repuesto.getMarca(),
                            repuesto.getPrecioVenta(),
                            datos.get(i).getCantidadReservada(),
                            repuesto.getStockMinimo(),
                            datos.get(i).isActivo()));
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean desactivar(int id) {
            for (Repuesto repuesto : datos) {
                if (repuesto.getId() == id) {
                    repuesto.setActivo(false);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean reactivar(int id) {
            for (Repuesto repuesto : datos) {
                if (repuesto.getId() == id) {
                    repuesto.setActivo(true);
                    return true;
                }
            }
            return false;
        }
    }
}
