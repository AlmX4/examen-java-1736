# Historial de Cambios — CarMotors SGT (`Proyecto-Java-Grupo4`)

> [!NOTE]
> Este archivo registra de forma cronológica todos los Pull Requests, ramas y cambios técnicos integrados al proyecto.
> **Regla del equipo:** cada vez que se acepte e integre un PR o rama, se debe agregar una entrada aquí. **`README.md` no se modifica en tareas intermedias** hasta la tarea `T8.4` del Sprint 8.

---

## [En curso] — Sprint 5: Ciclo de vida: completar, cancelar y alertas (`develop`)

### Pendiente de integración — `feat(inventario): implement StockObserver pattern for low stock alerts (T5.1)` (`feature/gestion-inventario` → `develop`, cierra `#45`, RN-22 / RF-05 / RNF-09 / RNF-10 / RNF-11)
- **Autor:** `@Sofi11017`
- **Revisión solicitada:** `@julio7879`
- **Cambios incluidos:**
  - Creación de la interfaz `StockObserver` (`com.carmotors.sgt.service.observer`) con `void onStockBajo(StockRepuesto stock)` aplicando el principio de segregación de interfaces (ISP).
  - Creación del observador desacoplado `AlertaStockBajoObserver` (`com.carmotors.sgt.service.observer`) que formatea y emite alertas en consola con datos completos de repuesto, stock físico, operativo, reservado, disponible, mínimo, déficit y proveedor habitual activo (o "No definido").
  - Extensión de `InventarioService` (`com.carmotors.sgt.service`) como sujeto observable: lista interna de observadores, métodos de suscripción `agregarObserver(StockObserver)` (con prevención de nulos y duplicados), `eliminarObserver(StockObserver)`, `getObservers()` y el método evaluador `evaluarYNotificarStockBajo(int repuestoId)` que evalúa si el repuesto activo tiene `disponible <= stockMinimo` para notificar a los suscriptores (`RN-22`), conservando todos los constructores existentes.
  - Corrección solicitada en revisión: `notificarStockBajo` ya no silencia las excepciones con un `catch` vacío; recorre una copia defensiva `new ArrayList<>(observers)` (evita `ConcurrentModificationException` si un observador se suscribe o desuscribe durante la notificación) y registra cada fallo con `java.util.logging.Logger` en `Level.WARNING`, incluyendo el observador y el repuesto afectados (`RNF-14`).
  - Registro de `AlertaStockBajoObserver` en `Main.java` durante el arranque de la aplicación.
  - Creación de la suite de pruebas unitarias `StockObserverTest` (8 pruebas) validando alertas al caer en o bajo el mínimo, silencio cuando supera el mínimo o el repuesto está inactivo, desuscripción efectiva, robustez ante observadores con fallos, desuscripción durante la notificación y formato de alerta por consola (195 pruebas en total, 0 fallos).

## [En curso] — Sprint 4: Servicios y órdenes (`develop`)

### Pendiente de integración — `feat(servicios): reserve parts on service lines (T4.7)` (`feature/gestion-servicios` → `develop`, cierra `#41`, RF-10 / RN-01 / RN-02 / RN-11 / HU-06)
- **Autor:** `@JopsWtD`
- **Cambios incluidos:**
  - Creación de la entidad `DetalleRepuesto` (`com.carmotors.sgt.model`) para `detalle_repuestos` (clave compuesta `detalleId` + `repuestoId`, sin `id`), con `precioUnitarioVenta` en `BigDecimal`.
  - Creación de `DetalleRepuestoDAO` / `DetalleRepuestoDAOImpl` (guardar, buscar por `(detalleId, repuestoId)` y listar por línea) con `PreparedStatement`, try-with-resources y constructor con `Connection` compartida.
  - Nuevo `RepuestoDAO.aumentarReservado(int, int)` (`UPDATE repuestos SET cantidad_reservada = cantidad_reservada + ? WHERE id = ?`; el cálculo lo hace MySQL y no toca `lotes`) y nuevo `OrdenServicioDetalleDAO.buscarPorId(int)`.
  - Creación de `StockInsuficienteException` (`RNF-12`) en la jerarquía de excepciones propias.
  - Creación de `OrdenServicioService.reservarRepuesto(detalleId, repuestoId, cantidad)`: valida antes de la transacción que la cantidad sea mayor que cero, que la línea y su orden existan, que el estado permita cambios (`EstadoOrdenFactory.desdeId(...).permiteEdicion()`), que el repuesto exista y esté activo, que no esté ya asignado a la línea y que la cantidad no supere el disponible de `InventarioService.calcularStock(...)` (`RN-01`; el mensaje informa el disponible). Congela `nombre_repuesto_snapshot` y `precio_unitario_venta` (escala 2, `HALF_UP`) (`RN-11`).
  - Aumento del reservado y registro en `detalle_repuestos` en una única transacción JDBC (una `Connection`, `setAutoCommit(false)`, `commit()` / `rollback()` ante cualquier error, incluidos los de tiempo de ejecución, restauración de auto-commit y cierre con try-with-resources) (`RN-02`, `RNF-08`, sección 5.6). El stock físico de los lotes no cambia.
  - `RepuestoDAO`, `DetalleRepuestoDAO` e `InventarioService` se inyectan por constructor; se conservan los constructores existentes de `OrdenServicioService`.
  - `OrdenServicioController` delega `reservarRepuesto` y `consultarDisponible`; `OrdenServicioView` añade la opción "6. Reservar repuestos en una línea" (rango 0–6, tras las opciones de T4.3 y T4.6): muestra las líneas de la orden, pide línea (solo acepta una línea de esa orden), repuesto y cantidad, e informa la reserva y el disponible restante sin cerrar la aplicación ante errores. Cableado en `Main.java` con el `RepuestoDAO` y el `InventarioService` existentes.
  - Creación de `OrdenServicioReservaTest` con 8 pruebas JUnit 5 en memoria: reserva válida (reservado y snapshot), cantidad cero o negativa, cantidad mayor al disponible (mensaje con el disponible), repuesto inexistente, repuesto inactivo, línea inexistente, orden Completada y repuesto ya asignado. Se actualizan los DAOs en memoria de `InventarioServiceTest`, `ProveedorRepuestoServiceTest`, `OrdenServicioServiceTest`, `RepuestoServiceTest`, `LoteServiceTest`, `StockObserverTest`, `LoteControllerTest` y `OrdenServicioControllerTest` (234 pruebas en total, 0 fallos).

### PR #96 — `feat(servicios): assign technicians to order lines (T4.6)` (`feature/gestion-servicios` → `develop`, cierra `#40`, RF-09 / RN-07 / RN-19 / RN-20 / RN-21 / HU-05)
- **Autor:** `@julio7879`
- **Cambios incluidos:**
  - Creación del modelo `DetalleTecnico` (`com.carmotors.sgt.model`) mapeando `detalle_tecnicos(detalle_id, tecnico_id, horas_trabajadas)` con `BigDecimal`.
  - Extensión de `OrdenServicioDetalleDAO` / `OrdenServicioDetalleDAOImpl` con `asignarTecnico(int, int, BigDecimal)`, `existeTecnicoAsignado(int, int)`, `listarTecnicosAsignados(int)` y `actualizarHorasTrabajadas(int, int, BigDecimal)`, conservando compatibilidad de firmas anteriores y soporte transaccional `FOR UPDATE`.
  - Extensión de `OrdenServicioService` conectando `SugerenciaTecnicoChain` para sugerir técnicos excluyendo ya asignados (`RN-20`/`RN-21`), asignar uno o varios técnicos por línea en transacción JDBC atómica rechazando inactivos/duplicados/especialidad sin confirmar (`RN-19`) y registrar/actualizar horas trabajadas no negativas con redondeo `HALF_UP` (tope `999.99`).
  - Exposición en `OrdenServicioController` e incorporación de las opciones `4. Asignar técnicos a una línea` y `5. Registrar horas trabajadas de un técnico` en `OrdenServicioView` (rango `0–5`) con aceptación con Enter de la sugerencia (nombre, especialidad y carga), advertencia de otra especialidad y captura amigable de errores.
  - Ampliación de `OrdenServicioServiceTest` (**6** pruebas nuevas para `T4.6`) y creación de `OrdenServicioControllerTest` (**3** pruebas JUnit 5 en memoria) validando cadena de sugerencia, asignaciones múltiples, `RN-19`, horas trabajadas y bloqueo de estados cerrados (**226** pruebas en total: **0** fallos, **0** errores, **2** omitidas).

### PR #95 — `feat(servicios): add technician suggestion chain (T4.5)` (`feature/gestion-servicios` → `develop`, cierra `#39`, RF-09 / RN-20 / RN-21)
- **Autor:** `@julio7879`
- **Cambios incluidos:**
  - Creación del DTO `SugerenciaTecnico` (`com.carmotors.sgt.service.chain`) con `tecnico`, `cargaActual`, `especialidadCoincide` y `criterioAplicado`.
  - Creación de la clase abstracta `ManejadorSugerencia` (`com.carmotors.sgt.service.chain`) con encadenamiento fluido `establecerSiguiente`, método plantilla `sugerir(...)` y helpers compartidos `obtenerCarga` y `seleccionarMenorCargaYMenorId` (exclusión estricta de técnicos inactivos y desempate determinista por menor `tecnico.getId()`).
  - Creación de los 3 eslabones en orden estricto (`RN-20`): `ManejadorEspecialistaLibre`, `ManejadorEspecialistaMenorCarga` y `ManejadorGeneralMenorCarga`.
  - Creación de `SugerenciaTecnicoChain` ensamblando `Libre -> EspecialistaMenorCarga -> GeneralMenorCarga`, validando `especialidadRequeridaId > 0` (`ValidacionException`), normalizando listas/mapas nulos y conectando la sobrecarga `sugerir(int, TecnicoService)` con `TecnicoService.listarActivos()` y `TecnicoService.obtenerCargasActivasPorTecnico()`.
  - Creación de `SugerenciaTecnicoChainTest` (**9** pruebas JUnit 5 en memoria) cubriendo los 3 eslabones, desempate por menor ID, exclusión de inactivos, retorno `null` controlado, validación de especialidad/servicio e integración directa con `TecnicoService` (**217** pruebas en total: **0** fallos, **0** errores, **2** omitidas).

### PR #94 — `feat(tecnicos): add technician workload query (T4.4)` (`feature/gestion-tecnicos` → `develop`, cierra `#38`, RF-34 / RN-21 / HU-16)
- **Autor:** `@julio7879`
- **Cambios incluidos:**
  - Creación del DTO `CargaLineaTecnico` (`com.carmotors.sgt.model`) con `ordenId`, `detalleId`, `placa`, `tipoServicio`, `estadoId`, `estadoNombre` y `horasTrabajadas` (`BigDecimal`).
  - Extensión de `TecnicoDAO` / `TecnicoDAOImpl` con `listarCargaPorTecnico(int tecnicoId)` (JOIN de `detalle_tecnicos`, `orden_servicio_detalle`, `ordenes_servicio`, `vehiculos` y `servicio_estados`, filtrando por `EstadoPendiente` y `EstadoEnProceso`, ordenado por `os.id ASC, osd.id ASC`) y `contarCargaActivaPorTecnico()` (`COUNT(DISTINCT dt.detalle_id)` agrupado por técnico), con `PreparedStatement` y soporte de conexión compartida.
  - Ampliación de `TecnicoService` (conservando el constructor existente) con `obtenerCargaTrabajo(int)`, `consultarCargaTrabajo(int)` y `obtenerCargasActivasPorTecnico()`, validando ID positivo y existencia del técnico (incluyendo técnicos inactivos con historial vigente).
  - Exposición en `TecnicoController` (`obtenerCargaTrabajo`, `consultarCargaTrabajo`) e incorporación de la opción `9. Consultar carga de trabajo de un técnico` en `TecnicoView` (rango `0–9`) con tabla formateada y manejo controlado de `CarMotorsException`.
  - Ampliación de `TecnicoServiceTest` (**14** pruebas) cubriendo líneas en `Pendiente` y `En Proceso`, exclusión de estados cerrados/cancelados, técnico sin carga, técnico inexistente, ID inválido, técnico inactivo con historial y `obtenerCargasActivasPorTecnico()` (**208** pruebas en total, **0** fallos).

### PR #93 — `feat(servicios): support adding and removing order lines (T4.3)` (`feature/gestion-servicios` → `develop`, cierra `#37`, RF-35 / RN-05 / RN-07 / RN-18 / RN-19)
- **Autor:** `@AlmX4`
- **Cambios incluidos:**
  - Contratos de edición transaccional declarados en las interfaces `OrdenServicioDAO`, `OrdenServicioDetalleDAO`, `RepuestoDAO` y `TecnicoDAO`, con guardas de conexión compartida en sus `*DAOImpl`.
  - Opciones en `OrdenServicioView` y `OrdenServicioController` para agregar y retirar líneas desde el submenú Órdenes de Servicio, validando controles nulos y delegando reglas a `OrdenServicioService`.
  - Las altas conservan el nombre y precio base del servicio (`BigDecimal` escala 2, `HALF_UP`); en `En Proceso`, la línea y la asignación de un técnico activo se confirman en la misma transacción JDBC (`setAutoCommit(false)` / `commit()` / `rollback()`).
  - Los retiros protegen la última línea (`RN-18`) y las líneas con horas trabajadas en `En Proceso` (`RF-35`), liberando exactamente las reservas en `repuestos.cantidad_reservada` en la misma transacción (`RN-05`).
  - Datos de prueba en `database/data.sql` y ampliación de `OrdenServicioServiceTest` con cobertura JUnit 5 en memoria para altas, retiros, estados bloqueados, liberación de reservas y confirmación de especialidad.

### Pendiente de integración — `feat(servicios): add service order creation (T4.2)` (`feature/gestion-servicios` → `develop`, cierra `#36`, RF-08 / RN-11 / RN-18 / HU-04)
- **Autor:** `@JopsWtD`
- **Cambios incluidos:**
  - Creación de las entidades `OrdenServicio` y `OrdenServicioDetalle` (`com.carmotors.sgt.model`) según `ordenes_servicio` y `orden_servicio_detalle`, con `costoManoObra` en `BigDecimal` y fechas en `LocalDateTime`.
  - Creación de `OrdenServicioDAO` / `OrdenServicioDAOImpl` (guardar, buscar por ID) y `OrdenServicioDetalleDAO` / `OrdenServicioDetalleDAOImpl` (guardar, listar líneas por orden) con `PreparedStatement`, try-with-resources, `RETURN_GENERATED_KEYS` y constructor con `Connection` compartida.
  - Creación de `OrdenServicioService.crearOrden(...)`: valida antes de abrir la transacción que el vehículo exista y su cliente dueño esté activo, la descripción del problema (obligatoria, máx. 255), al menos una línea (`RN-18`) y que cada tipo de servicio exista; congela `nombre_servicio_snapshot` y `costo_mano_obra` = `precio_mano_obra_base` (escala 2, `HALF_UP`); asigna `fecha_recepcion` actual y estado inicial desde `EstadoPendiente.getEstadoId()`.
  - Cabecera y líneas guardadas en una única transacción JDBC (una `Connection`, `setAutoCommit(false)`, `commit()` / `rollback()` ante cualquier error, restauración de auto-commit y cierre con try-with-resources) (`RNF-08`, sección 5.6).
  - Creación de `OrdenServicioController` (solo delega) y `OrdenServicioView` con la opción "Crear orden de servicio": búsqueda por placa, descripción, captura de líneas en ciclo mostrando el precio base y resumen final (ID, placa, estado, recepción y líneas con mano de obra).
  - Nuevo `ServiciosMenuView` en la opción `5` del menú principal con los submenús "Tipos de servicio" (vista existente sin cambios) y "Órdenes de servicio"; cableado en `Main.java`.
  - Creación de `OrdenServicioServiceTest` con DAOs en memoria: orden con una y varias líneas, vehículo inexistente, cliente inactivo, descripción vacía y de más de 255 caracteres, orden sin líneas y tipo de servicio inexistente (165 pruebas en total, 0 fallos).

### PR #89 — `feat(servicios): add order state pattern base (T4.1)` (`feature/gestion-servicios` → `develop`, cierra `#35`, RF-12 / RN-06)
- **Autor:** `@AlmX4`
- **Cambios incluidos:**
  - Creación de la interfaz `EstadoOrden` y de las clases `EstadoPendiente`, `EstadoEnProceso`, `EstadoCompletado`, `EstadoFacturado`, `EstadoEntregado` y `EstadoCancelada` en `com.carmotors.sgt.service.state`.
  - Implementación del flujo unidireccional Pendiente → En Proceso → Completado → Facturado → Entregado y cancelación permitida únicamente desde Pendiente o En Proceso.
  - Creación de `TransicionEstadoInvalidaException` en la jerarquía de excepciones propias, con mensajes claros para saltos, retrocesos y estados destino nulos.
  - Inclusión de la capacidad de edición por estado y de métodos de ciclo de vida para reutilización en las próximas tareas de servicios.
  - Creación de pruebas JUnit 5 para el ciclo válido, cancelaciones, saltos inválidos, retrocesos, estados terminales y transición nula.
  - Incorporación de `getEstadoId()` y `EstadoOrdenFactory.desdeId(int)` para el mapeo exacto entre `estado_id` y las seis clases de estado, con rechazo de IDs desconocidos.
  - Extracción de `transicionarA(EstadoOrden)` y `lanzarTransicionInvalida(EstadoOrden)` a `EstadoOrdenBase`, eliminando la duplicación entre las clases concretas.

## [En curso] — Sprint 2: Entidades dependientes de catálogos (`develop`)

### Pendiente de integración — `feat(inventario): add expiring and expired lots report (T3.8)` (`feature/gestion-inventario` → `develop`, cierra `#34`, RF-06 / RNF-11 / RNF-12 / HU-03)
- **Autor:** `@Sofi11017`
- **Revisión solicitada:** `@julio7879`
- **Cambios incluidos:**
  - Creación del DTO `LotePorVencer` (`com.carmotors.sgt.model`) con `lote`, `nombreRepuesto`, `nombreProveedor`, `estado` (`"VENCIDO"` o `"POR VENCER"`) y `diasRestantes` (long, negativo si ya venció, cero si vence hoy, positivo si futuro).
  - Declaración e implementación de `LoteDAO.listarPorVencer(LocalDate fechaLimite)` y `LoteDAOImpl.listarPorVencer` usando `PreparedStatement`: filtra `cantidad_actual > 0 AND fecha_caducidad IS NOT NULL AND fecha_caducidad <= ?` con orden `fecha_caducidad ASC, id ASC`, compatible con conexión compartida transaccional e independiente (`ConnectionFactory`).
  - Creación de `LoteService.generarReporteLotesPorVencer(int dias)` y sobrecarga por defecto `generarReporteLotesPorVencer()` (30 días por defecto, `RF-06`, `HU-03`): valida `dias > 0` (`ValidacionException`), clasifica con Lambdas y `Stream API` en `"VENCIDO"` (`fechaCaducidad < hoy`) vs. `"POR VENCER"` (`hoy <= fechaCaducidad <= hoy + dias`) y resuelve nombres asociados de repuestos (`RepuestoDAO`) y proveedores (`ProveedorDAO`).
  - Exposición en `LoteController` e incorporación de la opción `4. Reporte de lotes por vencer` en `LoteView` (rango `0–4` en `ConsoleReader`): lectura opcional de días con Enter por defecto (30 días), tabla formateada con alineación de columnas y captura controlada de `CarMotorsException` sin cerrar la consola (`RNF-12`).
  - Creación de pruebas unitarias en `LoteServiceTest` (exclusión de lotes agotados, sin caducidad y lejanos, clasificación vencido vs. por vencer con cálculo de días relativos a `LocalDate.now()`, validaciones de días inválidos y sobrecarga por defecto), `LotePorVencerTest` y `LoteControllerTest` (187 pruebas en total, 0 fallos).

### PR #91 — `feat(proveedores): compare active suppliers by spare part (T3.2)` (`feature/gestion-proveedores` → `develop`, cierra `#28`, RF-30 / RN-16 / RN-17 / RNF-11)
- **Autor:** `@Sofi11017`
- **Revisión solicitada:** `@julio7879`
- **Cambios incluidos:**
  - Creación del DTO `ComparacionProveedor` (`com.carmotors.sgt.model`) con `proveedorId`, `nombreProveedor`, `costo` (`BigDecimal` escala 2 `HALF_UP`), `tiempoEntregaDias`, `esHabitual`, `promedioEvaluacion` (`BigDecimal` escala 1 `HALF_UP` o `null` para "Sin evaluar") y `cantidadEvaluaciones`.
  - Extensión de `ProveedorRepuestoService` (`com.carmotors.sgt.service`) inyectando `EvaluacionProveedorDAO` (conservando constructores previos) e implementando `compararProveedoresPorRepuesto(int repuestoId)` con Lambdas y `Stream API`: valida repuesto existente, filtra solo proveedores activos (`RN-17`), calcula promedio con `BigDecimal` (`RN-16`) y ordena por costo ascendente (`RNF-11`).
  - Exposición en `ProveedorRepuestoController` y actualización de `Main.java` con la nueva inyección.
  - Implementación de `compararProveedoresDeRepuesto()` en `CatalogoProveedorView` y nueva opción `9. Comparar proveedores de un repuesto` en `ProveedorView` (rango `0–9`), mostrando tabla formateada con calificaciones o `"Sin evaluar"` y captura amigable de `CarMotorsException`.
  - Ampliación de `ProveedorRepuestoServiceTest` con 6 nuevas pruebas en memoria (21 pruebas en total) y creación de `ComparacionProveedorTest` (4 pruebas) validando orden por costo, filtrado de inactivos, promedios y manejo de excepciones (175 pruebas en total, 0 fallos).

### Pendiente de integración — `feat(inventario): add low stock report (T3.7)` (`feature/gestion-inventario` → `develop`, cierra `#33`)
- **Autor:** `@julio7879`
- **Cambios incluidos:**
  - Extensión de `StockRepuesto` (`com.carmotors.sgt.model`) con `proveedorHabitual` (`"No definido"` por defecto).
  - Ampliación de `InventarioService` (`com.carmotors.sgt.service`) inyectando `ProveedorRepuestoDAO` y `ProveedorDAO` (conservando constructores existentes) y añadiendo `generarReporteStockBajo()` mediante `Stream API` sobre `calcularStockActivos()` (`disponible <= stockMinimo`), resolviendo el proveedor habitual activo (`RN-18`, `RF-05`, `RN-22`, `RNF-11`).
  - Exposición en `InventarioController.generarReporteStockBajo()`, cableado en `Main.java` y opción `11. Reporte de stock bajo` en `RepuestoView` (rango `0–11`), mostrando métricas completas y aviso claro cuando no hay repuestos bajo mínimo.
  - Ampliación de `InventarioServiceTest` cubriendo stock menor/igual al mínimo, exclusión de activos suficientes e inactivos, proveedor habitual activo vs. `"No definido"` y lista vacía.

### PR #87 — `feat(inventario): add lot traceability (T3.4)` (`feature/gestion-inventario` → `develop`, cierra `#30`)
- **Autor:** `@AlmX4`
- **Cambios incluidos:**
  - Ampliación de `LoteDAO` / `LoteDAOImpl` con consulta por repuesto y consulta por código mediante `PreparedStatement`.
  - Consulta de lotes ordenada deterministamente: lotes con caducidad más próxima primero, después lotes sin caducidad por fecha de ingreso y ID (`RF-03`, sección 2.6).
  - Extensión de `LoteService` y `LoteController` para listar lotes por repuesto y consultar trazabilidad por código, validando el ID y la existencia del repuesto.
  - Integración de la opción `3. Listar lotes por repuesto` en `LoteView`, mostrando proveedor, fechas, cantidad inicial y cantidad restante; la búsqueda inexistente informa claramente sin cerrar la consola.
  - Creación de pruebas JUnit 5 para trazabilidad por código, lote inexistente, orden determinista de 4 criterios y repuesto inexistente en `LoteServiceTest`.

### PR #87 — `feat(inventario): add multi-criteria parts search and filters (T3.6)` (`feature/gestion-inventario` → `develop`, cierra `#32`)
- **Autor:** `@julio7879`
- **Cambios incluidos:**
  - Adición de `InputValidator.normalizarParaBusqueda(String)` (`com.carmotors.sgt.util`) para normalizar búsquedas sin distinguir mayúsculas ni tildes usando métodos estándar de `String`.
  - Extensión de `RepuestoService` (`com.carmotors.sgt.service`) inyectando `ProveedorRepuestoDAO` y `ProveedorDAO` (conservando constructores existentes) y agregando `buscarPorFiltros(nombre, tipo, marca, proveedor)` con carga previa SQL (`repuestoDAO`, `repuestoTipoDAO`, `proveedorDAO`, `proveedorRepuestoDAO`) y filtrado combinable en memoria mediante Lambdas y `Stream API` (`RF-04`, `RNF-11`).
  - Exposición en `RepuestoController.buscarPorFiltros(...)`, cableado en `Main.java` y nueva opción `10. Buscar y filtrar repuestos` en `RepuestoView` (rango `0–10`) con mensajes claros cuando no existen coincidencias.
  - Ampliación de `RepuestoServiceTest` cubriendo búsqueda individual por cada criterio, combinaciones múltiples, insensibilidad a mayúsculas y tildes, filtros vacíos y casos sin coincidencias.

### PR #86 — `refactor: rename repository classes to dao convention` (`refactor/rename-repository-classes` → `develop`)
- **Autor:** `@JopsWtD`
- **Cambios incluidos:**
  - Renombrado transversal dentro de `com.carmotors.sgt.repository` / `repository.impl` (`RNF-02`) de las 8 interfaces `*Repository` y 8 implementaciones `*RepositoryImpl` hacia la convención uniforme `*DAO` / `*DAOImpl` (`ClienteDAO`, `VehiculoDAO`, `TecnicoDAO`, `ServicioTipoDAO`, `EspecialidadDAO`, `MantenimientoTipoDAO`, `RepuestoTipoDAO`, `ServicioEstadoDAO` y sus respectivas `*DAOImpl`).
  - Actualización de referencias en `Main.java`, servicios (`ClienteService`, `RepuestoService`, `ServicioTipoService`, `TecnicoService`, `VehiculoService`) y suite de pruebas sin cambios de comportamiento, SQL ni reglas de negocio (138 pruebas en total, 0 fallos).

### Pendiente de integración — `feat(inventario): add stock calculation (T3.5)` (`feature/gestion-inventario` → `develop`, cierra `#31`)
- **Autor:** `@JopsWtD`
- **Cambios incluidos:**
  - Dos consultas de agregación nuevas en `LoteDAO` / `LoteDAOImpl` con `PreparedStatement` y `COALESCE(SUM(cantidad_actual), 0)`: `sumarStockFisico(repuestoId)` (todos los lotes) y `sumarStockOperativo(repuestoId, fecha)` (lotes con `fecha_caducidad` `NULL` o igual/posterior a la fecha). No se agrega columna de stock a `repuestos` (sección 2.5).
  - Creación de `StockRepuesto` (`com.carmotors.sgt.model`) como DTO de solo transporte: repuesto, stock físico, stock operativo, reservado, disponible, déficit y stock mínimo.
  - Creación de `InventarioService` (`com.carmotors.sgt.service`) con DAOs inyectados por constructor (`RF-07`, `RN-01`, sección 2.5): reservado = `repuestos.cantidad_reservada`, disponible = `MAX(0, operativo - reservado)` y déficit = `MAX(0, reservado - operativo)`, con `LocalDate.now()` como fecha de referencia. Valida ID positivo (`ValidacionException`) y existencia del repuesto (`EntidadNoEncontradaException`). `calcularStockActivos()` devuelve las métricas de todos los repuestos activos con Stream API (`stream()`, `map()`, `collect(Collectors.toList())`) para reutilizarlas en T3.7.
  - Creación de `InventarioController` (solo delega) y opción `9. Ver stock del repuesto` en `RepuestoView` (rango 0–9): muestra físico, operativo, reservado, disponible y mínimo, y una **ALERTA** con las unidades faltantes cuando hay reservas sin respaldo por lotes vencidos; captura `CarMotorsException` sin cerrar la aplicación. Se conservan los constructores existentes de la vista y se cablea en `Main.java`.
  - Creación de `InventarioServiceTest` con 9 pruebas JUnit 5 en memoria y fechas relativas a `LocalDate.now()`: repuesto sin lotes, solo lotes vigentes, lotes sin caducidad, lote que vence hoy y lote vencido ayer, disponible con reservas, déficit, repuesto inexistente, ID inválido y métricas de activos sin inactivos. `LoteServiceTest` actualiza su DAO en memoria con los dos métodos nuevos (138 pruebas en total, 0 fallos).

### Pendiente de integración — `feat(proveedores): add supplier-part catalog (T3.1)` (`feature/gestion-proveedores` → `develop`, cierra `#27`)
- **Autor:** `@JopsWtD`
- **Revisión solicitada:** `@julio7879`
- **Cambios incluidos:**
  - Creación de `ProveedorRepuesto` (`com.carmotors.sgt.model`) con clave compuesta `proveedorId` + `repuestoId`, `costo` (`BigDecimal`), `tiempoEntregaDias` y `esHabitual`.
  - Creación de `ProveedorRepuestoDAO` (`com.carmotors.sgt.repository`) y `ProveedorRepuestoDAOImpl` (`com.carmotors.sgt.repository.impl`) con `PreparedStatement`, try-with-resources y constructor dual con `Connection` compartida: guardar, actualizar costo y tiempo de entrega, eliminar, buscar por par, listar por proveedor o por repuesto, desmarcar y marcar habitual (`RNF-07`).
  - Creación de `ProveedorRepuestoService` (`com.carmotors.sgt.service`) con las reglas de `RF-29` y `RN-18`: proveedor y repuesto existentes, proveedor activo para asociarlo o marcarlo habitual, costo mayor que cero (escala 2, `HALF_UP`), tiempo de entrega no negativo, par único (`DatoDuplicadoException`) y como máximo un habitual por repuesto. El cambio de habitual desmarca el anterior y marca el nuevo en **una sola transacción JDBC** (`setAutoCommit(false)`, misma `Connection` para el DAO, `commit`/`rollback`, cierre con try-with-resources; `RNF-08`, sección 5.6).
  - Creación de `ProveedorRepuestoController` y `CatalogoProveedorView` (`com.carmotors.sgt.view`): opción `8. Catálogo de repuestos del proveedor` en `ProveedorView` (listar, asociar, actualizar condiciones, quitar con confirmación `(S/N)` y marcar habitual) y opción `7. Ver proveedores del repuesto` en `RepuestoView`. Cableado en `Main.java`, conservando los constructores existentes de ambas vistas.
  - Creación de `ProveedorRepuestoServiceTest` con 15 pruebas JUnit 5 en memoria: asociación válida, entidades inexistentes, proveedor inactivo, costo y tiempo inválidos, par duplicado, marcar y cambiar habitual, habitual inactivo, actualización, eliminación y listados (121 pruebas en total, 0 fallos).

### PR #84 — `feat(inventario): lot intake registration (T3.3)` (`feature/gestion-inventario` → `develop`, cierra `#29`)
- **Autor:** `@AlmX4`
- **Cambios incluidos:**
  - Creación de `Lote` (`com.carmotors.sgt.model`) con `id`, `codigoLote`, `proveedorId`, `repuestoId`, `fechaIngreso`, `fechaCaducidad`, `cantidadInicial` y `cantidadActual` siguiendo el modelo persistente del esquema `lotes`.
  - Creación de `LoteDAO` (`com.carmotors.sgt.repository`) y `LoteDAOImpl` (`com.carmotors.sgt.repository.impl`) con `PreparedStatement`, `RETURN_GENERATED_KEYS` y validación de `codigo_lote` único en la capa de servicio.
  - Creación de `LoteService` (`com.carmotors.sgt.service`) validando código obligatorio, cantidad inicial mayor que cero, fecha de ingreso obligatoria, caducidad estrictamente posterior, proveedor activo, repuesto existente y activo, y vínculo real entre proveedor y repuesto en `proveedor_repuestos` (`RF-02`, `RN-14`, `RN-17`, `RNF-08`). La operación se ejecuta dentro de una sola transacción JDBC con `setAutoCommit(false)`, `commit` y `rollback`.
  - Integración de `LoteController` y `LoteView` en el flujo MVC estándar, con acceso desde el submenú de inventario y manejo seguro de `CarMotorsException` sin cerrar la consola.
  - Ajuste de la ruta de validación para permitir pruebas unitarias con DAOs inyectados y mantener el comportamiento transaccional real cuando se usa la implementación JDBC del proyecto.
  - Creación de `LoteServiceTest` con pruebas JUnit 5 cubriendo lote válido, código duplicado, cantidad inválida, caducidad inválida, proveedor inactivo, proveedor inexistente, repuesto inexistente y repuesto inactivo; validación con `mvn test -Dtest=LoteServiceTest` y `mvn clean compile`.

### PR #82 — `feat(proveedores): add supplier ranking (T2.7)` (`feature/gestion-proveedores` → `develop`, cierra `#26`)
- **Autor:** `@JopsWtD`
- **Revisión solicitada:** `@julio7879`
- **Cambios incluidos:**
  - Creación de `RankingProveedor` (`com.carmotors.sgt.model`) como DTO de solo transporte: proveedor, promedio (`BigDecimal`, `null` si no tiene evaluaciones) y cantidad de evaluaciones.
  - Nuevo método `generarRanking()` en `EvaluacionProveedorService` (`RF-32`, `RN-16`, `RNF-11`): una sola consulta `listarTodos()` por DAO, agrupación con `Collectors.groupingBy`, promedio de `getPuntaje()` con un decimal y `RoundingMode.HALF_UP`, orden con `Comparator` (promedio descendente y desempate por nombre) y proveedores "Sin evaluar" al final ordenados por nombre. Incluye proveedores activos e inactivos, porque sus evaluaciones forman parte del historial (`RN-17`).
  - `EvaluacionProveedorController.generarRanking()` delega al servicio y `EvaluacionProveedorView` agrega la opción `4. Ranking de proveedores` (rango 0–4) con una tabla alineada (posición, proveedor, promedio o "Sin evaluar" y evaluaciones), capturando `CarMotorsException` sin cerrar la aplicación.
  - Seis pruebas JUnit 5 nuevas en `EvaluacionProveedorServiceTest`: redondeo a un decimal, orden descendente, desempate por nombre, proveedores sin evaluar al final, todos sin evaluar y ningún proveedor registrado (106 pruebas en total, 0 fallos).

### PR #81 — `feat(clientes): add search by document and normalized plate (T2.2)` (`feature/gestion-clientes` → `develop`, cierra `#21`)
- **Autor:** `@Sofi11017`
- **Revisión solicitada:** `@julio7879`
- **Cambios incluidos:**
  - Enriquecimiento de `ClienteService` (`com.carmotors.sgt.service`) con validación y normalización estricta de documento de identidad (`InputValidator.normalizarTexto`), soporte de búsqueda para clientes activos e inactivos (`RN-17`, `HU-09`), y mensaje explícito ante no coincidencias (`EntidadNoEncontradaException`).
  - Enriquecimiento de `VehiculoService` (`com.carmotors.sgt.service`) con normalización y validación estricta de formato alfanumérico de placas (`InputValidator.esPlacaValida` y `InputValidator.normalizarPlaca`, `RN-13`), y mensaje claro ante vehículo no encontrado.
  - Integración en `ClienteView` (`com.carmotors.sgt.view`) añadiendo la opción `11. Buscar vehículo por placa` (rango interactivo 0–11) y mostrando la ficha completa del vehículo junto con los datos de su cliente propietario asociado (`RF-19`).
  - Manejo amigable de excepciones capturando `EntidadNoEncontradaException` e informando al usuario sin romper el flujo de la consola ni usar `System.exit()`.
  - Creación de suite de pruebas unitarias en JUnit 5: `BusquedaClienteVehiculoTest` (8 pruebas completas), elevando las pruebas del proyecto a 95 pruebas exitosas sin fallos.

### PR #80 — `feat(clientes): add vehicle registration and listing (T2.1)` (`feature/gestion-clientes` → `develop`, cierra `#20`)
- **Autor:** `@Sofi11017`
- **Revisión solicitada:** `@julio7879`
- **Cambios incluidos:**
  - Creación de `Vehiculo` (`com.carmotors.sgt.model`) como POJO clásico con `id`, `clienteId`, `placa`, `marca`, `modelo`, constructores, getters, setters y `toString()`.
  - Creación de `VehiculoDAO` (`com.carmotors.sgt.repository`) y `VehiculoDAOImpl` (`com.carmotors.sgt.repository.impl`) con `PreparedStatement`, `RETURN_GENERATED_KEYS`, constructor dual con soporte para `Connection` compartida (`RNF-08`), retorno `null` controlado sin `Optional` y manejo de `SQLException` envuelto en `CarMotorsException` (`RNF-07`, `RNF-12`).
  - Creación de `VehiculoService` (`com.carmotors.sgt.service`) con validación de existencia y estado activo del cliente al registrar (`RN-17`), normalización y unicidad de placa alfanumérica en mayúsculas (`RN-13`), y consulta de vehículos por cliente permitida incluso para clientes inactivos para preservar su historial (`HU-09`).
  - Integración en `ClienteController` (`com.carmotors.sgt.controller`) y `ClienteView` (`com.carmotors.sgt.view`), añadiendo opciones 9 (registrar vehículo) y 10 (listar vehículos de cliente) al menú de consola, conectado en `Main.java`.
  - Creación de pruebas unitarias completas en JUnit 5: `VehiculoTest` (4 pruebas), `VehiculoServiceTest` (16 pruebas en memoria) y `ClienteControllerTest` (3 pruebas), totalizando 87 pruebas unitarias en el proyecto sin fallos.

### PR #79 — `feat(proveedores): add supplier evaluation registration (T2.6)` (`feature/gestion-proveedores` → `develop`, cierra `#25`)
- **Autor:** `@AlmX4`
- **Cambios incluidos:**
  - Creación de `EvaluacionProveedor` (`com.carmotors.sgt.model`) con `id`, `proveedor_id`, `fecha` por defecto en la fecha actual, `calidad` y `puntualidad` como enteros del 1 al 5 y `comentario` opcional con normalización segura.
  - Creación de `EvaluacionProveedorDAO` (`com.carmotors.sgt.repository`) y `EvaluacionProveedorDAOImpl` (`com.carmotors.sgt.repository.impl`) con consultas JDBC `PreparedStatement`, validación de proveedor y `ConnectionFactory.getInstance().getConnection()` para persistencia segura.
  - Creación de `EvaluacionProveedorService` (`com.carmotors.sgt.service`) validando rango 1..5, proveedor activo, existencia del proveedor y cálculo del promedio con un decimal para cada evaluación y para el historial del proveedor.
  - Creación de `EvaluacionProveedorController` y `EvaluacionProveedorView` (`com.carmotors.sgt.view`) conectados desde `ProveedorView` y `Main.java` para registrar evaluaciones, consultar historial y consultar promedio con mensajes amigables por consola.
  - Creación de pruebas unitarias JUnit 5 en `EvaluacionProveedorServiceTest` validando creación, rechazo de rangos inválidos, proveedores inactivos y promedio de evaluaciones.

### Pendiente de integración — `[S2 - T2.5] Catálogo de tipos de servicio` (`feature/gestion-servicios` → `develop`, Issue `#24`)
- **Autor:** `@julio7879`
- **Cambios incluidos:**
  - Creación de `ServicioTipo` (`com.carmotors.sgt.model`), `ServicioTipoDAO` (`com.carmotors.sgt.repository`) y `ServicioTipoDAOImpl` (`com.carmotors.sgt.repository.impl`) con `PreparedStatement`, `BigDecimal` escala 2 `HALF_UP`, `Types.INTEGER` para `intervalo_meses` opcional y constructor dual con `Connection` compartida (`RNF-07`, `RNF-08`, `RNF-15`).
  - Creación de `ServicioTipoService` (`com.carmotors.sgt.service`) validando nombre único sin distinguir mayúsculas (`DatoDuplicadoException`), existencia de `MantenimientoTipo` y `Especialidad` (`T1.4`), precio no negativo y la regla de dominio Preventivo (`intervaloMeses > 0`) vs. Correctivo (`intervaloMeses == null`).
  - Creación de `ServicioTipoController` y `ServicioTipoView` (`SubmenuVista` conectada en `Main.java`), usando `ConsoleReader`, confirmación `(S/N)` antes de eliminar (`RNF-24`) y manejo limpio de `CarMotorsException` (`RNF-12`).
  - Creación de pruebas unitarias JUnit 5 en `ServicioTipoServiceTest`.

### Pendiente de integración — `[S2 - T2.4] CRUD de repuestos` (`feature/gestion-inventario` → `develop`, Issue `#23`)
- **Autor:** `@AlmX4`
- **Cambios incluidos:**
  - Creación de `Repuesto` (`com.carmotors.sgt.model`) con validación de dominio para `repuestoTipoId`, `nombre`, `marca`, `precioVenta` `BigDecimal` escala 2 `HALF_UP`, `stockMinimo >= 0`, `cantidadReservada` protegida y estado activo/inactivo sin borrar físicamente (`RN-17`, `RNF-15`).
  - Creación de `RepuestoDAO` (`com.carmotors.sgt.repository`) y `RepuestoDAOImpl` (`com.carmotors.sgt.repository.impl`) con `PreparedStatement`, uso real de `conexionCompartida != null`, control de `null` sin `Optional` y entrega de errores a `CarMotorsException` (`RNF-07`, `RNF-08`, `RNF-12`).
  - Creación de `RepuestoService` (`com.carmotors.sgt.service`) con validaciones estrictas, duplicados por nombre y manejo de desactivación/reactivación lógica del inventario. 
  - Creación de `RepuestoController` y `RepuestoView` (`SubmenuVista` conectada en `Main.java`) con lista, búsqueda, alta, actualización y confirmación `(S/N)` antes de desactivar.
  - Creación de pruebas unitarias JUnit 5 en `RepuestoServiceTest`.

### PR #73 — `feat(tecnicos): implement technician CRUD with JDBC and console submenu (T2.3)` (`feature/gestion-tecnicos` → `develop`, cierra `#22`)
- **Autor:** `@julio7879`
- **Cambios incluidos:**
  - Creación de `Tecnico` (`com.carmotors.sgt.model`), `TecnicoDAO` (`com.carmotors.sgt.repository`) y `TecnicoDAOImpl` (`com.carmotors.sgt.repository.impl`) con `PreparedStatement`, retorno `null` controlado y constructor dual con `Connection` compartida (`RNF-07`, `RNF-08`).
  - Creación de `TecnicoService` (`com.carmotors.sgt.service`) con validación de nombre obligatorio, documento único (`DatoDuplicadoException`), especialidad válida contra `EspecialidadDAO` (`T1.4`) y desactivación lógica sin borrado físico (`RF-33`, `RN-17`).
  - Creación de `TecnicoController` y `TecnicoView` (`SubmenuVista` conectada en `Main.java`), usando `ConsoleReader` con confirmación `(S/N)` antes de desactivar y captura segura de `CarMotorsException` (`RNF-12`, `RNF-24`).
  - Creación de pruebas unitarias JUnit 5 en `TecnicoServiceTest`.

---

## [En curso] — Sprint 1: Piezas comunes (`develop`)

### Pendiente de integración — `[S1 - T1.5] CRUD de clientes` (`feature/gestion-clientes` → `develop`, Issue `#18`)
- **Autor:** `@JopsWtD`
- **Cambios incluidos:**
  - Creación de `Cliente` (`com.carmotors.sgt.model`), `ClienteDAO` (`com.carmotors.sgt.repository`) y `ClienteDAOImpl` (`com.carmotors.sgt.repository.impl`) con `PreparedStatement`, `Types.VARCHAR` para `telefono` opcional, retorno `null` controlado y constructor dual con `Connection` compartida (`RNF-07`, `RNF-08`).
  - Creación de `ClienteService` (`com.carmotors.sgt.service`) validando nombre, documento y email obligatorios, límites de longitud según `schema.sql`, formato de email con `InputValidator.esEmailValido` (normalizado a minúsculas), unicidad de documento y email (`DatoDuplicadoException`) y desactivación/reactivación lógica sin borrado físico (`RF-17`, `RN-13`, `RN-17`).
  - Creación de `ClienteController` y `ClienteView` (`SubmenuVista` conectada en `Main.java`) con captura segura de `CarMotorsException` (`RNF-12`).
  - Creación de pruebas unitarias JUnit 5 en `ClienteServiceTest` (9 casos).

### PR #76 — `fix(proveedores): align supplier CRUD with develop contracts and review blockers (T1.6)` (`fix/proveedores-t16` → `develop`, cierra `#19`)
- **Autor:** `@AlmX4`
- **Cambios incluidos:**
  - Creación de `Proveedor` (`com.carmotors.sgt.model`), `ProveedorDAO` (`com.carmotors.sgt.repository`) y `ProveedorDAOImpl` (`com.carmotors.sgt.repository.impl`) reutilizando `conexionCompartida` cuando existe (`RNF-08`), `PreparedStatement` (`RNF-07`), retorno `null` controlado y manejo de `SQLException` con `CarMotorsException`.
  - Creación de `ProveedorService` (`com.carmotors.sgt.service`) validando nombre obligatorio, límites de longitud según `schema.sql`, normalización a `null` de opcionales vacíos (`telefono`, `email`, `direccion`), unicidad de email sin distinguir mayúsculas (`DatoDuplicadoException`) y desactivación/reactivación lógica (`RF-28`, `RN-17`).
  - Creación de `ProveedorController` y `ProveedorView` (`SubmenuVista` conectada en `Main.java`), listando todos los proveedores con su estado (`ACTIVO`/`INACTIVO`) sin imprimir `"null"` literal, usando `ConsoleReader.leerEmailOpcional` y `ConsoleReader.leerConfirmacion` antes de desactivar (`RNF-12`, `RNF-24`).
  - Creación de pruebas unitarias JUnit 5 en `ProveedorServiceTest`.

### Pendiente de integración — `[S1 - T1.4] Tablas de consulta (catálogos fijos)` (`feature/setup-catalogos` → `develop`, Issue `#17`)
- **Autor:** `@Sofi11017`
- **Revisión solicitada:** `@julio7879`
- **Cambios incluidos:**
  - Creación de 4 entidades POJO clásicas en `com.carmotors.sgt.model`: `RepuestoTipo`, `Especialidad`, `MantenimientoTipo` y `ServicioEstado` con constructores, getters, setters y `toString()`.
  - Creación de 4 interfaces de persistencia de solo lectura en `com.carmotors.sgt.repository`: `RepuestoTipoDAO`, `EspecialidadDAO`, `MantenimientoTipoDAO` y `ServicioEstadoDAO` con métodos `listarTodos()` y `buscarPorId(int id)` retornando `null` controlado (sin `Optional`, respetando la regla de no crear paquete `dao`).
  - Creación de 4 implementaciones JDBC en `com.carmotors.sgt.repository.impl`: `RepuestoTipoDAOImpl`, `EspecialidadDAOImpl`, `MantenimientoTipoDAOImpl` y `ServicioEstadoDAOImpl` usando `ConnectionFactory.getInstance().getConnection()`, constructor opcional para conexión compartida (`RNF-08`), `PreparedStatement` y envoltorio seguro de `SQLException` en `CarMotorsException` sin `System.exit()`.
  - Pruebas unitarias JUnit 5 en `CatalogosModelTest` y `CatalogosRepositoryTest` con verificación condicional de base de datos.


### Pendiente de integración — `[S1 - T1.2] Excepciones propias y manejo de errores` (`feature/setup-excepciones` → `develop`, Issue `#15`)
- **Autor:** `@Sofi11017`
- **Revisión solicitada:** `@julio7879`
- **Cambios incluidos:**
  - Creación de `CarMotorsException` (`com.carmotors.sgt.exception`) como excepción raíz no chequeada (`RuntimeException`) del dominio con constructores estándar `(mensaje)` y `(mensaje, causa)` y documentación Javadoc del flujo Service -> Controller (RNF-12, RNF-23).
  - Creación de subclases específicas del dominio: `EntidadNoEncontradaException`, `DatoDuplicadoException` y `ValidacionException` bajo `com.carmotors.sgt.exception`.
  - Integración de `ConfiguracionException` como subclase de `CarMotorsException`, manteniendo total compatibilidad con la fábrica de conexiones.
  - Creación de pruebas unitarias JUnit 5 en `CarMotorsExceptionTest` validando jerarquía `instanceof`, constructores y simulación del flujo Service (lanza) -> Controller (captura sin abortar la aplicación ni usar `System.exit()`).


### Pendiente de integración — `[S0 - T0.8] Conexión a la base de datos` (`feature/setup-conexion-db` → `develop`, Issue `#11`)
- **Autor:** `@Sofi11017`
- **Revisión solicitada:** `@julio7879`
- **Cambios incluidos:**
  - Creación de `src/main/resources/config.properties.example` con plantilla segura (`db.url`, `db.user`, `db.password`, `factura.carpeta=output`, sin tasa de IVA ni secretos reales) cumpliendo con `RNF-14`.
  - Creación de `ConfiguracionException.java` (`com.carmotors.sgt.exception`) extendiendo `RuntimeException` con constructores estándar `(mensaje)` y `(mensaje, causa)` para evitar `System.exit()`.
  - Implementación de `ConnectionFactory.java` (`com.carmotors.sgt.config`) bajo el patrón creacional Singleton (`getInstance()` sincronizado), carga única de `config.properties` en codificación UTF-8 vía `Properties`, validación de propiedades obligatorias y método `getConnection()` retornando una `Connection` nueva en cada llamado para transacciones atómicas multi-tabla (`RN-04`, `RNF-08`).
  - Creación de pruebas unitarias JUnit 5 en `ConnectionFactoryTest` validando el Singleton, `getCarpetaFacturas()` y verificación condicional de conexión (SELECT 1 con `Assumptions.assumeTrue`) asegurando ejecución limpia de `mvn clean test` sin depender de MySQL activo localmente.


### Pendiente de integración — `refactor(setup): remove T0.8 connection files` (`refactor/remove-t08-from-t07` → `develop`)
- **Autor:** `@JopsWtD`
- **Motivo:** el PR #2 (`[S0 - T0.7]`, Issue `#10`) incluyó por error entregables de `[S0 - T0.8] Conexión a la base de datos` (Issue `#11`), asignada a `@Sofi11017`.
- **Cambios incluidos:**
  - Eliminación de `ConnectionFactory.java` (`com.carmotors.sgt.config`).
  - Eliminación de `ConfiguracionException.java` (`com.carmotors.sgt.exception`).
  - Eliminación de `config.properties.example` (`src/main/resources`).
  - Sin cambios en `pom.xml`, `.gitignore`, `Main.java`, `database/`, `util/` ni `view/`. `mvn clean test` en verde.

### Pendiente de integración — `[S1 - T1.3] Esqueleto del menú principal` (`feature/project-setup` → `develop`, Issue `#16`)
- **Autor:** `@julio7879`
- **Cambios incluidos:**
  - Creación de la interfaz `SubmenuVista` (`com.carmotors.sgt.view`) como contrato común para que cada módulo conecte su submenú sin acoplamiento.
  - Creación de `ConsolaMenuView` (`com.carmotors.sgt.view`) con el ciclo principal numerado (`1..7` y `0. Salir`) usando `ConsoleReader`, manejo seguro de módulos pendientes y captura de errores sin cerrar la aplicación (`RNF-03`, `RNF-12`, `RNF-24`).
  - Conexión inicial en `Main.java` y guía de integración de nuevos módulos documentada en `com.carmotors.sgt.view.package-info`.
  - Creación de pruebas unitarias JUnit 5 en `ConsolaMenuViewTest`.

---

## [2026-11-14] — Sprint 1 (`T1.1`), configuración multi-agente y grafo Obsidian (`develop`)

### PR #66 — `feat(setup): add ConsoleReader and InputValidator console utilities (T1.1)` (`feature/project-setup` → `develop`, commit `4fd42aa`, cierra `#14`)
- **Autor:** `@julio7879`
- **Cambios incluidos:**
  - Creación de `InputValidator` (`com.carmotors.sgt.util`) para validación y normalización de textos, enteros, rangos, importes `BigDecimal` (escala 2, `RoundingMode.HALF_UP`, `RNF-15`), fechas `yyyy-MM-dd` (`DateTimeFormatter`, `RNF-13`), emails, placas (`RN-13`) y confirmaciones `S/N` (`RNF-24`) al nivel del curso.
  - Creación de `ConsoleReader` (`com.carmotors.sgt.util`) con métodos estáticos seguros en una sola llamada y reintento automático sin abortar la aplicación (`RNF-12`, `RNF-13`, `RNF-24`).
  - Creación de pruebas unitarias JUnit 5 en `ConsoleReaderTest`.
  - Documentación del nivel técnico permitido de Java (`docs/vault/03-Gestion/Alcance-Tecnico-Java.md`, 20 sesiones del curso) conectada a `AGENTS.md`, `carmotors-rules-validator`, `carmotors-pr-reviewer`, `Definition-of-Done.md` y reorganización de `docs/vault/06-Skills/` y `00-MOC/00-Mapa-Agentes-Skills.md`.

### PR #65 — `chore: configure AGENTS.md, sprint plan, unified skills v1.3 and Obsidian graph` (`chore/agent-workspace-setup` → `develop`, commit `5b74cae`, cierra `#64`)
- **Autor:** `@julio7879`
- **Cambios incluidos:**
  - Incrustado del plan oficial de sprints en `docs/vault/03-Gestion/Plan-Sprints.md` (con `T0.10` marcada como completada) y enlace en `docs/vault/00-MOC/00-Indice-General.md`.
  - Creación de `AGENTS.md` en la raíz con arquitectura v1.3, reglas innegociables y matriz automática de modelos/modos (`Orchestrator (Argon)`, `Gemini 4 Hydrogen`, `Gemini 4 Argon`, `Gemini 3.8 Flash`, `/plan`, `/teamwork`).
  - Actualización de `.agents/README.md` y las 9 fichas `.agents/agent-*.md` con estrategia de modelo por módulo y alineación con el Documento Maestro v1.3.
  - Consolidación de `.agents/skills/carmotors-pr-reviewer/SKILL.md` en el repositorio y unificación de `carmotors-rules-validator` y `carmotors-qmd-consultant` (paquete `repository`, FEFO de 4 criterios, IVA fijo `0.19`, excepciones propias y protección de `README.md`).
  - Integración visual de los **9 agentes especialistas** y **3 skills** en la bóveda Obsidian (`docs/vault/05-Agentes/` y `docs/vault/.obsidian/graph.json`) con enlaces bidireccionales hacia módulos, arquitectura, gestión e historias de usuario (`[[00-Mapa-Agentes-Skills]]`).
  - Creación de este historial `CHANGELOG.md` y limpieza temporal de `README.md` hasta `T8.4`.

---

## [2026-11-13] — Estructura base Maven, JDBC y BD v1.3 (`develop`)

### PR #3 — `Feature/project structure` (`feature/project-structure` → `develop`, commit `668acae`)
- **Autor:** Andrés Jiménez (`@AlmX4`), con co-autoría de Jhorman (`@JopsWtD`) y Julio (`@julio7879`).
- **Tareas cubiertas:** `T0.10` (Issue `#13`, cerrado).
- **Cambios principales:**
  - Actualización de `database/schema.sql` al modelo lógico completo **v1.3 de 20 tablas** (`InnoDB`, `utf8mb4`), incorporando `factura_snapshot`, `factura_detalles`, fechas de ciclo (`fecha_completado`, `fecha_entrega`, `fecha_cancelacion`), campos *snapshot*, restricciones `CHECK` y FK compuesta opcional `(orden_detalle_id, repuesto_id)`.
  - Ampliación de `database/data.sql` con el conjunto inicial de datos de prueba y catálogos (incluido el estado `Cancelada` en `servicio_estados`).

### PR #2 — `Feature/project structure` (`feature/project-structure` → `develop`, commit `ba2bc89`)
- **Autor:** Jhorman (`@JopsWtD`).
- **Tareas cubiertas:** `T0.7`, `T0.8`, `T0.9` (Issues `#10`, `#11`, `#12`, cerrados).
- **Cambios principales:**
  - Configuración inicial de Maven en `pom.xml` (Java 17, `mysql-connector-j` 8.4.0, `junit-jupiter` 5.11.4 y `exec-maven-plugin` 3.5.0 apuntando a `com.carmotors.sgt.Main`).
  - Refactorización y esqueleto de paquetes en inglés bajo `com.carmotors.sgt`: `config`, `controller`, `exception`, `model`, `repository`, `repository.impl`, `service` (`builder`, `chain`, `facade`, `observer`, `state`), `util` y `view`.
  - Implementación de `Main.java`, `ConnectionFactory.java` (patrón Singleton JDBC leyendo `config.properties`) y `ConfiguracionException.java`.
  - Configuración de `src/main/resources/config.properties.example` (usando carpeta `output/` y definiendo el IVA fijo al 19% en código según DP-05) y ampliación de `.gitignore`.
  - Creación inicial de `database/schema.sql` y `database/data.sql`.

---

## [2026-11-13] — Backlog, Bóveda Obsidian v1.3 y Flujo Git (`main` / `develop`)

### Issues `#4`–`#63` en GitHub Project #6 (`2026-11-13` / `2026-11-14`)
- **Autor:** `@julio7879`.
- **Detalle:** Creación del tablero completo de Sprints `S0` a `S8` en GitHub (`#4`–`#13` cerrados para `S0`; `#14`–`#63` abiertos para `S1`–`S8`).

### Base inicial y Bóveda v1.3 (`main` / `develop`, commits `00eec57`..`e9f29c9`)
- **Autor:** `@julio7879`.
- **Cambios principales:**
  - Configuración de `.github/` (`CODEOWNERS`, `pull_request_template.md`, `ISSUE_TEMPLATE/*`), rulesets de protección para `main` y `develop`, y `.gitignore`.
  - Creación de la bóveda Obsidian en `docs/vault/` con `00-Documento-Maestro-v1.3.md` (35 RF, 22 RN, 25 RNF, 16 HU), `00-MOC/`, `01-Modulos/`, `02-Arquitectura/` (incluidos diagramas `.mmd`), `03-Gestion/` y `04-Historias/`.
  - Creación de scripts `scripts/sync-vault-from-master.ps1` y `scripts/check-traceability.ps1`.
  - Configuración inicial de `.agents/` (9 fichas especialistas por módulo y skills `carmotors-qmd-consultant` y `carmotors-rules-validator`).
