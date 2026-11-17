package com.carmotors.sgt.service.state;

import com.carmotors.sgt.exception.TransicionEstadoInvalidaException;

public class EstadoEnProceso implements EstadoOrden {

    @Override
    public String getNombre() {
        return "En Proceso";
    }

    @Override
    public boolean puedeTransicionarA(EstadoOrden destino) {
        return destino instanceof EstadoCompletado || destino instanceof EstadoCancelada;
    }

    @Override
    public EstadoOrden transicionarA(EstadoOrden destino) {
        if (destino != null && puedeTransicionarA(destino)) {
            return destino;
        }
        lanzarTransicionInvalida(destino);
        return this;
    }

    @Override
    public EstadoOrden iniciarProceso() {
        lanzarTransicionInvalida(new EstadoEnProceso());
        return this;
    }

    @Override
    public EstadoOrden completar() {
        return new EstadoCompletado();
    }

    @Override
    public EstadoOrden facturar() {
        lanzarTransicionInvalida(new EstadoFacturado());
        return this;
    }

    @Override
    public EstadoOrden entregar() {
        lanzarTransicionInvalida(new EstadoEntregado());
        return this;
    }

    @Override
    public EstadoOrden cancelar() {
        return new EstadoCancelada();
    }

    @Override
    public boolean permiteEdicion() {
        return true;
    }

    private void lanzarTransicionInvalida(EstadoOrden destino) {
        String nombreDestino = destino == null ? "estado nulo" : destino.getNombre();
        throw new TransicionEstadoInvalidaException(
                "Transición de estado inválida: no se puede pasar de En Proceso a " + nombreDestino + "."
        );
    }
}
