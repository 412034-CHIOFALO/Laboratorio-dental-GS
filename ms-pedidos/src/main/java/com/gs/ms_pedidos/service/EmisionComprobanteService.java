package com.gs.ms_pedidos.service;

import com.gs.ms_pedidos.client.FinanzasClient;
import com.gs.ms_pedidos.client.dto.ActualizarMontoRequestDTO;
import com.gs.ms_pedidos.client.dto.ComprobanteRequestDTO;
import com.gs.ms_pedidos.model.Pedido;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Genera la cuenta por cobrar (comprobante de deuda) en ms-finanzas cuando un
 * pedido se entrega. La entrega crea la <b>deuda</b>, no el cobro: el odontólogo
 * paga después (ver pagos a cuenta corriente en ms-finanzas).
 *
 * <p>Best-effort: si ms-finanzas no responde, la entrega NO se bloquea; el
 * comprobante queda sin generar y es reintentable (el flag {@code
 * comprobanteGenerado} sigue en false).</p>
 */
@Service
@RequiredArgsConstructor
public class EmisionComprobanteService {

    private static final Logger log = LoggerFactory.getLogger(EmisionComprobanteService.class);

    private final FinanzasClient finanzasClient;

    /** Vencimiento por defecto de la deuda: 30 días corridos desde la entrega. */
    private static final int DIAS_VENCIMIENTO = 30;

    public void emitirSiCorresponde(Pedido pedido, BigDecimal monto) {
        if (pedido.isComprobanteGenerado()) {
            log.debug("[COMPROBANTE] Pedido {} ya tenía comprobante. Skip.", pedido.getNroPedido());
            return;
        }
        if (monto == null || monto.signum() <= 0) {
            log.warn("[COMPROBANTE] Pedido {} sin monto a facturar — no se genera la deuda.",
                    pedido.getNroPedido());
            return;
        }
        try {
            ComprobanteRequestDTO dto = new ComprobanteRequestDTO(
                    pedido.getId(),
                    pedido.getNroPedido(),
                    pedido.getOdontologoId(),
                    pedido.getOdontologoNombre(),
                    pedido.getTrabajo(),
                    monto,
                    LocalDate.now(),
                    LocalDate.now().plusDays(DIAS_VENCIMIENTO),
                    "Deuda generada al entregar el pedido " + pedido.getNroPedido()
            );
            finanzasClient.emitirComprobante(dto);
            pedido.setComprobanteGenerado(true);
            log.info("[COMPROBANTE] Pedido {} entregado → deuda de ${} a {}",
                    pedido.getNroPedido(), monto, pedido.getOdontologoNombre());
        } catch (Exception e) {
            log.warn("[COMPROBANTE] No se pudo generar el comprobante del pedido {}: {} (reintentable)",
                    pedido.getNroPedido(), e.getMessage());
        }
    }

    /**
     * Sincroniza el monto del comprobante cuando se edita "monto a facturar" de
     * un pedido YA entregado (ver PedidoService.actualizar). Sin esto, corregir
     * el precio después de la entrega no tenía ningún efecto en la deuda real:
     * el comprobante en ms-finanzas quedaba congelado con el monto original de
     * la entrega para siempre, y el saldo pendiente del odontólogo nunca reflejaba
     * la corrección.
     *
     * <p>No hace nada si el pedido no tiene comprobante todavía (no está
     * entregado, o la emisión original falló) — no hay nada que sincronizar.
     * Best-effort: si ms-finanzas no responde, no bloquea la edición del pedido.</p>
     */
    public void sincronizarMontoSiCorresponde(Pedido pedido, BigDecimal nuevoMonto) {
        if (!pedido.isComprobanteGenerado() || nuevoMonto == null || nuevoMonto.signum() <= 0) {
            return;
        }
        try {
            finanzasClient.actualizarMontoComprobante(pedido.getId(), new ActualizarMontoRequestDTO(nuevoMonto));
            log.info("[COMPROBANTE] Pedido {} — monto de facturación corregido a ${}",
                    pedido.getNroPedido(), nuevoMonto);
        } catch (Exception e) {
            log.warn("[COMPROBANTE] No se pudo sincronizar el nuevo monto del pedido {}: {} (reintentable)",
                    pedido.getNroPedido(), e.getMessage());
        }
    }
}
