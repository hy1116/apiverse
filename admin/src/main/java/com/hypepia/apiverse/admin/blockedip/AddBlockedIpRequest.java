package com.hypepia.apiverse.admin.blockedip;

public record AddBlockedIpRequest(
        String ipAddress,
        String reason
) {}
