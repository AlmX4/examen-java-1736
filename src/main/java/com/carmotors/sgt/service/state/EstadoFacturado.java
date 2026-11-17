package com.carmotors.sgt.service.state;

import com.carmotors.sgt.exception.TransicionEstadoInvalidaException;

public class EstadoFacturado implements EstadoOrden {

    @Override
    public String getNombre() {
        return "Facturado";
    }

    @Override
    public boolean puedeTransicionarA(EstadoOrden destino) {
        return destino instanceof EstadoEntregado;
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
        lanzarTransicionInvalida(new EstadoCompletado());
        return this;
    }

    @Override
    public EstadoOrden facturar() {
        lanzarTransicionInvalida(new EstadoFacturado());
        return this;
    }

    @Override
    public EstadoOrden entregar() {
        return new EstadoEntregado();
    }

    @Override
    public EstadoOrden cancelar() {
        lanzarTransicionInvalida(new EstadoCancelada());
        return this;
    }

    @Override
    public boolean permiteEdicion() {
        return false;
    }

    private void lanzarTransicionInvalida(EstadoOrden destino) {
        String nombreDestino = destino == null ? "estado nulo" : destino.getNombre();
        throw new TransicionEstadoInvalidaException(
                "Transición de estado inválida: no se puede pasar de Facturado a " + nombreDestino + "."
        );
    }
}
