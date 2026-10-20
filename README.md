# Security Shift Manager - Backend

API REST para gestionar los horarios de los guardias de seguridad y los servicios (barrios, edificios, etc.) donde trabajan.

Permite responder preguntas como:

- ¿Qué guardia estuvo el 12 de octubre a las 16 hs en el Barrio Belgrano?
- ¿Cuántas horas trabajó cada guardia en el mes?
- ¿Qué turnos tiene un servicio en un día determinado?

La app mobile está en [security-shift-manager-mobile-azure](https://github.com/HerreroT/security-shift-manager-mobile-azure).

## Despliegue en Azure

El backend corre en **Azure Container Apps** con PostgreSQL 18 (Azure Database for PostgreSQL) y las imágenes versionadas en **Azure Container Registry**:

- API: https://ca-shiftmanager-backend.lemonground-a6322200.chilecentral.azurecontainerapps.io (versión desplegada en `/actuator/info`)
- CI/CD con GitHub Actions: un push a `dev` o `main` publica la imagen sin desplegar; un push a `deploy` la despliega; también se puede desplegar o volver a una versión anterior a mano ([docs/ci-cd.md](docs/ci-cd.md)).

| Documento | Contenido |
|---|---|
| [docs/azure.md](docs/azure.md) | Recursos, costos, creación por Portal y CLI, Service Principal, secretos, despliegue manual |
| [docs/ci-cd.md](docs/ci-cd.md) | Pipeline, condiciones de despliegue, versionado, rollback y guía de GitHub |
| [docs/docker.md](docs/docker.md) | Dockerfile multietapa y Docker Compose |
| [docs/seguridad.md](docs/seguridad.md) | JWT, roles, análisis de vulnerabilidades con Grype |

## Tecnologías

- Java 21 + Spring Boot 3.5
- Spring Data JPA + PostgreSQL (H2 en memoria para desarrollo y tests)
- Flyway para las migraciones de la base
- Spring Security con JWT
- Bean Validation
- springdoc-openapi (Swagger UI)

## Cómo correrlo

```bash
cd shiftmanager
./gradlew bootRun        # Windows: gradlew.bat bootRun
```

El servidor queda en `http://localhost:8090`. El puerto se cambia con la variable de entorno `PORT`. No usamos 8080 porque en Windows suele estar ocupado, y Expo usa el 8081.

Al arrancar se cargan:

- Un usuario administrador: `admin@shiftmanager.com` / `admin123`
- Datos de ejemplo: 8 guardias, 4 servicios y turnos del mes anterior y del actual, incluidos turnos nocturnos. Se desactivan con `SEED_DEMO_DATA=false`.

Por defecto la base es H2 en memoria, así que no hace falta instalar nada, pero los datos se reinician cada vez que se reinicia el servidor.

### Con PostgreSQL

El esquema lo crean las migraciones de Flyway (`src/main/resources/db/migration`), tanto en H2 como en PostgreSQL. Para usar PostgreSQL, activá el perfil `postgres` e indicá la conexión:

```bash
# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE="postgres"
$env:DB_URL="jdbc:postgresql://localhost:5432/shiftmanager"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="tu-password"
./gradlew bootRun
```

La base `shiftmanager` tiene que existir: `CREATE DATABASE shiftmanager;`.

> En Windows, PostgreSQL 18 rechaza la zona horaria que Java toma del sistema (`America/Buenos_Aires`). Si al conectar aparece `invalid value for parameter "TimeZone"`, arrancá con `-Duser.timezone=America/Argentina/Buenos_Aires` (la imagen Docker ya la trae configurada).

### Perfiles

| Perfil | Base de datos | Secretos | Uso |
|---|---|---|---|
| (ninguno) | H2 en memoria | Valores de desarrollo | Correr y probar sin instalar nada |
| `postgres` | PostgreSQL | Valores de desarrollo si faltan | PostgreSQL local |
| `prod` | PostgreSQL | **Obligatorios**: si falta `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL` o `ADMIN_PASSWORD`, la app no arranca | Docker y Azure |

### Variables de entorno

| Variable | Default | Uso |
|---|---|---|
| `PORT` | `8090` | Puerto HTTP |
| `SPRING_PROFILES_ACTIVE` | (vacío = H2) | `postgres` o `prod` |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | `localhost:5432/shiftmanager` (sin default en `prod`) | Conexión a PostgreSQL |
| `DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE` | `5` / `2` | Tamaño del pool de conexiones HikariCP (ver `application.properties`) |
| `JWT_SECRET` | secreto de desarrollo (sin default en `prod`) | Firma de los tokens (mínimo 32 caracteres) |
| `JWT_EXPIRATION_MS` | `86400000` (24 h) | Duración del token |
| `APP_VERSION`, `GIT_COMMIT` | `dev` / `unknown` | Versión y commit que muestra `/actuator/info` (los carga la imagen Docker) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | `admin@shiftmanager.com` / `admin123` | Primer administrador; se crea si no existe ningún usuario ADMIN |
| `SEED_DEMO_DATA` | `true` | Carga datos de ejemplo si no hay guardias |
| `CORS_ALLOWED_ORIGINS` | `*` | Orígenes permitidos por CORS |

## Deploy con Docker

La rama `deploy` es la que se despliega. Con Docker:

```bash
cp .env.example .env      # cambiar DB_PASSWORD, JWT_SECRET y ADMIN_PASSWORD
docker compose up -d --build
```

Esto levanta PostgreSQL 18 con un volumen persistente y el backend en el puerto 8090. Flyway crea las tablas en el primer arranque.

La imagen es distroless (sin shell, usuario sin privilegios) y lleva etiquetas OCI con la versión y el commit. Detalle del Dockerfile, comandos para detener, reconstruir, ver logs y borrar sin perder datos: [docs/docker.md](docs/docker.md).

### Conectarse desde la app mobile (Expo Go)

El servidor escucha en todas las interfaces de red (`0.0.0.0`). Para que la app mobile se conecte:

| Dónde corre la app | URL de la API |
|---|---|
| Celular con Expo Go (misma red WiFi que la PC) | `http://<IP-de-la-PC>:8090/api` |
| Emulador Android | `http://10.0.2.2:8090/api` |
| Simulador iOS / navegador | `http://localhost:8090/api` |

La IP de la PC se ve con `ipconfig` en Windows (dato "Dirección IPv4"). Si el celular no llega, revisá que el Firewall de Windows permita conexiones entrantes al puerto 8090 en redes privadas.

## Documentación de la API

Con el servidor corriendo, la documentación está en **http://localhost:8090/swagger-ui.html**.

1. Hacé login con `POST /api/auth/login`.
2. Copiá el `token` de la respuesta.
3. Pegalo en el botón **Authorize**.

Todos los endpoints de `/api/**` (salvo `/api/auth/**`) requieren el header `Authorization: Bearer <token>`.

### Salud y métricas (Spring Boot Actuator)

| Endpoint | Acceso | Para qué |
|---|---|---|
| `GET /actuator/health` | Público | Estado general (`UP` / `DOWN`); con token de ADMIN muestra el detalle (base de datos, disco) |
| `GET /actuator/health/liveness` | Público | ¿El proceso está vivo? (probe de liveness) |
| `GET /actuator/health/readiness` | Público | ¿Puede atender tráfico? Incluye la conexión a la base (probe de readiness) |
| `GET /actuator/info` | Público | Versión y commit de la imagen que está corriendo |
| `GET /actuator/metrics/**` | ADMIN | Métricas: `http.server.requests`, `hikaricp.connections.*`, `jvm.*` |

### Endpoints principales

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/auth/login` | Login, devuelve el JWT |
| GET | `/api/auth/me` | Usuario dueño del token (sirve para verificar que el token sigue siendo válido) |
| GET | `/api/guards?search=&active=` | Listado y búsqueda de guardias (nombre, apellido o DNI) |
| GET/POST/PUT | `/api/guards`, `/api/guards/{id}` | Alta, consulta y edición de guardias |
| DELETE / PATCH | `/api/guards/{id}`, `/api/guards/{id}/activate` | Baja lógica y reactivación |
| GET | `/api/guards/{id}/hours?from=2026-10-01&to=2026-10-31` | Horas trabajadas por el guardia en el rango |
| GET | `/api/services?search=&active=` | Listado y búsqueda de servicios (nombre o ubicación) |
| GET/POST/PUT | `/api/services`, `/api/services/{id}` | Alta, consulta y edición de servicios |
| DELETE / PATCH | `/api/services/{id}`, `/api/services/{id}/activate` | Baja lógica y reactivación |
| GET | `/api/services/{id}/on-duty?at=2026-10-12T16:00` | Qué guardia estaba en el servicio en ese momento |
| GET | `/api/shift-assignments?guardId=&serviceId=&from=&to=` | Turnos con filtros |
| GET/POST/PUT/DELETE | `/api/shift-assignments`, `/api/shift-assignments/{id}` | ABM de turnos |
| POST | `/api/shift-assignments/{id}/check-in`, `/check-out` | Registro del horario real |
| GET | `/api/reports/monthly-hours?month=2026-10` | Horas del mes por guardia |
| GET | `/api/reports/monthly-hours.csv?month=2026-10` | El mismo reporte en CSV (para Excel) |
| GET/POST | `/api/users` | Listado y alta de usuarios (solo ADMIN) |
| DELETE / PATCH | `/api/users/{id}`, `/api/users/{id}/activate` | Desactivar / reactivar usuarios (solo ADMIN) |

### Roles

| Rol | Permisos |
|---|---|
| `ADMIN` | Todo: guardias, servicios, turnos, check-in/out y usuarios |
| `SUPERVISOR` | Solo lectura: consulta guardias, servicios, turnos y reportes. Cualquier cambio devuelve 403 |

Con los datos de ejemplo se crea también un supervisor: `supervisor@shiftmanager.com` / `supervisor123`.

### Ejemplo: ¿quién estuvo en el Barrio Belgrano el 12/10 a las 16 hs?

```bash
TOKEN=$(curl -s -X POST localhost:8090/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@shiftmanager.com","password":"admin123"}' | jq -r .token)

curl -s "localhost:8090/api/services?search=belgrano" -H "Authorization: Bearer $TOKEN"
curl -s "localhost:8090/api/services/1/on-duty?at=2026-10-12T16:00" -H "Authorization: Bearer $TOKEN"
```

### Errores

Todos los errores tienen el mismo formato:

```json
{
  "timestamp": "2026-10-19T19:29:58Z",
  "status": 409,
  "error": "Conflict",
  "message": "Guard already has shift 194 at Barrio Belgrano from 12/10/2026 08:00 to 12/10/2026 20:00"
}
```

Los errores de validación (400) incluyen además `fields` con el error de cada campo.

## Reglas de negocio

Ver [DOMAIN_MODEL.md](shiftmanager/DOMAIN_MODEL.md). En resumen:

- Un turno tiene fecha y hora de inicio y de fin, así que puede cruzar la medianoche (por ejemplo, de 20:00 a 08:00).
- Las horas (`totalHours`) las calcula el servidor.
- Un guardia no puede tener turnos superpuestos.
- Los guardias y servicios no se borran, se desactivan, para no perder el historial.

## Tests

```bash
cd shiftmanager
./gradlew test
```

La colección de Postman con las pruebas de JWT, roles, salud y métricas está en [docs/postman](docs/postman). El detalle de la seguridad (JWT, roles, anotaciones y pruebas) está en [docs/seguridad.md](docs/seguridad.md).
