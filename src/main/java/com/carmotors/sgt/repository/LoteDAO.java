package com.carmotors.sgt.repository;

import com.carmotors.sgt.model.Lote;

/**
 * Acceso a datos del lote de inventario.
 */
public interface LoteDAO {

    Lote buscarPorCodigo(String codigoLote);

    int guardar(Lote lote);
}
