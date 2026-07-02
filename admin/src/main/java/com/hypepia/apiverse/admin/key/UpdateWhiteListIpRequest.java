package com.hypepia.apiverse.admin.key;

// whiteListIp: 콤마로 구분된 허용 IP 목록 (예: "1.2.3.4,5.6.7.8"). null/빈 문자열이면 제한 해제.
public record UpdateWhiteListIpRequest(String whiteListIp) {}
