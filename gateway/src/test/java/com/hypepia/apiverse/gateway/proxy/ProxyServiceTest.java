package com.hypepia.apiverse.gateway.proxy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyServiceTest {

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
}
