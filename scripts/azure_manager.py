#!/usr/bin/env python3
"""
Manual operations for the Security Shift Manager backend on Azure Container Apps.

Uses the same image naming as GitHub Actions, so a manual deploy and a CI deploy never
point to different images:

    <ACR>.azurecr.io/shiftmanager-backend:<git short sha>   (every build)
    <ACR>.azurecr.io/shiftmanager-backend:vX.Y.Z            (git tags)

Usage (PowerShell, from the repository root):

    python scripts/azure_manager.py                 # interactive menu
    python scripts/azure_manager.py build-push      # build + push :<sha>
    python scripts/azure_manager.py deploy --tag 1a2b3c4
    python scripts/azure_manager.py status | url | logs | revisions | tags | list
    python scripts/azure_manager.py stop | start | restart
    python scripts/azure_manager.py delete          # asks for confirmation

Authentication: the current `az login` session, or the Service Principal with --sp
(credentials read from %USERPROFILE%\\.shiftmanager\\azure-sp.env, never from the repository).
Runtime secrets for the first deploy are read from %USERPROFILE%\\.shiftmanager\\azure-secrets.env.
"""

import argparse
import datetime
import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

# Resource names. Same values as the GitHub Variables of the repository; override with env vars.
RESOURCE_GROUP = os.environ.get("AZURE_RESOURCE_GROUP", "rg-shiftmanager")
ACR_NAME = os.environ.get("ACR_NAME", "acrshiftmanagerth")
ENVIRONMENT = os.environ.get("CONTAINER_APP_ENVIRONMENT", "cae-shiftmanager")
APP_NAME = os.environ.get("CONTAINER_APP_NAME", "ca-shiftmanager-backend")
IMAGE_NAME = os.environ.get("IMAGE_NAME", "shiftmanager-backend")
PG_SERVER = os.environ.get("PG_SERVER", "psql-shiftmanager-th")

ACR_SERVER = f"{ACR_NAME}.azurecr.io"
TARGET_PORT = "8090"
# GA REST API version used to read and patch the template (probes).
ACA_API_VERSION = "2026-01-01"
REPO_ROOT = Path(__file__).resolve().parent.parent
SECRETS_DIR = Path.home() / ".shiftmanager"

try:
    # Line buffering keeps our messages in order with the output of az and docker.
    sys.stdout.reconfigure(encoding="utf-8", line_buffering=True)
except AttributeError:
    pass


# ---------------------------------------------------------------- helpers

def info(msg):
    print(f"\n>> {msg}")


def fail(msg):
    print(f"\nERROR: {msg}")
    sys.exit(1)


def tool(name):
    path = shutil.which(name)
    if not path and name == "az" and sys.platform == "win32":
        candidate = Path(r"C:\Program Files\Microsoft SDKs\Azure\CLI2\wbin\az.cmd")
        path = str(candidate) if candidate.exists() else None
    if not path:
        fail(f"'{name}' no está instalado o no está en el PATH.")
    return path


def run(cmd, capture=False, check=True, quiet_args=0):
    """Runs a command. The last `quiet_args` arguments (secrets) are hidden in the echo."""
    shown = cmd if not quiet_args else cmd[:-quiet_args] + ["***"] * quiet_args
    if not capture:
        print("   $ " + " ".join(shown))
    exe = tool(cmd[0])
    result = subprocess.run([exe] + cmd[1:], text=True, capture_output=capture, encoding="utf-8")
    if check and result.returncode != 0:
        if capture and result.stderr:
            print(result.stderr.strip())
        fail(f"falló: {' '.join(shown[:4])} ...")
    return result.stdout.strip() if capture else result.returncode


def az_json(*args):
    out = run(["az", *args, "-o", "json"], capture=True)
    return json.loads(out) if out else None


def read_env_file(name):
    path = SECRETS_DIR / name
    if not path.exists():
        fail(f"no existe {path}. Ver docs/azure.md.")
    values = {}
    for line in path.read_text(encoding="utf-8-sig").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip()
    return values


def git_sha():
    return run(["git", "-C", str(REPO_ROOT), "rev-parse", "--short=7", "HEAD"], capture=True)


def confirm(question):
    return input(f"{question} [s/N]: ").strip().lower() in ("s", "si", "sí", "y", "yes")


def app_exists():
    result = subprocess.run([tool("az"), "containerapp", "show", "-g", RESOURCE_GROUP, "-n", APP_NAME,
                             "-o", "none"], capture_output=True, text=True)
    return result.returncode == 0


# ---------------------------------------------------------------- operations

def login(use_sp):
    if use_sp:
        sp = read_env_file("azure-sp.env")
        info("Login con el Service Principal")
        run(["az", "login", "--service-principal", "-u", sp["AZURE_CLIENT_ID"], "--tenant",
             sp["AZURE_TENANT_ID"], "-o", "none", "-p", sp["AZURE_CLIENT_SECRET"]], quiet_args=1)
        run(["az", "account", "set", "--subscription", sp["AZURE_SUBSCRIPTION_ID"]])
    account = az_json("account", "show")
    if not account:
        fail("no hay sesión de Azure. Ejecutá 'az login'.")
    print(f"   Suscripción: {account['name']} | identidad: {account['user']['type']}")


def build_push(extra_tag=None):
    dirty = run(["git", "-C", str(REPO_ROOT), "status", "--porcelain", "shiftmanager"], capture=True)
    if dirty:
        print("   AVISO: hay cambios sin commitear en shiftmanager/; la imagen no corresponderá exactamente al commit.")
    sha = git_sha()
    full_sha = run(["git", "-C", str(REPO_ROOT), "rev-parse", "HEAD"], capture=True)
    image = f"{ACR_SERVER}/{IMAGE_NAME}"
    tags = [f"{image}:{sha}"] + ([f"{image}:{extra_tag}"] if extra_tag else [])
    version = extra_tag or sha
    now = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    info(f"Build {', '.join(tags)}")
    cmd = ["docker", "build", "--platform", "linux/amd64",
           "--build-arg", f"VERSION={version}", "--build-arg", f"BUILD_DATE={now}",
           "--build-arg", f"VCS_REF={full_sha}"]
    for tag in tags:
        cmd += ["-t", tag]
    run(cmd + [str(REPO_ROOT / "shiftmanager")])
    info(f"Login en {ACR_SERVER}")
    run(["az", "acr", "login", "--name", ACR_NAME])
    for tag in tags:
        info(f"Push {tag}")
        run(["docker", "push", tag])
    show_tags()
    return sha


def show_tags():
    info(f"Imágenes en {ACR_SERVER}/{IMAGE_NAME} (más nuevas primero)")
    run(["az", "acr", "repository", "show-tags", "-n", ACR_NAME, "--repository", IMAGE_NAME,
         "--orderby", "time_desc", "--top", "15", "-o", "table"])


def ensure_tag_exists(tag):
    tags = az_json("acr", "repository", "show-tags", "-n", ACR_NAME, "--repository", IMAGE_NAME) or []
    if tag not in tags:
        fail(f"la imagen {IMAGE_NAME}:{tag} no existe en el ACR. Tags disponibles: {', '.join(tags[:10])}")


def deploy(tag):
    ensure_tag_exists(tag)
    image = f"{ACR_SERVER}/{IMAGE_NAME}:{tag}"
    if app_exists():
        info(f"La app existe: nueva revisión con {image}")
        run(["az", "containerapp", "update", "-g", RESOURCE_GROUP, "-n", APP_NAME, "--image", image,
             "-o", "none"])
    else:
        create_app(image)
    status()


def create_app(image):
    secrets = read_env_file("azure-secrets.env")
    pg_host = f"{PG_SERVER}.postgres.database.azure.com"
    info(f"La app no existe: creando {APP_NAME} con {image}")
    run(["az", "containerapp", "create", "-g", RESOURCE_GROUP, "-n", APP_NAME, "--environment", ENVIRONMENT,
         "--image", image, "--registry-server", ACR_SERVER, "--registry-identity", "system",
         "--ingress", "external", "--target-port", TARGET_PORT,
         "--cpu", "0.5", "--memory", "1.0Gi", "--min-replicas", "0", "--max-replicas", "1",
         "--env-vars",
         "SPRING_PROFILES_ACTIVE=prod",
         f"DB_URL=jdbc:postgresql://{pg_host}:5432/shiftmanager?sslmode=require",
         f"DB_USERNAME={secrets['DB_USERNAME']}",
         "DB_PASSWORD=secretref:db-password",
         "JWT_SECRET=secretref:jwt-secret",
         f"ADMIN_EMAIL={secrets['ADMIN_EMAIL']}",
         "ADMIN_PASSWORD=secretref:admin-password",
         "SEED_DEMO_DATA=true",
         "-o", "none",
         "--secrets",
         f"db-password={secrets['DB_PASSWORD']}",
         f"jwt-secret={secrets['JWT_SECRET']}",
         f"admin-password={secrets['ADMIN_PASSWORD']}"], quiet_args=3)
    configure_probes()


def configure_probes():
    """HTTP probes against Actuator (the CLI has no flags for probes, so the template is patched)."""
    info("Configurando probes HTTP (startup, liveness, readiness)")
    app_id = run(["az", "containerapp", "show", "-g", RESOURCE_GROUP, "-n", APP_NAME, "--query", "id", "-o", "tsv"],
                 capture=True)
    api_url = f"https://management.azure.com{app_id}?api-version={ACA_API_VERSION}"
    # Read and write with the same REST API version, so the template has exactly the fields it accepts.
    template = az_json("rest", "--method", "get", "--url", api_url)["properties"]["template"]
    template["containers"][0]["probes"] = [
        {"type": "Startup", "httpGet": {"path": "/actuator/health/liveness", "port": int(TARGET_PORT)},
         "initialDelaySeconds": 10, "periodSeconds": 5, "failureThreshold": 30, "timeoutSeconds": 3},
        {"type": "Liveness", "httpGet": {"path": "/actuator/health/liveness", "port": int(TARGET_PORT)},
         "periodSeconds": 30, "failureThreshold": 3, "timeoutSeconds": 3},
        {"type": "Readiness", "httpGet": {"path": "/actuator/health/readiness", "port": int(TARGET_PORT)},
         "periodSeconds": 10, "failureThreshold": 3, "timeoutSeconds": 3},
    ]
    with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False, encoding="utf-8") as body:
        json.dump({"properties": {"template": template}}, body)
    try:
        run(["az", "rest", "--method", "patch", "--url", api_url, "--body", f"@{body.name}", "-o", "none"])
    finally:
        os.unlink(body.name)


def status():
    app = az_json("containerapp", "show", "-g", RESOURCE_GROUP, "-n", APP_NAME)
    props = app["properties"]
    scale = props["template"].get("scale", {})
    info(f"Estado de {APP_NAME}")
    print(f"   Estado:           {props.get('runningStatus')} ({props.get('provisioningState')})")
    print(f"   Imagen:           {props['template']['containers'][0]['image']}")
    print(f"   Revisión activa:  {props.get('latestReadyRevisionName')}")
    print(f"   Réplicas:         min {scale.get('minReplicas')} / max {scale.get('maxReplicas')}")
    print(f"   URL:              https://{props['configuration']['ingress']['fqdn']}")


def url():
    fqdn = run(["az", "containerapp", "show", "-g", RESOURCE_GROUP, "-n", APP_NAME,
                "--query", "properties.configuration.ingress.fqdn", "-o", "tsv"], capture=True)
    print(f"https://{fqdn}")


def revisions():
    run(["az", "containerapp", "revision", "list", "-g", RESOURCE_GROUP, "-n", APP_NAME, "--query",
         "[].{revision:name, creada:properties.createdTime, activa:properties.active, "
         "trafico:properties.trafficWeight, replicas:properties.replicas, "
         "imagen:properties.template.containers[0].image}", "-o", "table"])


def logs(follow):
    cmd = ["az", "containerapp", "logs", "show", "-g", RESOURCE_GROUP, "-n", APP_NAME, "--tail", "100",
           "--format", "text"]
    run(cmd + (["--follow"] if follow else []), check=False)


def stop():
    info(f"Deteniendo {APP_NAME} (deja de atender peticiones; no se pierden datos ni configuración)")
    run(["az", "containerapp", "stop", "-g", RESOURCE_GROUP, "-n", APP_NAME, "-o", "none"])
    status()


def start():
    info(f"Iniciando {APP_NAME}")
    run(["az", "containerapp", "start", "-g", RESOURCE_GROUP, "-n", APP_NAME, "-o", "none"])
    status()


def restart():
    revision = run(["az", "containerapp", "show", "-g", RESOURCE_GROUP, "-n", APP_NAME,
                    "--query", "properties.latestReadyRevisionName", "-o", "tsv"], capture=True)
    info(f"Reiniciando la revisión {revision}")
    run(["az", "containerapp", "revision", "restart", "-g", RESOURCE_GROUP, "-n", APP_NAME,
         "--revision", revision, "-o", "none"])


def scale(minimum):
    info(f"Réplicas mínimas = {minimum}")
    run(["az", "containerapp", "update", "-g", RESOURCE_GROUP, "-n", APP_NAME,
         "--min-replicas", str(minimum), "-o", "none"])
    status()


def list_apps():
    run(["az", "containerapp", "list", "-g", RESOURCE_GROUP, "--query",
         "[].{app:name, estado:properties.runningStatus, "
         "imagen:properties.template.containers[0].image, url:properties.configuration.ingress.fqdn}",
         "-o", "table"])


def delete():
    print(f"\nVas a ELIMINAR la Container App '{APP_NAME}'. La base de datos y las imágenes del ACR NO se borran,")
    print("pero la URL deja de existir y hay que volver a crear la app (deploy) para recuperarla.")
    if input(f"Escribí el nombre de la app para confirmar ({APP_NAME}): ").strip() != APP_NAME:
        print("Cancelado.")
        return
    run(["az", "containerapp", "delete", "-g", RESOURCE_GROUP, "-n", APP_NAME, "--yes"])
    list_apps()


# ---------------------------------------------------------------- CLI

MENU = [
    ("Build + push de la imagen (:sha)", lambda: build_push()),
    ("Ver imágenes del ACR", show_tags),
    ("Deploy de una imagen (elegir tag)", lambda: deploy(input("Tag a desplegar (ej. 1a2b3c4 o v1.2.0): ").strip())),
    ("Estado y URL", status),
    ("Revisiones", revisions),
    ("Logs (últimas 100 líneas)", lambda: logs(False)),
    ("Detener la app", stop),
    ("Iniciar la app", start),
    ("Reiniciar la revisión", restart),
    ("Réplicas mínimas 1 (para la demo)", lambda: scale(1)),
    ("Réplicas mínimas 0 (ahorro)", lambda: scale(0)),
    ("Listar apps", list_apps),
    ("Eliminar la app", delete),
]


def menu():
    while True:
        print(f"\n=== {APP_NAME} | {ACR_SERVER}/{IMAGE_NAME} | commit actual {git_sha()} ===")
        for i, (label, _) in enumerate(MENU, 1):
            print(f"  {i:2}. {label}")
        print("   0. Salir")
        choice = input("Opción: ").strip()
        if choice == "0":
            return
        if choice.isdigit() and 1 <= int(choice) <= len(MENU):
            MENU[int(choice) - 1][1]()


def main():
    parser = argparse.ArgumentParser(description="Operación del backend en Azure Container Apps")
    parser.add_argument("--sp", action="store_true", help="autenticarse con el Service Principal")
    sub = parser.add_subparsers(dest="command")
    bp = sub.add_parser("build-push", help="build y push de :<sha> (y opcionalmente otro tag)")
    bp.add_argument("--tag", help="tag adicional, por ejemplo v1.2.0")
    dp = sub.add_parser("deploy", help="despliega un tag existente en el ACR (crea la app si no existe)")
    dp.add_argument("--tag", help="tag a desplegar (por defecto el commit actual)")
    lg = sub.add_parser("logs", help="logs de la app")
    lg.add_argument("-f", "--follow", action="store_true")
    sc = sub.add_parser("scale", help="réplicas mínimas")
    sc.add_argument("min", type=int, choices=[0, 1])
    for name in ("tags", "status", "url", "revisions", "stop", "start", "restart", "list", "delete", "probes"):
        sub.add_parser(name)
    args = parser.parse_args()

    login(args.sp)
    actions = {
        "build-push": lambda: build_push(args.tag),
        "deploy": lambda: deploy(args.tag or git_sha()),
        "logs": lambda: logs(args.follow),
        "scale": lambda: scale(args.min),
        "tags": show_tags, "status": status, "url": url, "revisions": revisions,
        "stop": stop, "start": start, "restart": restart, "list": list_apps, "delete": delete,
        "probes": configure_probes,
    }
    if args.command:
        actions[args.command]()
    else:
        menu()


if __name__ == "__main__":
    main()
