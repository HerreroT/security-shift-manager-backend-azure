# Security Shift Manager - Backend

API REST para gestionar los horarios de los guardias de seguridad y los servicios (barrios, edificios, etc.) donde trabajan.

Permite responder preguntas como:

- ¿Qué guardia estuvo el 12 de octubre a las 16 hs en el Barrio Belgrano?
- ¿Cuántas horas trabajó cada guardia en el mes?
- ¿Qué turnos tiene un servicio en un día determinado?

La app mobile está en [security-shift-manager-mobile](https://github.com/HerreroT/security-shift-manager-mobile).

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

### Variables de entorno

| Variable | Default | Uso |
|---|---|---|
| `PORT` | `8090` | Puerto HTTP |
| `SPRING_PROFILES_ACTIVE` | (vacío = H2) | `postgres` para usar PostgreSQL |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | `localhost:5432/shiftmanager` | Conexión a PostgreSQL |
| `JWT_SECRET` | secreto de desarrollo | Firma de los tokens (mínimo 32 caracteres) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | `admin@shiftmanager.com` / `admin123` | Primer administrador; se crea solo si no hay usuarios |
| `SEED_DEMO_DATA` | `true` | Carga datos de ejemplo si no hay guardias |
| `CORS_ALLOWED_ORIGINS` | `*` | Orígenes permitidos por CORS |

## Deploy con Docker

La rama `deploy` es la que se despliega. Con Docker:

```bash
cp .env.example .env      # cambiar DB_PASSWORD, JWT_SECRET y ADMIN_PASSWORD
docker compose up -d --build
```

Esto levanta PostgreSQL 18 con un volumen persistente y el backend en el puerto 8090. Flyway crea las tablas en el primer arranque.

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

### Endpoints principales

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/auth/login` | Login, devuelve el JWT |
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
