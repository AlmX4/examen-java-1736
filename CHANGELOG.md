# Historial de Cambios — CarMotors SGT (`Proyecto-Java-Grupo4`)

> [!NOTE]
> Este archivo registra de forma cronológica todos los Pull Requests, ramas y cambios técnicos integrados al proyecto.
> **Regla del equipo:** cada vez que se acepte e integre un PR o rama, se debe agregar una entrada aquí. **`README.md` no se modifica en tareas intermedias** hasta la tarea `T8.4` del Sprint 8.

---

## [En curso] — Sprint 2: Entidades dependientes de catálogos (`develop`)

### Pendiente de integración — `[S2 - T2.5] Catálogo de tipos de servicio` (`feature/gestion-servicios` → `develop`, Issue `#24`)
- **Autor:** `@julio7879`
- **Cambios incluidos:**
  - Creación de `ServicioTipo` (`com.carmotors.sgt.model`), `ServicioTipoRepository` (`com.carmotors.sgt.repository`) y `ServicioTipoRepositoryImpl` (`com.carmotors.sgt.repository.impl`) con `PreparedStatement`, `BigDecimal` escala 2 `HALF_UP`, `Types.INTEGER` para `intervalo_meses` opcional y constructor dual con `Connection` compartida (`RNF-07`, `RNF-08`, `RNF-15`).
  - Creación de `ServicioTipoService` (`com.carmotors.sgt.service`) validando nombre único sin distinguir mayúsculas (`DatoDuplicadoException`), existencia de `MantenimientoTipo` y `Especialidad` (`T1.4`), precio no negativo y la regla de dominio Preventivo (`intervaloMeses > 0`) vs. Correctivo (`intervaloMeses == null`).
  - Creación de `ServicioTipoController` y `ServicioTipoView` (`SubmenuVista` conectada en `Main.java`), usando `ConsoleReader`, confirmación `(S/N)` antes de eliminar (`RNF-24`) y manejo limpio de `CarMotorsException` (`RNF-12`).
  - Creación de pruebas unitarias JUnit 5 en `ServicioTipoServiceTest`.

### PR #73 — `feat(tecnicos): implement technician CRUD with JDBC and console submenu (T2.3)` (`feature/gestion-tecnicos` → `develop`, cierra `#22`)
- **Autor:** `@julio7879`
- **Cambios incluidos:**
  - Creación de `Tecnico` (`com.carmotors.sgt.model`), `TecnicoRepository` (`com.carmotors.sgt.repository`) y `TecnicoRepositoryImpl` (`com.carmotors.sgt.repository.impl`) con `PreparedStatement`, retorno `null` controlado y constructor dual con `Connection` compartida (`RNF-07`, `RNF-08`).
  - Creación de `TecnicoService` (`com.carmotors.sgt.service`) con validación de nombre obligatorio, documento único (`DatoDuplicadoException`), especialidad válida contra `EspecialidadRepository` (`T1.4`) y desactivación lógica sin borrado físico (`RF-33`, `RN-17`).
  - Creación de `TecnicoController` y `TecnicoView` (`SubmenuVista` conectada en `Main.java`), usando `ConsoleReader` con confirmación `(S/N)` antes de desactivar y captura segura de `CarMotorsException` (`RNF-12`, `RNF-24`).
  - Creación de pruebas unitarias JUnit 5 en `TecnicoServiceTest`.

---

## [En curso] — Sprint 1: Piezas comunes (`develop`)

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
  - Creación de 4 interfaces de persistencia de solo lectura en `com.carmotors.sgt.repository`: `RepuestoTipoRepository`, `EspecialidadRepository`, `MantenimientoTipoRepository` y `ServicioEstadoRepository` con métodos `listarTodos()` y `buscarPorId(int id)` retornando `null` controlado (sin `Optional`, respetando la regla de no crear paquete `dao`).
  - Creación de 4 implementaciones JDBC en `com.carmotors.sgt.repository.impl`: `RepuestoTipoRepositoryImpl`, `EspecialidadRepositoryImpl`, `MantenimientoTipoRepositoryImpl` y `ServicioEstadoRepositoryImpl` usando `ConnectionFactory.getInstance().getConnection()`, constructor opcional para conexión compartida (`RNF-08`), `PreparedStatement` y envoltorio seguro de `SQLException` en `CarMotorsException` sin `System.exit()`.
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
