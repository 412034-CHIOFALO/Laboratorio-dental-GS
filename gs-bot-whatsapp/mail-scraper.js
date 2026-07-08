/**
 * ═══════════════════════════════════════════════════════════════════════════
 *  Scraper de mails — Laboratorio GS
 *
 *  Monitorea una casilla de email via IMAP y convierte cada email no leído
 *  en un pedido dentro del sistema:
 *
 *    1) Conecta a IMAP y busca mensajes UNSEEN en INBOX.
 *    2) Parsea el cuerpo con Gemini → extrae paciente, trabajo, fecha,
 *       prioridad, precio y observaciones.
 *    3) Crea el pedido via POST /api/pedidos (autenticado con JWT de bot).
 *    4) Sube cada adjunto (STL, OBJ, imágenes) como escaneo del pedido.
 *    5) Marca el email como leído y envía una respuesta de confirmación.
 *
 *  Activación: MAIL_ENABLED=true en .env
 *  Intervalo de polling: MAIL_POLL_INTERVAL (segundos, default 120)
 * ═══════════════════════════════════════════════════════════════════════════
 */

require('dotenv').config();

const MAIL_ENABLED = process.env.MAIL_ENABLED === 'true';

if (!MAIL_ENABLED) {
  module.exports = { iniciar: () => console.log('[MailScraper] Desactivado (MAIL_ENABLED != true).') };
  return;
}

const { ImapFlow }     = require('imapflow');
const { simpleParser } = require('mailparser');
const nodemailer       = require('nodemailer');
const FormData         = require('form-data');
const axios            = require('axios');
const { GoogleGenerativeAI } = require('@google/generative-ai');

// ─── Configuración ───────────────────────────────────────────────────────────
const BACKEND_URL         = process.env.BACKEND_URL || 'http://localhost:8080';
const GEMINI_API_KEY      = process.env.GEMINI_API_KEY || '';
const GEMINI_MODEL        = process.env.GEMINI_MODEL || 'gemini-2.5-flash';
const BOT_PEDIDOS_USER    = process.env.BOT_PEDIDOS_USER || '';
const BOT_PEDIDOS_PASS    = process.env.BOT_PEDIDOS_PASSWORD || '';
const POLL_INTERVAL_MS    = parseInt(process.env.MAIL_POLL_INTERVAL || '120', 10) * 1000;

const IMAP_HOST     = process.env.MAIL_IMAP_HOST || '';
const IMAP_PORT     = parseInt(process.env.MAIL_IMAP_PORT || '993', 10);
const IMAP_SECURE   = process.env.MAIL_IMAP_SECURE !== 'false';
const IMAP_USER     = process.env.MAIL_IMAP_USER || '';
const IMAP_PASSWORD = process.env.MAIL_IMAP_PASSWORD || '';

const SMTP_HOST     = process.env.MAIL_SMTP_HOST || '';
const SMTP_PORT     = parseInt(process.env.MAIL_SMTP_PORT || '587', 10);
const SMTP_USER     = process.env.MAIL_SMTP_USER || IMAP_USER;
const SMTP_PASSWORD = process.env.MAIL_SMTP_PASSWORD || IMAP_PASSWORD;

// ─── Gemini ──────────────────────────────────────────────────────────────────
const genAI       = new GoogleGenerativeAI(GEMINI_API_KEY);
const geminiModel = genAI.getGenerativeModel({ model: GEMINI_MODEL });

/**
 * Envía el cuerpo del email a Gemini y extrae los campos del pedido.
 * Devuelve un objeto con: paciente, trabajo, fechaEntrega, prioridad,
 * precioAcordado, observaciones — o lanza error si Gemini falla.
 */
async function extraerDatosConGemini(cuerpoEmail, remitenteNombre, fechaHoy) {
  const prompt =
    'Sos un asistente de un laboratorio dental argentino. ' +
    'Analizá el siguiente email de un odontólogo que solicita un trabajo y extraé los datos del pedido.\n\n' +
    `Fecha de hoy: ${fechaHoy}\n` +
    `Remitente (odontólogo): ${remitenteNombre}\n\n` +
    'Email:\n' +
    cuerpoEmail + '\n\n' +
    'Devolvé SOLO un JSON válido sin markdown ni texto extra, con esta estructura exacta:\n' +
    '{\n' +
    '  "paciente": "<nombre del paciente, o null>",\n' +
    '  "trabajo": "<tipo de trabajo dental, ej: Corona zirconio, Prótesis superior — obligatorio>",\n' +
    '  "fechaEntrega": "<YYYY-MM-DD. Si dicen el viernes, calculá desde la fecha de hoy. null si no hay>",\n' +
    '  "prioridad": "<URGENTE | ALTA | NORMAL según el tono del pedido>",\n' +
    '  "precioAcordado": <número sin símbolo si se menciona, si no null>,\n' +
    '  "observaciones": "<instrucciones especiales de material, color, forma, etc. null si no hay>"\n' +
    '}\n\n' +
    'Reglas:\n' +
    '- "trabajo" es obligatorio — inferilo aunque sea parcialmente del contexto.\n' +
    '- Fechas relativas ("el viernes", "la semana que viene") → calculá la fecha absoluta desde hoy.\n' +
    '- Si algo no figura, poné null. No inventes datos.';

  const result = await geminiModel.generateContent(prompt);
  let txt = result.response.text().trim()
    .replace(/^```json\s*/i, '').replace(/^```\s*/, '').replace(/```\s*$/, '').trim();
  return JSON.parse(txt);
}

// ─── JWT del bot ──────────────────────────────────────────────────────────────
let _jwtToken     = null;
let _jwtExpiresAt = 0;

async function getToken() {
  // Reutilizamos el token hasta 1 minuto antes de que expire
  if (_jwtToken && Date.now() < _jwtExpiresAt - 60_000) return _jwtToken;

  const res = await axios.post(
    `${BACKEND_URL}/api/auth/login`,
    { username: BOT_PEDIDOS_USER, password: BOT_PEDIDOS_PASS },
    { timeout: 10_000 },
  );
  _jwtToken = res.data.access_token;

  // Decodificamos el payload para saber cuándo expira
  const payload = JSON.parse(
    Buffer.from(_jwtToken.split('.')[1], 'base64url').toString('utf8')
  );
  _jwtExpiresAt = payload.exp * 1000;
  console.log(`[MailScraper] JWT obtenido (expira: ${new Date(_jwtExpiresAt).toISOString()})`);
  return _jwtToken;
}

// ─── SMTP (respuesta de confirmación) ────────────────────────────────────────
let _transporter = null;

function getTransporter() {
  if (_transporter) return _transporter;
  if (!SMTP_HOST) return null;
  _transporter = nodemailer.createTransport({
    host: SMTP_HOST,
    port: SMTP_PORT,
    secure: SMTP_PORT === 465,
    auth: { user: SMTP_USER, pass: SMTP_PASSWORD },
  });
  return _transporter;
}

async function enviarRespuesta(to, nombre, asuntoOriginal, pedido, mensajeError) {
  const t = getTransporter();
  if (!t || !to) return;

  const cuerpo = pedido
    ? `Hola ${nombre},\n\n` +
      `Tu solicitud fue recibida y registrada correctamente en el sistema.\n\n` +
      `  Nro. de pedido : ${pedido.nroPedido}\n` +
      `  Trabajo        : ${pedido.trabajo}\n` +
      `  Paciente       : ${pedido.paciente}\n` +
      `  Fecha entrega  : ${pedido.fechaEntrega}\n\n` +
      `Ante cualquier consulta comuníquese con el laboratorio.\n\nLaboratorio GS`
    : `Hola ${nombre},\n\n` +
      `Recibimos tu email pero ${mensajeError || 'no pudimos procesarlo automáticamente'}.\n\n` +
      `Por favor comuníquese directamente con el laboratorio.\n\nLaboratorio GS`;

  try {
    await t.sendMail({
      from: `"Laboratorio GS" <${SMTP_USER}>`,
      to,
      subject: `Re: ${asuntoOriginal}`,
      text: cuerpo,
    });
    console.log(`[MailScraper]   📬 Respuesta enviada → ${to}`);
  } catch (e) {
    console.warn('[MailScraper]   ⚠ No se pudo enviar respuesta:', e.message);
  }
}

// ─── Procesamiento de un email ────────────────────────────────────────────────
async function procesarEmail(uid, envelope, source) {
  const remitenteNombre = envelope.from?.[0]?.name || envelope.from?.[0]?.address || 'Odontólogo';
  const remitenteEmail  = envelope.from?.[0]?.address || null;
  const asunto          = envelope.subject || '(sin asunto)';
  const fechaHoy        = new Date().toISOString().slice(0, 10);

  console.log(`\n[MailScraper] ── Email de ${remitenteNombre} <${remitenteEmail}>`);
  console.log(`[MailScraper]    Asunto: ${asunto}`);

  // Parsear MIME para obtener texto y adjuntos
  const parsed = await simpleParser(source);
  const cuerpo = parsed.text
    || (parsed.html ? parsed.html.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim() : '');
  const adjuntos = (parsed.attachments || []).filter(a => a.content && a.filename);

  console.log(`[MailScraper]    Adjuntos: ${adjuntos.length}`);

  // Extraer datos del pedido con Gemini
  let datos;
  try {
    datos = await extraerDatosConGemini(cuerpo, remitenteNombre, fechaHoy);
    console.log('[MailScraper]    ✨ Gemini:', JSON.stringify(datos));
  } catch (e) {
    console.error('[MailScraper]    ❌ Gemini falló:', e.message);
    await enviarRespuesta(remitenteEmail, remitenteNombre, asunto, null,
      'no fue posible interpretar el contenido del email');
    return true;  // problema de contenido: ya respondimos, no reintentar
  }

  if (!datos.trabajo) {
    console.log('[MailScraper]    ⚠ Sin trabajo identificado — email descartado');
    await enviarRespuesta(remitenteEmail, remitenteNombre, asunto, null,
      'no se pudo determinar el tipo de trabajo. Por favor reenvíe con más detalles');
    return true;  // problema de contenido: ya respondimos, no reintentar
  }

  // Fecha de entrega por defecto: 10 días corridos si Gemini no la detectó
  const fechaEntrega = datos.fechaEntrega || (() => {
    const d = new Date();
    d.setDate(d.getDate() + 10);
    return d.toISOString().slice(0, 10);
  })();

  // Crear pedido
  let token, pedidoCreado;
  try {
    token = await getToken();
    const res = await axios.post(
      `${BACKEND_URL}/api/pedidos`,
      {
        odontologoNombre: remitenteNombre,
        paciente:         datos.paciente || 'Paciente por confirmar',
        trabajo:          datos.trabajo,
        fechaEntrega,
        prioridad:        datos.prioridad || 'NORMAL',
        precioAcordado:   datos.precioAcordado || null,
        observaciones:    [
          datos.observaciones,
          `Pedido recibido por email desde ${remitenteEmail}`,
        ].filter(Boolean).join(' | '),
      },
      {
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        timeout: 15_000,
      },
    );
    pedidoCreado = res.data;
    console.log(`[MailScraper]    ✅ Pedido creado: ${pedidoCreado.nroPedido} (ID ${pedidoCreado.id})`);
  } catch (e) {
    // Si el token venció durante la llamada, reintentar una vez
    if (e.response?.status === 401) {
      _jwtToken = null;
      try {
        token = await getToken();
        const res = await axios.post(
          `${BACKEND_URL}/api/pedidos`,
          { odontologoNombre: remitenteNombre, paciente: datos.paciente || 'Paciente por confirmar',
            trabajo: datos.trabajo, fechaEntrega, prioridad: datos.prioridad || 'NORMAL',
            precioAcordado: datos.precioAcordado || null,
            observaciones: [datos.observaciones, `Pedido recibido por email desde ${remitenteEmail}`]
              .filter(Boolean).join(' | ') },
          { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, timeout: 15_000 },
        );
        pedidoCreado = res.data;
        console.log(`[MailScraper]    ✅ Pedido creado (reintento): ${pedidoCreado.nroPedido}`);
      } catch (e2) {
        console.error('[MailScraper]    ❌ Error creando pedido:', e2.response?.data?.message || e2.message);
        return false;  // falla de infraestructura: NO marcar leído, reintentar luego
      }
    } else if (e.response?.status >= 400 && e.response?.status < 500) {
      // Error del propio contenido del email (ej: fecha no futura, texto muy largo).
      // Reintentar no lo va a arreglar — se cae en loop infinito si no lo resolvemos acá.
      console.error('[MailScraper]    ❌ Datos del pedido rechazados:', e.response?.data?.mensaje || e.response?.data?.error || e.message);
      await enviarRespuesta(remitenteEmail, remitenteNombre, asunto, null,
        'no pudimos registrar el pedido con los datos detectados. Por favor comuníquese con el laboratorio');
      return true;  // problema de contenido: ya respondimos, no reintentar
    } else {
      console.error('[MailScraper]    ❌ Error creando pedido:', e.response?.data?.message || e.message);
      return false;  // falla de infraestructura: NO marcar leído, reintentar luego
    }
  }

  // Subir adjuntos como escaneos
  for (const adj of adjuntos) {
    try {
      const form = new FormData();
      form.append('file', adj.content, {
        filename:    adj.filename,
        contentType: adj.contentType || 'application/octet-stream',
      });
      form.append('descripcion', adj.filename);

      await axios.post(
        `${BACKEND_URL}/api/pedidos/${pedidoCreado.id}/escaneos`,
        form,
        {
          headers: { ...form.getHeaders(), Authorization: `Bearer ${token}` },
          timeout: 30_000,
          maxContentLength: Infinity,
          maxBodyLength: Infinity,
        },
      );
      console.log(`[MailScraper]    📎 Adjunto subido: ${adj.filename}`);
    } catch (e) {
      console.warn(`[MailScraper]    ⚠ No se pudo subir ${adj.filename}:`, e.message);
    }
  }

  // Respuesta de confirmación al odontólogo
  await enviarRespuesta(remitenteEmail, remitenteNombre, asunto, pedidoCreado, null);
  return true;  // pedido creado OK → marcar leído
}

// ─── Poll IMAP ────────────────────────────────────────────────────────────────
async function pollMail() {
  const imap = new ImapFlow({
    host:   IMAP_HOST,
    port:   IMAP_PORT,
    secure: IMAP_SECURE,
    auth:   { user: IMAP_USER, pass: IMAP_PASSWORD },
    logger: false,
  });

  try {
    await imap.connect();
    const lock = await imap.getMailboxLock('INBOX');
    try {
      // Buscar UIDs de mensajes no leídos
      const uids = await imap.search({ seen: false }, { uid: true });
      if (!uids || uids.length === 0) return;

      console.log(`[MailScraper] ${uids.length} email(s) nuevo(s)`);

      for await (const msg of imap.fetch(uids, { envelope: true, source: true }, { uid: true })) {
        let resuelto = false;
        try {
          // Solo se marca leído si el email quedó resuelto (pedido creado o
          // descartado por contenido). Si falló por infraestructura (backend
          // caído), se deja sin leer para reintentarlo en el próximo poll y no
          // perder el pedido.
          resuelto = await procesarEmail(msg.uid, msg.envelope, msg.source);
        } catch (e) {
          console.error('[MailScraper] Error procesando email:', e.message);
          resuelto = false;
        }
        if (resuelto) {
          await imap.messageFlagsAdd({ uid: msg.uid }, ['\\Seen'], { uid: true });
        } else {
          console.warn(`[MailScraper] Email uid=${msg.uid} sin marcar — se reintentará.`);
        }
      }
    } finally {
      lock.release();
    }
  } catch (e) {
    console.error('[MailScraper] Error IMAP:', e.message);
  } finally {
    try { await imap.logout(); } catch (_) {}
  }
}

// ─── Inicio ───────────────────────────────────────────────────────────────────
function iniciar() {
  if (!IMAP_HOST || !IMAP_USER || !BOT_PEDIDOS_USER) {
    console.warn('[MailScraper] Faltan variables de entorno (MAIL_IMAP_HOST, MAIL_IMAP_USER, BOT_PEDIDOS_USER).');
    return;
  }
  console.log(`[MailScraper] Iniciado — chequeando ${IMAP_HOST} cada ${POLL_INTERVAL_MS / 1000}s`);
  pollMail();
  setInterval(pollMail, POLL_INTERVAL_MS);
}

module.exports = { iniciar };
