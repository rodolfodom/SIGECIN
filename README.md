# SIGECIN

Aplicación web monolítica para reservar citas en negocios locales: Spring Boot con renderizado del lado del servidor (Thymeleaf), MariaDB y autenticación con JWT en cookies.

Funciona en **macOS, Windows y Linux**, en x64 y ARM: es Java puro, sin librerías nativas.

## Requisitos

| Herramienta | Versión | ¿Cuándo se necesita? |
|---|---|---|
| Java (JDK) | **17** | Siempre |
| Docker | Reciente, **corriendo** | Para ejecutar con Docker (opción A) y para las pruebas automatizadas |
| MariaDB | **10.7.3** | Solo para ejecutar sin Docker (opción B) |

- No hace falta instalar Maven: el proyecto incluye el wrapper. En macOS/Linux se usa `./mvnw` y en Windows `mvnw.cmd`.
- El wrapper necesita encontrar el JDK 17: la variable `JAVA_HOME` debe apuntar a él (o `java` debe estar en el `PATH`). Para comprobarlo: `java -version`.
- **Windows:** Docker Desktop requiere WSL 2.
- **Linux:** para usar Docker sin `sudo` (necesario para las pruebas), agrega tu usuario al grupo `docker`: `sudo usermod -aG docker $USER` y vuelve a iniciar sesión.

### Comandos según el sistema

Los ejemplos de este documento se muestran en estas tres variantes:

| | macOS / Linux (bash, zsh) | Windows PowerShell | Windows CMD |
|---|---|---|---|
| Maven | `./mvnw …` | `.\mvnw.cmd …` | `mvnw.cmd …` |
| Variable de entorno | `DB_PASSWORD=secreto ./mvnw …` (en la misma línea) | `$env:DB_PASSWORD="secreto"` (antes del comando) | `set DB_PASSWORD=secreto` (antes del comando) |
| Ruta del script | `db/ddl_sigecin.sql` | `db\ddl_sigecin.sql` | `db\ddl_sigecin.sql` |

## Formas de ejecutar

| Opción | Docker | MariaDB instalada | Datos | Recomendada para |
|---|---|---|---|---|
| **A1.** Arranque rápido | Sí | No | Se recrean en cada arranque | Probar la aplicación |
| **A2.** MariaDB en un contenedor persistente | Sí | No | Se conservan entre arranques | Desarrollo con datos propios |
| **B.** Sin Docker | No | Sí (10.7.3) | Se conservan | Equipos sin Docker |

En todas, la aplicación queda en **http://localhost:8080**.

## Opción A1 — Arranque rápido con Docker

Un solo comando: levanta MariaDB 10.7.3 en un contenedor, carga `db/ddl_sigecin.sql` (esquema y datos de prueba) y arranca la aplicación.

```bash
./mvnw spring-boot:test-run          # macOS / Linux
```
```powershell
.\mvnw.cmd spring-boot:test-run      # Windows PowerShell (en CMD: mvnw.cmd spring-boot:test-run)
```

La base se crea desde cero en cada arranque y desaparece al detener la aplicación (`Ctrl+C`).

## Opción A2 — MariaDB en un contenedor persistente

1. **Crear el contenedor** (una sola vez). `--default-time-zone=-06:00` fija la zona horaria (ver *Notas técnicas*). Los comandos de Docker son iguales en todos los sistemas; en CMD escríbelo en una sola línea, sin `\`:

   ```bash
   docker run -d --name sigecin-db -p 3306:3306 \
     -e MARIADB_ROOT_PASSWORD=secreto \
     mariadb:10.7.3 --default-time-zone=-06:00
   ```

2. **Cargar el esquema y los datos** (también para reiniciar los datos más adelante: el script borra y recrea la base):

   ```bash
   docker exec -i sigecin-db mariadb -uroot -psecreto < db/ddl_sigecin.sql                 # macOS / Linux
   ```
   ```bat
   docker exec -i sigecin-db mariadb -uroot -psecreto < db\ddl_sigecin.sql                 :: Windows CMD
   ```
   ```powershell
   cmd /c "docker exec -i sigecin-db mariadb -uroot -psecreto < db\ddl_sigecin.sql"        # Windows PowerShell
   ```

   En PowerShell se usa `cmd /c` porque PowerShell no admite `<` y, al pasar el archivo con `Get-Content`, puede alterar los acentos.

3. **Arrancar la aplicación:**

   ```bash
   DB_PASSWORD=secreto ./mvnw spring-boot:run                 # macOS / Linux
   ```
   ```powershell
   $env:DB_PASSWORD="secreto"; .\mvnw.cmd spring-boot:run     # Windows PowerShell
   ```
   ```bat
   set DB_PASSWORD=secreto
   mvnw.cmd spring-boot:run
   ```

4. Para detener y reanudar la base: `docker stop sigecin-db` / `docker start sigecin-db`. Para borrarla: `docker rm -f sigecin-db`.

## Opción B — Sin Docker (MariaDB 10.7.3 instalada)

1. **Instalar MariaDB 10.7.3.** La serie 10.7 ya no tiene soporte, así que no está en los repositorios actuales; sus instaladores están en el archivo oficial: <https://archive.mariadb.org/mariadb-10.7.3/>.
   - **Windows:** instalador `.msi` (carpeta `winx64-packages`). Durante la instalación se define la contraseña de `root` y se registra el servicio `MariaDB`.
   - **Linux:** paquetes de la carpeta `repo/` para tu distribución, o el binario genérico (`bintar-linux-*`).
   - **macOS:** Homebrew no ofrece la 10.7; en macOS es más simple usar la opción A.

2. **Fijar la zona horaria** en la configuración del servidor y reiniciarlo:

   ```ini
   [mysqld]
   default-time-zone = '-06:00'
   ```

   | Sistema | Archivo de configuración | Reinicio |
   |---|---|---|
   | Linux (Debian/Ubuntu) | `/etc/mysql/mariadb.conf.d/50-server.cnf` | `sudo systemctl restart mariadb` |
   | Linux (Fedora/RHEL) | `/etc/my.cnf.d/server.cnf` | `sudo systemctl restart mariadb` |
   | Windows | `my.ini` en la carpeta de datos (p. ej. `C:\Program Files\MariaDB 10.7\data\my.ini`) | `net stop MariaDB` y `net start MariaDB` (consola como administrador) |

   Para verificar: `SELECT @@time_zone, NOW();` debe mostrar `-06:00` y la hora de la Ciudad de México.

3. **Cargar el esquema y los datos** con el cliente de línea de comandos (el script usa `DELIMITER`, que solo entiende ese cliente). Si tu instalación no tiene el comando `mariadb`, usa `mysql`:

   ```bash
   mariadb -u root -p < db/ddl_sigecin.sql                    # macOS / Linux
   ```
   ```bat
   mariadb -u root -p < db\ddl_sigecin.sql                    :: Windows CMD
   ```
   ```powershell
   cmd /c "mariadb -u root -p < db\ddl_sigecin.sql"           # Windows PowerShell
   ```

4. **Arrancar la aplicación** con las credenciales (mismos comandos que en el paso 3 de la opción A2). Si la base no está en `localhost:3306`, define también `DB_URL` y, si no usas `root`, `DB_USERNAME`.

### Generar un jar ejecutable

Sirve para cualquier opción en la que la base ya esté corriendo (A2 o B). Las pruebas necesitan Docker, así que **sin Docker hay que empaquetar con `-DskipTests`**:

```bash
./mvnw clean package -DskipTests
DB_PASSWORD=secreto java -jar target/sigecin-0.0.1-SNAPSHOT.jar
```
```powershell
.\mvnw.cmd clean package "-DskipTests"
$env:DB_PASSWORD="secreto"; java -jar target\sigecin-0.0.1-SNAPSHOT.jar
```

En PowerShell, los argumentos `-D…` van entre comillas, porque PowerShell puede partirlos en el punto.

## Usuarios de prueba

| Rol | Correo | Contraseña | Notas |
|---|---|---|---|
| Cliente | `carlos.ramirez@unam.mx` | `pass1234` | Tiene citas y favoritos |
| Cliente | `laura.sanchez@unam.mx` | `pass1234` | |
| Cliente | `miguel.flores@unam.mx` | `pass1234` | |
| Cliente | `sofia.herrera@unam.mx` | `pass1234` | Cuenta **inactiva**: no puede iniciar sesión |
| Negocio | `barberia.estilo@unam.mx` | `pass5678` | Barbería El Estilo |
| Negocio | `spa.serenidad@unam.mx` | `pass5678` | Spa Serenidad (tiene un servicio inactivo) |
| Negocio | `clinica.sonrisa@unam.mx` | `pass5678` | Clínica Dental Sonrisa |

El script genera citas del mes en curso con fechas relativas al día en que se ejecuta, de modo que el dashboard, el calendario y el reporte siempre tienen datos.

### Recorrido sugerido

1. **Sin sesión:** `/businesses` (búsqueda) → detalle de un negocio → "Reservar" → disponibilidad.
2. **Cliente:** reservar una hora → "Mis citas" → detalle → cancelar con un motivo; marcar y quitar favoritos.
3. **Negocio:** dashboard (`/business/dashboard`) → calendario (`/business/appointments`, vistas de semana y mes) → confirmar o cancelar una cita → servicios → horario → perfil (dar de baja y reactivar) → reportes (descargar el PDF del mes).
4. **Negocio nuevo:** registrar un usuario con rol Negocio; cualquier ruta `/business/**` lo lleva a dar de alta su negocio, que no es visible para los clientes hasta tener horario y al menos un servicio activo.

## Pruebas

**Requieren Docker corriendo** en cualquier sistema. Testcontainers levanta **MariaDB 10.7.3**, la versión requerida, con `db/ddl_sigecin.sql`, así que toda la suite (incluidas las pruebas de concurrencia) se valida contra esa versión. No hace falta crear el contenedor a mano.

```bash
./mvnw test                                           # suite completa
./mvnw test -Dtest=BookingConcurrencyTests            # una clase
./mvnw test -Dtest='Business*Tests'                   # varias clases por patrón
./mvnw test -Dtest=AuthFlowTests#logoutClearsCookie   # un método
```
```powershell
.\mvnw.cmd test
.\mvnw.cmd test "-Dtest=BookingConcurrencyTests"
.\mvnw.cmd test "-Dtest=Business*Tests"
```

| Paquete de pruebas | Qué cubre |
|---|---|
| `*/repository` | Mapeo de entidades contra el DDL real y consultas de los repositorios |
| `*/web` | Controladores con MockMvc: validación, flujos, permisos por rol y 404 por pertenencia |
| `auth/security`, `auth/service` | Renovación y rotación de refresh tokens, detección de reutilización |
| `appointment/service` | Concurrencia de reservas y tarea programada de citas vencidas |
| `report/*` | Dashboard y reporte PDF (los esperados se calculan con SQL directo; el PDF se lee con iText) |

Casi todas las clases son `@IntegrationTest` + `@Transactional`: cada prueba se revierte. Las pruebas de **concurrencia** (`BookingConcurrencyTests`, `RefreshTokenConcurrencyTests`) usan transacciones reales en varios hilos y limpian sus datos al terminar; no deben marcarse `@Transactional`, porque así no habría contención de bloqueos.

## Variables de entorno

| Variable | Por defecto | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:mariadb://localhost:3306/sigecin` | URL JDBC |
| `DB_USERNAME` | `root` | Usuario de la BD |
| `DB_PASSWORD` | *(vacío)* | Contraseña de la BD |
| `JWT_SECRET` | Valor solo para desarrollo | Llave HS256 de los JWT; **obligatoria en producción**, mínimo 32 caracteres |
| `COOKIE_SECURE` | `false` | `true` en producción (cookies solo por HTTPS) |
| `SERVER_PORT` | `8080` | Puerto HTTP de la aplicación |

Las duraciones de los tokens están en `application.properties` (`sigecin.auth.*`): acceso 15 min, refresh 7 días.

## Solución de problemas

| Síntoma | Causa y solución |
|---|---|
| Las pruebas o `spring-boot:test-run` fallan con `Previous attempts to find a Docker environment failed` | Docker no está corriendo. Ábrelo y espera a que termine de arrancar. En Linux, revisa los permisos del grupo `docker`. |
| `JAVA_HOME is not defined correctly` o `release version 17 not supported` | El wrapper no encuentra un JDK 17. Instálalo y apunta `JAVA_HOME` a él. |
| `Port 8080 was already in use` | Otra aplicación usa el puerto. Arranca con `SERVER_PORT=8081` (o el equivalente en Windows). |
| `docker run` falla porque el puerto 3306 está ocupado | Ya hay una MariaDB/MySQL local. Usa otro puerto (`-p 3307:3306`) y `DB_URL=jdbc:mariadb://localhost:3307/sigecin`. |
| `Access denied for user 'root'` | La contraseña no coincide: define `DB_PASSWORD` con la de tu servidor o contenedor. |
| Los acentos se ven mal (`BarberÃ­a`) | El script se cargó con un cliente o herramienta que no respetó UTF-8. Vuelve a cargarlo con el cliente de línea de comandos, como se indica arriba. |
| Las "citas de hoy" o el mes del dashboard no coinciden con la hora local | MariaDB no tiene `default-time-zone = '-06:00'`. Revisa con `SELECT @@time_zone, NOW();`. |

## Stack

Java 17, Spring Boot 3.5 (Web MVC, Thymeleaf, Security, Validation, Data JPA, OAuth2 Resource Server para JWT), MariaDB 10.7.3, Hibernate con `ddl-auto=validate`, iText 9 (PDF), Bootstrap 5.3 (archivos locales en `static/vendor/`), Chart.js 4 (WebJar), JUnit 5 + MockMvc + Testcontainers.

## Estructura del código

Organización por funcionalidad y, dentro de cada módulo, un paquete por tipo de archivo. Ninguna clase vive en la raíz de un módulo.

```
com.sigecin
├── config/            SecurityConfig, JwtConfig, WebConfig, AuthProperties
├── common/            converter/, enums/, exception/, web/ (flash en cookie, redirecciones locales)
├── auth/              registro, login, JWT y refresh tokens
├── user/              usuarios y roles
├── business/          negocio, horario, búsqueda pública y favoritos
├── serviceoffering/   servicios del negocio (tabla service)
├── appointment/       disponibilidad, reservas, estados y calendario
└── report/            dashboard y reporte mensual en PDF
```

| Subpaquete | Contenido |
|---|---|
| `entity/` | Entidades JPA |
| `enums/` | Enums de catálogo (`role`, `appointment_status`…) con su `converter/` al id numérico |
| `repository/` | Spring Data JPA; las vistas `v_*` se leen con `JdbcClient` |
| `dto/` | Records entre la capa web y los servicios, y filas de las vistas |
| `service/` | Lógica de negocio (`@Transactional`) |
| `exception/` | Excepciones del módulo |
| `security/` | Filtros y componentes de Spring Security (solo `auth`) |
| `web/`, `web/form/`, `web/view/` | Controladores, formularios (`@Valid`) y modelos de vista |

```
src/main/resources
├── templates/   layout/ (fragmentos comunes), auth/, client/, business/, error/
├── static/      css/sigecin.css, js/forms.js (validación en el navegador), js/dashboard.js, vendor/bootstrap/
└── messages.properties   todos los textos de la interfaz y de validación
db/ddl_sigecin.sql        esquema, vistas y datos de prueba (fuente de verdad del esquema)
```

## Notas técnicas

- **Esquema:** `db/ddl_sigecin.sql` es la fuente de verdad; Hibernate solo valida (`ddl-auto=validate`). Si se cambia el DDL hay que actualizar las entidades. Los `TINYINT`/`SMALLINT UNSIGNED` se mapean con `@JdbcTypeCode` o con convertidores a `Byte`.
- **Sesión sin estado:** JWT de acceso en la cookie `SIGECIN_TOKEN` y refresh token rotativo en `SIGECIN_REFRESH` (en la BD solo su hash). El token CSRF y los mensajes flash también viajan en cookies: no se crea `HttpSession`.
- **CSRF:** el filtro del JWT se registra a mano y no con `oauth2ResourceServer()`, porque ese DSL exime de CSRF a las peticiones con token, y aquí el token va en una cookie.
- **Concurrencia:** `BookingService.book` y `RefreshTokenService.renew` bloquean una fila y luego consultan e insertan. Ambos usan `READ COMMITTED`: con `REPEATABLE READ` la reserva permitía horarios duplicados y la rotación de tokens producía deadlocks. **No cambiar el aislamiento sin volver a correr las pruebas de concurrencia.**
- **Cambios de estado de una cita:** son `UPDATE` condicionados al estado actual, así que dos acciones simultáneas no se pisan.
- **Tarea programada:** `ExpiredAppointmentCanceller` corre al arrancar y cada 5 minutos; supone una sola instancia de la aplicación.
- **Open-in-view desactivado:** las relaciones que usa una plantilla se cargan en el repositorio (`@EntityGraph` o `join fetch`).
- **Zona horaria:** `America/Mexico_City` en la JVM (se fija en `SigecinApplication.main`) y en Hibernate. En MariaDB se usa el **desfase fijo `-06:00`** y no el nombre de la zona: la imagen y los paquetes de MariaDB 10.7.3 traen datos de zonas horarias anteriores a 2022 y aplicarían el horario de verano que México eliminó, lo que desfasaría una hora el "hoy" de las vistas (`CURDATE()`) entre abril y octubre.
