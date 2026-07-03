package com.hypepia.apiverse.gateway.proxy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ProxyServiceTest {

    private static ServerHttpRequest requestWithForwardedFor(String forwardedFor, String remoteIp) {
        ServerHttpRequest req = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        if (forwardedFor != null) {
            headers.set("X-Forwarded-For", forwardedFor);
        }
        given(req.getHeaders()).willReturn(headers);
        given(req.getRemoteAddress()).willReturn(new InetSocketAddress(remoteIp, 12345));
        return req;
    }

    @Test
    void resolveClientIp_distrust_forwarded_header_uses_socket_address() {
        ServerHttpRequest req = requestWithForwardedFor("9.9.9.9", "10.0.0.5");

        assertThat(ProxyService.resolveClientIp(req, false)).isEqualTo("10.0.0.5");
    }

    @Test
    void resolveClientIp_trust_forwarded_header_uses_last_hop_not_client_supplied_first_hop() {
        // 클라이언트가 조작 가능한 첫 항목("9.9.9.9")이 아니라, 신뢰 가능한 프록시가 실제로
        // 관찰한 마지막 항목("10.0.0.5")을 써야 스푸핑을 막을 수 있다.
        ServerHttpRequest req = requestWithForwardedFor("9.9.9.9, 10.0.0.5", "172.17.0.2");

        assertThat(ProxyService.resolveClientIp(req, true)).isEqualTo("10.0.0.5");
    }

    @Test
    void resolveClientIp_trust_forwarded_header_but_header_absent_falls_back_to_socket_address() {
        ServerHttpRequest req = requestWithForwardedFor(null, "10.0.0.5");

        assertThat(ProxyService.resolveClientIp(req, true)).isEqualTo("10.0.0.5");
    }

    @Test
    void isIpAllowed_blank_whitelist_allows_any_ip() {
        assertThat(ProxyService.isIpAllowed(null, "1.2.3.4")).isTrue();
        assertThat(ProxyService.isIpAllowed("", "1.2.3.4")).isTrue();
        assertThat(ProxyService.isIpAllowed("   ", "1.2.3.4")).isTrue();
    }

    @Test
    void isIpAllowed_single_ip_exact_match() {
        assertThat(ProxyService.isIpAllowed("1.2.3.4", "1.2.3.4")).isTrue();
        assertThat(ProxyService.isIpAllowed("1.2.3.4", "9.9.9.9")).isFalse();
    }

    @Test
    void isIpAllowed_comma_separated_list_matches_any() {
        assertThat(ProxyService.isIpAllowed("1.2.3.4,5.6.7.8", "5.6.7.8")).isTrue();
        assertThat(ProxyService.isIpAllowed("1.2.3.4, 5.6.7.8", "5.6.7.8")).isTrue();
        assertThat(ProxyService.isIpAllowed("1.2.3.4,5.6.7.8", "9.9.9.9")).isFalse();
    }

    @Test
    void toIPv4IfPossible_ipv4_address_unchanged() throws UnknownHostException {
        assertThat(ProxyService.toIPv4IfPossible(InetAddress.getByName("1.2.3.4"))).isEqualTo("1.2.3.4");
    }

    @Test
    void toIPv4IfPossible_ipv6_loopback_becomes_127_0_0_1() throws UnknownHostException {
        assertThat(ProxyService.toIPv4IfPossible(InetAddress.getByName("::1"))).isEqualTo("127.0.0.1");
    }

    @Test
    void toIPv4IfPossible_ipv4_mapped_ipv6_extracts_ipv4() throws UnknownHostException {
        assertThat(ProxyService.toIPv4IfPossible(InetAddress.getByName("::ffff:192.168.1.10"))).isEqualTo("192.168.1.10");
    }
}
