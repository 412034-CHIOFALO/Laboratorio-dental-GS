package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.SueldoRequest;
import com.gs.ms_finanzas.dto.SueldoResponse;
import com.gs.ms_finanzas.exception.BusinessException;
import com.gs.ms_finanzas.model.EstadoSueldo;
import com.gs.ms_finanzas.model.SueldoEmpleado;
import com.gs.ms_finanzas.repository.SueldoEmpleadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SueldoService implements ISueldoService {

    private final SueldoEmpleadoRepository sueldoRepo;

    public List<SueldoResponse> listarPorMes(int anio, int mes) {
        return sueldoRepo.findByAnioAndMesOrderByEmpleadoNombreAsc(anio, mes).stream()
            .map(SueldoResponse::from)
            .toList();
    }

    public List<SueldoResponse> listarPendientesMes(int anio, int mes) {
        return sueldoRepo.findByAnioAndMesAndEstadoOrderByEmpleadoNombreAsc(anio, mes, EstadoSueldo.PENDIENTE).stream()
            .map(SueldoResponse::from)
            .toList();
    }

    @Transactional
    public SueldoResponse registrar(SueldoRequest request) {
        if (sueldoRepo.existsByEmpleadoIdAndAnioAndMes(request.getEmpleadoId(), request.getAnio(), request.getMes())) {
            throw new BusinessException("Ya existe un sueldo registrado para el empleado "
                + request.getEmpleadoNombre() + " en " + request.getMes() + "/" + request.getAnio());
        }
        SueldoEmpleado s = SueldoEmpleado.builder()
            .empleadoId(request.getEmpleadoId())
            .empleadoNombre(request.getEmpleadoNombre())
            .monto(request.getMonto())
            .mes(request.getMes())
            .anio(request.getAnio())
            .observaciones(request.getObservaciones())
            .build();
        return SueldoResponse.from(sueldoRepo.save(s));
    }
}
