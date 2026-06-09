package com.gys.ms_finanzas.service;

import com.gys.ms_finanzas.dto.*;
import com.gys.ms_finanzas.exception.BusinessException;
import com.gys.ms_finanzas.exception.ResourceNotFoundException;
import com.gys.ms_finanzas.model.*;
import com.gys.ms_finanzas.repository.ConfiguracionSueldoRepository;
import com.gys.ms_finanzas.repository.PagoSueldoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GestionSueldoService implements IGestionSueldoService {

    private static final Logger log = LoggerFactory.getLogger(GestionSueldoService.class);

    private final ConfiguracionSueldoRepository configRepo;
    private final PagoSueldoRepository pagoRepo;
    private final MinioStorageService minioStorage;

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

    @Override
    @Transactional
    public PagoSueldoResponse registrarPagoAutomatico(PagoAutomaticoRequest req) {
        // Anti-duplicado: si ya registramos este nro de operación, no repetir
        if (req.getIdOperacion() != null && !req.getIdOperacion().isBlank()
                && pagoRepo.existsByIdOperacion(req.getIdOperacion())) {
            throw new BusinessException("El comprobante con operación " + req.getIdOperacion()
                    + " ya fue registrado anteriormente.");
        }

        // Resolver el empleado por id, teléfono o nombre (en ese orden de confianza)
        ConfiguracionSueldo c;
        if (req.getReceptorUsuarioId() != null) {
            c = getConfig(req.getReceptorUsuarioId());
        } else if (req.getReceptorTelefono() != null && !req.getReceptorTelefono().isBlank()) {
            c = configRepo.findAllByOrderByEmpleadoNombreAsc().stream()
                    .filter(x -> req.getReceptorTelefono().equals(x.getTelefono()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(
                            "No se encontró ningún empleado con el teléfono " + req.getReceptorTelefono()));
        } else if (req.getReceptorNombre() != null && !req.getReceptorNombre().isBlank()) {
            c = resolverPorNombre(req.getReceptorNombre());
        } else {
            throw new BusinessException("Falta identificar al receptor (id, teléfono o nombre)");
        }

        PagoSueldo pago = aplicarPago(
                c,
                req.getMonto(),
                ManejoSobrante.DESCONTAR_PROXIMO, // el bot no decide; default razonable
                req.getFecha(),
                OrigenPago.BOT_WHATSAPP,
                req.getNota()
        );
        // Trazabilidad del bot
        pago.setCargadoPorNombre(req.getCargadoPorNombre());
        pago.setCargadoPorTelefono(req.getCargadoPorTelefono());
        pago.setEmisor(req.getEmisor());
        pago.setGrupoOrigen(req.getGrupoOrigen());
        pago.setIdOperacion(req.getIdOperacion());

        // Guardar el archivo del comprobante en MinIO (si vino)
        String comprobanteRef = req.getComprobanteUrl();
        if (req.getComprobanteBase64() != null && !req.getComprobanteBase64().isBlank()) {
            try {
                byte[] datos = java.util.Base64.getDecoder().decode(req.getComprobanteBase64());
                String objectName = minioStorage.subir(datos, req.getComprobanteMime(), req.getComprobanteNombre());
                if (objectName != null) comprobanteRef = objectName;
            } catch (Exception e) {
                log.warn("[SUELDOS-BOT] No se pudo guardar el comprobante en MinIO: {}", e.getMessage());
            }
        }
        pago.setComprobanteUrl(comprobanteRef);

        log.info("[SUELDOS-BOT] Pago automático: {} recibió {} (emisor: {}, cargado por: {})",
                c.getEmpleadoNombre(), req.getMonto(), req.getEmisor(), req.getCargadoPorNombre());

        return PagoSueldoResponse.from(pagoRepo.save(pago));
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
    public BigDecimal totalDevengado() {
        return configRepo.totalDevengado();
    }

    @Override
    public String urlComprobante(Long pagoId) {
        PagoSueldo pago = pagoRepo.findById(pagoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", pagoId));
        if (pago.getComprobanteUrl() == null || pago.getComprobanteUrl().isBlank()) {
            throw new BusinessException("Este pago no tiene comprobante guardado");
        }
        String url = minioStorage.urlTemporal(pago.getComprobanteUrl(), 30);  // 30 min
        if (url == null) {
            throw new BusinessException("No se pudo generar el enlace al comprobante");
        }
        return url;
    }

    // ── Lógica común de aplicación de un pago ────────────────────────

    /**
     * Aplica un pago al saldo del empleado y construye el PagoSueldo (sin
     * persistir aún — el caller lo guarda). Modifica la config in-place y la
     * persiste.
     *
     * Reglas:
     *  - pago <= devengado  → descuenta del devengado, excedente = 0
     *  - pago >  devengado  → devengado = 0, excedente = diferencia;
     *      si manejo = DESCONTAR_PROXIMO, el excedente se suma a saldoSobrante.
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
            // CUBRE_LAB / DEVUELVE_EMPLEADO: no afecta el saldo del empleado.
            // En el sistema real, generaría un asiento en caja (gasto/ingreso del lab).
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

    /**
     * Resuelve un empleado por nombre (lo que viene del pie del mensaje del bot).
     * Matchea sin distinguir mayúsculas y de forma parcial, para tolerar
     * variaciones ("Carlos" → "Carlos López"). Si hay ambigüedad, falla.
     */
    private ConfiguracionSueldo resolverPorNombre(String nombre) {
        String q = nombre.trim().toLowerCase();
        List<ConfiguracionSueldo> matches = configRepo.findAllByOrderByEmpleadoNombreAsc().stream()
                .filter(x -> x.getEmpleadoNombre() != null
                          && x.getEmpleadoNombre().toLowerCase().contains(q))
                .toList();

        if (matches.isEmpty()) {
            throw new BusinessException("No se encontró ningún empleado que coincida con \"" + nombre + "\"");
        }
        if (matches.size() > 1) {
            String nombres = matches.stream().map(ConfiguracionSueldo::getEmpleadoNombre)
                    .reduce((a, b) -> a + ", " + b).orElse("");
            throw new BusinessException("El nombre \"" + nombre + "\" coincide con varios empleados: " + nombres
                    + ". Cargalo a mano para evitar errores.");
        }
        return matches.get(0);
    }
}

