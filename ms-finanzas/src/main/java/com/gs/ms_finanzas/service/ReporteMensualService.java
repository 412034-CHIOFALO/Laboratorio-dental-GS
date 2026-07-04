package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.ReporteMensualResponse;
import com.gs.ms_finanzas.exception.BusinessException;
import com.gs.ms_finanzas.model.ReporteMensual;
import com.gs.ms_finanzas.repository.ReporteMensualRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Genera, archiva y lista los reportes financieros mensuales.
 *
 * <p>El PDF se produce con {@link ReporteCajaPdfService} (mismo contenido que la descarga
 * manual) y se guarda en MinIO; los metadatos quedan en la tabla {@code reporte_mensual}
 * (uno por mes, se sobrescribe al regenerar). Los reportes se consultan/descargan desde
 * la sección Documentos del panel.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReporteMensualService {

    private final ReporteCajaPdfService pdfService;
    private final MinioStorageService storage;
    private final ReporteMensualRepository repo;

    /**
     * Genera (o regenera) el reporte del mes indicado y lo archiva en el sistema.
     *
     * @param anio       año del período.
     * @param mes        mes del período (1-12).
     * @param automatico {@code true} si lo dispara la tarea programada.
     * @return metadatos del reporte archivado.
     */
    @Transactional
    public ReporteMensualResponse generar(int anio, int mes, boolean automatico) {
        if (mes < 1 || mes > 12) {
            throw new BusinessException("El mes debe estar entre 1 y 12.");
        }

        byte[] pdf = pdfService.resumenMensual(anio, mes);
        String nombre = String.format("resumen-mensual-%d-%02d.pdf", anio, mes);
        String objectName = storage.subirReporte(pdf, anio, mes);
        if (objectName == null) {
            throw new BusinessException("No se pudo archivar el reporte: el almacenamiento no está disponible.");
        }

        ReporteMensual r = repo.findByAnioAndMes(anio, mes).orElseGet(ReporteMensual::new);
        r.setAnio(anio);
        r.setMes(mes);
        r.setObjectName(objectName);
        r.setNombreArchivo(nombre);
        r.setGeneradoEn(LocalDateTime.now());
        r.setAutomatico(automatico);
        repo.save(r);

        log.info("[REPORTE] Reporte mensual {}/{} generado ({}).", mes, anio, automatico ? "automatico" : "manual");
        return ReporteMensualResponse.from(r);
    }

    /** Genera el reporte del mes anterior al actual (lo usa la tarea programada). */
    @Transactional
    public void generarMesAnterior() {
        YearMonth anterior = YearMonth.now().minusMonths(1);
        generar(anterior.getYear(), anterior.getMonthValue(), true);
    }

    /** Lista todos los reportes archivados, del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public List<ReporteMensualResponse> listar() {
        return repo.findAllByOrderByAnioDescMesDesc().stream()
            .map(ReporteMensualResponse::from)
            .toList();
    }

    /** Devuelve una URL temporal (presigned) para descargar el PDF del reporte. */
    @Transactional(readOnly = true)
    public String urlDescarga(Long id) {
        ReporteMensual r = repo.findById(id)
            .orElseThrow(() -> new BusinessException("El reporte solicitado no existe."));
        String url = storage.urlTemporal(r.getObjectName(), 15);
        if (url == null) {
            throw new BusinessException("No se pudo generar el enlace de descarga del reporte.");
        }
        return url;
    }
}
