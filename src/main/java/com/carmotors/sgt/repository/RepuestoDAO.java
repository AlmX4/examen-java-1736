package com.carmotors.sgt.repository;

import java.util.List;

import com.carmotors.sgt.model.Repuesto;

public interface RepuestoDAO {

    List<Repuesto> listarTodos();

    List<Repuesto> listarActivos();

    Repuesto buscarPorId(int id);

    Repuesto buscarPorNombre(String nombre);

    int guardar(Repuesto repuesto);

    boolean actualizar(Repuesto repuesto);

    boolean desactivar(int id);

    boolean reactivar(int id);

    /**
     * Descuenta unidades reservadas de un repuesto dentro de una transacción activa.
     *
     * @param repuestoId identificador del repuesto
     * @param cantidad   unidades a liberar
     * @return {@code true} si existía reserva suficiente y se actualizó el registro
     */
    boolean descontarReserva(int repuestoId, int cantidad);
}
