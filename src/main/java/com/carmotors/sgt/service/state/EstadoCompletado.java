package com.carmotors.sgt.service.state;

import com.carmotors.sgt.exception.TransicionEstadoInvalidaException;

public class EstadoCompletado implements EstadoOrden {

    @Override
    public String getNombre() {
        return "Completado";
    }

    @Override
    public boolean puedeTransicionarA(EstadoOrden destino) {
        return destino instanceof EstadoFacturado;
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
        return new EstadoFacturado();
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
                "Transición de estado inválida: no se puede pasar de Completado a " + nombreDestino + "."
        );
    }
}
