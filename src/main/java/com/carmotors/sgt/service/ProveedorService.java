package com.carmotors.sgt.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

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
        this.proveedorDAO = Objects.requireNonNull(proveedorDAO, "El DAO de proveedores es obligatorio.");
    }

    public List<Proveedor> listarTodos() {
        return proveedorDAO.listarTodos();
    }

    public List<Proveedor> listarActivos() {
        return proveedorDAO.listarActivos();
    }

    public Proveedor buscarPorId(int id) {
        return proveedorDAO.buscarPorId(id).orElse(null);
    }

    public Proveedor crearProveedor(Proveedor proveedor) {
        validarProveedor(proveedor);
        String emailNormalizado = normalizarEmail(proveedor.getEmail());
        proveedor.setNombre(proveedor.getNombre().trim());
        proveedor.setEmail(emailNormalizado);
        proveedor.setTelefono(normalizarTelefono(proveedor.getTelefono()));
        proveedor.setDireccion(normalizarTexto(proveedor.getDireccion()));

        Optional<Proveedor> existente = proveedorDAO.buscarPorEmail(emailNormalizado);
        if (existente.isPresent()) {
            throw new IllegalArgumentException("Ya existe un proveedor registrado con ese email.");
        }

        int idGenerado = proveedorDAO.guardar(proveedor);
        proveedor.setId(idGenerado);
        return proveedor;
    }

    public Proveedor actualizarProveedor(Proveedor proveedor) {
        if (proveedor == null || proveedor.getId() <= 0) {
            throw new IllegalArgumentException("Debe indicar el identificador del proveedor a actualizar.");
        }

        validarProveedor(proveedor);
        String emailNormalizado = normalizarEmail(proveedor.getEmail());
        proveedor.setNombre(proveedor.getNombre().trim());
        proveedor.setEmail(emailNormalizado);
        proveedor.setTelefono(normalizarTelefono(proveedor.getTelefono()));
        proveedor.setDireccion(normalizarTexto(proveedor.getDireccion()));

        Optional<Proveedor> existente = proveedorDAO.buscarPorEmail(emailNormalizado);
        if (existente.isPresent() && existente.get().getId() != proveedor.getId()) {
            throw new IllegalArgumentException("Ya existe otro proveedor registrado con ese email.");
        }

        boolean actualizado = proveedorDAO.actualizar(proveedor);
        if (!actualizado) {
            throw new IllegalArgumentException("No se pudo actualizar el proveedor indicado.");
        }
        return proveedor;
    }

    public Proveedor desactivarProveedor(int id) {
        Proveedor proveedor = buscarPorId(id);
        if (proveedor == null) {
            throw new IllegalArgumentException("El proveedor no existe.");
        }
        if (!proveedor.isActivo()) {
            return proveedor;
        }
        boolean actualizado = proveedorDAO.desactivar(id);
        if (!actualizado) {
            throw new IllegalArgumentException("No se pudo desactivar el proveedor.");
        }
        proveedor.setActivo(false);
        return proveedor;
    }

    public Proveedor reactivarProveedor(int id) {
        Proveedor proveedor = buscarPorId(id);
        if (proveedor == null) {
            throw new IllegalArgumentException("El proveedor no existe.");
        }
        if (proveedor.isActivo()) {
            return proveedor;
        }
        boolean actualizado = proveedorDAO.reactivar(id);
        if (!actualizado) {
            throw new IllegalArgumentException("No se pudo reactivar el proveedor.");
        }
        proveedor.setActivo(true);
        return proveedor;
    }

    private void validarProveedor(Proveedor proveedor) {
        if (proveedor == null) {
            throw new IllegalArgumentException("El proveedor no puede ser nulo.");
        }

        if (!InputValidator.esTextoNoVacio(proveedor.getNombre())) {
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio.");
        }

        String email = proveedor.getEmail();
        if (!InputValidator.esEmailValido(email)) {
            throw new IllegalArgumentException("El email del proveedor no es válido.");
        }
    }

    private String normalizarEmail(String email) {
        return InputValidator.normalizarTexto(email).toLowerCase(Locale.ROOT);
    }

    private String normalizarTelefono(String telefono) {
        if (telefono == null) {
            return "";
        }
        return InputValidator.normalizarTexto(telefono);
    }

    private String normalizarTexto(String valor) {
        return InputValidator.normalizarTexto(valor);
    }
}
