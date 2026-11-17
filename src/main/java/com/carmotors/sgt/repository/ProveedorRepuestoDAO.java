package com.carmotors.sgt.repository;

import java.util.List;

import com.carmotors.sgt.model.ProveedorRepuesto;

/**
 * Acceso a datos del catálogo proveedor-repuesto (tabla {@code proveedor_repuestos}).
 * Solo persiste y consulta; las reglas de RF-29 y RN-18 viven en el servicio.
 */
public interface ProveedorRepuestoDAO {

    List<ProveedorRepuesto> listarPorProveedor(int proveedorId);

    List<ProveedorRepuesto> listarPorRepuesto(int repuestoId);

    ProveedorRepuesto buscarPorProveedorYRepuesto(int proveedorId, int repuestoId);

    boolean guardar(ProveedorRepuesto proveedorRepuesto);

    boolean actualizarCondiciones(ProveedorRepuesto proveedorRepuesto);

    boolean eliminar(int proveedorId, int repuestoId);

    /**
     * Quita la marca de habitual a todos los proveedores del repuesto.
     *
     * @return cantidad de filas desmarcadas (0 si no había habitual)
     */
    int desmarcarHabitual(int repuestoId);

    boolean marcarHabitual(int proveedorId, int repuestoId);
}
