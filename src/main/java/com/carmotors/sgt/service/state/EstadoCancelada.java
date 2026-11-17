package com.carmotors.sgt.service.state;

import com.carmotors.sgt.exception.TransicionEstadoInvalidaException;

public class EstadoCancelada implements EstadoOrden {

    @Override
    public String getNombre() {
        return "Cancelada";
    }

    @Override
    public boolean puedeTransicionarA(EstadoOrden destino) {
        return false;
    }

    @Override
    public EstadoOrden transicionarA(EstadoOrden destino) {
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
        lanzarTransicionInvalida(new EstadoEntregado());
        return this;
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
                "Transición de estado inválida: no se puede pasar de Cancelada a " + nombreDestino + "."
        );
    }
}
