/**
 * ═══════════════════════════════════════════════════════════════════════════
 *  Bot de WhatsApp — Laboratorio GS
 *
 *  Escucha los grupos de comprobantes, detecta los pagos triangulados y los
 *  registra en el sistema. Maneja dos formas de mandar el comprobante:
 *
 *    1) Comprobante + "Emisor (Receptor)" en el MISMO mensaje (caption)
 *    2) Comprobante en un mensaje, y "Emisor (Receptor)" en el SIGUIENTE
 *
 *  Lee el monto y el N° de operación del comprobante (PDF o imagen) y valida
 *  que el receptor del pie coincida con el "Para" del comprobante.
 *
 *  La sesión se guarda con LocalAuth → escaneás el QR UNA sola vez.
 * ═══════════════════════════════════════════════════════════════════════════
 */

require('dotenv').config();
const { Client, LocalAuth } = require('whatsapp-web.js');
const qrcode = require('qrcode-terminal');
const axios = require('axios');
const { createWorker } = require('tesseract.js');
const pdfParse = require('pdf-parse/lib/pdf-parse.js');
const { GoogleGenerativeAI } = require('@google/generative-ai');
const http = require('http');
const QRCode = require('qrcode');

let ocrWorker = null;

// ─── Configuración ──────────────────────────────────────────────────────────
const BACKEND_URL     = process.env.BACKEND_URL || 'http://localhost:8080';
const BACKEND_ENABLED = process.env.BACKEND_ENABLED === 'true';
const BOT_API_KEY     = process.env.BOT_API_KEY || '';
const GRUPOS = (process.env.GRUPOS || '')
  .split(',').map(g => g.trim().toLowerCase()).filter(Boolean);

// ─── Gemini (IA para leer cualquier billetera + fotos) ───────────────────────
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || '';
const GEMINI_MODEL   = process.env.GEMINI_MODEL || 'gemini-2.5-flash';
const GEMINI_ENABLED = process.env.GEMINI_ENABLED === 'true' && !!GEMINI_API_KEY;
if (process.env.GEMINI_ENABLED === 'true' && !GEMINI_API_KEY) {
  console.warn('[Bot] ADVERTENCIA: GEMINI_ENABLED=true pero GEMINI_API_KEY está vacía — se usarán reglas locales.');
}
let geminiModel = null;
if (GEMINI_ENABLED) {
  const genAI = new GoogleGenerativeAI(GEMINI_API_KEY);
  geminiModel = genAI.getGenerativeModel({ model: GEMINI_MODEL });
}

// Comprobantes esperando su "Emisor (Receptor)". Clave = grupo|usuario.
const pendientes = new Map();
const TIMEOUT_PENDIENTE = 5 * 60 * 1000;   // 5 minutos

// Estado del bot expuesto a la pantalla web "Estado del bot".
let estadoBot = {
  conectado: false,
  qrDataUrl: null,                         // QR como imagen (data URL) para re-vincular desde el navegador
  motivo: 'Iniciando...',
  ultimaActualizacion: new Date().toISOString(),
};
const BOT_HTTP_PORT = parseInt(process.env.BOT_HTTP_PORT || '3001', 10);

// ─── Cliente de WhatsApp ─────────────────────────────────────────────────────
const client = new Client({
  authStrategy: new LocalAuth(),
  puppeteer: {
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
    // En Docker usamos el Chromium del sistema; en dev queda undefined (puppeteer usa el suyo).
    executablePath: process.env.PUPPETEER_EXECUTABLE_PATH || undefined,
  },
});

client.on('qr', async (qr) => {
  console.log('\n┌──────────────────────────────────────────────────────┐');
  console.log('│  Escaneá el QR (terminal) o desde la pantalla web:   │');
  console.log('│  WhatsApp → Dispositivos vinculados → Vincular        │');
  console.log('└──────────────────────────────────────────────────────┘\n');
  qrcode.generate(qr, { small: true });
  // Lo exponemos también como imagen para la pantalla "Estado del bot"
  try { estadoBot.qrDataUrl = await QRCode.toDataURL(qr); } catch (e) { estadoBot.qrDataUrl = null; }
  estadoBot.conectado = false;
  estadoBot.motivo = 'Esperando vinculación — escaneá el QR';
  estadoBot.ultimaActualizacion = new Date().toISOString();
});

client.on('authenticated', () => console.log('🔐 Autenticado — sesión guardada.'));
client.on('auth_failure', (m) => {
  console.error('❌ Falló la autenticación:', m);
  estadoBot.conectado = false;
  estadoBot.motivo = 'Falló la autenticación';
  estadoBot.ultimaActualizacion = new Date().toISOString();
});
client.on('disconnected', async (r) => {
  console.warn('⚠️  Bot desconectado:', r, '— intentando reconectar...');
  estadoBot.conectado = false;
  estadoBot.qrDataUrl = null;
  estadoBot.motivo = 'Desconectado: ' + String(r);
  estadoBot.ultimaActualizacion = new Date().toISOString();
  // Reintenta: si se perdió la sesión, vuelve a disparar 'qr' (nuevo QR para la pantalla)
  try { await client.initialize(); } catch (e) { console.error('No se pudo reiniciar:', e.message); }
});

client.on('ready', async () => {
  estadoBot.conectado = true;
  estadoBot.qrDataUrl = null;
  estadoBot.motivo = GRUPOS.length
    ? `Conectado y escuchando ${GRUPOS.length} grupo(s).`
    : 'Conectado y escuchando todos los grupos.';
  estadoBot.ultimaActualizacion = new Date().toISOString();
  console.log('\n✅ Bot CONECTADO y escuchando.');
  console.log(GRUPOS.length ? `   Grupos: ${GRUPOS.join(' · ')}` : '   Escuchando TODOS los grupos.');
  console.log(`   Backend: ${BACKEND_ENABLED ? BACKEND_URL + ' (ACTIVO)' : 'desactivado (solo logueo)'}`);
  console.log(`   Lectura: ${GEMINI_ENABLED ? '✨ IA Gemini (cualquier billetera + fotos)' : 'reglas locales'}`);
  console.log('   Preparando OCR...');
  ocrWorker = await createWorker('spa');
  console.log('   ✅ OCR listo.\n');
});

// ─── Manejo de mensajes ──────────────────────────────────────────────────────
client.on('message', async (msg) => {
  try {
    const chat = await msg.getChat();
    if (!chat.isGroup) return;
    if (GRUPOS.length && !GRUPOS.includes(chat.name.toLowerCase())) return;

    const contacto = await msg.getContact();
    const clave = `${chat.id._serialized}|${contacto.number}`;

    if (msg.hasMedia) {
      // ── Llegó un comprobante ──
      const lectura = await leerComprobante(msg);
      const pie = parsearPie(msg.body || '');

      if (pie.receptor) {
        // Comprobante + pie en el mismo mensaje → procesar directo
        await procesarPago(msg, chat, contacto, pie, lectura);
      } else {
        // Sin pie → guardar y esperar el siguiente mensaje
        pendientes.set(clave, { msg, chat, contacto, lectura, ts: Date.now() });
        console.log(`\n📎 [${chat.name}] Comprobante de ${contacto.pushname || contacto.number} — esperando "Emisor (Receptor)"...`);
        await msg.reply('📎 Recibí el comprobante. Ahora mandá quién a quién: *Emisor (Receptor)*\nEj: Dr. García (Carlos López)');
      }

    } else if (msg.body) {
      // ── Llegó texto: ¿es el pie de un comprobante pendiente? ──
      const pie = parsearPie(msg.body);
      if (!pie.receptor) return;

      const pend = pendientes.get(clave);
      if (pend && (Date.now() - pend.ts) < TIMEOUT_PENDIENTE) {
        pendientes.delete(clave);
        await procesarPago(pend.msg, pend.chat, pend.contacto, pie, pend.lectura, msg);
      }
    }
  } catch (err) {
    console.error('❌ Error:', err.message);
  }
});

// ─── Procesar un pago (comprobante + pie ya emparejados) ─────────────────────
async function procesarPago(msgComprobante, chat, contacto, pie, lectura, msgPie) {
  const cargadoPorNombre   = contacto.pushname || contacto.name || 'Desconocido';
  const cargadoPorTelefono = contacto.number;
  const responder = (txt) => (msgPie || msgComprobante).reply(txt);

  // Monto: el del comprobante (PDF) o, si no se leyó, el que pusieron en el pie
  const monto = lectura.monto ?? pie.montoManual;
  const origenMonto = lectura.monto ? `comprobante (${lectura.confianza})` : (pie.montoManual ? 'pie del mensaje' : 'no hay');
  const confianzaMonto = lectura.monto ? lectura.confianza : (pie.montoManual ? 'manual' : 'baja');

  console.log('\n📩 ─────────── COMPROBANTE A PROCESAR ───────────');
  console.log(`   Grupo:        ${chat.name}`);
  console.log(`   Cargado por:  ${cargadoPorNombre} (${cargadoPorTelefono})`);
  console.log(`   Pie:          Emisor="${pie.emisor}"  Receptor="${pie.receptor}"`);
  console.log(`   Monto:        ${monto ?? '⚠ no detectado'} (origen: ${origenMonto})`);
  console.log(`   N° operación: ${lectura.idOperacion ?? '⚠ no detectado'}`);
  console.log(`   Comprobante:  De="${lectura.de ?? '?'}"  Para="${lectura.para ?? '?'}"`);

  if (!monto) {
    console.log('   ⚠ No pude leer el monto (ni del comprobante ni del pie).');
    await responder(
      `⚠️ Recibí el comprobante para *${pie.receptor}* pero no pude leer el monto.\n` +
      `Si es una foto, reenviá el pie con el monto:\n*${pie.emisor || 'Emisor'} (${pie.receptor}) 10000*\n` +
      `O mandá el comprobante en *PDF* (se lee solo).`
    );
    return;
  }

  // ── Validación cruzada: pie vs De/Para del comprobante ──
  const validacion = validarPersonas(pie, lectura);
  console.log(`   Validación:   ${validacion.ok ? '✅ coincide' : '⚠ NO coincide'} (${validacion.detalle})`);

  const montoFmt = monto.toLocaleString('es-AR');

  if (!BACKEND_ENABLED) {
    console.log('   ℹ Backend desactivado — solo logueo.');
    console.log('   ───────────────────────────────────────────────\n');
    await responder(
      `🧪 *Modo prueba* — esto detecté:\n` +
      `• Monto: *$${montoFmt}*\n` +
      `• Pagó: ${pie.emisor || '(sin emisor)'}\n` +
      `• Recibió: *${pie.receptor}*\n` +
      `• N° operación: ${lectura.idOperacion || '—'}\n` +
      (validacion.ok ? `✅ Los datos coinciden con el comprobante.` : `⚠️ *No coincide:* ${validacion.detalle}`)
    );
    return;
  }

  // ── Registrar en el backend ──
  let resultado;
  try {
    resultado = await registrarPago({
      monto,
      emisor: pie.emisor,
      receptorNombre: pie.receptor,
      idOperacion: lectura.idOperacion,
      cargadoPorNombre, cargadoPorTelefono,
      grupoOrigen: chat.name,
      // archivo del comprobante para guardar en MinIO
      comprobanteBase64: lectura.comprobanteBase64,
      comprobanteMime: lectura.comprobanteMime,
      comprobanteNombre: lectura.comprobanteNombre,
    });
  } catch (e) {
    const msgErr = e.response?.data?.mensaje || e.message;
    console.log('   ❌ Error al conectar con el backend:', msgErr);
    await responder(`⚠️ No se pudo registrar (error de conexión con el sistema): ${msgErr}`);
    return;
  }

  // El backend SIEMPRE responde 200 con el estado del registro
  const estado = resultado?.estado;
  console.log(`   Resultado:    ${estado} — ${resultado?.mensaje || ''}`);
  console.log('   ───────────────────────────────────────────────\n');

  if (estado === 'DUPLICADO') {
    await responder(`ℹ️ Ese comprobante ya estaba registrado (operación ${lectura.idOperacion || '—'}).`);
    return;
  }
  if (estado !== 'REGISTRADO') {
    await responder(`⚠️ No se registró: ${resultado?.mensaje || 'el receptor no es un empleado ni un proveedor conocido.'}`);
    return;
  }

  // Registrado OK — distinguimos sueldo vs pago a proveedor
  const tipoTxt = resultado.tipoReceptor === 'PROVEEDOR' ? 'Pago a proveedor' : 'Sueldo';
  let aviso = '';
  if (confianzaMonto === 'baja') aviso += `\n⚠️ No estoy seguro del monto, *verificalo*.`;
  if (!validacion.ok)            aviso += `\n⚠️ El comprobante no coincide del todo con "${pie.receptor}", *revisalo*.`;

  await responder(
    `✅ *Cargado al sistema correctamente* (${tipoTxt})\n` +
    `• Monto: $${montoFmt}\n` +
    `• Pagó: ${pie.emisor || '—'}\n` +
    `• Recibió: ${resultado.receptorResuelto || pie.receptor}\n` +
    `• N° operación: ${lectura.idOperacion || '—'}` +
    aviso
  );
}

// ─── Lectura del comprobante (PDF o imagen) ──────────────────────────────────
async function leerComprobante(msg) {
  const vacio = { monto: null, confianza: 'baja', idOperacion: null, de: null, para: null, texto: '' };
  try {
    const media = await msg.downloadMedia();
    if (!media || !media.data) return vacio;
    const buffer = Buffer.from(media.data, 'base64');
    const esPdf = media.mimetype === 'application/pdf';
    const esImg = media.mimetype.startsWith('image/');
    if (!esPdf && !esImg) return vacio;

    // Archivo para guardar en MinIO (lo manda el bot al backend)
    const archivo = {
      comprobanteBase64: media.data,
      comprobanteMime: media.mimetype,
      comprobanteNombre: media.filename || ('comprobante' + (esPdf ? '.pdf' : '.jpg')),
    };

    // Texto del PDF (para reglas y para Gemini-texto)
    let texto = '';
    if (esPdf) {
      texto = (await pdfParse(buffer)).text || '';
      console.log('   (PDF — texto extraído)');
    }

    // 1) IA Gemini — generaliza a CUALQUIER billetera y lee fotos
    if (GEMINI_ENABLED && geminiModel) {
      const g = await leerConGemini(media, texto);
      if (g && g.monto) {
        console.log('   ✨ Leído con IA (Gemini)');
        return { monto: g.monto, confianza: 'alta', idOperacion: g.idOperacion, de: g.de, para: g.para, texto, ...archivo };
      }
    }

    // 2) Fallback: reglas locales (MP / Personal Pay)
    if (esImg && !texto && ocrWorker) {
      texto = (await ocrWorker.recognize(buffer)).data.text || '';
      console.log('   (imagen — OCR local)');
    }
    logTexto(texto);
    const { monto, confianza } = extraerMonto(texto);
    const idOperacion = extraerIdOperacion(texto);
    const { de, para } = extraerDePara(texto);
    return { monto, confianza, idOperacion, de, para, texto, ...archivo };
  } catch (e) {
    console.log('   (error leyendo comprobante:', e.message, ')');
    return vacio;
  }
}

function logTexto(texto) {
  if (!texto) return;
  console.log('   ┌─ Texto del comprobante ─────────');
  texto.split('\n').filter(l => l.trim()).slice(0, 25).forEach(l => console.log('   │ ' + l.trim()));
  console.log('   └─────────────────────────────────');
}

/**
 * Lee el comprobante con IA (Gemini). Funciona con cualquier billetera y con
 * fotos (visión). Devuelve { monto, de, para, idOperacion } o null si falla.
 */
async function leerConGemini(media, textoPdf) {
  if (!geminiModel) return null;
  try {
    const instruccion =
      'Sos un extractor de datos de comprobantes de transferencia argentinos ' +
      '(Mercado Pago, Personal Pay, Ualá, Naranja X, MODO, bancos, etc). ' +
      'Devolvé SOLO un JSON válido, sin markdown ni texto extra, con esta forma exacta:\n' +
      '{"monto": <entero en pesos sin decimales ni puntos, o null>, ' +
      '"emisor": <nombre de quien ENVIÓ el dinero (origen/de), o null>, ' +
      '"receptor": <nombre de quien RECIBIÓ el dinero (destino/para), o null>, ' +
      '"idOperacion": <número o código de operación, o null>}\n' +
      'Reglas: el monto es la cantidad transferida (ej 60000), no el CBU ni el nro de cuenta. ' +
      'Si un dato no figura, poné null. No inventes nada.';

    // Tanto imagen como PDF se mandan como archivo directo: Gemini lee el
    // original (con acentos correctos), mucho mejor que el texto extraído por
    // pdf-parse, que rompe los acentos ("Nicolás" → "Nicol S").
    const result = await geminiModel.generateContent([
      instruccion,
      { inlineData: { mimeType: media.mimetype, data: media.data } },
    ]);

    let txt = result.response.text().trim()
      .replace(/^```json\s*/i, '').replace(/^```\s*/, '').replace(/```\s*$/, '').trim();
    const d = JSON.parse(txt);
    return {
      monto: d.monto ? Math.round(Number(d.monto)) : null,
      de: d.emisor || null,
      para: d.receptor || null,
      idOperacion: d.idOperacion ? String(d.idOperacion) : null,
    };
  } catch (e) {
    console.log('   (Gemini falló:', e.message, '— uso reglas)');
    return null;
  }
}

// ─── Parsers ─────────────────────────────────────────────────────────────────

/**
 * Pie: "EMISOR (RECEPTOR)" → { emisor, receptor, montoManual }.
 * El monto es OPCIONAL: si lo ponés (antes o después del paréntesis), se usa
 * como respaldo cuando el comprobante es una foto que el OCR no pudo leer.
 *   "Dr García (Carlos López)"        → sin monto (se lee del comprobante)
 *   "Dr García (Carlos López) 10000"  → con monto manual
 */
function parsearPie(texto) {
  const r = { emisor: null, receptor: null, montoManual: null };
  if (!texto) return r;
  const m = texto.match(/^(.*?)\(([^)]+)\)(.*)$/s);
  if (!m) return r;
  r.emisor = m[1].trim() || null;
  r.receptor = m[2].trim();

  // Monto opcional: buscar un número en lo que rodea al paréntesis
  const resto = `${m[1]} ${m[3]}`;
  const mm = resto.match(/\$?\s*(\d{1,3}(?:\.\d{3})+(?:,\d{2})?|\d{3,}(?:,\d{2})?)/);
  if (mm) {
    const v = Math.round(parseFloat(mm[1].replace(/\./g, '').replace(',', '.')));
    if (v >= 100) r.montoManual = v;
  }
  return r;
}

/**
 * Monto: solo números con formato de dinero ($60.000 / $60.000,00). Descarta IDs.
 * Maneja el caso de Personal Pay donde el monto viene partido en líneas
 * ("$" / "60.000" / "00") uniendo el texto antes de buscar.
 */
function extraerMonto(texto) {
  const claves = /(monto|importe|total|transferiste|enviaste|enviaron|pagaste|recibiste|enviado|recibido)/i;

  // Unimos líneas para tolerar montos partidos: "$\n60.000\n00" → "$ 60.000 00"
  // y también el caso normal. Trabajamos sobre el texto "aplanado" por bloques.
  const lineas = texto.split('\n');
  const candidatos = [];

  for (let i = 0; i < lineas.length; i++) {
    // Bloque: la línea + las 2 siguientes (por si el monto está partido)
    const bloque = (lineas[i] + ' ' + (lineas[i + 1] || '') + ' ' + (lineas[i + 2] || '')).trim();
    const enClave = claves.test(lineas[i]) || claves.test(lineas[Math.max(0, i - 1)] || '');

    // $ opcional, miles obligatorio (60.000), centavos opcionales separados o con coma
    const regex = /(\$\s*)?(\d{1,3}(?:\.\d{3})+)(?:[,\s](\d{2})\b)?/g;
    let m;
    while ((m = regex.exec(bloque)) !== null) {
      const simbolo = !!m[1];
      const entero = parseInt(m[2].replace(/\./g, ''), 10);
      if (entero < 100 || entero > 99_000_000) continue;
      candidatos.push({ valor: entero, simbolo, enClave });
    }
  }

  if (!candidatos.length) return { monto: null, confianza: 'baja' };

  // Dedup por valor (el bloque solapa líneas y puede repetir)
  const unicos = [];
  const vistos = new Set();
  for (const c of candidatos) {
    const k = c.valor + (c.simbolo ? 'S' : '') + (c.enClave ? 'C' : '');
    if (!vistos.has(k)) { vistos.add(k); unicos.push(c); }
  }

  unicos.sort((a, b) =>
    (a.simbolo !== b.simbolo) ? (a.simbolo ? -1 : 1)
    : (a.enClave !== b.enClave) ? (a.enClave ? -1 : 1)
    : (b.valor - a.valor));
  const mejor = unicos[0];
  return { monto: mejor.valor, confianza: (mejor.simbolo || mejor.enClave) ? 'alta' : 'media' };
}

/** N° de operación / código del comprobante. */
function extraerIdOperacion(texto) {
  const patrones = [
    /n[uú]mero\s+de\s+operaci[oó]n[^\d]*([0-9]{6,})/i,
    /(?:n[º°o]?\.?\s*de\s*)?operaci[oó]n[:\s#nro.]*([A-Z0-9-]{6,})/i,
    /c[oó]digo\s+de\s+identificaci[oó]n[:\s]*([A-Z0-9-]{6,})/i,
    /(?:n[º°o]?\.?\s*)?transacci[oó]n[:\s#nro.]*([A-Z0-9-]{6,})/i,
  ];
  for (const p of patrones) {
    const m = texto.match(p);
    if (m) return m[1].trim();
  }
  return null;
}

/**
 * Extrae emisor y receptor del comprobante. Soporta muchas billeteras
 * reconociendo varias etiquetas, en tres formas:
 *   - etiqueta sola, nombre en la línea siguiente  (Mercado Pago: "De"\n"Nombre")
 *   - etiqueta + nombre en la misma línea          ("Origen: Juan")
 *   - etiqueta pegada al nombre                     (Personal Pay: "OrigenJuan")
 */
const ETIQ_EMISOR   = ['de', 'origen', 'remitente', 'ordenante', 'enviado por', 'titular origen'];
const ETIQ_RECEPTOR = ['para', 'destino', 'destinatario', 'beneficiario', 'enviado a', 'acreditado en', 'titular destino'];

function extraerDePara(texto) {
  const lineas = texto.split('\n').map(l => l.trim()).filter(Boolean);
  let de = null, para = null;
  for (let i = 0; i < lineas.length; i++) {
    if (!de)   { const v = matchEtiqueta(lineas[i], lineas[i + 1], ETIQ_EMISOR);   if (v) de = v; }
    if (!para) { const v = matchEtiqueta(lineas[i], lineas[i + 1], ETIQ_RECEPTOR); if (v) para = v; }
  }
  return { de, para };
}

/** Intenta extraer el nombre que sigue a alguna de las etiquetas dadas. */
function matchEtiqueta(linea, siguiente, etiquetas) {
  const low = linea.toLowerCase();
  for (const e of etiquetas) {
    // etiqueta sola en su línea → nombre en la siguiente (MP)
    if (low === e || low === e + ':') return (siguiente || '').trim() || null;
    // etiqueta + nombre en la misma línea, con separador
    if (low.startsWith(e + ' ') || low.startsWith(e + ':')) {
      return linea.slice(e.length).replace(/^[:\s]+/, '').trim() || null;
    }
    // etiqueta pegada al nombre (Personal Pay), solo etiquetas de una palabra,
    // y validando que lo que sigue empiece con mayúscula (un nombre real)
    if (!e.includes(' ') && low.startsWith(e) && linea.length > e.length) {
      const resto = linea.slice(e.length);
      if (/^[A-ZÁÉÍÓÚÑ]/.test(resto)) return resto.trim();
    }
  }
  return null;
}

/**
 * Compara dos nombres de forma flexible. Tolera:
 *  - acentos (normaliza)
 *  - palabras partidas por mala extracción de PDF ("Nicolás" → "Nicol S")
 *  - prefijos ("nicol" ↔ "nicolas")
 *  - títulos (Dr, Dra) que se ignoran
 *
 * Coincide si comparten una palabra (exacta o por prefijo de 4+ letras), o si
 * los nombres completos son muy similares (tolerancia a 1-2 caracteres).
 */
function nombresCoinciden(a, b) {
  if (!a || !b) return { ok: false, comun: null };
  const norm = s => s.toLowerCase()
    .normalize('NFD').replace(/[̀-ͯ]/g, '')          // quitar acentos
    .replace(/[^a-z\s]/g, ' ').replace(/\s+/g, ' ').trim();
  const TITULOS = new Set(['dr', 'dra', 'dro', 'sr', 'sra', 'lic']);

  const na = norm(a), nb = norm(b);
  const pa = na.split(' ').filter(w => w.length >= 3 && !TITULOS.has(w));
  const pb = nb.split(' ').filter(w => w.length >= 3 && !TITULOS.has(w));

  // 1) Palabra exacta o por prefijo (4+ letras) → tolera "nicol" vs "nicolas"
  for (const wa of pa) {
    for (const wb of pb) {
      if (wa === wb) return { ok: true, comun: wa };
      if (wa.length >= 4 && wb.length >= 4 && (wa.startsWith(wb) || wb.startsWith(wa))) {
        return { ok: true, comun: wa.length <= wb.length ? wa : wb };
      }
    }
  }

  // 2) Nombres completos sin espacios, muy similares (Levenshtein ≤ 20%)
  //    Cubre "nicolas" vs "nicol s" → "nicolas" vs "nicols" (distancia 1)
  const ca = na.replace(/\s/g, ''), cb = nb.replace(/\s/g, '');
  if (ca.length >= 5 && cb.length >= 5) {
    const d = distanciaLevenshtein(ca, cb);
    if (d / Math.max(ca.length, cb.length) <= 0.2) return { ok: true, comun: '~similar' };
  }

  return { ok: false, comun: null };
}

/** Distancia de edición (cuántos cambios para pasar de a a b). */
function distanciaLevenshtein(a, b) {
  const m = a.length, n = b.length;
  const dp = Array.from({ length: m + 1 }, (_, i) => {
    const row = new Array(n + 1).fill(0);
    row[0] = i;
    return row;
  });
  for (let j = 0; j <= n; j++) dp[0][j] = j;
  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      dp[i][j] = Math.min(
        dp[i - 1][j] + 1,
        dp[i][j - 1] + 1,
        dp[i - 1][j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1)
      );
    }
  }
  return dp[m][n];
}

/**
 * Valida el pie contra el comprobante. Compara:
 *   - emisor del pie  vs  "De"/"Origen" del comprobante
 *   - receptor del pie vs  "Para"/"Destino" del comprobante
 *
 * Pasa SOLO si TODOS los campos presentes en el comprobante coinciden.
 * Si el comprobante tiene De y Para, ambos deben coincidir (no alcanza con uno).
 * Así se detecta si alguien pone un emisor o receptor falso.
 */
function validarPersonas(pie, lectura) {
  const checks = [];
  if (lectura.de)   checks.push({ campo: 'emisor',   pieVal: pie.emisor,   compVal: lectura.de,   ...nombresCoinciden(pie.emisor, lectura.de) });
  if (lectura.para) checks.push({ campo: 'receptor', pieVal: pie.receptor, compVal: lectura.para, ...nombresCoinciden(pie.receptor, lectura.para) });

  if (!checks.length) return { ok: true, detalle: 'comprobante sin datos para validar' };

  const fallidos = checks.filter(c => !c.ok);
  if (fallidos.length === 0) {
    const det = checks.map(c => `${c.campo} por "${c.comun}"`).join(', ');
    return { ok: true, detalle: `coinciden ${det}` };
  }

  // Al menos un campo NO coincide → la validación falla
  const det = fallidos.map(c => `${c.campo} "${c.pieVal}" ≠ comprobante "${c.compVal}"`).join('; ');
  return { ok: false, detalle: det };
}

// ─── Backend ─────────────────────────────────────────────────────────────────
// Reintenta hasta 3 veces con 3 s de pausa si el backend no responde.
async function registrarPago(datos) {
  const headers = { 'Content-Type': 'application/json' };
  if (BOT_API_KEY) headers['X-Bot-Api-Key'] = BOT_API_KEY;
  const body = {
    receptorNombre:    datos.receptorNombre,
    monto:             datos.monto,
    emisor:            datos.emisor,
    idOperacion:       datos.idOperacion,
    cargadoPorNombre:  datos.cargadoPorNombre,
    cargadoPorTelefono: datos.cargadoPorTelefono,
    grupoOrigen:       datos.grupoOrigen,
    comprobanteBase64: datos.comprobanteBase64,
    comprobanteMime:   datos.comprobanteMime,
    comprobanteNombre: datos.comprobanteNombre,
  };

  const MAX_INTENTOS = 3;
  let lastError;
  for (let intento = 1; intento <= MAX_INTENTOS; intento++) {
    try {
      const res = await axios.post(
        `${BACKEND_URL}/api/finanzas/sueldos/pago-automatico`,
        body,
        { headers, timeout: 15000 },
      );
      return res.data;  // { estado, tipoReceptor, receptorResuelto, mensaje, ... }
    } catch (err) {
      lastError = err;
      if (intento < MAX_INTENTOS) {
        console.warn(`[Bot] Backend no respondió (intento ${intento}/${MAX_INTENTOS}). Reintentando en 3 s…`);
        await new Promise(r => setTimeout(r, 3000));
      }
    }
  }
  throw lastError;
}

// ─── Limpieza periódica de pendientes vencidos ──────────────────────────────
setInterval(() => {
  const ahora = Date.now();
  for (const [k, v] of pendientes) {
    if (ahora - v.ts > TIMEOUT_PENDIENTE) pendientes.delete(k);
  }
}, 60 * 1000);

// ─── API HTTP interna ─────────────────────────────────────────────────────────
// Usada por la UI (estado + QR) y por ms-pedidos (notificaciones proactivas).
// Todos los POST requieren X-Bot-Api-Key coincidente con BOT_API_KEY del .env.
http.createServer((req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, X-Bot-Api-Key');

  // ── GET /api/bot/estado ────────────────────────────────────────────────────
  if (req.method === 'GET' && req.url.startsWith('/api/bot/estado')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({
      conectado: estadoBot.conectado,
      qrDataUrl: estadoBot.qrDataUrl,
      motivo: estadoBot.motivo,
      ultimaActualizacion: estadoBot.ultimaActualizacion,
      grupos: GRUPOS,
      backendActivo: BACKEND_ENABLED,
    }));
  }

  // CORS preflight
  if (req.method === 'OPTIONS') { res.writeHead(204); return res.end(); }

  // Autenticación para todos los POST
  if (req.method === 'POST') {
    if (BOT_API_KEY && req.headers['x-bot-api-key'] !== BOT_API_KEY) {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ error: 'API key inválida' }));
    }

    // Leer body JSON
    const chunks = [];
    req.on('data', c => chunks.push(c));
    req.on('end', async () => {
      let body = {};
      try { body = JSON.parse(Buffer.concat(chunks).toString()); } catch (_) { /* sin body */ }

      // ── POST /api/bot/regenerar-qr ────────────────────────────────────────
      if (req.url === '/api/bot/regenerar-qr') {
        try {
          console.log('🔄 Regenerando QR por solicitud de la UI...');
          await client.logout();
          await client.initialize();
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ ok: true, mensaje: 'Cerrando sesión y regenerando QR...' }));
        } catch (e) {
          console.error('Error regenerando QR:', e.message);
          res.writeHead(500, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: e.message }));
        }
        return;
      }

      // ── POST /api/bot/notificar ───────────────────────────────────────────
      // Body: { telefono: string, nombre: string, nroPedido: string, trabajo: string }
      if (req.url === '/api/bot/notificar') {
        const { telefono, nombre, nroPedido, trabajo } = body;
        if (!telefono || !nroPedido) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          return res.end(JSON.stringify({ error: 'Faltan campos: telefono, nroPedido' }));
        }
        if (!estadoBot.conectado) {
          res.writeHead(503, { 'Content-Type': 'application/json' });
          return res.end(JSON.stringify({ error: 'Bot no conectado — no se puede enviar mensaje' }));
        }
        try {
          const chatId = normalizarTelefono(telefono);
          const texto =
            `*Laboratorio G&S*\n` +
            `Hola ${nombre || 'Dr./Dra.'}, su trabajo *${trabajo || 'trabajo solicitado'}* ` +
            `(pedido *${nroPedido}*) ya está listo para retirar.\n` +
            `_Por favor coordine el retiro con el laboratorio._`;
          await client.sendMessage(chatId, texto);
          console.log(`📲 Notificación WhatsApp enviada a ${chatId} — pedido ${nroPedido}`);
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ ok: true, chatId }));
        } catch (e) {
          console.error('Error enviando notificación WhatsApp:', e.message);
          res.writeHead(500, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: e.message }));
        }
        return;
      }

      res.writeHead(404); res.end();
    });
    return;
  }

  res.writeHead(404); res.end();
}).listen(BOT_HTTP_PORT, () =>
  console.log(`🌐 Bot HTTP: http://localhost:${BOT_HTTP_PORT}/api/bot/estado`));

/** Normaliza el teléfono a formato WhatsApp {countryCode}{number}@c.us.
 *  Soporta formatos argentinos: 011-XXXX-XXXX, +54 9 11 XXXX, 15XXXXXXXX, etc. */
function normalizarTelefono(telefono) {
  const digitos = telefono.replace(/\D/g, '');
  // Si ya tiene código de país Argentina (54) y 12+ dígitos: usar directo
  if (digitos.length >= 12 && digitos.startsWith('54')) return digitos + '@c.us';
  // Si empieza con 0 (formato local): reemplazar 0 inicial por 54
  if (digitos.startsWith('0')) return '54' + digitos.slice(1) + '@c.us';
  // Si empieza con 9 y 11 dígitos (formato sin 0): agregar 54
  return '54' + digitos + '@c.us';
}

console.log('🤖 Iniciando bot de WhatsApp GS...');
client.initialize();
