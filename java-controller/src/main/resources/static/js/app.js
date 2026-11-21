// Interfaz de monitores académicos.
// Solo habla con el controlador Java mediante rutas relativas /api/...
'use strict';

const API = '/api';

// Monitores cargados en la tabla, por código (para abrir los modales sin pedir de nuevo).
const monitores = new Map();

const $ = (selector) => document.querySelector(selector);

const modalEditar = new bootstrap.Modal($('#modal-editar'));
const modalEliminar = new bootstrap.Modal($('#modal-eliminar'));
let codigoAEliminar = null;

// ---------------------------------------------------------------------------
// Comunicación con Java
// ---------------------------------------------------------------------------

/**
 * Hace una petición a Java y devuelve el JSON de la respuesta (o null si es 204).
 * Si la respuesta es un error, lanza un Error con el mensaje del campo "error"
 * que manda el backend (400, 404, 409, 502...).
 */
async function api(metodo, ruta, cuerpo) {
  const opciones = { method: metodo, headers: { Accept: 'application/json' } };
  if (cuerpo !== undefined) {
    opciones.headers['Content-Type'] = 'application/json';
    opciones.body = JSON.stringify(cuerpo);
  }

  let respuesta;
  try {
    respuesta = await fetch(API + ruta, opciones);
  } catch {
    throw crearError('No se pudo conectar con el controlador Java.', 0);
  }

  if (respuesta.status === 204) return null;

  const texto = await respuesta.text();
  let datos = null;
  try {
    datos = texto ? JSON.parse(texto) : null;
  } catch {
    // Cuerpo que no es JSON: se maneja abajo con un mensaje genérico.
  }

  if (!respuesta.ok) {
    const mensaje = datos && datos.error ? datos.error : `Error ${respuesta.status} del servidor.`;
    throw crearError(mensaje, respuesta.status);
  }
  return datos;
}

function crearError(mensaje, estado) {
  const error = new Error(mensaje);
  error.estado = estado;
  return error;
}

// ---------------------------------------------------------------------------
// Mensajes (toasts y alertas)
// ---------------------------------------------------------------------------

function mostrarToast(mensaje, tipo = 'success') {
  const toast = document.createElement('div');
  toast.className = `toast align-items-center text-bg-${tipo} border-0`;
  toast.setAttribute('role', tipo === 'danger' ? 'alert' : 'status');
  toast.setAttribute('aria-live', tipo === 'danger' ? 'assertive' : 'polite');
  toast.innerHTML = `
    <div class="d-flex">
      <div class="toast-body"></div>
      <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Cerrar"></button>
    </div>`;
  toast.querySelector('.toast-body').textContent = mensaje;
  $('#toasts').appendChild(toast);
  toast.addEventListener('hidden.bs.toast', () => toast.remove());
  new bootstrap.Toast(toast, { delay: tipo === 'danger' ? 7000 : 4000 }).show();
}

function mostrarError(error) {
  mostrarToast(etiquetaError(error) + error.message, 'danger');
}

/** Prefijo con el código HTTP, útil en la demo para ver qué respondió el backend. */
function etiquetaError(error) {
  return error.estado ? `[${error.estado}] ` : '';
}

/** Muestra una alerta de Bootstrap dentro de un contenedor (por ejemplo, un modal). */
function mostrarAlerta(contenedor, mensaje, tipo = 'danger') {
  contenedor.innerHTML = '';
  const alerta = document.createElement('div');
  alerta.className = `alert alert-${tipo} py-2`;
  alerta.setAttribute('role', 'alert');
  alerta.textContent = mensaje;
  contenedor.appendChild(alerta);
}

function bloquear(boton, ocupado) {
  boton.disabled = ocupado;
  if (ocupado) {
    boton.dataset.texto = boton.textContent;
    boton.textContent = 'Procesando…';
  } else if (boton.dataset.texto) {
    boton.textContent = boton.dataset.texto;
  }
}

// ---------------------------------------------------------------------------
// Materias (desplegables)
// ---------------------------------------------------------------------------

async function cargarMaterias() {
  const selects = document.querySelectorAll('.select-materia');
  try {
    const materias = await api('GET', '/materias');
    selects.forEach((select) => {
      select.innerHTML = '<option value="">Selecciona una materia</option>';
      materias.forEach((materia) => select.add(new Option(materia.nombre, materia.id)));
    });
  } catch (error) {
    selects.forEach((select) => {
      select.innerHTML = '<option value="">No se pudieron cargar las materias</option>';
    });
    mostrarError(error);
  }
}

// ---------------------------------------------------------------------------
// RF2: listado
// ---------------------------------------------------------------------------

async function cargarMonitores() {
  const cuerpo = $('#tabla-monitores');
  try {
    const lista = await api('GET', '/monitores');
    monitores.clear();
    lista.forEach((monitor) => monitores.set(monitor.codigo, monitor));
    pintarTabla(lista);
  } catch (error) {
    cuerpo.innerHTML = '';
    cuerpo.appendChild(filaMensaje('No se pudo cargar el listado: ' + error.message, 'text-danger'));
    $('#contador').textContent = '0';
    mostrarError(error);
  }
}

function pintarTabla(lista) {
  const cuerpo = $('#tabla-monitores');
  cuerpo.innerHTML = '';
  $('#contador').textContent = lista.length;

  if (lista.length === 0) {
    cuerpo.appendChild(filaMensaje('No hay monitores registrados.', 'text-body-secondary'));
    return;
  }

  lista.forEach((monitor) => {
    const fila = document.createElement('tr');
    fila.dataset.codigo = monitor.codigo;
    fila.append(
      celda(monitor.codigo, 'codigo'),
      celda(monitor.nombre),
      celda(monitor.correo),
      celda(monitor.materiaNombre ?? `Materia ${monitor.materiaId}`),
    );

    const acciones = document.createElement('td');
    acciones.className = 'text-end acciones';
    acciones.append(
      boton('Editar', 'btn-outline-primary', () => abrirEdicion(monitor.codigo)),
      boton('Eliminar', 'btn-outline-danger ms-1', () => abrirEliminacion(monitor.codigo)),
    );
    fila.appendChild(acciones);
    cuerpo.appendChild(fila);
  });
}

function celda(texto, clase) {
  const td = document.createElement('td');
  td.textContent = texto;
  if (clase) td.className = clase;
  return td;
}

function boton(texto, clases, alHacerClic) {
  const b = document.createElement('button');
  b.type = 'button';
  b.className = `btn btn-sm ${clases}`;
  b.textContent = texto;
  b.addEventListener('click', alHacerClic);
  return b;
}

function filaMensaje(texto, clase) {
  const fila = document.createElement('tr');
  const td = document.createElement('td');
  td.colSpan = 5;
  td.className = `text-center py-4 ${clase}`;
  td.textContent = texto;
  fila.appendChild(td);
  return fila;
}

// ---------------------------------------------------------------------------
// RF1: registro
// ---------------------------------------------------------------------------

$('#form-registro').addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const form = evento.currentTarget;
  if (!form.checkValidity()) {
    form.reportValidity();
    return;
  }

  const monitor = {
    codigo: $('#reg-codigo').value.trim(),
    nombre: $('#reg-nombre').value.trim(),
    correo: $('#reg-correo').value.trim(),
    materiaId: Number($('#reg-materia').value),
  };

  const btn = $('#btn-registrar');
  bloquear(btn, true);
  try {
    await api('POST', '/monitores', monitor);
    mostrarToast(`Monitor ${monitor.codigo} registrado.`);
    form.reset();
    await cargarMonitores();
  } catch (error) {
    mostrarError(error); // 400 validación, 409 código duplicado, 502 PHP caído...
  } finally {
    bloquear(btn, false);
  }
});

// ---------------------------------------------------------------------------
// RF3: búsqueda por código
// ---------------------------------------------------------------------------

$('#form-buscar').addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const codigo = $('#buscar-codigo').value.trim();
  const resultado = $('#resultado-busqueda');
  quitarResaltado();
  if (!codigo) return;

  try {
    const monitor = await api('GET', '/monitores/' + encodeURIComponent(codigo));
    resultado.innerHTML = '';
    resultado.appendChild(tarjetaResultado(monitor));
    const fila = document.querySelector(`#tabla-monitores tr[data-codigo="${CSS.escape(monitor.codigo)}"]`);
    if (fila) fila.classList.add('resaltado');
  } catch (error) {
    if (error.estado === 404) {
      mostrarAlerta(resultado, `No existe un monitor con el código "${codigo}".`, 'warning');
    } else {
      mostrarAlerta(resultado, etiquetaError(error) + error.message, 'danger');
    }
  }
});

$('#btn-limpiar-busqueda').addEventListener('click', () => {
  $('#buscar-codigo').value = '';
  $('#resultado-busqueda').innerHTML = '';
  quitarResaltado();
});

function quitarResaltado() {
  document.querySelectorAll('#tabla-monitores tr.resaltado').forEach((f) => f.classList.remove('resaltado'));
}

function tarjetaResultado(monitor) {
  const alerta = document.createElement('div');
  alerta.className = 'alert alert-success mb-0';
  const datos = [
    ['Código', monitor.codigo],
    ['Nombre', monitor.nombre],
    ['Correo', monitor.correo],
    ['Materia', monitor.materiaNombre ?? `Materia ${monitor.materiaId}`],
  ];
  const lista = document.createElement('dl');
  lista.className = 'row mb-0';
  datos.forEach(([etiqueta, valor]) => {
    const dt = document.createElement('dt');
    dt.className = 'col-sm-3';
    dt.textContent = etiqueta;
    const dd = document.createElement('dd');
    dd.className = 'col-sm-9 mb-1';
    dd.textContent = valor;
    lista.append(dt, dd);
  });
  alerta.appendChild(lista);
  return alerta;
}

// ---------------------------------------------------------------------------
// RF4: edición
// ---------------------------------------------------------------------------

function abrirEdicion(codigo) {
  const monitor = monitores.get(codigo);
  if (!monitor) return;
  $('#error-editar').innerHTML = '';
  $('#edit-codigo').value = monitor.codigo;
  $('#edit-nombre').value = monitor.nombre;
  $('#edit-correo').value = monitor.correo;
  $('#edit-materia').value = String(monitor.materiaId);
  modalEditar.show();
}

$('#form-editar').addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const form = evento.currentTarget;
  if (!form.checkValidity()) {
    form.reportValidity();
    return;
  }

  const codigo = $('#edit-codigo').value;
  // El PUT siempre envía los tres campos (acuerdo del equipo); el código va en la ruta.
  const cambios = {
    nombre: $('#edit-nombre').value.trim(),
    correo: $('#edit-correo').value.trim(),
    materiaId: Number($('#edit-materia').value),
  };

  const btn = $('#btn-guardar');
  bloquear(btn, true);
  try {
    await api('PUT', '/monitores/' + encodeURIComponent(codigo), cambios);
    modalEditar.hide();
    mostrarToast(`Monitor ${codigo} actualizado.`);
    await cargarMonitores();
  } catch (error) {
    mostrarAlerta($('#error-editar'), etiquetaError(error) + error.message);
    if (error.estado === 404) await cargarMonitores(); // ya no existe: refresca la tabla
  } finally {
    bloquear(btn, false);
  }
});

// ---------------------------------------------------------------------------
// RF5: eliminación (con modal de confirmación, sin confirm())
// ---------------------------------------------------------------------------

function abrirEliminacion(codigo) {
  const monitor = monitores.get(codigo);
  if (!monitor) return;
  codigoAEliminar = codigo;
  $('#error-eliminar').innerHTML = '';
  $('#eliminar-descripcion').textContent = `${monitor.codigo} (${monitor.nombre})`;
  modalEliminar.show();
}

$('#btn-confirmar-eliminar').addEventListener('click', async () => {
  if (!codigoAEliminar) return;
  const codigo = codigoAEliminar;
  const btn = $('#btn-confirmar-eliminar');
  bloquear(btn, true);
  try {
    await api('DELETE', '/monitores/' + encodeURIComponent(codigo));
    modalEliminar.hide();
    mostrarToast(`Monitor ${codigo} eliminado.`);
    codigoAEliminar = null;
    await cargarMonitores();
  } catch (error) {
    mostrarAlerta($('#error-eliminar'), etiquetaError(error) + error.message);
    if (error.estado === 404) await cargarMonitores();
  } finally {
    bloquear(btn, false);
  }
});

// ---------------------------------------------------------------------------
// Inicio
// ---------------------------------------------------------------------------

$('#btn-recargar').addEventListener('click', cargarMonitores);

cargarMaterias();
cargarMonitores();
