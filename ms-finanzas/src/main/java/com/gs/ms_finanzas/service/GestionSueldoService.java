package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.*;
import com.gs.ms_finanzas.exception.BusinessException;
import com.gs.ms_finanzas.exception.ResourceNotFoundException;
import com.gs.ms_finanzas.model.*;
import com.gs.ms_finanzas.repository.ConfiguracionSueldoRepository;
import com.gs.ms_finanzas.repository.PagoSueldoRepository;
import com.gs.ms_finanzas.repository.ProveedorRepository;
import com.gs.ms_finanzas.repository.RegistroPagoBotRepository;
import com.gs.ms_finanzas.repository.ComprobanteRepository;
import com.gs.ms_finanzas.repository.DeudaProveedorRepository;
import com.gs.ms_finanzas.repository.CajaMovimientoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GestionSueldoService implements IGestionSueldoService {

    private static final Logger log = LoggerFactory.getLogger(GestionSueldoService.class);

    private final ConfiguracionSueldoRepository configRepo;
    private final PagoSueldoRepository pagoRepo;
    private final MinioStorageService minioStorage;
    private final ProveedorRepository proveedorRepo;
    private final RegistroPagoBotRepository registroRepo;
    private final ComprobanteRepository comprobanteRepo;
    private final DeudaProveedorRepository deudaProveedorRepo;
    private final CajaMovimientoRepository cajaMovimientoRepo;

    @Override
    public List<EmpleadoSueldoResponse> listarEmpleados() {
        return configRepo.findAllByOrderByEmpleadoNombreAsc().stream()
                .map(EmpleadoSueldoResponse::from)
                .toList();
    }

    @Override
    public EmpleadoSueldoResponse buscarEmpleado(Long usuarioId) {
        return EmpleadoSueldoResponse.from(getConfig(usuarioId));
    }

    @Override
    @Transactional
    public EmpleadoSueldoResponse guardarConfig(Long usuarioId, ConfigSueldoRequest req) {
        ConfiguracionSueldo c = configRepo.findByEmpleadoId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", usuarioId));
        c.setFrecuencia(req.getFrecuencia());
        c.setMontoBase(req.getMontoBase());
        return EmpleadoSueldoResponse.from(configRepo.save(c));
    }

    @Override
    @Transactional
    public PagoSueldoResponse registrarPago(PagoSueldoRequest req) {
        ConfiguracionSueldo c = getConfig(req.getUsuarioId());

        PagoSueldo pago = aplicarPago(
                c,
                req.getMonto(),
                req.getManejoSobrante() != null ? req.getManejoSobrante() : ManejoSobrante.DESCONTAR_PROXIMO,
                req.getFecha(),
                OrigenPago.MANUAL,
                req.getNota()
        );
        return PagoSueldoResponse.from(pagoRepo.save(pago));
    }

    /**
     * Procesa un comprobante detectado por el bot de WhatsApp.
     *
     * Clasifica al receptor (Para) y registra el pago según corresponda:
     *   1. EMPLEADO  → pago de sueldo (descuenta del devengado).
     *   2. PROVEEDOR → pago a proveedor.
     *   3. Ninguno   → rechazo.
     *
     * SIEMPRE deja un {@link RegistroPagoBot} (incluso rechazos y duplicados),
     * que es lo que alimenta el historial del bot en el front. Por eso NO lanza
     * excepción ante un rechazo: devuelve el resultado con su estado y mensaje.
     */
    @Override
    @Transactional
    public RegistroPagoBotResponse registrarPagoAutomatico(PagoAutomaticoRequest req) {
        RegistroPagoBot reg = RegistroPagoBot.builder()
                .monto(req.getMonto())
                .idOperacion(req.getIdOperacion())
                .emisor(req.getEmisor())
                .receptorNombre(req.getReceptorNombre())
                .cargadoPorNombre(req.getCargadoPorNombre())
                .cargadoPorTelefono(req.getCargadoPorTelefono())
                .grupoOrigen(req.getGrupoOrigen())
                .build();

        // Guardar el archivo del comprobante (best-effort), aun si después se rechaza
        String comprobanteRef = guardarComprobante(req);
        reg.setComprobanteUrl(comprobanteRef);

        // Anti-duplicado por nro de operación (sobre registros ya exitosos)
        if (req.getIdOperacion() != null && !req.getIdOperacion().isBlank()
                && registroRepo.existsByIdOperacionAndEstado(req.getIdOperacion(), EstadoRegistroBot.REGISTRADO)) {
            reg.setEstado(EstadoRegistroBot.DUPLICADO);
            reg.setTipoReceptor(TipoReceptorBot.DESCONOCIDO);
            reg.setMensaje("El comprobante (operación " + req.getIdOperacion() + ") ya fue registrado antes.");
            return RegistroPagoBotResponse.from(registroRepo.save(reg));
        }

        // 1) ¿Es un empleado? → pago de sueldo
        Optional<ConfiguracionSueldo> empleado = resolverEmpleadoOpt(req);
        if (empleado.isPresent()) {
            ConfiguracionSueldo c = empleado.get();
            try {
                PagoSueldo pago = aplicarPago(c, req.getMonto(), ManejoSobrante.DESCONTAR_PROXIMO,
                        req.getFecha(), OrigenPago.BOT_WHATSAPP, req.getNota());
                pago.setCargadoPorNombre(req.getCargadoPorNombre());
                pago.setCargadoPorTelefono(req.getCargadoPorTelefono());
                pago.setEmisor(req.getEmisor());
                pago.setGrupoOrigen(req.getGrupoOrigen());
                pago.setIdOperacion(req.getIdOperacion());
                pago.setComprobanteUrl(comprobanteRef);
                pagoRepo.save(pago);

                reg.setEstado(EstadoRegistroBot.REGISTRADO);
                reg.setTipoReceptor(TipoReceptorBot.EMPLEADO);
                reg.setReceptorId(c.getEmpleadoId());
                reg.setReceptorResuelto(c.getEmpleadoNombre());
                reg.setMensaje("Sueldo registrado para " + c.getEmpleadoNombre());
                log.info("[BOT] Sueldo: {} recibió {} (emisor: {})", c.getEmpleadoNombre(), req.getMonto(), req.getEmisor());
                return RegistroPagoBotResponse.from(registroRepo.save(reg));
            } catch (BusinessException e) {
                // ej: empleado inactivo → se rechaza pero queda registrado
                reg.setEstado(EstadoRegistroBot.RECHAZADO);
                reg.setTipoReceptor(TipoReceptorBot.EMPLEADO);
                reg.setReceptorId(c.getEmpleadoId());
                reg.setReceptorResuelto(c.getEmpleadoNombre());
                reg.setMensaje(e.getMessage());
                return RegistroPagoBotResponse.from(registroRepo.save(reg));
            }
        }

        // 2) ¿Es un proveedor? → pago a proveedor (directo o triangulado)
        Optional<Proveedor> proveedor = resolverProveedorOpt(req.getReceptorNombre());
        if (proveedor.isPresent()) {
            Proveedor p = proveedor.get();
            reg.setEstado(EstadoRegistroBot.REGISTRADO);
            reg.setTipoReceptor(TipoReceptorBot.PROVEEDOR);
            reg.setReceptorId(p.getId());
            reg.setReceptorResuelto(p.getNombre());

            // ¿El emisor es un odontólogo? → TRIANGULADO (el odontólogo le paga al proveedor del lab)
            Optional<Comprobante> odo = resolverOdontologoEmisor(req.getEmisor());
            if (odo.isPresent()) {
                Comprobante oc = odo.get();
                BigDecimal settOdo  = settleDeudaOdontologo(oc.getOdontologoId(), req.getMonto());
                BigDecimal settProv = settleDeudaProveedor(p.getId(), req.getMonto());
                // Caja Compensación: entra del odontólogo y sale al proveedor → neto 0
                registrarMovimiento(TipoMovimientoCaja.INGRESO, TipoCaja.COMPENSACION, req.getMonto(),
                        "Triangulado: " + oc.getOdontologoNombre() + " paga a " + p.getNombre(), req.getIdOperacion());
                registrarMovimiento(TipoMovimientoCaja.EGRESO, TipoCaja.COMPENSACION, req.getMonto(),
                        "Triangulado: a proveedor " + p.getNombre() + " por cuenta de " + oc.getOdontologoNombre(), req.getIdOperacion());
                reg.setMensaje("Triangulado: " + oc.getOdontologoNombre() + " → " + p.getNombre()
                        + " (odontólogo -$" + settOdo.toBigInteger() + ", proveedor -$" + settProv.toBigInteger() + ")");
                log.info("[BOT] Triangulado: {} → {} por {}", oc.getOdontologoNombre(), p.getNombre(), req.getMonto());
                return RegistroPagoBotResponse.from(registroRepo.save(reg));
            }

            // Pago directo del laboratorio al proveedor (sale por la caja bancaria)
            BigDecimal settProv = settleDeudaProveedor(p.getId(), req.getMonto());
            registrarMovimiento(TipoMovimientoCaja.EGRESO, TipoCaja.BANCARIA, req.getMonto(),
                    "Pago a proveedor: " + p.getNombre(), req.getIdOperacion());
            reg.setMensaje("Pago a proveedor: " + p.getNombre()
                    + (settProv.signum() > 0 ? " (deuda -$" + settProv.toBigInteger() + ")" : ""));
            log.info("[BOT] Pago a proveedor {}: {} (emisor: {})", p.getNombre(), req.getMonto(), req.getEmisor());
            return RegistroPagoBotResponse.from(registroRepo.save(reg));
        }

        // 3) No se reconoció ni empleado ni proveedor
        reg.setEstado(EstadoRegistroBot.RECHAZADO);
        reg.setTipoReceptor(TipoReceptorBot.DESCONOCIDO);
        reg.setMensaje("No se encontró ningún empleado ni proveedor que coincida con \""
                + (req.getReceptorNombre() != null ? req.getReceptorNombre() : "?") + "\"");
        log.info("[BOT] Rechazado: receptor \"{}\" no es empleado ni proveedor", req.getReceptorNombre());
        return RegistroPagoBotResponse.from(registroRepo.save(reg));
    }

    @Override
    @Transactional
    public EmpleadoSueldoResponse ajustarDevengado(Long usuarioId, BigDecimal nuevoDevengado) {
        if (nuevoDevengado == null || nuevoDevengado.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El devengado no puede ser negativo");
        }
        ConfiguracionSueldo c = getConfig(usuarioId);
        c.setSaldoDevengado(nuevoDevengado);
        return EmpleadoSueldoResponse.from(configRepo.save(c));
    }

    @Override
    public List<PagoSueldoResponse> historialPagos(Long usuarioId) {
        return pagoRepo.findByEmpleadoIdOrderByFechaDescIdDesc(usuarioId).stream()
                .map(PagoSueldoResponse::from)
                .toList();
    }

    @Override
    public List<PagoSueldoResponse> historialPagosGlobal() {
        return pagoRepo.findAllByOrderByFechaDescIdDesc().stream()
                .map(PagoSueldoResponse::from)
                .toList();
    }

    @Override
    public List<RegistroPagoBotResponse> listarRegistrosBot() {
        return registroRepo.findAllByOrderByFechaHoraDesc().stream()
                .map(RegistroPagoBotResponse::from)
                .toList();
    }

    @Override
    public BigDecimal totalDevengado() {
        return configRepo.totalDevengado();
    }

    @Override
    public String urlComprobante(Long pagoId) {
        PagoSueldo pago = pagoRepo.findById(pagoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", pagoId));
        return urlDeComprobante(pago.getComprobanteUrl());
    }

    @Override
    public String urlComprobanteRegistro(Long registroId) {
        RegistroPagoBot r = registroRepo.findById(registroId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro", registroId));
        return urlDeComprobante(r.getComprobanteUrl());
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private String urlDeComprobante(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            throw new BusinessException("No tiene comprobante guardado");
        }
        String url = minioStorage.urlTemporal(objectName, 30);  // 30 min
        if (url == null) {
            throw new BusinessException("No se pudo generar el enlace al comprobante");
        }
        return url;
    }

    /** Guarda el comprobante en MinIO (best-effort). Devuelve la ref o la que vino. */
    private String guardarComprobante(PagoAutomaticoRequest req) {
        String ref = req.getComprobanteUrl();
        if (req.getComprobanteBase64() != null && !req.getComprobanteBase64().isBlank()) {
            try {
                byte[] datos = java.util.Base64.getDecoder().decode(req.getComprobanteBase64());
                String objectName = minioStorage.subir(datos, req.getComprobanteMime(), req.getComprobanteNombre());
                if (objectName != null) ref = objectName;
            } catch (Exception e) {
                log.warn("[BOT] No se pudo guardar el comprobante en MinIO: {}", e.getMessage());
            }
        }
        return ref;
    }

    /** Resuelve al empleado por id, teléfono o nombre. Vacío si no hay match único. */
    private Optional<ConfiguracionSueldo> resolverEmpleadoOpt(PagoAutomaticoRequest req) {
        if (req.getReceptorUsuarioId() != null) {
            return configRepo.findByEmpleadoId(req.getReceptorUsuarioId());
        }
        if (req.getReceptorTelefono() != null && !req.getReceptorTelefono().isBlank()) {
            return configRepo.findAllByOrderByEmpleadoNombreAsc().stream()
                    .filter(x -> req.getReceptorTelefono().equals(x.getTelefono()))
                    .findFirst();
        }
        if (req.getReceptorNombre() != null && !req.getReceptorNombre().isBlank()) {
            String q = req.getReceptorNombre().trim().toLowerCase();
            List<ConfiguracionSueldo> matches = configRepo.findAllByOrderByEmpleadoNombreAsc().stream()
                    .filter(x -> x.getEmpleadoNombre() != null && x.getEmpleadoNombre().toLowerCase().contains(q))
                    .toList();
            if (matches.size() == 1) return Optional.of(matches.get(0));
        }
        return Optional.empty();
    }

    /** Resuelve al proveedor por nombre (match parcial, case-insensitive). */
    private Optional<Proveedor> resolverProveedorOpt(String nombre) {
        if (nombre == null || nombre.isBlank()) return Optional.empty();
        String q = nombre.trim().toLowerCase();
        return proveedorRepo.findByActivoTrue().stream()
                .filter(p -> p.getNombre() != null && p.getNombre().toLowerCase().contains(q))
                .findFirst();
    }

    /**
     * ¿El emisor es un odontólogo? Se busca en los comprobantes (que llevan el
     * snapshot del odontólogo). Vacío si no matchea → entonces NO es triangulado.
     * Matchea por palabra (apellido) para tolerar "Dr. García" vs "Dr. Martín García".
     */
    private Optional<Comprobante> resolverOdontologoEmisor(String emisor) {
        if (emisor == null || emisor.isBlank()) return Optional.empty();
        String[] palabras = emisor.trim().toLowerCase().split("\\s+");
        return comprobanteRepo.findAll().stream()
                .filter(c -> {
                    String nom = c.getOdontologoNombre() == null ? "" : c.getOdontologoNombre().toLowerCase();
                    for (String w : palabras) if (w.length() > 3 && nom.contains(w)) return true;
                    return false;
                })
                .findFirst();
    }

    /** Marca comprobantes PENDIENTE del odontólogo como COBRADO (más viejos primero) hasta cubrir el monto. */
    private BigDecimal settleDeudaOdontologo(Long odontologoId, BigDecimal monto) {
        BigDecimal restante = monto, settled = BigDecimal.ZERO;
        List<Comprobante> pend = comprobanteRepo.findByOdontologoIdAndEstadoPago(odontologoId, EstadoPago.PENDIENTE)
                .stream().sorted(Comparator.comparing(Comprobante::getFechaEmision)).toList();
        for (Comprobante c : pend) {
            if (restante.compareTo(c.getMonto()) < 0) break;  // el modelo no soporta pago parcial de un comprobante
            c.setEstadoPago(EstadoPago.COBRADO);
            c.setFechaCobro(LocalDate.now());
            comprobanteRepo.save(c);
            restante = restante.subtract(c.getMonto());
            settled = settled.add(c.getMonto());
        }
        return settled;
    }

    /** Marca DeudaProveedor PENDIENTE como PAGADO (más viejas primero) hasta cubrir el monto. */
    private BigDecimal settleDeudaProveedor(Long proveedorId, BigDecimal monto) {
        BigDecimal restante = monto, settled = BigDecimal.ZERO;
        List<DeudaProveedor> pend = deudaProveedorRepo.findByProveedorIdOrderByFechaCreacionDesc(proveedorId)
                .stream().filter(d -> d.getEstado() == EstadoDeuda.PENDIENTE)
                .sorted(Comparator.comparing(DeudaProveedor::getFechaCreacion)).toList();
        for (DeudaProveedor d : pend) {
            if (restante.compareTo(d.getMonto()) < 0) break;
            d.setEstado(EstadoDeuda.PAGADO);
            d.setFechaPago(LocalDate.now());
            deudaProveedorRepo.save(d);
            restante = restante.subtract(d.getMonto());
            settled = settled.add(d.getMonto());
        }
        return settled;
    }

    /** Registra un movimiento de caja (lo usan el triangulado y el pago directo a proveedor). */
    private void registrarMovimiento(TipoMovimientoCaja tipo, TipoCaja caja, BigDecimal monto, String concepto, String ref) {
        cajaMovimientoRepo.save(CajaMovimiento.builder()
                .tipo(tipo).tipoCaja(caja).monto(monto)
                .concepto(concepto).referencia(ref).creadoPor("bot").build());
    }

    // ── Lógica común de aplicación de un pago de sueldo ──────────────

    /**
     * Aplica un pago al saldo del empleado y construye el PagoSueldo (sin
     * persistir aún — el caller lo guarda). Modifica la config in-place y la
     * persiste.
     */
    private PagoSueldo aplicarPago(ConfiguracionSueldo c, BigDecimal monto,
                                   ManejoSobrante manejo, LocalDate fecha,
                                   OrigenPago origen, String nota) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El monto debe ser mayor a cero");
        }
        if (!c.isActivo()) {
            throw new BusinessException("El empleado " + c.getEmpleadoNombre() + " está inactivo");
        }

        BigDecimal excedente = BigDecimal.ZERO;
        BigDecimal restante = c.getSaldoDevengado().subtract(monto);

        if (restante.compareTo(BigDecimal.ZERO) >= 0) {
            c.setSaldoDevengado(restante);
        } else {
            excedente = restante.abs();
            c.setSaldoDevengado(BigDecimal.ZERO);
            if (manejo == ManejoSobrante.DESCONTAR_PROXIMO) {
                c.setSaldoSobrante(c.getSaldoSobrante().add(excedente));
            }
        }

        LocalDate fechaPago = fecha != null ? fecha : LocalDate.now();
        c.setUltimoPago(fechaPago);
        configRepo.save(c);

        return PagoSueldo.builder()
                .empleadoId(c.getEmpleadoId())
                .empleadoNombre(c.getEmpleadoNombre())
                .monto(monto)
                .fecha(fechaPago)
                .origen(origen)
                .manejoSobrante(manejo)
                .montoExcedente(excedente)
                .nota(nota)
                .build();
    }

    private ConfiguracionSueldo getConfig(Long usuarioId) {
        return configRepo.findByEmpleadoId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", usuarioId));
    }
}
