# Contexto del proyecto — Monitores académicos distribuidos

**Para:** todo el equipo y sus asistentes de IA (Claude, Copilot, ChatGPT...)
**Integrantes:** 1 (interfaz + controlador Java) · 2 (PHP) · 3 (Python + MySQL)
**Creado:** miércoles 18 de noviembre de 2026 · **Entrega:** sábado 21 de noviembre de 2026

> **Cómo usar este documento con su Claude u otro modelo:** díganle que lea `docs/AGENTS.md` al empezar la sesión (o péguenlo como contexto). Describe lo que ya está hecho, la estructura del repo, el contrato de la API y las reglas del equipo. **El contrato está congelado**: si su modelo propone cambiar una ruta, un campo o un código de estado, deténganse y avísenle al equipo antes de hacerlo.

## 0. Mantener este documento al día

Este archivo es **la fuente común de verdad** del equipo. Si cambia algo que afecte a otro integrante, se actualiza aquí.

**Hay que actualizarlo cuando cambie:**
- el contrato (rutas, campos, códigos de estado o mensajes de error), **siempre después de acordarlo con el equipo**;
- puertos, variables de entorno (`PHP_URL`, `PYTHON_URL`...) o cómo se arranca un servicio;
- el esquema de la BD o los datos semilla;
- versiones o dependencias que otro necesite conocer;
- el estado de cada capa (lo que ya funciona y lo que falta);
- reglas de trabajo, ramas o fechas de integración.

**No hace falta** para cambios internos que no se ven desde fuera (refactorizar, renombrar clases, mejorar logs).

**Cómo:**
1. Editar la sección que corresponda y agregar una línea en el **registro de cambios** (abajo), con la fecha, quién y qué cambió.
2. Subirlo **en el mismo commit** que el cambio de código, o en uno propio como `Actualiza AGENTS.md: <qué cambió>`.
3. **Avisar en el grupo**: los demás no se enteran solos de que el archivo cambió.

**Para el asistente de IA:** si haces un cambio que entra en la lista anterior, actualiza este archivo y el registro de cambios en el mismo trabajo, y recuérdale al integrante que avise al equipo. Si el cambio toca el contrato, **no lo hagas**: detente y pide que se acuerde primero con el equipo.

### Registro de cambios

| Fecha | Quién | Cambio |
|---|---|---|
| 2026-11-18 | Integrante 1 | Versión inicial: arquitectura, estructura, reglas de git, contrato, validaciones, indicaciones para PHP y Python, versiones, integración y pruebas. |
| 2026-11-18 | Integrante 2 | Estado actual: la rama `java` ya está integrada en `main`. Decisiones del equipo: el PUT siempre envía los tres campos; plan B con SQLite si MySQL falla en PC3; este archivo se edita solo en `main`. |

---

## 1. Arquitectura (la exige el taller)

```
Interfaz HTML/JS ──► Controlador Java ──► Servicio PHP ──► Servicio Python ──► MySQL
     (PC1)              (PC1 :8080)          (PC2 :8000)        (PC3 :5000)      (PC3)
```

| PC | Responsable | Componente | Puerto | Escucha en |
|---|---|---|---|---|
| PC1 | Integrante 1 | Interfaz + Java (Spring Boot) | 8080 | `0.0.0.0` |
| PC2 | Integrante 2 | Servicio PHP | 8000 | `0.0.0.0` |
| PC3 | Integrante 3 | Servicio Python (FastAPI) + MySQL | 5000 | `0.0.0.0` |

- Toda la comunicación es **HTTP con JSON**.
- Cada capa habla **solo con la siguiente**: la interfaz solo llama a Java; Java solo llama a PHP; PHP solo llama a Python; solo Python toca la BD.
- **Todo el CRUD** pasa por la cadena completa. Nadie se salta una capa.

## 2. Estado actual (lo que ya está hecho)

- **Repo:** `https://github.com/jAlejandro423/monitores-distribuidos`
- **`main`** tiene la estructura de carpetas, el `.gitignore` raíz, este `AGENTS.md` y el controlador Java completo, incluida la conexión con PHP.
- **Controlador Java: terminado.** Expone los 6 endpoints, valida los datos (RF6) y reenvía todo a PHP. La rama `java` ya está integrada: hoy `java` y `main` apuntan al mismo commit.
- Se probó de punta a punta contra un **mock de PHP** que implementa el contrato en memoria. Es decir: **si su servicio cumple el contrato de este documento, debería conectarse con Java sin cambios.**
- Falta: la interfaz Bootstrap (Integrante 1, viernes en la mañana), el servicio PHP y el servicio Python con la BD.

## 3. Estructura del repo

```
monitores-distribuidos/
├── .gitignore          ← raíz (ya ignora .env, target/, __pycache__/, .venv/, php-service/vendor/, .idea/, .vscode/)
├── java-controller/    ← Integrante 1 (Spring Boot). La interfaz va en src/main/resources/static/
├── php-service/        ← Integrante 2
├── python-service/     ← Integrante 3
├── database/           ← Integrante 3 (schema.sql)
└── docs/               ← este AGENTS.md, documento final, capturas, guías
```

**Ojo, cambio frente al plan de trabajo:** **no hay carpeta `frontend/`**. La interfaz vive dentro de `java-controller/src/main/resources/static/` y la sirve el mismo Spring Boot en `http://<IP-PC1>:8080/`. Así la interfaz y la API comparten origen y no hay problemas de CORS.

Las carpetas `php-service/`, `python-service/`, `database/` y `docs/` existen pero solo tienen un `.gitkeep` (para que git las suba vacías). Pueden borrar el `.gitkeep` cuando agreguen sus archivos.

## 4. Reglas de trabajo con git

1. **Cada quien trabaja en su rama y en su carpeta:** Integrante 2 en la rama `php` y en `php-service/`; Integrante 3 en la rama `python` y en `python-service/` + `database/`. (Si ya usan otro nombre de rama, avisen; lo importante es no trabajar directo en `main`.)
2. **No modificar carpetas ajenas.** Si necesitan un cambio en otra capa, pídanlo al responsable.
3. **Merge a `main` coordinado:** se avisa en el grupo antes de integrar, y `main` siempre debe quedar funcionando.
4. **Commits pequeños, en español, uno por funcionalidad.** Ejemplo: `Agrega endpoint DELETE de monitores`.
5. **Nunca subir credenciales ni IPs.** La contraseña de MySQL, las IPs y las URLs van en un `.env` o en variables de entorno; `.env` ya está en el `.gitignore`. Si quieren, suban un `.env.example` sin valores reales.
6. Antes de hacer `push`, revisen con `git status` que no van archivos de `vendor/`, `__pycache__/`, `.venv/` ni del IDE.
7. **`docs/AGENTS.md` se edita solo en `main`.** Cada integrante lo trae a su rama con `git merge main`. Así no hay versiones distintas del documento en cada rama.

## 5. Contrato de la API (congelado)

**Las tres capas exponen exactamente las mismas rutas.** Solo cambia el host: Java en `:8080`, PHP en `:8000`, Python en `:5000`.

### Objetos

Monitor (lo que se envía al crear):
```json
{"codigo": "MON001", "nombre": "Ana Pérez", "correo": "ana@ucaldas.edu.co", "materiaId": 2}
```
Monitor en las respuestas (se agrega `materiaNombre`):
```json
{"codigo": "MON001", "nombre": "Ana Pérez", "correo": "ana@ucaldas.edu.co", "materiaId": 2, "materiaNombre": "Redes I"}
```
Materia:
```json
{"id": 2, "nombre": "Redes I"}
```
Error (**siempre** esta forma, en todas las capas):
```json
{"error": "mensaje legible para mostrar en la interfaz"}
```

### Rutas

| Método | Ruta | Cuerpo de entrada | Éxito | Errores |
|---|---|---|---|---|
| GET | `/api/materias` | — | 200, arreglo de materias | — |
| GET | `/api/monitores` | — | 200, arreglo de monitores (puede ser `[]`) | — |
| GET | `/api/monitores/{codigo}` | — | 200, un monitor | 404 si no existe |
| POST | `/api/monitores` | monitor completo (con `codigo`) | **201**, el monitor creado | 400 datos incompletos o materia inexistente · **409 código duplicado** |
| PUT | `/api/monitores/{codigo}` | `{"nombre", "correo", "materiaId"}` (**sin `codigo`**) | 200, el monitor actualizado | 400 · 404 si no existe |
| DELETE | `/api/monitores/{codigo}` | — | **204, sin cuerpo** | 404 si no existe |

- El `codigo` va en la ruta en GET, PUT y DELETE; en el PUT **no se edita** y no viene en el cuerpo.
- El PUT **siempre envía los tres campos** (`nombre`, `correo`, `materiaId`). No hay actualizaciones parciales.
- Si una capa no logra conectarse con la siguiente → **502** con `{"error": "Servicio X no disponible"}`. (Java usa `"Servicio PHP no disponible"`.)

### Ejemplos de mensajes de error sugeridos

| Caso | Estado | Cuerpo |
|---|---|---|
| Monitor no existe | 404 | `{"error": "No existe un monitor con código MON999"}` |
| Código duplicado | 409 | `{"error": "Ya existe un monitor con código MON001"}` |
| Materia inexistente | 400 | `{"error": "La materia 99 no existe"}` |
| Python no responde (en PHP) | 502 | `{"error": "Servicio Python no disponible"}` |

El texto exacto lo deciden ustedes; lo obligatorio es el **código de estado** y la forma `{"error": "..."}`. Ese texto es el que verá el usuario en la interfaz, así que debe ser claro y en español.

## 6. ¿Quién valida qué?

| Regla | Quién la aplica |
|---|---|
| Campos obligatorios, longitud, formato de correo y de código (RF6) | **Java** (Bean Validation). Si fallan, responde 400 y la petición **nunca llega a PHP**. |
| Código duplicado → 409 | **Python** (la BD es la única que lo sabe: violación de la llave primaria). |
| Materia inexistente → 400 | **Python** (violación de la llave foránea `materia_id`). |
| Monitor no existe → 404 | **Python**. |

Reglas que ya aplica Java (para que sepan qué datos les llegan):
- `codigo`: obligatorio, 3–20 caracteres, solo letras, números y guion (`^[A-Za-z0-9-]{3,20}$`)
- `nombre`: obligatorio, 3–120 caracteres
- `correo`: obligatorio, formato de email, máximo 120 caracteres
- `materiaId`: obligatorio, entero positivo

**PHP no necesita repetir estas validaciones.** Python puede hacer validaciones mínimas (por ejemplo, con Pydantic), pero **Python es el dueño del 404, el 409 y el 400 por materia inexistente**: Java no consulta la BD y no duplica esas reglas, solo propaga lo que reciba.

## 7. Qué hace Java con lo que responde PHP

Esto le importa sobre todo a PHP, pero también a Python, porque PHP le pasa sus respuestas:

- Java envía `Content-Type: application/json` en POST y PUT, y codifica el `codigo` en la URL.
- **Cualquier respuesta 4xx o 5xx de PHP**, Java la devuelve a la interfaz **con el mismo código de estado y el mismo cuerpo**, siempre que el cuerpo sea JSON con el campo `error`.
- Si el error **no** trae `{"error": ...}` (una página HTML de error de PHP, un cuerpo vacío, un warning de PHP impreso), Java mantiene el código pero reemplaza el cuerpo por `{"error": "El servicio PHP respondió con error N"}`. **Funciona, pero el usuario verá un mensaje genérico.** Por eso es importante que todos los errores salgan en el formato del contrato.
- Si PHP responde **2xx con algo que no es JSON válido** (por ejemplo, un `Notice:` o `Warning:` de PHP impreso antes del JSON), Java responde **502 "Respuesta inválida del servicio PHP"**. En PHP, desactiven la salida de errores al navegador (`display_errors=0`) y envíen solo el JSON.
- Los campos extra en las respuestas se ignoran; los nombres de los campos deben ser **exactamente** los del contrato (camelCase: `materiaId`, `materiaNombre`).
- **Tiempos de espera de Java:** 3 s para conectar y 10 s para leer. Si PHP tarda más (por ejemplo, porque está esperando a un Python caído sin timeout), Java responde 502. **PHP debería tener su propio timeout hacia Python** (unos 5 s) para poder responder su propio 502 a tiempo.
- Java registra en su consola cada reenvío (`POST http://<IP-PC2>:8000/api/monitores -> 409 (3 ms)`). En la integración eso nos dice rápido en qué capa está el problema.

## 8. Indicaciones para PHP (Integrante 2)

- **Exponer las 6 rutas del contrato** y reenviar cada una a Python con el **mismo método, la misma ruta, el mismo cuerpo** y `Content-Type: application/json`.
- **Propagar el código de estado y el cuerpo de Python tal cual** (incluidos 400, 404 y 409). No reinterpretar ni reescribir los errores.
- En el DELETE, si Python responde 204, PHP responde 204 **sin cuerpo**. En el POST, si Python responde 201, PHP responde 201.
- Si Python no responde o no hay conexión → **502** `{"error": "Servicio Python no disponible"}`.
- Leer la URL de Python de una variable de entorno o archivo de configuración (por ejemplo `PYTHON_URL=http://<IP-PC3>:5000`), **nunca escrita en el código**.
- Responder siempre con `Content-Type: application/json; charset=utf-8` (y `json_encode(..., JSON_UNESCAPED_UNICODE)` para que las tildes salgan bien).
- Ojo con cURL de PHP: por defecto no lanza error en 4xx/5xx, así que hay que leer el código con `curl_getinfo($ch, CURLINFO_HTTP_CODE)` y devolverlo con `http_response_code(...)`. Si `curl_exec` devuelve `false`, es falta de conexión → 502.
- Escuchar en todas las interfaces: con el servidor integrado, `php -S 0.0.0.0:8000 ...` (no `localhost:8000`, porque así PC1 no puede entrar).
- Registrar cada petición reenviada (método, ruta, estado) en la consola: ayuda mucho en la integración.
- CORS no hace falta: a PHP solo lo llama Java, nunca el navegador.

## 9. Indicaciones para Python + BD (Integrante 3)

Esquema acordado (`database/schema.sql`):
- `materia(id INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(100) UNIQUE)`
- `monitor(codigo VARCHAR(20) PRIMARY KEY, nombre VARCHAR(120), correo VARCHAR(120), materia_id INT, FOREIGN KEY → materia.id)`
- Datos semilla de materias: `1 Algoritmos`, `2 Redes I`, `3 Ingeniería de Software III`.

Puntos clave:
- **En el JSON el campo es `materiaId` (camelCase); en la BD la columna es `materia_id`.** Hay que mapearlo (por ejemplo, con un alias en Pydantic o armando el diccionario a mano).
- **`materiaNombre`** se obtiene con un `JOIN` con `materia`. Va en los GET (lista y uno) y, de preferencia, también en la respuesta del POST y del PUT, para que la interfaz pueda mostrarlo sin consultar de nuevo.
- **POST:** si el código ya existe (llave primaria duplicada, error 1062 de MySQL) → **409**. Si la materia no existe (llave foránea, error 1452) → **400**. Si todo sale bien → **201** con el monitor creado.
- **PUT:** el cuerpo **no trae `codigo`** (va en la ruta). Si el monitor no existe → 404; si la materia no existe → 400.
- **DELETE:** **204 sin cuerpo**; 404 si no existe.
- **Formato de error:** FastAPI por defecto responde `{"detail": "..."}` y, en fallos de validación, **422**. **Ninguno de los dos está en el contrato.** Hay que agregar manejadores de excepciones para que todo salga como `{"error": "..."}` y para que la validación responda **400**, no 422.
- **Tildes:** la BD y la conexión en `utf8mb4` ("Ingeniería", "Pérez"). FastAPI ya responde JSON en UTF-8.
- **Escuchar en todas las interfaces:** `uvicorn main:app --host 0.0.0.0 --port 5000`.
- **Credenciales de MySQL** en un `.env` (no se sube). Si MySQL está en el mismo PC3, no hace falta abrir su puerto en el firewall: solo el 5000.
- **Plan B si MySQL falla en PC3: SQLite.** Hay que activar `PRAGMA foreign_keys=ON` en cada conexión (SQLite no revisa las llaves foráneas por defecto) y mapear `sqlite3.IntegrityError` igual que los errores de MySQL: llave primaria duplicada → **409** (equivale al 1062) y materia inexistente por llave foránea → **400** (equivale al 1452). El contrato no cambia.

## 10. Versiones

Como las capas solo se comunican por HTTP con JSON, **no tienen que coincidir en versión**: cada quien puede usar la que tenga instalada. Pero hay que **fijarlas y anotarlas**, por tres razones: el documento final y las guías de ejecución de cada PC las necesitan, la demo debe poder correr en otro computador, y el código que genere su modelo depende de la versión (por ejemplo, Pydantic 1 y 2 son bastante distintos).

### PC1 (ya fijadas)

| Componente | Versión |
|---|---|
| Java | 17 (en el `pom.xml`) |
| Spring Boot | 3.5.7 |
| Maven | 3.9.9 (vía Maven Wrapper, `mvnw`: no hay que instalarlo) |
| Bootstrap (interfaz) | 5.3, por CDN |

### Mínimos recomendados para PC2 y PC3

| Componente | Mínimo | Por qué |
|---|---|---|
| PHP | **8.0 o superior** (ideal 8.2+) | Mejor manejo de errores y de JSON; las versiones 7.x ya no tienen soporte. Hace falta la extensión **curl** (o usar `file_get_contents` con contexto HTTP). |
| Python | **3.10 o superior** | Lo piden las versiones recientes de FastAPI y Pydantic 2. |
| FastAPI | versión reciente con **Pydantic 2** | Si su modelo genera código de Pydantic 1 (`orm_mode`, `.dict()`), no va a funcionar con Pydantic 2 (`from_attributes`, `.model_dump()`). Díganle qué versión usan. |
| MySQL | **8.0** | Usa `utf8mb4` por defecto, lo que evita problemas con las tildes. En 5.7 o en MariaDB hay que configurarlo a mano. |
| Conector MySQL para Python | el que elijan (`mysql-connector-python`, `PyMySQL`, SQLAlchemy...) | Da igual cuál, pero que quede fijado. |

### Qué debe hacer cada uno

- **Python (Integrante 3):** un `python-service/requirements.txt` con **versiones exactas** (`fastapi==...`, `uvicorn==...`, el conector de MySQL). Lo más fácil: `pip freeze > requirements.txt` desde su `.venv`. Así `pip install -r requirements.txt` instala lo mismo en cualquier PC.
- **PHP (Integrante 2):** anotar la versión (`php -v`) y las extensiones que usa en un `php-service/README.md`. Si usan Composer, subir `composer.json` y `composer.lock` (la carpeta `vendor/` no se sube).
- **BD (Integrante 3):** anotar la versión de MySQL (`mysql --version`) y dejar en `schema.sql` el `CHARACTER SET utf8mb4`.
- **Todos:** pasarle las versiones al Integrante 1 o al Integrante 2 para el documento final, junto con los pasos para correr su PC.

## 11. Integración en red

- Antes de cada integración: ver la IP con `ipconfig` (Windows) o `ip a` (Linux/macOS) y compartirla en el grupo. Las IPs pueden cambiar de un día a otro.
- **Abrir el puerto en el firewall** (en Windows: regla de entrada TCP): PC1 → 8080, PC2 → 8000, PC3 → 5000.
- Comprobar primero con `ping` entre los PC, y después desde el navegador de otro PC: `http://<IP-PC2>:8000/api/materias`, `http://<IP-PC3>:5000/api/materias`.
- Si el Wi-Fi de la universidad aísla los equipos: hotspot de un celular, o Tailscale/ZeroTier.
- Java se arranca apuntando a PHP con la variable `PHP_URL=http://<IP-PC2>:8000`. Lo equivalente en PHP sería `PYTHON_URL=http://<IP-PC3>:5000`.
- Fechas: **integración n.º 1 jueves 19 en la noche** (mínimo POST y GET de punta a punta) e **integración final viernes 20 en la tarde**.

## 12. Cómo probar su servicio antes de la integración

Pueden probar su capa sola con `curl` o con la colección de Postman del equipo. Cambien el puerto según la capa (8000 PHP, 5000 Python).

```bash
curl -i http://localhost:5000/api/materias
```
```bash
curl -i http://localhost:5000/api/monitores/MON999
```
```bash
curl -i -X POST http://localhost:5000/api/monitores -H "Content-Type: application/json" --data-binary @monitor.json
```
```bash
curl -i -X DELETE http://localhost:5000/api/monitores/MON001
```

Contenido de `monitor.json` (guardado en UTF-8):
```json
{"codigo": "MON003", "nombre": "Luisa Díaz", "correo": "luisa@ucaldas.edu.co", "materiaId": 3}
```

Lista mínima que su servicio debería pasar:
- [ ] GET de materias y de monitores → 200 con arreglos JSON
- [ ] GET de un código inexistente → 404 con `{"error": ...}`
- [ ] POST válido → 201; repetir el mismo POST → 409
- [ ] POST con `materiaId: 99` → 400
- [ ] PUT sin `codigo` en el cuerpo → 200 con el monitor actualizado; PUT a un código inexistente → 404
- [ ] DELETE → 204 sin cuerpo; repetirlo → 404
- [ ] Ningún error sale como `{"detail": ...}`, 422 ni HTML
- [ ] Las tildes salen bien en las respuestas
- [ ] (PHP) Con Python apagado → 502 `{"error": ...}`

**Tip para Git Bash en Windows:** si mandan el JSON directamente con `-d '{...}'` y lleva tildes, Git Bash puede dañarlas y el servidor recibirá un JSON inválido. Usen un archivo con `--data-binary @archivo.json`, o Postman.

**Referencia:** tengo un mock de PHP (Python con la librería estándar) que implementa este contrato en memoria, con los 404, 409 y 400. Si a alguno le sirve como ejemplo del comportamiento esperado, me lo piden y se lo paso.

## 13. Resumen de reglas para su modelo

1. No cambiar rutas, campos, nombres (camelCase) ni códigos de estado del contrato sin acordarlo con el equipo.
2. Todos los errores salen como `{"error": "mensaje en español"}`.
3. Nunca escribir IPs, URLs de otros PC ni contraseñas en el código: variables de entorno o `.env`.
4. Escuchar en `0.0.0.0`, no en `localhost`.
5. Cada capa habla solo con la siguiente.
6. Trabajar solo en su carpeta y en su rama; commits pequeños en español.
7. Registrar cada petición en la consola (método, ruta, estado).
8. Fijar las versiones (`requirements.txt` con versiones exactas en Python; la versión de PHP en un README) y generar código acorde a esas versiones.
