-- ============================================================
--  SIGECIN - Sistema de Gestión de Citas para Negocios Locales
--  Script DDL + datos de prueba (MariaDB / MySQL)
-- ============================================================

-- El script está en UTF-8. Se declara explícitamente porque el cliente de
-- MariaDB 10.x usa latin1 por defecto: sin esta línea los acentos de los datos
-- de prueba (Barbería, Clínica…) se guardarían con doble codificación.
SET NAMES utf8mb4;

DROP DATABASE IF EXISTS sigecin;

CREATE DATABASE sigecin
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE sigecin;


-- ============================================================
--  MÓDULO: Usuarios y Seguridad
-- ============================================================

CREATE TABLE role (
    id    TINYINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    name  VARCHAR(50)       NOT NULL,
    CONSTRAINT pk_role      PRIMARY KEY (id),
    CONSTRAINT uq_role_name UNIQUE (name)
) ENGINE=InnoDB;

-- Códigos internos de rol (no se muestran al usuario)
INSERT INTO role (name) VALUES
    ('CLIENT'),
    ('BUSINESS');


CREATE TABLE user (
    id          BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    role_id     TINYINT UNSIGNED  NOT NULL,
    full_name   VARCHAR(120)      NOT NULL,
    email       VARCHAR(150)      NOT NULL,
    password    VARCHAR(255)      NOT NULL,   -- hash bcrypt, nunca texto plano
    active      TINYINT(1)        NOT NULL DEFAULT 1,
    created_at  DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_user       PRIMARY KEY (id),
    CONSTRAINT uq_user_email UNIQUE (email),
    CONSTRAINT fk_user_role  FOREIGN KEY (role_id) REFERENCES role (id)
) ENGINE=InnoDB;


-- Refresh tokens de las sesiones (el access token JWT dura poco y no se guarda).
-- Solo se almacena el hash SHA-256 del token; el valor en claro vive únicamente en
-- la cookie HttpOnly del navegador. Cada uso rota el token: el anterior queda revocado
-- y apunta a su reemplazo. Todos los tokens de un mismo inicio de sesión comparten
-- family_id; si se presenta un token ya rotado (posible robo), se revoca la familia.
CREATE TABLE refresh_token (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED  NOT NULL,
    family_id       CHAR(36)         NOT NULL,   -- UUID del inicio de sesión
    token_hash      CHAR(64)         NOT NULL,   -- SHA-256 en hexadecimal
    expires_at      DATETIME         NOT NULL,
    revoked_at      DATETIME             NULL,   -- NULL mientras el token sea usable
    replaced_by_id  BIGINT UNSIGNED      NULL,   -- token que lo reemplazó al rotar (NULL si se revocó por logout o robo)
    created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_refresh_token             PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token_hash        UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_user        FOREIGN KEY (user_id)        REFERENCES user (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_token_replaced_by FOREIGN KEY (replaced_by_id) REFERENCES refresh_token (id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_refresh_token_family  ON refresh_token (family_id);
CREATE INDEX idx_refresh_token_expires ON refresh_token (expires_at);


-- ============================================================
--  MÓDULO: Gestión de Negocios
-- ============================================================

CREATE TABLE business_category (
    id    SMALLINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    name  VARCHAR(80)        NOT NULL,   -- se muestra al usuario, por eso va en español
    CONSTRAINT pk_business_category      PRIMARY KEY (id),
    CONSTRAINT uq_business_category_name UNIQUE (name)
) ENGINE=InnoDB;

INSERT INTO business_category (name) VALUES
    ('Barbería'),
    ('Salón de belleza'),
    ('Consultorio médico'),
    ('Clínica dental'),
    ('Spa'),
    ('Otro');


CREATE TABLE business (
    id           BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    user_id      BIGINT UNSIGNED   NOT NULL,   -- propietario (rol BUSINESS)
    category_id  SMALLINT UNSIGNED NOT NULL,
    name         VARCHAR(150)      NOT NULL,
    description  TEXT,
    phone        VARCHAR(20),
    address      VARCHAR(255),
    active       TINYINT(1)        NOT NULL DEFAULT 1,
    created_at   DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_business          PRIMARY KEY (id),
    CONSTRAINT uq_business_user     UNIQUE (user_id),   -- cada propietario tiene un solo negocio
    CONSTRAINT fk_business_user     FOREIGN KEY (user_id)     REFERENCES user (id),
    CONSTRAINT fk_business_category FOREIGN KEY (category_id) REFERENCES business_category (id)
) ENGINE=InnoDB;


CREATE TABLE business_schedule (
    id           BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    business_id  BIGINT UNSIGNED  NOT NULL,
    day_of_week  TINYINT UNSIGNED NOT NULL,   -- 1=lunes ... 7=domingo (ISO 8601)
    open_time    TIME             NOT NULL,
    close_time   TIME             NOT NULL,
    active       TINYINT(1)       NOT NULL DEFAULT 1,
    CONSTRAINT pk_business_schedule     PRIMARY KEY (id),
    CONSTRAINT fk_business_schedule     FOREIGN KEY (business_id) REFERENCES business (id),
    CONSTRAINT uq_business_schedule_day UNIQUE (business_id, day_of_week),
    CONSTRAINT ck_schedule_time_range   CHECK (close_time > open_time),
    CONSTRAINT ck_schedule_day_of_week  CHECK (day_of_week BETWEEN 1 AND 7)
) ENGINE=InnoDB;


-- Relación muchos a muchos: un cliente puede tener muchos negocios favoritos
-- y un negocio puede ser favorito de muchos clientes.
-- La PK compuesta evita duplicados. Que user_id tenga rol CLIENT se valida
-- en la capa de servicio, ya que un CHECK no puede consultar otra tabla.
CREATE TABLE client_favorite_business (
    user_id      BIGINT UNSIGNED NOT NULL,   -- usuario con rol CLIENT
    business_id  BIGINT UNSIGNED NOT NULL,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_client_favorite_business PRIMARY KEY (user_id, business_id),
    CONSTRAINT fk_favorite_user     FOREIGN KEY (user_id)     REFERENCES user (id)     ON DELETE CASCADE,
    CONSTRAINT fk_favorite_business FOREIGN KEY (business_id) REFERENCES business (id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- ============================================================
--  MÓDULO: Servicios
-- ============================================================

CREATE TABLE service_status (
    id    TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name  VARCHAR(50)      NOT NULL,
    CONSTRAINT pk_service_status      PRIMARY KEY (id),
    CONSTRAINT uq_service_status_name UNIQUE (name)
) ENGINE=InnoDB;

-- 1 = ACTIVE, 2 = INACTIVE
INSERT INTO service_status (name) VALUES
    ('ACTIVE'),
    ('INACTIVE');


CREATE TABLE service (
    id            BIGINT UNSIGNED           NOT NULL AUTO_INCREMENT,
    business_id   BIGINT UNSIGNED           NOT NULL,
    name          VARCHAR(120)              NOT NULL,
    description   VARCHAR(255),
    duration_min  SMALLINT UNSIGNED         NOT NULL,   -- duración en minutos
    price         DECIMAL(10,2)             NOT NULL,
    status_id     TINYINT UNSIGNED          NOT NULL DEFAULT 1,
    created_at    DATETIME                  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME                  NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                     ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_service          PRIMARY KEY (id),
    CONSTRAINT fk_service_business FOREIGN KEY (business_id) REFERENCES business (id),
    CONSTRAINT fk_service_status   FOREIGN KEY (status_id)   REFERENCES service_status (id),
    CONSTRAINT ck_service_price    CHECK (price >= 0),
    CONSTRAINT ck_service_duration CHECK (duration_min > 0)
) ENGINE=InnoDB;


-- ============================================================
--  MÓDULO: Citas
-- ============================================================

CREATE TABLE appointment_status (
    id    TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name  VARCHAR(50)      NOT NULL,
    CONSTRAINT pk_appointment_status      PRIMARY KEY (id),
    CONSTRAINT uq_appointment_status_name UNIQUE (name)
) ENGINE=InnoDB;

-- 1 = PENDING, 2 = CONFIRMED, 3 = CANCELLED
INSERT INTO appointment_status (name) VALUES
    ('PENDING'),
    ('CONFIRMED'),
    ('CANCELLED');


-- Actor que puede cancelar una cita
CREATE TABLE cancelled_by (
    id    TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name  VARCHAR(20)      NOT NULL,
    CONSTRAINT pk_cancelled_by      PRIMARY KEY (id),
    CONSTRAINT uq_cancelled_by_name UNIQUE (name)
) ENGINE=InnoDB;

-- 1 = CLIENT, 2 = BUSINESS (el dueño del negocio),
-- 3 = SYSTEM (cancelaciones automáticas de la aplicación; no se ofrece en formularios)
INSERT INTO cancelled_by (name) VALUES
    ('CLIENT'),
    ('BUSINESS'),
    ('SYSTEM');


CREATE TABLE appointment_cancellation_reason (
    id               TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name             VARCHAR(100)     NOT NULL,   -- se muestra al usuario, por eso va en español
    cancelled_by_id  TINYINT UNSIGNED NOT NULL,
    CONSTRAINT pk_appointment_cancellation_reason      PRIMARY KEY (id),
    CONSTRAINT uq_appointment_cancellation_reason_name UNIQUE (name),
    CONSTRAINT fk_cancellation_reason_cancelled_by
        FOREIGN KEY (cancelled_by_id) REFERENCES cancelled_by (id)
) ENGINE=InnoDB;

INSERT INTO appointment_cancellation_reason (name, cancelled_by_id) VALUES
    ('El cliente no pudo asistir',          1),
    ('Problemas de salud',                  1),
    ('Cambio de planes',                    1),
    ('Ubicación muy lejana',                1),
    ('No se encontró el negocio',           2),
    ('Conflicto de horario del negocio',    2),
    ('Cancelada por el dueño del negocio',  2),
    ('Cancelada automáticamente: no fue confirmada a tiempo', 3);


CREATE TABLE appointment (
    id                    BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    client_id             BIGINT UNSIGNED  NOT NULL,   -- usuario con rol CLIENT
    service_id            BIGINT UNSIGNED  NOT NULL,
    start_time            DATETIME         NOT NULL,
    end_time              DATETIME         NOT NULL,   -- calculado: start_time + duration_min
    price_at_booking      DECIMAL(10,2)    NOT NULL,   -- precio del servicio al reservar (histórico estable)
    status_id             TINYINT UNSIGNED NOT NULL DEFAULT 1,
    client_notes          VARCHAR(500),
    cancelled_reason_id   TINYINT UNSIGNED     NULL,   -- NULL cuando la cita no está cancelada
    created_at            DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                    ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_appointment                   PRIMARY KEY (id),
    CONSTRAINT fk_appointment_client            FOREIGN KEY (client_id)           REFERENCES user (id),
    CONSTRAINT fk_appointment_service           FOREIGN KEY (service_id)          REFERENCES service (id),
    CONSTRAINT fk_appointment_status            FOREIGN KEY (status_id)           REFERENCES appointment_status (id),
    CONSTRAINT fk_appointment_cancelled_reason  FOREIGN KEY (cancelled_reason_id) REFERENCES appointment_cancellation_reason (id),
    CONSTRAINT ck_appointment_time              CHECK (end_time > start_time),
    CONSTRAINT ck_appointment_price             CHECK (price_at_booking >= 0)
) ENGINE=InnoDB;

-- Índice para las consultas de disponibilidad (service -> business)
CREATE INDEX idx_appointment_service_schedule
    ON appointment (service_id, start_time, end_time);

CREATE INDEX idx_appointment_client
    ON appointment (client_id);

CREATE INDEX idx_appointment_status
    ON appointment (status_id);


-- ============================================================
--  PROCEDIMIENTOS ALMACENADOS
-- ============================================================

DELIMITER $$

-- Verifica si un rango de tiempo está libre para un negocio.
-- Ignora las citas canceladas (status_id = 3).
-- OJO: por sí sola NO es segura ante reservas simultáneas. La creación de citas
-- se hace en la capa de servicio (Spring), que bloquea la fila del negocio con
-- @Lock(PESSIMISTIC_WRITE) dentro de una transacción antes de verificar.
CREATE PROCEDURE sp_check_availability(
    IN  p_business_id  BIGINT UNSIGNED,
    IN  p_start_time   DATETIME,
    IN  p_end_time     DATETIME,
    OUT p_available    TINYINT   -- 1 = disponible, 0 = conflicto
)
BEGIN
    DECLARE v_conflicts INT DEFAULT 0;

    SELECT COUNT(*) INTO v_conflicts
    FROM appointment a
    JOIN service s ON s.id = a.service_id
    WHERE s.business_id = p_business_id
      AND a.status_id  != 3               -- 3 = CANCELLED
      AND a.start_time  < p_end_time
      AND a.end_time    > p_start_time;

    IF v_conflicts = 0 THEN
        SET p_available = 1;
    ELSE
        SET p_available = 0;
    END IF;
END$$


DELIMITER ;


-- ============================================================
--  VISTAS: calendario, dashboard y reporte mensual
-- ============================================================
-- Las vistas no reciben parámetros: exponen business_id (y año/mes cuando
-- aplica) para que la capa de servicio filtre con WHERE.
-- Reglas comunes:
--   * "Reserva" = cita no cancelada (PENDING o CONFIRMED).
--   * "Ingreso estimado" = suma del precio de las citas CONFIRMED.
--   * Los meses o negocios sin citas no generan fila: la capa de servicio
--     debe tratar la ausencia de fila como cero.
--   * Las fechas "de hoy" y "del mes en curso" usan la zona horaria del
--     servidor de BD; debe coincidir con la de la aplicación (America/Mexico_City).

-- Detalle de cada cita con negocio, cliente, servicio y estado.
-- Base del calendario, de "próximas citas" y de la tabla del reporte PDF.
CREATE VIEW v_appointment_detail AS
SELECT a.id            AS appointment_id,
       b.id            AS business_id,
       b.name          AS business_name,
       a.client_id     AS client_id,
       c.full_name     AS client_name,
       s.id            AS service_id,
       s.name          AS service_name,
       a.price_at_booking AS price,   -- precio congelado al reservar, no el actual del servicio
       s.duration_min  AS duration_min,
       a.start_time    AS start_time,
       a.end_time      AS end_time,
       st.name         AS status,
       a.client_notes  AS client_notes,
       r.name          AS cancellation_reason
  FROM appointment a
  JOIN service            s  ON s.id  = a.service_id
  JOIN business           b  ON b.id  = s.business_id
  JOIN user               c  ON c.id  = a.client_id
  JOIN appointment_status st ON st.id = a.status_id
  LEFT JOIN appointment_cancellation_reason r ON r.id = a.cancelled_reason_id;


-- Cuántos clientes tienen a cada negocio como favorito (0 si ninguno).
CREATE VIEW v_business_favorite_count AS
SELECT b.id              AS business_id,
       COUNT(f.user_id)  AS favorites_count
  FROM business b
  LEFT JOIN client_favorite_business f ON f.business_id = b.id
 GROUP BY b.id;


-- Indicadores del dashboard (una fila por negocio):
-- citas de hoy, ingreso estimado del mes en curso y favoritos.
-- "Próximas citas" y "servicio más popular" se consultan aparte
-- (v_appointment_detail y v_service_ranking_monthly).
CREATE VIEW v_dashboard_summary AS
SELECT b.id AS business_id,
       (SELECT COUNT(*)
          FROM v_appointment_detail d
         WHERE d.business_id = b.id
           AND d.status <> 'CANCELLED'
           AND d.start_time >= CURDATE()
           AND d.start_time <  CURDATE() + INTERVAL 1 DAY)            AS today_appointments,
       (SELECT COALESCE(SUM(d.price), 0)
          FROM v_appointment_detail d
         WHERE d.business_id = b.id
           AND d.status = 'CONFIRMED'
           AND d.start_time >= DATE_FORMAT(CURDATE(), '%Y-%m-01')
           AND d.start_time <  DATE_FORMAT(CURDATE(), '%Y-%m-01') + INTERVAL 1 MONTH)
                                                                      AS month_estimated_income,
       (SELECT fc.favorites_count
          FROM v_business_favorite_count fc
         WHERE fc.business_id = b.id)                                 AS favorites_count
  FROM business b;


-- Resumen ejecutivo mensual (una fila por negocio y mes con citas).
CREATE VIEW v_monthly_summary AS
SELECT business_id,
       YEAR(start_time)                                         AS period_year,
       MONTH(start_time)                                        AS period_month,
       COUNT(*)                                                 AS total_appointments,
       SUM(CASE WHEN status = 'PENDING'   THEN 1 ELSE 0 END)    AS pending_appointments,
       SUM(CASE WHEN status = 'CONFIRMED' THEN 1 ELSE 0 END)    AS confirmed_appointments,
       SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END)    AS cancelled_appointments,
       COALESCE(SUM(CASE WHEN status = 'CONFIRMED' THEN price END), 0) AS estimated_income
  FROM v_appointment_detail
 GROUP BY business_id, YEAR(start_time), MONTH(start_time);


-- Ranking de servicios por número de reservas en cada mes.
-- ranking = 1 es el servicio más popular (puede haber empates).
CREATE VIEW v_service_ranking_monthly AS
SELECT t.business_id,
       t.period_year,
       t.period_month,
       t.service_id,
       t.service_name,
       t.bookings,
       RANK() OVER (PARTITION BY t.business_id, t.period_year, t.period_month
                    ORDER BY t.bookings DESC) AS ranking
  FROM (SELECT business_id,
               YEAR(start_time)   AS period_year,
               MONTH(start_time)  AS period_month,
               service_id,
               service_name,
               COUNT(*)           AS bookings
          FROM v_appointment_detail
         WHERE status <> 'CANCELLED'
         GROUP BY business_id, YEAR(start_time), MONTH(start_time), service_id, service_name) t;


-- Distribución de reservas por semana del mes (gráfica de barras del reporte).
-- Semana 1 = días 1-7, semana 2 = días 8-14, ..., semana 5 = días 29-31.
CREATE VIEW v_weekly_distribution AS
SELECT business_id,
       YEAR(start_time)                          AS period_year,
       MONTH(start_time)                         AS period_month,
       FLOOR((DAYOFMONTH(start_time) - 1) / 7) + 1 AS week_of_month,
       COUNT(*)                                  AS appointments
  FROM v_appointment_detail
 WHERE status <> 'CANCELLED'
 GROUP BY business_id, YEAR(start_time), MONTH(start_time),
          FLOOR((DAYOFMONTH(start_time) - 1) / 7) + 1;


-- ============================================================
--  DATOS DE PRUEBA
-- ============================================================
-- Contraseñas de prueba (solo para desarrollo):
--   usuarios CLIENT   -> pass1234
--   usuarios BUSINESS -> pass5678
-- Los hashes son bcrypt reales (prefijo $2a$, compatible con
-- BCryptPasswordEncoder de Spring Security).

-- Clientes (role_id = 1)
INSERT INTO user (role_id, full_name, email, password, active) VALUES
    (1, 'Carlos Ramírez Torres',  'carlos.ramirez@unam.mx', '$2a$10$siC4QqUj/z.w6gKBUvK.U.F4isjoIu/PmezwYMlYDbS8hjxhm/eJC', 1),
    (1, 'Laura Sánchez Méndez',   'laura.sanchez@unam.mx',  '$2a$10$siC4QqUj/z.w6gKBUvK.U.F4isjoIu/PmezwYMlYDbS8hjxhm/eJC', 1),
    (1, 'Miguel Ángel Flores',    'miguel.flores@unam.mx',  '$2a$10$siC4QqUj/z.w6gKBUvK.U.F4isjoIu/PmezwYMlYDbS8hjxhm/eJC', 1),
    (1, 'Sofía Herrera Guzmán',   'sofia.herrera@unam.mx',  '$2a$10$siC4QqUj/z.w6gKBUvK.U.F4isjoIu/PmezwYMlYDbS8hjxhm/eJC', 0);

-- Propietarios de negocio (role_id = 2)
INSERT INTO user (role_id, full_name, email, password, active) VALUES
    (2, 'Barbería El Estilo',     'barberia.estilo@unam.mx', '$2a$10$8RttrGMPZ/6J.66TDppJ3uCdElWya.P13lY5QNReJH/w3htvcXL6K', 1),
    (2, 'Spa Serenidad',          'spa.serenidad@unam.mx',   '$2a$10$8RttrGMPZ/6J.66TDppJ3uCdElWya.P13lY5QNReJH/w3htvcXL6K', 1),
    (2, 'Clínica Dental Sonrisa', 'clinica.sonrisa@unam.mx', '$2a$10$8RttrGMPZ/6J.66TDppJ3uCdElWya.P13lY5QNReJH/w3htvcXL6K', 1);

-- Negocios (user_id 5, 6 y 7 son usuarios BUSINESS)
INSERT INTO business (user_id, category_id, name, description, phone, address) VALUES
    (5, 1, 'Barbería El Estilo',     'Cortes modernos y clásicos',           '555-1001', 'Av. Reforma 100, CDMX'),
    (6, 5, 'Spa Serenidad',          'Tratamientos de relajación y belleza', '555-2002', 'Calle Paz 45, CDMX'),
    (7, 4, 'Clínica Dental Sonrisa', 'Odontología general y estética',       '555-3003', 'Blvd. Salud 77, CDMX');

-- Horarios (1=lunes ... 7=domingo)
INSERT INTO business_schedule (business_id, day_of_week, open_time, close_time) VALUES
    (1, 1, '09:00', '19:00'),
    (1, 2, '09:00', '19:00'),
    (1, 3, '09:00', '19:00'),
    (1, 4, '09:00', '19:00'),
    (1, 5, '09:00', '19:00'),
    (1, 6, '10:00', '15:00'),
    (2, 1, '10:00', '20:00'),
    (2, 2, '10:00', '20:00'),
    (2, 3, '10:00', '20:00'),
    (2, 4, '10:00', '20:00'),
    (2, 5, '10:00', '20:00'),
    (3, 1, '08:00', '17:00'),
    (3, 2, '08:00', '17:00'),
    (3, 3, '08:00', '17:00'),
    (3, 4, '08:00', '17:00'),
    (3, 5, '08:00', '17:00');

-- Servicios (status_id: 1=ACTIVE, 2=INACTIVE)
INSERT INTO service (business_id, name, description, duration_min, price, status_id) VALUES
    (1, 'Corte clásico',       'Corte tradicional con tijera',        30,   120.00, 1),
    (1, 'Corte + barba',       'Corte y arreglo de barba',            50,   180.00, 1),
    (1, 'Tinte',               'Coloración completa',                 90,   350.00, 1),
    (2, 'Masaje relajante',    'Masaje corporal de 60 minutos',       60,   450.00, 1),
    (2, 'Facial hidratante',   'Limpieza e hidratación facial',       45,   300.00, 1),
    (2, 'Manicure + pedicure', 'Servicio completo de manos y pies',   75,   280.00, 2),
    (3, 'Limpieza dental',     'Limpieza profunda',                   40,   500.00, 1),
    (3, 'Blanqueamiento',      'Blanqueamiento con luz LED',          60,  1200.00, 1);

-- ------------------------------------------------------------
-- Citas históricas (abril 2026)
-- status_id: 1=PENDING, 2=CONFIRMED, 3=CANCELLED
-- Se insertan directo porque son datos históricos. Como la aplicación cancela
-- automáticamente las citas PENDING vencidas, ninguna cita pasada queda pendiente:
-- la cita 2 aparece cancelada por el sistema (motivo 8) y la 4, confirmada.
-- ------------------------------------------------------------
INSERT INTO appointment (client_id, service_id, start_time, end_time, price_at_booking, status_id, client_notes, cancelled_reason_id) VALUES
    (1, 1, '2026-04-21 10:00:00', '2026-04-21 10:30:00', 120.00, 2, 'Primera visita',          NULL),
    (2, 2, '2026-04-21 11:00:00', '2026-04-21 11:50:00', 180.00, 3, NULL,                      8),
    (3, 4, '2026-04-22 15:00:00', '2026-04-22 16:00:00', 450.00, 2, 'Cumpleaños',              NULL),
    (1, 7, '2026-04-23 09:00:00', '2026-04-23 09:40:00', 500.00, 2, 'Traer estudios previos',  NULL),
    (4, 5, '2026-04-24 12:00:00', '2026-04-24 12:45:00', 300.00, 3, NULL,                      1);


-- ------------------------------------------------------------
-- Citas del mes en curso (para demostrar el dashboard y el reporte)
-- Las fechas se calculan a partir de la fecha en que se ejecuta el script, así
-- que el dashboard siempre tiene datos. Todas respetan el horario de cada negocio
-- y no se traslapan. El estado se ajusta según la fecha, como lo haría la aplicación:
--   * una cita PENDING cuya hora ya pasó se inserta como CONFIRMED.
-- Notas:
--   * Se distribuyen en las tres primeras semanas laborales del mes (lunes a viernes)
--     para que la gráfica semanal tenga varias barras.
--   * Las citas de "hoy" solo se insertan en los días en que cada negocio abre.
-- ------------------------------------------------------------
SET @month_start  = DATE_FORMAT(CURDATE(), '%Y-%m-01');
SET @first_monday = DATE_ADD(@month_start, INTERVAL (7 - WEEKDAY(@month_start)) % 7 DAY);

-- Semanas del mes (omite el día de hoy, que se llena en el bloque siguiente)
INSERT INTO appointment (client_id, service_id, start_time, end_time, price_at_booking, status_id, client_notes, cancelled_reason_id)
SELECT t.client_id,
       t.service_id,
       t.start_time,
       DATE_ADD(t.start_time, INTERVAL s.duration_min MINUTE),
       s.price,
       CASE t.wanted
            WHEN 'CANCELLED' THEN 3
            WHEN 'CONFIRMED' THEN 2
            ELSE IF(t.start_time <= NOW(), 2, 1)
       END,
       t.notes,
       t.reason_id
  FROM (
        -- Barbería El Estilo (negocio 1): lunes a viernes 09-19, sábado 10-15
        SELECT 1 AS client_id, 1 AS service_id, TIMESTAMP(@first_monday + INTERVAL 0 DAY,  '10:00:00') AS start_time, 'CONFIRMED' AS wanted, CAST(NULL AS CHAR(500)) AS notes, CAST(NULL AS UNSIGNED) AS reason_id
        UNION ALL SELECT 2, 2, TIMESTAMP(@first_monday + INTERVAL 0 DAY,  '11:00:00'), 'CONFIRMED', NULL, NULL
        UNION ALL SELECT 3, 3, TIMESTAMP(@first_monday + INTERVAL 2 DAY,  '09:00:00'), 'CONFIRMED', 'Cambio de color', NULL
        UNION ALL SELECT 1, 2, TIMESTAMP(@first_monday + INTERVAL 4 DAY,  '16:00:00'), 'CANCELLED', NULL, 3
        UNION ALL SELECT 2, 1, TIMESTAMP(@first_monday + INTERVAL 8 DAY,  '10:00:00'), 'CONFIRMED', NULL, NULL
        UNION ALL SELECT 3, 2, TIMESTAMP(@first_monday + INTERVAL 8 DAY,  '12:00:00'), 'CONFIRMED', NULL, NULL
        UNION ALL SELECT 1, 1, TIMESTAMP(@first_monday + INTERVAL 10 DAY, '17:00:00'), 'CONFIRMED', NULL, NULL
        UNION ALL SELECT 3, 2, TIMESTAMP(@first_monday + INTERVAL 12 DAY, '11:00:00'), 'CANCELLED', NULL, 6
        UNION ALL SELECT 1, 3, TIMESTAMP(@first_monday + INTERVAL 14 DAY, '10:00:00'), 'CONFIRMED', NULL, NULL
        UNION ALL SELECT 2, 1, TIMESTAMP(@first_monday + INTERVAL 14 DAY, '15:00:00'), 'PENDING',   NULL, NULL
        UNION ALL SELECT 3, 2, TIMESTAMP(@first_monday + INTERVAL 16 DAY, '11:00:00'), 'PENDING',   NULL, NULL
        UNION ALL SELECT 2, 1, TIMESTAMP(@first_monday + INTERVAL 18 DAY, '17:00:00'), 'PENDING',   NULL, NULL
        -- Spa Serenidad (negocio 2): lunes a viernes 10-20
        UNION ALL SELECT 2, 4, TIMESTAMP(@first_monday + INTERVAL 1 DAY,  '11:00:00'), 'CONFIRMED', 'Dolor de espalda', NULL
        UNION ALL SELECT 3, 5, TIMESTAMP(@first_monday + INTERVAL 3 DAY,  '16:00:00'), 'CONFIRMED', NULL, NULL
        UNION ALL SELECT 1, 4, TIMESTAMP(@first_monday + INTERVAL 7 DAY,  '12:00:00'), 'CONFIRMED', NULL, NULL
        UNION ALL SELECT 2, 5, TIMESTAMP(@first_monday + INTERVAL 7 DAY,  '17:00:00'), 'CANCELLED', NULL, 1
        UNION ALL SELECT 3, 4, TIMESTAMP(@first_monday + INTERVAL 11 DAY, '18:00:00'), 'PENDING',   NULL, NULL
        UNION ALL SELECT 1, 5, TIMESTAMP(@first_monday + INTERVAL 16 DAY, '15:00:00'), 'PENDING',   NULL, NULL
        UNION ALL SELECT 2, 4, TIMESTAMP(@first_monday + INTERVAL 17 DAY, '11:00:00'), 'PENDING',   NULL, NULL
        -- Clínica Dental Sonrisa (negocio 3): lunes a viernes 08-17
        UNION ALL SELECT 1, 7, TIMESTAMP(@first_monday + INTERVAL 0 DAY,  '09:00:00'), 'CONFIRMED', NULL, NULL
        UNION ALL SELECT 3, 8, TIMESTAMP(@first_monday + INTERVAL 3 DAY,  '10:00:00'), 'CONFIRMED', 'Primera sesión', NULL
        UNION ALL SELECT 2, 7, TIMESTAMP(@first_monday + INTERVAL 9 DAY,  '08:30:00'), 'CONFIRMED', NULL, NULL
        UNION ALL SELECT 1, 8, TIMESTAMP(@first_monday + INTERVAL 11 DAY, '13:00:00'), 'CANCELLED', NULL, 6
        UNION ALL SELECT 2, 8, TIMESTAMP(@first_monday + INTERVAL 15 DAY, '09:00:00'), 'PENDING',   NULL, NULL
        UNION ALL SELECT 3, 7, TIMESTAMP(@first_monday + INTERVAL 17 DAY, '14:00:00'), 'PENDING',   NULL, NULL
       ) t
  JOIN service s ON s.id = t.service_id
 WHERE DATE(t.start_time) <> CURDATE();

-- Citas de hoy (solo en los días en que cada negocio abre)
INSERT INTO appointment (client_id, service_id, start_time, end_time, price_at_booking, status_id, client_notes, cancelled_reason_id)
SELECT t.client_id,
       t.service_id,
       t.start_time,
       DATE_ADD(t.start_time, INTERVAL s.duration_min MINUTE),
       s.price,
       CASE t.wanted
            WHEN 'CONFIRMED' THEN 2
            ELSE IF(t.start_time <= NOW(), 2, 1)
       END,
       NULL,
       NULL
  FROM (
        -- Barbería (abre de lunes a sábado; el horario del sábado llega hasta las 15:00)
        SELECT 1 AS client_id, 1 AS service_id, TIMESTAMP(CURDATE(), '10:30:00') AS start_time, 'CONFIRMED' AS wanted
         WHERE WEEKDAY(CURDATE()) < 6
        UNION ALL SELECT 2, 2, TIMESTAMP(CURDATE(), '12:00:00'), 'PENDING'   WHERE WEEKDAY(CURDATE()) < 6
        UNION ALL SELECT 3, 1, TIMESTAMP(CURDATE(), '13:30:00'), 'PENDING'   WHERE WEEKDAY(CURDATE()) < 6
        -- Spa y clínica (abren de lunes a viernes)
        UNION ALL SELECT 3, 5, TIMESTAMP(CURDATE(), '11:00:00'), 'CONFIRMED' WHERE WEEKDAY(CURDATE()) < 5
        UNION ALL SELECT 1, 4, TIMESTAMP(CURDATE(), '16:00:00'), 'PENDING'   WHERE WEEKDAY(CURDATE()) < 5
        UNION ALL SELECT 2, 7, TIMESTAMP(CURDATE(), '08:30:00'), 'CONFIRMED' WHERE WEEKDAY(CURDATE()) < 5
        UNION ALL SELECT 1, 8, TIMESTAMP(CURDATE(), '11:00:00'), 'PENDING'   WHERE WEEKDAY(CURDATE()) < 5
       ) t
  JOIN service s ON s.id = t.service_id;

-- Favoritos (relación muchos a muchos cliente <-> negocio)
INSERT INTO client_favorite_business (user_id, business_id) VALUES
    (1, 1),  -- Carlos -> Barbería El Estilo
    (1, 3),  -- Carlos -> Clínica Dental Sonrisa
    (2, 1),  -- Laura -> Barbería El Estilo
    (2, 2),  -- Laura -> Spa Serenidad
    (3, 2);  -- Miguel Ángel -> Spa Serenidad
