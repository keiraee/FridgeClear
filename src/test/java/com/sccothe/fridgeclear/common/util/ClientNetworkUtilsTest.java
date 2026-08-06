package com.sccothe.fridgeclear.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientNetworkUtilsTest {

    @Test
    void prefersFirstPublicIpFromXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1, 127.0.0.1");
        request.setRemoteAddr("127.0.0.1");

        assertThat(ClientNetworkUtils.resolveClientIp(request)).isEqualTo("203.0.113.5");
    }

    @Test
    void usesXRealIpWhenForwardedChainIsLocalOnly() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "127.0.0.1");
        request.addHeader("X-Real-IP", "198.51.100.9");
        request.setRemoteAddr("127.0.0.1");

        assertThat(ClientNetworkUtils.resolveClientIp(request)).isEqualTo("198.51.100.9");
    }

    @Test
    void normalizesIpv6Loopback() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("0:0:0:0:0:0:0:1");

        assertThat(ClientNetworkUtils.resolveClientIp(request)).isEqualTo("127.0.0.1");
    }
}
