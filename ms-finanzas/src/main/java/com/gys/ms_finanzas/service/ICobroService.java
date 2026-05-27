package com.gys.ms_finanzas.service;

import com.gys.ms_finanzas.dto.CobroRequest;
import com.gys.ms_finanzas.dto.RegistroCobroResponse;

public interface ICobroService {
    RegistroCobroResponse registrarCobro(CobroRequest request);
}
