package com.hypepia.apiverse.admin.key;

// monthlyQuota: -1이면 무제한, 그 외 0 이상 값이어야 함
public record UpdateQuotaRequest(Integer monthlyQuota) {}
