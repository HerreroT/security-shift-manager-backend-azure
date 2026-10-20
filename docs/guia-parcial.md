# Guía para el parcial

Cómo explicar y demostrar todo lo implementado. Para el detalle de cada tema: [arquitectura](arquitectura.md), [docker](docker.md), [azure](azure.md), [ci-cd](ci-cd.md), [seguridad](seguridad.md), [monitoreo](monitoreo.md).

## Datos a mano

| Dato | Valor |
|---|---|
| URL del backend | https://ca-shiftmanager-backend.lemonground-a6322200.chilecentral.azurecontainerapps.io |
| Repositorio | https://github.com/HerreroT/security-shift-manager-backend-azure (ramas `dev`, `main`, `deploy`) |
| Suscripción / región | Azure for Students / `chilecentral` |
| Resource group | `rg-shiftmanager` |
| ACR / imagen | `acrshiftmanagerth.azurecr.io/shiftmanager-backend:<sha>` |
| Environment / app | `cae-shiftmanager` / `ca-shiftmanager-backend` |
| Base de datos | `psql-shiftmanager-th` (PostgreSQL 18, base `shiftmanager`) |
| Service Principal | `sp-shiftmanager-github` |
| Contraseña del admin (Postman) | `%USERPROFILE%\.shiftmanager\azure-secrets.env` → `ADMIN_PASSWORD` (nunca en el repo) |

## Antes de la demo (checklist, 10 minutos antes)

```powershell
cd C:\Users\TOMAS\Desktop\security-shift-manager-backend-azure
git checkout dev; git pull
az account show --query name                      # sesión de Azure activa ("Azure for Students")
az postgres flexible-server show -g rg-shiftmanager -n psql-shiftmanager-th --query state   # "Ready" (si dice Stopped: ... start)
python scripts/azure_manager.py scale 1           # evita la espera de ~1 minuto del arranque en frío
python scripts/azure_manager.py status            # Running + URL
curl.exe https://ca-shiftmanager-backend.lemonground-a6322200.chilecentral.azurecontainerapps.io/actuator/health
```

Abrir: Postman (colección `docs/postman`, environment *Azure* con `adminPassword`), la pestaña **Actions** del repo y el Portal en `ca-shiftmanager-backend`. Al terminar: `python scripts/azure_manager.py scale 0`.

## Simulacro de defensa

### Parte 1: "Hacé un cambio, subilo a GitHub, construí una imagen versionada, publicala en ACR, pero no hagas deploy"

```powershell
git checkout dev; git pull
git checkout -b feature/cambio-parcial
# Cambio visible: el mensaje de shiftmanager/src/main/java/com/security/shiftmanager/controller/GreetingController.java
#   "Hello from Security Shift Manager"  ->  "Hello from Security Shift Manager - parcial"
git add shiftmanager/src/main/java/com/security/shiftmanager/controller/GreetingController.java
git commit -m "feat: cambia el saludo para la demo del parcial"
git push -u origin feature/cambio-parcial
gh pr create --base dev --fill
gh pr merge --merge                     # dispara CI/CD en dev = Caso A
git checkout dev; git pull
git rev-parse --short=7 HEAD            # <SHA> = tag de la imagen que va a generar el pipeline
gh run watch                            # (o pestaña Actions) Build y tests ✓, Imagen ✓, Deploy: skipped
```

Mostrar:
1. **Actions** → el run de `dev` → job *Deploy* en gris (*skipped*) → *Summary* → **Imagen publicada** `...:<SHA>`.
2. La imagen en el ACR: `python scripts/azure_manager.py tags` (o Portal → `acrshiftmanagerth` → **Repositories** → `shiftmanager-backend`).
3. Que **no** se desplegó: `/actuator/info` sigue mostrando la versión anterior; `/hello` sigue con el mensaje viejo.

Qué decir: *"El pipeline compiló, corrió los 33 tests, construyó la imagen para linux/amd64 con las etiquetas OCI, la analizó con Grype y la publicó en el ACR con el SHA del commit como tag. No desplegó porque la condición de deploy es llegar a la rama `deploy` o pedirlo a mano."*

### Parte 2: "Ahora desplegá esa misma versión, comprobá que funciona, mostrame los logs y detené la aplicación"

Desplegar **la misma imagen** (sin reconstruir):
- Web: **Actions** → **CI/CD** → **Run workflow** → rama `main` → marcar *Desplegar* → `image_tag` = `<SHA>` → **Run workflow**.
- Terminal: `gh workflow run ci-cd.yml --ref main -f deploy=true -f image_tag=<SHA>` y `gh run watch`.
- (Alternativa manual: `python scripts/azure_manager.py deploy --tag <SHA>`.)

Comprobar:

```powershell
curl.exe https://ca-shiftmanager-backend.lemonground-a6322200.chilecentral.azurecontainerapps.io/actuator/info   # version = <SHA>
curl.exe https://ca-shiftmanager-backend.lemonground-a6322200.chilecentral.azurecontainerapps.io/hello           # mensaje nuevo
python scripts/azure_manager.py revisions      # revisión nueva con la imagen :<SHA> y 100 % del tráfico
```

Más: Postman → *Login correcto* → *Listar guardias* (funciona con la base de Azure).

Logs:

```powershell
python scripts/azure_manager.py logs           # últimas 100 líneas (arranque, ACCESS de cada petición, logins)
```

Portal: app → **Monitoring** → **Log stream**, o **Logs** con la consulta 1 de `docs/monitoreo.md`.

Detener:

```powershell
python scripts/azure_manager.py stop           # Stopped; la URL responde 404
python scripts/azure_manager.py start          # para volver (~80 s hasta responder)
```

Portal: app → **Overview** → **Stop** / **Start**.

---

## Preguntas

Formato: **Técnica** (respuesta precisa) · **Para decir** (versión oral) · **Dónde** · **Demo**.

### Integración y despliegue continuos

**1. ¿Qué es integración continua?**
- **Técnica**: práctica de integrar los cambios al repositorio frecuentemente, con un proceso automático que compila, ejecuta los tests y empaqueta cada cambio para detectar errores enseguida.
- **Para decir**: "cada vez que subimos código, GitHub Actions lo compila, corre los tests y arma la imagen; si algo se rompe, nos enteramos en ese commit".
- **Dónde**: `.github/workflows/ci-cd.yml`, jobs *Build y tests* e *Imagen, Grype y push al ACR*.
- **Demo**: abrir un run en **Actions**.

**2. ¿Qué es despliegue continuo?**
- **Técnica**: publicar automáticamente en el entorno real el artefacto que ya pasó la CI, cuando se cumple una condición.
- **Para decir**: "cuando el código llega a la rama `deploy`, la imagen ya probada se pone en producción sola en Azure".
- **Dónde**: job *Deploy a Azure Container Apps*.
- **Demo**: run de la rama `deploy` (job en verde, *Summary* → **Deploy**).

**3. ¿Qué sucede cuando hacemos push a GitHub?**
- **Técnica**: el evento `push` dispara `ci-cd.yml` (y `codeql.yml` en `dev`/`main`): tests → build linux/amd64 → Grype → login con el SP → push `:<sha>` al ACR → deploy solo si la rama es `deploy`.
- **Para decir**: "se ejecuta el pipeline: prueba, construye, escanea, publica la imagen y, según la rama, despliega".
- **Dónde**: `ci-cd.yml` (comentario inicial con la tabla de casos), `docs/ci-cd.md`.
- **Demo**: `gh run list --limit 5`.

**4. ¿Cómo generamos una imagen sin desplegar?**
- **Técnica**: push a `dev` o `main` (Caso A), o Run workflow con *Desplegar* desmarcado; el job de deploy tiene un `if:` que lo omite.
- **Para decir**: "subiendo a `dev`: el pipeline publica la imagen pero no la despliega".
- **Dónde**: `if:` del job `deploy` en `ci-cd.yml`.
- **Demo**: Parte 1 del simulacro.

**5. ¿Cómo condicionamos el despliegue según la rama, versión o commit?**
- **Técnica**: condición `if:` del job `deploy`: `push` a `refs/heads/deploy`, o `workflow_dispatch` con `inputs.deploy == true` (y opcionalmente `inputs.image_tag` para elegir versión/commit). La variable `CD_ENABLED=false` desactiva todo. Los tags `vX.Y.Z` publican la versión sin desplegar.
- **Para decir**: "despliega solo la rama `deploy` o una ejecución manual donde elijo qué versión; además hay un interruptor para apagar los deploys sin tocar código".
- **Dónde**: `ci-cd.yml`; Settings → Secrets and variables → Actions → Variables → `CD_ENABLED`.
- **Demo**: `gh variable set CD_ENABLED --body false` + Run workflow con deploy → job *skipped*; volver a `true`.

### Imágenes y Azure Container Registry

**6. ¿Dónde queda almacenada la imagen versionada?**
- **Técnica**: en Azure Container Registry `acrshiftmanagerth.azurecr.io`, repositorio `shiftmanager-backend`, con tag = SHA corto del commit (y `vX.Y.Z` en versiones).
- **Para decir**: "en nuestro registro privado de Azure, una imagen por commit".
- **Dónde**: ACR `acrshiftmanagerth`.
- **Demo**: `python scripts/azure_manager.py tags`; Portal → ACR → **Repositories**.

**7. ¿Qué es Azure Container Registry?**
- **Técnica**: registro privado de imágenes de contenedores (compatible con Docker/OCI) administrado por Azure, con autenticación de Entra ID y roles (AcrPush/AcrPull).
- **Para decir**: "es como Docker Hub pero privado y dentro de Azure".
- **Dónde**: `docs/azure.md` → Recursos.
- **Demo**: `az acr show -n acrshiftmanagerth --query "{servidor:loginServer, sku:sku.name}"`.

**8. ¿De qué grupo de recursos depende nuestro ACR?**
- **Técnica**: `rg-shiftmanager`.
- **Para decir**: "del resource group del proyecto, `rg-shiftmanager`, donde están todos los recursos".
- **Demo**: `az acr show -n acrshiftmanagerth --query resourceGroup`.

**9. ¿De qué suscripción depende el grupo de recursos?**
- **Técnica**: "Azure for Students" (directorio `alumno.um.edu.ar`).
- **Para decir**: "de mi suscripción de estudiante, que es la que paga".
- **Demo**: `az group show -n rg-shiftmanager --query id` (empieza con `/subscriptions/<id>`), `az account show --query name`.

### Service Principal y autenticación

**10. ¿Para qué usamos Service Principal?**
- **Técnica**: identidad de aplicación en Entra ID para que GitHub Actions se autentique en Azure sin usar una cuenta personal, con roles limitados.
- **Para decir**: "es un usuario para el pipeline, con permisos solo para subir imágenes y actualizar la app".
- **Dónde**: `sp-shiftmanager-github`; `docs/azure.md` → Service Principal.
- **Demo**: `az role assignment list --assignee <clientId> --all -o table` (AcrPush en el ACR, Reader en el RG, Contributor en la app).

**11. ¿Dónde utilizamos las credenciales del Service Principal?**
- **Técnica**: en el Secret `AZURE_CREDENTIALS` de GitHub (pasos `azure/login` de los jobs *Imagen* y *Deploy*) y, para el script manual con `--sp`, en `%USERPROFILE%\.shiftmanager\azure-sp.env` (fuera del repo).
- **Para decir**: "en los secretos de GitHub; el código nunca las ve".
- **Demo**: `gh secret list` (muestra el nombre, no el valor).

**12. ¿Cómo funciona la autenticación de GitHub Actions contra Azure?**
- **Técnica**: `azure/login` envía clientId + clientSecret + tenantId a Entra ID y obtiene un token; con esa sesión `az acr login` obtiene un token del ACR para `docker push`, y `az containerapp update` usa el rol Contributor sobre la app.
- **Para decir**: "el pipeline inicia sesión con el SP, y con esa sesión entra al registro y actualiza la app". Mejora posible: OIDC (sin secreto permanente).
- **Dónde**: `ci-cd.yml` pasos *Login en Azure* y *Push al ACR*.
- **Demo**: log del paso *Login en Azure* en un run.

**13. ¿Cómo publicamos una imagen versionada?**
- **Técnica**: `docker build -t <acr>/shiftmanager-backend:<sha> --build-arg VERSION/BUILD_DATE/VCS_REF ...`, `az acr login`, `docker push`. Lo hacen el pipeline y `scripts/azure_manager.py build-push`.
- **Para decir**: "construimos con el SHA como tag y la subimos al ACR".
- **Demo**: `python scripts/azure_manager.py build-push` (muestra cada comando).

**14. ¿Cómo comprobamos que la imagen existe en ACR?**
- **Demo / técnica**: `az acr repository show-tags -n acrshiftmanagerth --repository shiftmanager-backend --orderby time_desc -o table`; `az acr repository show -n acrshiftmanagerth --image shiftmanager-backend:<sha>` (digest y fecha); Portal → ACR → Repositories. El job de deploy también lo verifica antes de desplegar.

**15. ¿Cómo sabemos qué versión está ejecutándose en Azure?**
- **Técnica**: `GET /actuator/info` devuelve `app.version` y `app.commit`, que vienen de los build args de la imagen (`APP_VERSION`, `GIT_COMMIT`); además la imagen de la revisión activa.
- **Para decir**: "le preguntamos a la propia app: `/actuator/info` dice qué commit está corriendo".
- **Demo**: `curl.exe <url>/actuator/info`; `python scripts/azure_manager.py status`.

### Administración de Container Apps

**16. ¿Cómo detenemos una aplicación?** `python scripts/azure_manager.py stop` o Portal → **Overview** → **Stop**. Queda `Stopped`, la URL responde 404, no consume cómputo, no se pierde nada. (La CLI actual no trae `az containerapp stop`; el script usa la API REST, igual que el Portal.)

**17. ¿Cómo iniciamos una aplicación?** `python scripts/azure_manager.py start` o **Overview** → **Start**. Responde en ~80 s (arranque del contenedor + Spring Boot).

**18. ¿Cómo eliminamos una aplicación?** `python scripts/azure_manager.py delete` (pide escribir el nombre) o `az containerapp delete -g rg-shiftmanager -n ca-shiftmanager-backend --yes`. Se pierden la URL y la configuración (variables, secretos, probes, roles); no se pierden la base ni las imágenes. Se recrea con `deploy --tag <sha>`. **No hacerlo en la demo salvo que lo pidan.**

**19. ¿Cómo consultamos los logs?** `python scripts/azure_manager.py logs [-f]`; `az containerapp logs show -g rg-shiftmanager -n ca-shiftmanager-backend --tail 100 --follow`; Portal → **Log stream** o **Logs** (KQL, `docs/monitoreo.md`).

**20. ¿Cómo sabemos que una aplicación funciona correctamente?**
- **Técnica**: health (`UP`), readiness con la base (`UP`), probes de Azure pasando (revisión *Healthy*, `RestartCount` 0), y peticiones reales que responden bien (Postman, access log con 200).
- **Para decir**: "no alcanza con que Azure diga Running: la app responde UP, se conecta a la base y las peticiones reales funcionan".
- **Demo**: `/actuator/health`, Postman (health con detalle ADMIN), consulta KQL 1.

**21. ¿Cómo probamos el login mediante Postman?** Importar `docs/postman/shiftmanager.postman_collection.json` y el environment `azure`, completar `adminPassword`; ejecutar *Login correcto* (200, guarda el token y muestra el payload en la consola), *Login incorrecto* (401), *Sin token* (401), *Token alterado* (401), *Usuario autenticado /me* (200). Botón **Run** para ejecutar todo (8/8 aserciones).

**22. ¿Cuál es la URL real donde se ejecuta el backend?** https://ca-shiftmanager-backend.lemonground-a6322200.chilecentral.azurecontainerapps.io (`python scripts/azure_manager.py url`). HTTP redirige a HTTPS.

### JWT y seguridad

**23. ¿Cómo funciona JWT?**
- **Técnica**: al hacer login, el servidor firma con HMAC-SHA (clave `JWT_SECRET`) un token con `sub` (email), `iat` y `exp`. El cliente lo manda en `Authorization: Bearer`. El servidor no guarda sesión: verifica la firma y la expiración en cada petición.
- **Para decir**: "es un pase firmado: dice quién sos y hasta cuándo vale; si alguien lo modifica, la firma no coincide".
- **Dónde**: `security/JwtService.java`, `controller/AuthController.java`.
- **Demo**: Postman *Login correcto* → consola con el payload decodificado.

**24. ¿Cómo se validan los tokens?** `JwtAuthenticationFilter` (un `OncePerRequestFilter`) toma el header, `JwtService.isTokenValid` verifica firma y `exp` con jjwt, carga el usuario con `AppUserDetailsService` y exige que exista y esté activo; recién entonces pone la autenticación en el `SecurityContext`. Probado: firma alterada, otra clave, `alg: none`, vencido y usuario desactivado → 401.

**25. ¿Cómo protegemos los endpoints?** `SecurityFilterChain` en `SecurityConfig`: sin sesión, públicos solo login/health/info/Swagger; `GET /api/**` para ADMIN y SUPERVISOR; escritura y `/api/users/**` solo ADMIN; sin token → 401, sin rol → 403. Demo: Postman carpeta *3. Roles* (supervisor recibe 403 al crear).

**26. ¿Para qué sirve UserDetailsService?** Interfaz de Spring Security para cargar un usuario por su nombre de login. `AppUserDetailsService` lo busca por email en la tabla `users` y lo devuelve como `SecurityUser` (`UserDetails`: hash BCrypt, rol, activo). Lo usan el login (`DaoAuthenticationProvider`) y el filtro JWT.

**27. ¿Para qué sirve HikariCP?** Es el pool de conexiones JDBC de Spring Boot: mantiene conexiones abiertas para reutilizarlas en vez de abrir una por petición (costoso, sobre todo con SSL hacia Azure). Configurado: máximo 5, mínimo 2 ociosas, timeouts de 10 s / 5 min / 25 min y keepalive, porque PostgreSQL B1ms admite 50 conexiones. Antes mantenía 10 abiertas siempre. Dónde: `application.properties`. Demo: `/actuator/metrics/hikaricp.connections.active` (Postman).

### Configuración y base de datos

**28. ¿Dónde almacenamos las variables de entorno y los secretos?**
- Local: `.env` (no versionado; plantilla `.env.example`).
- Azure (app): variables de entorno de la Container App; los sensibles como **secrets** (`db-password`, `jwt-secret`, `admin-password`) referenciados con `secretref:`.
- Pipeline: GitHub **Secrets** (`AZURE_CREDENTIALS`) y **Variables** (nombres de recursos).
- Demo: `az containerapp secret list -g rg-shiftmanager -n ca-shiftmanager-backend --query "[].name"` (solo nombres).

**29. ¿Dónde configuramos la conexión a la base?** `application-prod.properties` (`spring.datasource.url=${DB_URL}`, usuario y contraseña desde variables); en Azure `DB_URL=jdbc:postgresql://psql-shiftmanager-th.postgres.database.azure.com:5432/shiftmanager?sslmode=require`. Pool en `application.properties`. Esquema: migraciones Flyway `db/migration/V1`, `V2`.

**30. ¿Cómo verificamos la conectividad con la base?** `/actuator/health/readiness` (incluye la base); con token ADMIN `/actuator/health` → `components.db` (`PostgreSQL`, `UP`); en los logs de arranque `Database: jdbc:postgresql://...` y Flyway validando 2 migraciones. Si la base se detiene, readiness pasa a 503 y vuelve sola.

### Vulnerabilidades

**31. ¿Qué es un CVE?** Identificador público de una vulnerabilidad conocida (por ejemplo `CVE-2026-65905`, en Tomcat). GitHub usa también ids `GHSA-...`.

**32. ¿Qué es CVSS?** Puntaje de 0 a 10 que mide la gravedad técnica (crítica 9-10, alta 7-8.9, media 4-6.9). No mide el riesgo concreto para nuestra app: una 9.8 en una función que no usamos no es explotable.

**33. ¿Cómo utilizamos Grype?** Localmente `grype docker:shiftmanager-backend:<tag>`; en el pipeline con `anchore/scan-action`: un informe completo (artefacto `grype-report` y *Summary*) y un control que falla si hay altas/críticas **con corrección disponible** (`--only-fixed --fail-on high`).

**34. ¿Qué vulnerabilidades encontramos?** Imagen inicial (Ubuntu + Spring Boot 3.5.13): 236, de ellas 9 críticas y 31 altas (Tomcat, Spring MVC, Jackson, driver PostgreSQL, Go stdlib del binario `pebble`). Informe: `docs/reports/grype-1-inicial.txt`.

**35. ¿Cómo las solucionamos?** Imagen base distroless Debian 13 (236 → 135) y actualizaciones de parche: Spring Boot 3.5.16, Tomcat 10.1.61, Jackson 2.21.7, PostgreSQL 42.7.12, log4j-api 2.25.5, commons-lang3 3.20.0 (135 → 72, 0 corregibles). Tests y Postman después de cada cambio. `docs/seguridad.md`.

**36. ¿Qué hacemos si no tienen solución disponible?** Analizar si son explotables en nuestra app y documentarlas: las 2 críticas de spring-webmvc (XsltView y SSE) no tienen parche en 6.2.x y la app no usa esas funciones (0 usos); las 21 altas de Debian están marcadas *won't fix*. Se reconstruye la imagen periódicamente para tomar los parches cuando salgan; migrar a Spring Boot 4 las eliminaría.

**37. ¿Qué diferencias hay entre Grype y Trivy?** Los dos escanean imágenes. Trivy además analiza IaC, secretos y licencias, y usa la severidad que asigna la distribución (Debian) en lugar del CVSS de NVD; por eso sobre la imagen final dio 64 (4 altas) contra 72 (21 altas) de Grype. Coinciden en las 2 críticas.

**38. ¿Qué es CodeQL?** Análisis estático (SAST) de GitHub: analiza nuestro código fuente buscando patrones de vulnerabilidades (inyección SQL, XSS, crypto débil, inyección en workflows). Resultado con 120 reglas Java y 23 de Actions: 7 alertas; corregidas 6 (contraseña en un log y 5 actions sin fijar a SHA) y descartada 1 con justificación (CSRF no aplica a una API con JWT en header). `codeql.yml`, Security → Code scanning. No bloquea ataques en ejecución.

**39. ¿Qué diferencia hay entre CodeQL y Grype?** CodeQL mira **nuestro código** (errores de programación); Grype mira **los paquetes de la imagen** (CVE conocidos en librerías y sistema operativo). Se complementan.

### Monitoreo

**40. ¿Cómo comprobamos que la aplicación recibe peticiones?** Access log (`ACCESS <ip> "<método> <ruta>" <código> <bytes> <ms>ms`) en Log Analytics (consulta 1) y la métrica `Requests` de Azure Monitor (por código 2xx/4xx/5xx).

**41. ¿Cómo obtenemos métricas del login?** `/actuator/metrics/http.server.requests?tag=uri:/api/auth/login` (cantidad, tiempo total y máximo, separado por status 200/401); logs `Login succeeded` / `Login failed: <motivo>` (consulta KQL 2).

**42. ¿Cómo sabemos cuánto tarda una petición?** Campo `ms` del access log; `ResponseTime` en Azure Monitor; `TOTAL_TIME`/`MAX` en `http.server.requests`. Ejemplo real: `/api/guards` 23 ms promedio, login ~230 ms (BCrypt es lento a propósito).

**43. ¿Qué relación existe entre los logs y los 12 factores?** Factor XI: la app escribe sus logs como flujo de eventos en stdout y no administra archivos; la plataforma (Container Apps) los recolecta y los manda a Log Analytics. Es una parte de la observabilidad: se complementa con métricas y health checks.

**44. ¿Cómo configuramos desarrollo y producción?** Perfiles de Spring: sin perfil = H2 en memoria con valores de desarrollo; `postgres` = PostgreSQL local; `prod` = PostgreSQL con todos los secretos obligatorios por variables de entorno (si falta uno no arranca). Docker y Azure usan `prod` (factor III).

**45. ¿Cómo volveríamos a una imagen anterior?** Run workflow con *Desplegar* + `image_tag` = tag anterior (no reconstruye), o `python scripts/azure_manager.py deploy --tag <tag>`. Probado: rollback `4553c58` → `f2cbce5` y vuelta. Funciona porque cada imagen queda en el ACR con su tag (factor V).

### Docker y mobile

**46. ¿Cómo funciona el Dockerfile multietapa?** Etapa *build* (`eclipse-temurin:21-jdk`): descarga dependencias en una capa cacheable, compila con Gradle y separa el jar en capas. Etapa final (`distroless/java21-debian13:nonroot`): copia solo las capas del jar y el health check; sin JDK, Gradle ni shell; usuario 65532. Resultado: 395 MB contra 589 MB y menos superficie de ataque.

**47. ¿Qué diferencia existe entre Dockerfile y Docker Compose?** El Dockerfile es la receta para construir **una imagen**; Compose describe cómo **ejecutar varios contenedores juntos** (backend + PostgreSQL, variables, red, volumen, orden de arranque y health checks).

**48. ¿Cómo se conecta React Native con Spring Boot en Azure?** La app lee `EXPO_PUBLIC_API_URL` (`.env` o `eas.json`) = `https://.../api`; `lib/api.ts` usa `fetch` con `Authorization: Bearer <token>`; el token se guarda cifrado con `expo-secure-store`; ante un 401 cierra la sesión. El timeout es de 90 s por el arranque en frío. Demo: Expo Go en el iPhone con el `.env` apuntando a Azure.

**49. ¿Cómo comprobamos que el backend usa credenciales reales y no datos hardcodeados?**
- El perfil `prod` no tiene valores por defecto: sin `JWT_SECRET` no arranca (probado: *Could not resolve placeholder 'JWT_SECRET'*).
- En Azure, `admin123` responde 401; solo funciona la contraseña generada.
- La imagen no contiene secretos (`docker image inspect` / `docker history`) y el repo fue escaneado con gitleaks (0 hallazgos).
- Los secrets están en la Container App: `az containerapp secret list ...` muestra los nombres.

**50. ¿Qué haríamos si el pipeline falla durante la construcción, el análisis de seguridad o el despliegue?**
- **Construcción/tests**: leer el paso fallido (`gh run view <id> --log-failed`), reproducir con `./gradlew test` o `docker build`, corregir y volver a subir. Nunca desactivar tests para pasar.
- **Grype**: ver el artefacto `grype-report`, actualizar la dependencia o la imagen base; si no hay parche, evaluarla y documentarla. No se despliega una imagen que no pasó el control.
- **Despliegue**: `python scripts/azure_manager.py logs` y `revisions` (falta de variable, base detenida, permisos del SP). La revisión anterior sigue atendiendo; rollback con Run workflow + `image_tag`. Re-ejecutar con **Re-run failed jobs**.
