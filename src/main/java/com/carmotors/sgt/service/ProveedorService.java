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

    private static final int MAX_LONGITUD_NOMBRE = 100;
    private static final int MAX_LONGITUD_TELEFONO = 20;
    private static final int MAX_LONGITUD_EMAIL = 100;
    private static final int MAX_LONGITUD_DIRECCION = 150;

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
        proveedor.setDireccion(normalizarTextoOpcional(proveedor.getDireccion()));

        if (emailNormalizado != null) {
            Proveedor existente = proveedorDAO.buscarPorEmail(emailNormalizado);
            if (existente != null) {
                throw new DatoDuplicadoException("Ya existe un proveedor registrado con ese email.");
            }
        }

        proveedor.setActivo(true);
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
        proveedor.setDireccion(normalizarTextoOpcional(proveedor.getDireccion()));

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
        if (nombre.length() > MAX_LONGITUD_NOMBRE) {
            throw new ValidacionException("El nombre del proveedor no puede superar los " + MAX_LONGITUD_NOMBRE + " caracteres.");
        }
        proveedor.setNombre(nombre);

        String email = proveedor.getEmail();
        if (email != null && !email.trim().isEmpty()) {
            String emailLimpio = InputValidator.normalizarTexto(email);
            if (emailLimpio.length() > MAX_LONGITUD_EMAIL) {
                throw new ValidacionException("El email del proveedor no puede superar los " + MAX_LONGITUD_EMAIL + " caracteres.");
            }
            if (!InputValidator.esEmailValido(emailLimpio)) {
                throw new ValidacionException("El email del proveedor no es válido.");
            }
        }

        String telefono = InputValidator.normalizarTexto(proveedor.getTelefono());
        if (!telefono.isEmpty() && telefono.length() > MAX_LONGITUD_TELEFONO) {
            throw new ValidacionException("El teléfono del proveedor no puede superar los " + MAX_LONGITUD_TELEFONO + " caracteres.");
        }

        String direccion = InputValidator.normalizarTexto(proveedor.getDireccion());
        if (!direccion.isEmpty() && direccion.length() > MAX_LONGITUD_DIRECCION) {
            throw new ValidacionException("La dirección del proveedor no puede superar los " + MAX_LONGITUD_DIRECCION + " caracteres.");
        }
    }

    private String normalizarEmail(String email) {
        String limpio = InputValidator.normalizarTexto(email);
        return limpio.isEmpty() ? null : limpio.toLowerCase();
    }

    private String normalizarTelefono(String telefono) {
        return normalizarTextoOpcional(telefono);
    }

    private String normalizarTextoOpcional(String valor) {
        String limpio = InputValidator.normalizarTexto(valor);
        return limpio.isEmpty() ? null : limpio;
    }
}
