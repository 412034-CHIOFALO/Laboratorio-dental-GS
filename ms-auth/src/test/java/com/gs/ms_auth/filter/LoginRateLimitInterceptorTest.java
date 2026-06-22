package com.gs.ms_auth.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimitInterceptorTest {

    private final LoginRateLimitInterceptor interceptor = new LoginRateLimitInterceptor();

    private MockHttpServletRequest loginReq(String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod("POST");
        req.setRequestURI("/api/auth/login");
        req.setRemoteAddr(ip);
        return req;
    }

    @Test
    void rutaNoLogin_pasaSiempre() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod("GET");
        req.setRequestURI("/api/auth/usuarios");
        assertThat(interceptor.preHandle(req, new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void dentroDelLimite_pasaYAgregaHeader() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        boolean ok = interceptor.preHandle(loginReq("1.1.1.1"), res, new Object());
        assertThat(ok).isTrue();
        assertThat(res.getHeader("X-RateLimit-Remaining")).isNotNull();
    }

    @Test
    void excedeLimite_devuelve429() throws Exception {
        String ip = "2.2.2.2";
        // 5 permitidos
        for (int i = 0; i < 5; i++) {
            assertThat(interceptor.preHandle(loginReq(ip), new MockHttpServletResponse(), new Object())).isTrue();
        }
        // el sexto se bloquea
        MockHttpServletResponse res = new MockHttpServletResponse();
        boolean ok = interceptor.preHandle(loginReq(ip), res, new Object());
        assertThat(ok).isFalse();
        assertThat(res.getStatus()).isEqualTo(429);
    }

    @Test
    void resuelveIp_porXForwardedFor() throws Exception {
        MockHttpServletRequest req = loginReq("9.9.9.9");
        req.addHeader("X-Forwarded-For", "8.8.8.8, 7.7.7.7");
        assertThat(interceptor.preHandle(req, new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void resuelveIp_porXRealIp() throws Exception {
        MockHttpServletRequest req = loginReq("9.9.9.9");
        req.addHeader("X-Real-IP", "6.6.6.6");
        assertThat(interceptor.preHandle(req, new MockHttpServletResponse(), new Object())).isTrue();
    }
}
