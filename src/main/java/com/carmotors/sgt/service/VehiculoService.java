package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.repository.ClienteRepository;
import com.carmotors.sgt.repository.VehiculoRepository;
import com.carmotors.sgt.util.InputValidator;

import java.util.List;

/**
 * Capa de lógica de negocio para la gestión de vehículos (RF-18, RN-13, RN-17, HU-09).
 */
public class VehiculoService {

    private static final int MAX_LONGITUD_MARCA = 50;
    private static final int MAX_LONGITUD_MODELO = 50;

    private final VehiculoRepository vehiculoRepository;
    private final ClienteRepository clienteRepository;

    public VehiculoService(VehiculoRepository vehiculoRepository, ClienteRepository clienteRepository) {
        if (vehiculoRepository == null) {
            throw new IllegalArgumentException("El repositorio de vehículos es obligatorio.");
        }
        if (clienteRepository == null) {
            throw new IllegalArgumentException("El repositorio de clientes es obligatorio.");
        }
        this.vehiculoRepository = vehiculoRepository;
        this.clienteRepository = clienteRepository;
    }

    /**
     * Registra un nuevo vehículo validando reglas de negocio (RF-18, RN-13, RN-17).
     *
     * @param vehiculo datos del vehículo a registrar
     * @return vehículo registrado con su ID generado
     */
    public Vehiculo registrarVehiculo(Vehiculo vehiculo) {
        if (vehiculo == null) {
            throw new ValidacionException("Los datos del vehículo no pueden ser nulos.");
        }

        if (vehiculo.getClienteId() <= 0) {
            throw new ValidacionException("El ID del cliente debe ser un número positivo.");
        }

        // RN-17: Verificar existencia y estado del cliente
        Cliente cliente = clienteRepository.buscarPorId(vehiculo.getClienteId());
        if (cliente == null) {
            throw new EntidadNoEncontradaException(
                    "No se encontró un cliente con el ID: " + vehiculo.getClienteId());
        }
        if (!cliente.isActivo()) {
            throw new ValidacionException("No se puede registrar un vehículo a un cliente inactivo.");
        }

        // RN-13: Normalización y validación de placa
        String placaOriginal = vehiculo.getPlaca();
        if (!InputValidator.esPlacaValida(placaOriginal)) {
            throw new ValidacionException(
                    "Formato de placa inválido. Debe tener entre 5 y 10 caracteres alfanuméricos.");
        }
        String placaNormalizada = InputValidator.normalizarPlaca(placaOriginal);
        vehiculo.setPlaca(placaNormalizada);

        // Validación de marca y modelo
        String marcaNormalizada = InputValidator.normalizarTexto(vehiculo.getMarca());
        if (!InputValidator.esTextoNoVacio(marcaNormalizada)) {
            throw new ValidacionException("La marca del vehículo es obligatoria.");
        }
        if (marcaNormalizada.length() > MAX_LONGITUD_MARCA) {
            throw new ValidacionException(
                    "La marca no puede superar los " + MAX_LONGITUD_MARCA + " caracteres.");
        }
        vehiculo.setMarca(marcaNormalizada);

        String modeloNormalizado = InputValidator.normalizarTexto(vehiculo.getModelo());
        if (!InputValidator.esTextoNoVacio(modeloNormalizado)) {
            throw new ValidacionException("El modelo del vehículo es obligatorio.");
        }
        if (modeloNormalizado.length() > MAX_LONGITUD_MODELO) {
            throw new ValidacionException(
                    "El modelo no puede superar los " + MAX_LONGITUD_MODELO + " caracteres.");
        }
        vehiculo.setModelo(modeloNormalizado);

        // RN-13: Unicidad de placa
        Vehiculo existente = vehiculoRepository.buscarPorPlaca(placaNormalizada);
        if (existente != null) {
            throw new DatoDuplicadoException(
                    "Ya existe un vehículo registrado con la placa: " + placaNormalizada);
        }

        int idGenerado = vehiculoRepository.guardar(vehiculo);
        vehiculo.setId(idGenerado);
        return vehiculo;
    }

    /**
     * Busca un vehículo por su identificador primario.
     *
     * @param id identificador único del vehículo
     * @return vehículo encontrado
     */
    public Vehiculo buscarPorId(int id) {
        if (id <= 0) {
            throw new ValidacionException("El ID del vehículo debe ser un número positivo.");
        }
        Vehiculo vehiculo = vehiculoRepository.buscarPorId(id);
        if (vehiculo == null) {
            throw new EntidadNoEncontradaException("No se encontró un vehículo con el ID: " + id);
        }
        return vehiculo;
    }

    /**
     * Busca un vehículo por su placa normalizada (RN-13).
     *
     * @param placa placa del vehículo
     * @return vehículo encontrado
     */
    public Vehiculo buscarPorPlaca(String placa) {
        String placaNormalizada = InputValidator.normalizarPlaca(placa);
        if (!InputValidator.esTextoNoVacio(placaNormalizada)) {
            throw new ValidacionException("La placa no puede estar vacía.");
        }
        Vehiculo vehiculo = vehiculoRepository.buscarPorPlaca(placaNormalizada);
        if (vehiculo == null) {
            throw new EntidadNoEncontradaException(
                    "No se encontró un vehículo con la placa: " + placaNormalizada);
        }
        return vehiculo;
    }

    /**
     * Retorna los vehículos asociados a un cliente (HU-09).
     * Permite consultar incluso si el cliente se encuentra inactivo para preservar su historial.
     *
     * @param clienteId identificador del cliente
     * @return lista de vehículos del cliente
     */
    public List<Vehiculo> listarPorCliente(int clienteId) {
        if (clienteId <= 0) {
            throw new ValidacionException("El ID del cliente debe ser un número positivo.");
        }
        Cliente cliente = clienteRepository.buscarPorId(clienteId);
        if (cliente == null) {
            throw new EntidadNoEncontradaException("No se encontró un cliente con el ID: " + clienteId);
        }
        return vehiculoRepository.listarPorCliente(clienteId);
    }

    /**
     * Retorna todos los vehículos registrados en el taller.
     *
     * @return lista de vehículos
     */
    public List<Vehiculo> listarTodos() {
        return vehiculoRepository.listarTodos();
    }
}
