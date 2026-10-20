# Defensa paso a paso

Qué hacer, qué mostrar y qué decir ante cada pedido del profesor. Para la versión técnica de cada respuesta y las 50 preguntas: [guia-parcial.md](guia-parcial.md).

Todos los comandos se ejecutan en PowerShell, en la carpeta del repositorio del backend:

```powershell
cd $HOME\Desktop\security-shift-manager-backend-azure
```

URL del backend: `https://ca-shiftmanager-backend.lemonground-a6322200.chilecentral.azurecontainerapps.io`

---

## 0. Antes de empezar (10 minutos antes)

```powershell
git checkout dev; git pull
az account show --query name                     # "Azure for Students" (si falla: az login)
gh auth status                                   # logueado como HerreroT (si falla: gh auth login)
az postgres flexible-server show -g rg-shiftmanager -n psql-shiftmanager-th --query state   # "Ready"
python scripts/azure_manager.py scale 1          # deja una réplica encendida: no hay espera de arranque
python scripts/azure_manager.py version          # OK: GitHub y Azure tienen la misma versión
```

Si la base dice `Stopped`: `az postgres flexible-server start -g rg-shiftmanager -n psql-shiftmanager-th` (tarda unos minutos).

Dejar abiertos: la pestaña **Actions** del repo en GitHub, el Portal de Azure en `ca-shiftmanager-backend` y Postman con la colección de `docs/postman`.

---

## 1. "Contame qué hiciste" (1 minuto)

> "Es una API en Spring Boot para gestionar turnos de guardias de seguridad, con login JWT y una app mobile en React Native. La empaqueté en una imagen Docker multietapa con distroless. Cada vez que subo código a GitHub, GitHub Actions corre los tests, construye la imagen, la escanea con Grype y la sube a Azure Container Registry con el commit como versión. Si el código llega a la rama `deploy`, la despliega en Azure Container Apps, que se conecta a un PostgreSQL de Azure. Los secretos están en GitHub y en Azure, nunca en el código, y lo monitoreo con Actuator, Log Analytics y Azure Monitor."

El recorrido: **código → GitHub → Actions (tests, imagen, Grype) → ACR → Container App → PostgreSQL**. Diagrama en [arquitectura.md](arquitectura.md).

---

## 2. "Prendé la aplicación"

**Hacé**

```powershell
python scripts/azure_manager.py start
```

Portal: `ca-shiftmanager-backend` → **Overview** → **Start**.

**Decí**: *"La inicio. Azure levanta el contenedor desde la imagen del registro y Spring Boot arranca en unos 30 segundos."*

## 3. "Fijate si prendió"

**Hacé**

```powershell
python scripts/azure_manager.py status           # Estado: Running
curl.exe https://ca-shiftmanager-backend.lemonground-a6322200.chilecentral.azurecontainerapps.io/actuator/health
```

**Mostrá**: `{"status":"UP"}`. La primera vez puede tardar hasta un minuto.

**Decí**: *"Running solo significa que el contenedor existe. Para saber que funciona le pregunto a la app: el health de Actuator da UP, y el de readiness también revisa la conexión con la base. Azure usa esos mismos endpoints como probes: si fallan, no le manda tráfico o la reinicia."*

## 4. "Apagala"

**Hacé**

```powershell
python scripts/azure_manager.py stop             # Estado: Stopped
curl.exe https://ca-shiftmanager-backend.lemonground-a6322200.chilecentral.azurecontainerapps.io/actuator/health   # 404
```

Portal: **Overview** → **Stop**.

**Decí**: *"Detenida no consume cómputo y no se pierde nada: la configuración, la imagen y la base siguen ahí. Con start vuelve igual que antes."*

Después: `python scripts/azure_manager.py start` para seguir con la demo (unos 80 segundos hasta que responde).

## 5. "Mostrame las métricas" / "¿Cuánto consume?"

**Hacé**

```powershell
python scripts/azure_manager.py metrics --hours 24
```

Portal: app → **Monitoring** → **Metrics** → *Requests*, *CPU Usage*, *Memory Working Set Bytes*, *Replica Count*. Para el dinero: **Cost Management** → **Cost analysis**.

**Mostrá**: peticiones, tiempo de respuesta promedio, CPU y memoria, reinicios en 0 y minutos con la réplica encendida contra la capa gratuita.

**Decí**: *"Container Apps cobra por segundo de CPU y de memoria mientras hay una réplica encendida. Con réplicas mínimas en 0, cuando nadie la usa se apaga sola y no cuesta nada; el primer pedido la vuelve a encender. Lo que sí cobra por hora es la base PostgreSQL mientras está prendida, por eso la apago cuando no la uso."*

## 6. "Mostrame los logs"

**Hacé**

```powershell
python scripts/azure_manager.py logs             # últimas 100 líneas
python scripts/azure_manager.py logs -f          # en vivo (Ctrl+C para salir)
```

Portal: app → **Monitoring** → **Log stream** (en vivo) o **Logs** (consultas KQL de [monitoreo.md](monitoreo.md)).

**Mostrá**: las líneas `ACCESS` (una por petición, con método, ruta, código y milisegundos) y `Login succeeded` / `Login failed`.

**Decí**: *"La app escribe los logs en la salida estándar, como pide el factor XI de los 12 factores, y Azure los junta en Log Analytics, donde los puedo consultar con KQL."*

## 7. "Hacé un cambio cualquiera, una letra, y subilo"

**Hacé**

1. Abrí `shiftmanager/src/main/java/com/security/shiftmanager/controller/GreetingController.java` y cambiá una letra de `"Hello from Security Shift Manager"`.
2. Subilo:

```powershell
git checkout dev; git pull
git checkout -b feature/cambio-parcial
git commit -am "feat: cambia el saludo"
git push -u origin feature/cambio-parcial
gh pr create --base dev --fill; gh pr merge --merge
```

3. Si pide que llegue a Azure, seguí hasta `deploy`:

```powershell
gh pr create --base main --head dev --fill; gh pr merge --merge
git fetch origin; git push origin origin/main:deploy
```

**Decí**: *"Trabajo con ramas: feature, después dev, después main y finalmente deploy. En dev y main el pipeline solo construye y publica la imagen; recién al llegar a deploy la despliega."*

## 8. "Mostrame GitHub Actions"

**Mostrá**: pestaña **Actions** → el run que se acaba de disparar (o en la terminal: `gh run watch`). Son 3 jobs, unos 5 a 6 minutos en total:

| Job | Qué hace |
|---|---|
| **Build y tests** | Compila con Gradle y corre los 33 tests |
| **Imagen, Grype y push al ACR** | Construye la imagen Docker, la escanea con Grype (corta si hay vulnerabilidades altas o críticas con solución) y la sube al ACR con el commit como tag |
| **Deploy a Azure Container Apps** | Solo en la rama `deploy`: actualiza la app con la imagen nueva y verifica en `/actuator/info` que corre esa versión |

En un run de `dev` o `main` el job de Deploy aparece en gris (*skipped*): es a propósito.

**Decí**: *"El push dispara el workflow. Si falla un test, no se construye la imagen; si Grype encuentra algo grave, no se publica; y solo se despliega lo que llega a deploy."*

## 9. "Compará el versionado: ¿está la misma versión en GitHub y en Azure?"

**Hacé**

```powershell
python scripts/azure_manager.py version
```

**Mostrá**: `OK: GitHub y Azure tienen la misma versión (abc1234 / abc1234 / abc1234)`. Los tres valores son:
1. el último commit de la rama `deploy` en GitHub;
2. la imagen que tiene la Container App;
3. lo que responde la app en `/actuator/info`.

A mano, en el navegador:
- GitHub: el SHA del último commit de la rama `deploy`.
- Portal: app → **Containers** → la imagen termina en `:<sha>`.
- `https://ca-shiftmanager-backend.lemonground-a6322200.chilecentral.azurecontainerapps.io/actuator/info` → `version`.
- `https://ca-shiftmanager-backend.lemonground-a6322200.chilecentral.azurecontainerapps.io/hello` → el mensaje con la letra cambiada.

**Decí**: *"La versión es el SHA corto del commit. El pipeline la usa como tag de la imagen y también la mete adentro de la imagen, así la propia app dice qué commit está corriendo. Por eso puedo comparar las tres cosas y saber que en Azure está exactamente lo que está en GitHub."*

## 10. "Generá una imagen pero no la despliegues"

**Hacé**: el paso 7 solo hasta `dev` (el PR a `dev`).

**Mostrá**: el run de `dev` en Actions con Deploy en *skipped*, la imagen nueva con `python scripts/azure_manager.py tags`, y que `version` sigue mostrando la versión anterior en Azure.

**Decí**: *"La condición del job de deploy es la rama: en dev se construye y publica, pero no se despliega."*

## 11. "Desplegá una versión específica" / "Volvé a la versión anterior"

**Hacé**: Actions → **CI/CD** → **Run workflow** → rama `main` → marcar *Desplegar* → `image_tag` = el SHA que quieras → **Run workflow**. Por terminal:

```powershell
python scripts/azure_manager.py tags                                   # elegir un SHA de la lista
gh workflow run ci-cd.yml --ref main -f deploy=true -f image_tag=<SHA>
gh run watch
python scripts/azure_manager.py version
```

**Decí**: *"Cada imagen queda guardada en el registro con su SHA, así que volver atrás es desplegar una imagen que ya existe, sin recompilar."*

## 12. "Probá el login con Postman"

**Hacé**: Postman → environment *Azure* (con `adminPassword` cargado) → carpeta *2. Autenticación JWT*:
- *Login correcto* → 200 + token;
- *Login incorrecto* → 401;
- *Sin token* → 401;
- *Token alterado* → 401.

Después, carpeta *4. Negocio* → *Listar guardias* → 200. Botón **Run** para ejecutar toda la colección.

**Decí**: *"El login devuelve un JWT firmado con una clave que está como secreto en Azure. Lo mando en el header Authorization; si alguien cambia un carácter, la firma no coincide y da 401."*

## 13. "¿Dónde están los secretos?"

**Hacé**

```powershell
gh secret list                                                               # AZURE_CREDENTIALS (solo el nombre)
az containerapp secret list -g rg-shiftmanager -n ca-shiftmanager-backend --query "[].name"   # db-password, jwt-secret, admin-password
```

**Decí**: *"En ningún archivo del repo. El pipeline usa el secret de GitHub con las credenciales del Service Principal. La app recibe la contraseña de la base, la clave JWT y la del admin como secrets de la Container App. Si falta alguno, la app no arranca a propósito."*

## 14. "¿Qué es el Service Principal?"

**Decí**: *"Es una identidad para el pipeline, para que GitHub Actions entre a Azure sin usar mi cuenta personal. Tiene solo los permisos necesarios: subir imágenes al registro, leer el resource group y actualizar la app; no puede borrar ni crear otras cosas."*

**Mostrá**: en un run, el paso *Login en Azure*.

## 15. "¿Qué vulnerabilidades encontraste?" (Grype)

**Decí**: *"La primera imagen tenía 236 vulnerabilidades, 9 críticas. Cambié la imagen base a distroless y actualicé Spring Boot, Tomcat, Jackson y el driver de PostgreSQL: quedaron 72 y ninguna con solución disponible. Las 2 críticas que quedan son de funciones de Spring que la app no usa. El pipeline corta si aparece una alta o crítica que tenga parche."*

**Mostrá**: `docs/reports/` (los tres informes) y, en un run, el *Summary* con el informe de Grype.

## 16. "¿Qué es CodeQL?"

**Decí**: *"Grype revisa las librerías de la imagen; CodeQL revisa mi código. Encontró 7 alertas: corregí 6 (un log que mostraba una contraseña y actions sin fijar a un commit) y la de CSRF la justifiqué, porque la API usa JWT en el header y no cookies."*

**Mostrá**: GitHub → **Security** → **Code scanning**.

## 17. "Explicame el Dockerfile" / "¿Docker Compose?"

**Decí**: *"Tiene dos etapas: en la primera compilo con el JDK y Gradle; en la segunda copio solo el jar a una imagen distroless, que no tiene shell ni herramientas y corre con un usuario sin privilegios. Queda más chica y con menos vulnerabilidades. Docker Compose es para correrla localmente junto con PostgreSQL."*

## 18. "¿Cómo se conecta a la base?"

**Decí**: *"Por JDBC con SSL a un PostgreSQL de Azure, con un pool de conexiones HikariCP de 5 conexiones como máximo. La URL, el usuario y la contraseña vienen de variables de entorno. Flyway crea las tablas al arrancar."*

**Mostrá**: Postman → carpeta *5. Salud detallada y métricas (ADMIN)* → *Health con detalle* → `db: UP`.

## 19. "¿Y la app mobile?"

**Decí**: *"La app de React Native apunta a la URL HTTPS de Azure, hace login, guarda el token cifrado en el celular y lo manda en cada pedido. Si el token vence, vuelve al login."*

**Mostrá**: Expo Go en el celular (`npx expo start -c` en el repo mobile, con el `.env` apuntando a Azure).

---

## Si algo sale mal

| Qué pasa | Qué hacer |
|---|---|
| La URL tarda mucho la primera vez | Es el arranque en frío (0 réplicas). Esperá un minuto; para evitarlo: `scale 1` |
| La URL da 404 | La app está detenida: `python scripts/azure_manager.py start` |
| health da `DOWN` o el login da 503 | La base está apagada: `az postgres flexible-server start -g rg-shiftmanager -n psql-shiftmanager-th` |
| `az` pide login o da error de sesión | `az login` y elegir *Azure for Students* |
| `gh` da error de autenticación | `gh auth login` |
| El run de Actions falla | Abrir el job en rojo y leer el paso que falló, o `gh run view --log-failed`. La versión anterior sigue funcionando en Azure. Corregir y volver a subir, o **Re-run failed jobs** |
| `version` dice DISTINTAS | El deploy todavía está corriendo (esperar el run) o falló (ver Actions) |
| `git push` rechazado | `git pull` y volver a intentar |

## Al terminar

```powershell
python scripts/azure_manager.py scale 0          # vuelve a apagarse sola cuando no se usa
```

Si no vas a usar el proyecto por varios días: `az postgres flexible-server stop -g rg-shiftmanager -n psql-shiftmanager-th`.
