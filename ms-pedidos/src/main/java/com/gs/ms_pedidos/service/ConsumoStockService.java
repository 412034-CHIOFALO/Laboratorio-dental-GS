package com.gs.ms_pedidos.service;

import com.gs.ms_pedidos.client.CatalogoClient;
import com.gs.ms_pedidos.client.StockClient;
import com.gs.ms_pedidos.client.dto.IngredienteRecetaDTO;
import com.gs.ms_pedidos.client.dto.MovimientoStockRequest;
import com.gs.ms_pedidos.client.dto.TipoTrabajoDTO;
import com.gs.ms_pedidos.model.Pedido;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Orquesta el descuento automático de stock cuando un pedido entra en producción.
 *
 * Flujo:
 *   1. Pedido pasa a EN_PROCESO (lo dispara PedidoService.actualizarEstado)
 *   2. Si el pedido tiene catalogoTrabajoId y stockConsumido = false:
 *      a. Llama a ms-catalogo → obtiene la receta del trabajo
 *      b. Por cada ingrediente, llama a ms-stock (SALIDA) con motivo
 *         "Producción pedido P-XXX"
 *   3. Marca el pedido como stockConsumido = true
 *
 * Comportamiento ante errores:
 *   - Si ms-catalogo no responde / no encuentra el trabajo → log warn, no
 *     descuenta nada, pero permite el cambio de estado (el técnico puede
 *     ajustar stock manual después).
 *   - Si ms-stock falla en un material puntual → log warn, sigue con el resto.
 *     No queremos bloquear producción por un error de infra del stock.
 *
 * Esto sigue el principio "best effort" — la operación primaria (cambio de
 * estado del pedido) NO debe verse afectada por fallos del flujo accesorio
 * (descuento de stock).
 */
@Service
@RequiredArgsConstructor
public class ConsumoStockService {

    private static final Logger log = LoggerFactory.getLogger(ConsumoStockService.class);

    private final CatalogoClient catalogoClient;
    private final StockClient stockClient;

    /**
     * Descuenta del stock todos los materiales de la receta del pedido.
     * Idempotente: si ya se consumió, no hace nada.
     *
     * @return true si se consumió (o ya estaba consumido); false si hubo
     *         algún problema serio y conviene reintentar manualmente.
     */
    public boolean descontarSiCorresponde(Pedido pedido) {
        if (pedido.isStockConsumido()) {
            log.debug("[CONSUMO-STOCK] Pedido {} ya tenía stock consumido. Skip.", pedido.getNroPedido());
            return true;
        }
        if (pedido.getCatalogoTrabajoId() == null) {
            log.info("[CONSUMO-STOCK] Pedido {} no tiene catalogoTrabajoId (trabajo custom). " +
                    "Skip descuento automático.", pedido.getNroPedido());
            return true; // no hay nada que descontar — caso "trabajo custom"
        }

        // 1. Pedir la receta a ms-catalogo
        TipoTrabajoDTO trabajo;
        try {
            trabajo = catalogoClient.buscarPorId(pedido.getCatalogoTrabajoId());
        } catch (Exception e) {
            log.warn("[CONSUMO-STOCK] No se pudo obtener la receta del trabajo {} desde ms-catalogo: {}. " +
                    "Pedido {} continúa sin descuento automático.",
                    pedido.getCatalogoTrabajoId(), e.getMessage(), pedido.getNroPedido());
            return false;
        }

        List<IngredienteRecetaDTO> receta = trabajo.receta();
        if (receta == null || receta.isEmpty()) {
            log.info("[CONSUMO-STOCK] Trabajo {} no tiene receta. Pedido {} sin descuento.",
                    trabajo.nombre(), pedido.getNroPedido());
            pedido.setStockConsumido(true);
            pedido.setFechaStockConsumido(LocalDateTime.now());
            return true;
        }

        // 2. Por cada ingrediente, registrar SALIDA en stock
        String motivo = String.format("Producción pedido %s", pedido.getNroPedido());
        int ok = 0, errores = 0;
        for (IngredienteRecetaDTO ing : receta) {
            try {
                MovimientoStockRequest mov = new MovimientoStockRequest(
                        ing.materialId(),
                        "SALIDA",
                        ing.cantidad() != null ? ing.cantidad().doubleValue() : 0.0,
                        motivo,
                        pedido.getId()
                );
                stockClient.registrarMovimiento(mov);
                ok++;
                log.info("[CONSUMO-STOCK] {} descontó {} {} de '{}' para pedido {}",
                        pedido.getNroPedido(),
                        formatBigDec(ing.cantidad()),
                        ing.unidad(),
                        ing.materialNombre(),
                        pedido.getNroPedido());
            } catch (Exception e) {
                errores++;
                log.warn("[CONSUMO-STOCK] Error descontando material id={} ({}) para pedido {}: {}",
                        ing.materialId(), ing.materialNombre(), pedido.getNroPedido(), e.getMessage());
            }
        }

        // 3. Marcar como consumido si al menos uno se procesó OK
        if (ok > 0) {
            pedido.setStockConsumido(true);
            pedido.setFechaStockConsumido(LocalDateTime.now());
            log.info("[CONSUMO-STOCK] Pedido {}: {} OK / {} errores", pedido.getNroPedido(), ok, errores);
        }
        return errores == 0;
    }

    private static String formatBigDec(BigDecimal b) {
        return b == null ? "?" : b.stripTrailingZeros().toPlainString();
    }
}
