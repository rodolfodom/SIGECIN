# SIGECIN

Aplicación web monolítica para reservar citas en negocios locales: Spring Boot con renderizado del lado del servidor (Thymeleaf), MariaDB y autenticación con JWT en cookies. Además expone una API REST protegida con JWT (encabezado `Authorization: Bearer`) y refresh token, lista para probarse con Postman.

## Cumplimiento del proyecto final

| Requisito | Dónde se cumple |
|---|---|
| Proyecto Maven con JDK 17 | `pom.xml` (`java.version=17`, wrapper `mvnw`) |
| Logging (SLF4J) | `@Slf4j` de Lombok en servicios, controladores y filtros; configuración `logging.*` en `application.properties` (consola y `logs/sigecin.log`). Ver [Logging](#logging) |
| Lombok | Dependencia en `pom.xml`; `@Getter`/`@Setter` en entidades y formularios, `@RequiredArgsConstructor` para la inyección por constructor, `@Slf4j` para los loggers |
| Archivo de configuración | `src/main/resources/application.properties` |
| Spring Security con JWT y Thymeleaf con cookies | `config/SecurityConfig.java`: dos cadenas de filtros. Las vistas Thymeleaf usan el JWT en la cookie HttpOnly `SIGECIN_TOKEN` (con CSRF); la API `/api/**` usa el JWT en el encabezado `Authorization` |
| JWT con refresh token | Refresh token rotativo (`auth/service/RefreshTokenService.java`): en la web viaja en la cookie `SIGECIN_REFRESH` y se renueva solo (`RefreshTokenFilter`); en la API se usa `POST /api/auth/refresh` |
| Pruebas con Postman | Colección `postman/SIGECIN.postman_collection.json`. Ver [API REST con JWT (Postman)](#api-rest-con-jwt-postman) |
| README con ejecución y manual de uso | Este archivo: [Formas de ejecutar](#formas-de-ejecutar) y [Manual de uso](#manual-de-uso) |
| Entrega sin `target/` | `target/` está en `.gitignore`. Ver [Entrega](#entrega) |

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

## Manual de uso

SIGECIN tiene dos tipos de usuario: el **cliente**, que busca negocios y reserva citas, y el **negocio**, cuyo dueño publica servicios y horario y atiende las citas. Con la aplicación corriendo, abre **http://localhost:8080**.

### 1. Sin sesión: explorar el catálogo

1. La página de inicio redirige a **Negocios** (`/businesses`). Busca por nombre (por ejemplo, "barber") y/o filtra por categoría.
2. Abre un negocio para ver su descripción, teléfono, dirección, horario semanal y servicios con duración y precio.
3. Al pulsar **Reservar** en un servicio se muestra la disponibilidad. Para elegir una hora hay que iniciar sesión como cliente; después del login se vuelve a la misma página.

### 2. Registro e inicio de sesión

1. **Crear cuenta** (`/register`): nombre, correo, contraseña (con confirmación) y rol (**Cliente** o **Negocio**). El correo no puede repetirse.
2. **Iniciar sesión** (`/login`) con el correo y la contraseña, o con uno de los [usuarios de prueba](#usuarios-de-prueba). Una cuenta inactiva no puede entrar.
3. Al entrar, el cliente llega a **Negocios** y el negocio a su **Dashboard**.
4. **La sesión se mantiene sola:** el JWT de acceso dura 15 minutos y se renueva automáticamente con el refresh token (7 días). Si el refresh token vence o se revoca, se pide iniciar sesión otra vez con el aviso "Tu sesión expiró".
5. **Cerrar sesión** (botón de la barra superior) revoca el refresh token y borra las cookies.

### 3. Cliente

1. **Reservar:** en el detalle de un negocio elige un servicio y una fecha. Se muestran las horas libres cada 15 minutos. Elige una hora, escribe notas opcionales y confirma. La cita queda **Pendiente**. Si otra persona tomó la hora un instante antes, se muestra el aviso y se puede elegir otra.
2. **Mis citas** (`/appointments`): lista de citas con filtros por estado y por rango de fechas.
3. **Detalle de una cita:** servicio, negocio, horario, precio y estado. Mientras no haya empezado, la cita se puede **cancelar** eligiendo un motivo.
4. **Favoritos** (`/favorites`): en el detalle de un negocio pulsa "Agregar a favoritos"; desde la lista se puede abrir o quitar.

### 4. Negocio

1. **Alta del negocio:** un usuario recién registrado con rol Negocio es dirigido a capturar los datos de su negocio (nombre, categoría, descripción, teléfono, dirección).
2. **Servicios** (`/business/services`): crea servicios con nombre, duración y precio; edítalos o desactívalos.
3. **Horario** (`/business/schedule`): marca los días que abre con su hora de apertura y cierre.
4. El negocio **aparece en el catálogo** solo cuando está activo y tiene al menos un servicio activo y un día de horario. El perfil indica qué falta.
5. **Dashboard** (`/business/dashboard`): citas de hoy, ingreso estimado del mes, servicio más popular, clientes que lo tienen en favoritos y gráficas de reservas por semana y por servicio.
6. **Calendario** (`/business/appointments`): vistas de semana y de mes. Desde el detalle de una cita se puede **confirmar** (si está pendiente) o **cancelar** con un motivo.
7. **Reportes** (`/business/reports`): elige mes y año y descarga el **PDF** del reporte mensual.
8. **Perfil** (`/business/profile`): edita los datos; **dar de baja** oculta el negocio y cancela sus citas futuras; **reactivar** lo vuelve a publicar (las citas canceladas no se restauran).

### 5. API REST con Postman

Los mismos casos de uso del cliente y del negocio están disponibles como API REST con JWT. El flujo completo está en la sección siguiente.

## API REST con JWT (Postman)

La API vive en `/api/**` y es independiente de las vistas: **no usa cookies ni CSRF**. El JWT de acceso se envía en el encabezado `Authorization: Bearer <accessToken>`, y el refresh token viaja en el cuerpo de las peticiones de `refresh` y `logout`.

### Importar la colección

1. Arranca la aplicación (cualquier [forma de ejecutar](#formas-de-ejecutar)).
2. En Postman: **Import** → selecciona `postman/SIGECIN.postman_collection.json`.
3. La variable de colección `baseUrl` vale `http://localhost:8080`; cámbiala si usas otro puerto.
4. Ejecuta las peticiones en orden (o la colección completa con **Run collection**). Los scripts de la colección guardan solos `accessToken`, `refreshToken`, la primera hora libre (`startTime`) y el id de la cita creada (`appointmentId`). Las peticiones de la carpeta 3 requieren el login como cliente, y las de la carpeta 4 el login como negocio.

### Flujo de autenticación

```
POST /api/auth/login    {email, password}  → 200 {tokenType, accessToken, expiresIn, refreshToken, refreshExpiresIn}
GET  /api/...           Authorization: Bearer <accessToken>
       … el accessToken vence a los 15 min → 401 …
POST /api/auth/refresh  {refreshToken}     → 200 tokens nuevos (el refresh token anterior queda revocado)
POST /api/auth/logout   {refreshToken}     → 204 (el refresh token ya no sirve; refresh → 401)
```

- **Rotación:** cada `refresh` entrega un refresh token nuevo y revoca el anterior. Si se presenta un token ya rotado después de la ventana de gracia (30 s), se considera robado y se revoca toda la sesión.
- **Ventana de gracia:** si dos peticiones simultáneas renuevan con el mismo token, la segunda recibe solo un `accessToken` nuevo (sin `refreshToken`); el token rotado ya llegó en la primera respuesta.
- Los errores se devuelven como JSON `application/problem+json` (`status`, `detail` y, en reglas de negocio, `code`).

### Endpoints

| Método y ruta | Acceso | Descripción |
|---|---|---|
| `POST /api/auth/login` | Público | Credenciales → par de tokens (401 credenciales inválidas, 403 cuenta inactiva) |
| `POST /api/auth/refresh` | Público (con refresh token) | Rota el refresh token y emite otro JWT |
| `POST /api/auth/logout` | Público (con refresh token) | Revoca el refresh token |
| `GET /api/me` | Con token | Usuario del JWT |
| `GET /api/businesses?name=&categoryId=&page=&size=` | Público | Búsqueda de negocios visibles (paginada) |
| `GET /api/businesses/{id}` | Público | Detalle: datos, horario semanal y servicios activos |
| `GET /api/businesses/{id}/availability?serviceId=&date=` | Público | Horas libres de un servicio en una fecha |
| `GET /api/client/appointments?status=&from=&to=&page=` | CLIENT | Mis citas (paginadas) |
| `GET /api/client/appointments/{id}` | CLIENT | Detalle de mi cita |
| `POST /api/client/appointments` | CLIENT | Reservar `{serviceId, startTime, clientNotes}` → 201 (422 si la hora no está disponible) |
| `POST /api/client/appointments/{id}/cancel` | CLIENT | Cancelar `{reasonId}` |
| `GET /api/client/cancellation-reasons` | CLIENT | Motivos de cancelación del cliente |
| `GET /api/business/appointments?from=&to=&status=` | BUSINESS | Citas del negocio en `[from, to)` (por defecto, 7 días desde hoy) |
| `POST /api/business/appointments/{id}/confirm` | BUSINESS | Confirmar una cita pendiente → 204 |
| `POST /api/business/appointments/{id}/cancel` | BUSINESS | Cancelar `{reasonId}` → 204 |
| `GET /api/business/cancellation-reasons` | BUSINESS | Motivos de cancelación del negocio |

Sin token, o con un token inválido o vencido, la API responde **401**. Con un token de otro rol responde **403**, y una cita de otro usuario responde **404**.

### Ejemplo con curl

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"carlos.ramirez@unam.mx","password":"pass1234"}'
# copia el accessToken de la respuesta
curl -s http://localhost:8080/api/client/appointments -H "Authorization: Bearer <accessToken>"
```

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
| `api/web` | API REST: login, Bearer obligatorio, rotación y revocación del refresh token, roles, reserva y cancelación |
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
| `LOG_LEVEL` | `INFO` | Nivel de log del paquete `com.sigecin` (`DEBUG` muestra la renovación de tokens) |
| `LOG_FILE` | `logs/sigecin.log` | Archivo de log |

Las duraciones de los tokens están en `application.properties` (`sigecin.auth.*`): acceso 15 min, refresh 7 días.

## Logging

Se usa **SLF4J** con Logback (incluido en Spring Boot). Las clases declaran el logger con `@Slf4j` de Lombok. Se configura en `application.properties`:

```properties
logging.level.com.sigecin=${LOG_LEVEL:INFO}
logging.file.name=${LOG_FILE:logs/sigecin.log}
logging.logback.rollingpolicy.max-file-size=10MB
logging.logback.rollingpolicy.max-history=7
```

Los mensajes se ven en la consola y en `logs/sigecin.log` (ignorado por git). Qué se registra:

| Nivel | Eventos |
|---|---|
| `INFO` | Registro de usuarios, inicios de sesión (web y API), cierre de sesión, reservas, confirmaciones y cancelaciones, alta, baja y reactivación de negocios, cambios de servicios y horario, reportes PDF generados, tareas programadas |
| `WARN` | Inicios de sesión fallidos o de cuentas inactivas, reutilización de un refresh token (posible robo) |
| `DEBUG` | Emisión, rotación y renovación de tokens (activar con `LOG_LEVEL=DEBUG`) |

Nunca se registran contraseñas ni tokens; los usuarios aparecen por su id.

## Entrega

Para entregar el código en un zip **sin `target/`** (ni `.idea/`, ni logs), genera el archivo desde git; solo incluye los archivos versionados:

```bash
git archive --format=zip --output=../proyecto_final.zip HEAD
```

Otra opción es subir el repositorio a GitHub como **público** y compartir el enlace.

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

Java 17, Spring Boot 3.5 (Web MVC, Thymeleaf, Security, Validation, Data JPA, OAuth2 Resource Server para JWT), Lombok, SLF4J/Logback, MariaDB 10.7.3, Hibernate con `ddl-auto=validate`, iText 9 (PDF), Bootstrap 5.3 (archivos locales en `static/vendor/`), Chart.js 4 (WebJar), JUnit 5 + MockMvc + Testcontainers.

## Estructura del código

Organización por funcionalidad y, dentro de cada módulo, un paquete por tipo de archivo. Ninguna clase vive en la raíz de un módulo.

```
com.sigecin
├── config/            SecurityConfig, JwtConfig, WebConfig, AuthProperties
├── common/            converter/, enums/, exception/, web/ (flash en cookie, redirecciones locales)
├── auth/              registro, login, JWT y refresh tokens
├── api/               API REST con JWT en el encabezado Authorization (dto/, web/)
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
postman/                  colección de Postman de la API REST
```

## Notas técnicas

- **Esquema:** `db/ddl_sigecin.sql` es la fuente de verdad; Hibernate solo valida (`ddl-auto=validate`). Si se cambia el DDL hay que actualizar las entidades. Los `TINYINT`/`SMALLINT UNSIGNED` se mapean con `@JdbcTypeCode` o con convertidores a `Byte`.
- **Sesión sin estado:** JWT de acceso en la cookie `SIGECIN_TOKEN` y refresh token rotativo en `SIGECIN_REFRESH` (en la BD solo su hash). El token CSRF y los mensajes flash también viajan en cookies: no se crea `HttpSession`.
- **Dos cadenas de seguridad:** `/api/**` (orden 1) es un resource server OAuth2 estándar: JWT en `Authorization: Bearer`, sin cookies, sin CSRF y sin sesión. El resto (orden 2) son las vistas con el JWT en cookie. Ambas firman y validan con la misma llave HS256 y comparten `RefreshTokenService`, así que un login web y uno de API son sesiones independientes con las mismas reglas de rotación.
- **CSRF:** el filtro del JWT se registra a mano y no con `oauth2ResourceServer()`, porque ese DSL exime de CSRF a las peticiones con token, y aquí el token va en una cookie.
- **Concurrencia:** `BookingService.book` y `RefreshTokenService.renew` bloquean una fila y luego consultan e insertan. Ambos usan `READ COMMITTED`: con `REPEATABLE READ` la reserva permitía horarios duplicados y la rotación de tokens producía deadlocks. **No cambiar el aislamiento sin volver a correr las pruebas de concurrencia.**
- **Cambios de estado de una cita:** son `UPDATE` condicionados al estado actual, así que dos acciones simultáneas no se pisan.
- **Tarea programada:** `ExpiredAppointmentCanceller` corre al arrancar y cada 5 minutos; supone una sola instancia de la aplicación.
- **Open-in-view desactivado:** las relaciones que usa una plantilla se cargan en el repositorio (`@EntityGraph` o `join fetch`).
- **Zona horaria:** `America/Mexico_City` en la JVM (se fija en `SigecinApplication.main`) y en Hibernate. En MariaDB se usa el **desfase fijo `-06:00`** y no el nombre de la zona: la imagen y los paquetes de MariaDB 10.7.3 traen datos de zonas horarias anteriores a 2022 y aplicarían el horario de verano que México eliminó, lo que desfasaría una hora el "hoy" de las vistas (`CURDATE()`) entre abril y octubre.
