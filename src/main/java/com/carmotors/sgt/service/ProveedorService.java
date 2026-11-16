package com.carmotors.sgt.service;

import java.util.List;

import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.repository.ProveedorDAO;
import com.carmotors.sgt.repository.impl.ProveedorDAOImpl;
import com.carmotors.sgt.util.InputValidator;

public class ProveedorService {

    private final ProveedorDAO proveedorDAO;

    public ProveedorService() {
        this(new ProveedorDAOImpl());
    }

    public ProveedorService(ProveedorDAO proveedorDAO) {
        if (proveedorDAO == null) {
            throw new ValidacionException("El DAO de proveedores es obligatorio.");
        }
        this.proveedorDAO = proveedorDAO;
    }

    public List<Proveedor> listarTodos() {
        return proveedorDAO.listarTodos();
    }

    public List<Proveedor> listarActivos() {
        return proveedorDAO.listarActivos();
    }

    public Proveedor buscarPorId(int id) {
        if (id <= 0) {
            throw new ValidacionException("El ID del proveedor debe ser un número positivo.");
        }

        Proveedor proveedor = proveedorDAO.buscarPorId(id);
        if (proveedor == null) {
            throw new EntidadNoEncontradaException("No se encontró un proveedor con el ID: " + id);
        }
        return proveedor;
    }

    public Proveedor crearProveedor(Proveedor proveedor) {
        validarProveedor(proveedor);

        String emailNormalizado = normalizarEmail(proveedor.getEmail());
        proveedor.setNombre(InputValidator.normalizarTexto(proveedor.getNombre()));
        proveedor.setEmail(emailNormalizado);
        proveedor.setTelefono(normalizarTelefono(proveedor.getTelefono()));
        proveedor.setDireccion(normalizarTexto(proveedor.getDireccion()));

        if (emailNormalizado != null) {
            Proveedor existente = proveedorDAO.buscarPorEmail(emailNormalizado);
            if (existente != null) {
                throw new DatoDuplicadoException("Ya existe un proveedor registrado con ese email.");
            }
        }

        int idGenerado = proveedorDAO.guardar(proveedor);
        proveedor.setId(idGenerado);
        return proveedor;
    }

    public Proveedor actualizarProveedor(Proveedor proveedor) {
        if (proveedor == null || proveedor.getId() <= 0) {
            throw new ValidacionException("Debe indicar el identificador del proveedor a actualizar.");
        }

        Proveedor actual = proveedorDAO.buscarPorId(proveedor.getId());
        if (actual == null) {
            throw new EntidadNoEncontradaException("No existe un proveedor con el ID: " + proveedor.getId());
        }

        validarProveedor(proveedor);
        String emailNormalizado = normalizarEmail(proveedor.getEmail());
        proveedor.setNombre(InputValidator.normalizarTexto(proveedor.getNombre()));
        proveedor.setEmail(emailNormalizado);
        proveedor.setTelefono(normalizarTelefono(proveedor.getTelefono()));
        proveedor.setDireccion(normalizarTexto(proveedor.getDireccion()));

        if (emailNormalizado != null) {
            Proveedor existente = proveedorDAO.buscarPorEmail(emailNormalizado);
            if (existente != null && existente.getId() != proveedor.getId()) {
                throw new DatoDuplicadoException("Ya existe otro proveedor registrado con ese email.");
            }
        }

        boolean actualizado = proveedorDAO.actualizar(proveedor);
        if (!actualizado) {
            throw new ValidacionException("No se pudo actualizar el proveedor indicado.");
        }
        return proveedor;
    }

    public Proveedor desactivarProveedor(int id) {
        Proveedor proveedor = buscarPorId(id);
        if (!proveedor.isActivo()) {
            return proveedor;
        }

        boolean actualizado = proveedorDAO.desactivar(id);
        if (!actualizado) {
            throw new ValidacionException("No se pudo desactivar el proveedor.");
        }
        proveedor.setActivo(false);
        return proveedor;
    }

    public Proveedor reactivarProveedor(int id) {
        Proveedor proveedor = buscarPorId(id);
        if (proveedor.isActivo()) {
            return proveedor;
        }

        boolean actualizado = proveedorDAO.reactivar(id);
        if (!actualizado) {
            throw new ValidacionException("No se pudo reactivar el proveedor.");
        }
        proveedor.setActivo(true);
        return proveedor;
    }

    private void validarProveedor(Proveedor proveedor) {
        if (proveedor == null) {
            throw new ValidacionException("El proveedor no puede ser nulo.");
        }

        String nombre = InputValidator.normalizarTexto(proveedor.getNombre());
        if (nombre.isEmpty()) {
            throw new ValidacionException("El nombre del proveedor es obligatorio.");
        }
        proveedor.setNombre(nombre);

        String email = proveedor.getEmail();
        if (email != null && !email.trim().isEmpty() && !InputValidator.esEmailValido(email)) {
            throw new ValidacionException("El email del proveedor no es válido.");
        }
    }

    private String normalizarEmail(String email) {
        String limpio = InputValidator.normalizarTexto(email);
        return limpio.isEmpty() ? null : limpio.toLowerCase();
    }

    private String normalizarTelefono(String telefono) {
        return telefono == null ? "" : InputValidator.normalizarTexto(telefono);
    }

    private String normalizarTexto(String valor) {
        return InputValidator.normalizarTexto(valor);
    }
}
