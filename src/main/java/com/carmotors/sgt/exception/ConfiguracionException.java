package com.carmotors.sgt.exception;

/**
 * Excepción de configuración de la aplicación (archivo no encontrado,
 * error de lectura I/O o propiedad obligatoria ausente/en blanco).
 *
 * <p>Extiende {@link RuntimeException} para no forzar captura obligatoria
 * pero permitir un fallo controlado al iniciar la aplicación sin usar
 * {@code System.exit()}.</p>
 */
public class ConfiguracionException extends RuntimeException {

    public ConfiguracionException(String mensaje) {
        super(mensaje);
    }

    public ConfiguracionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
