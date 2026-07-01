package com.hypepia.apiverse.core.repository;

public interface DailyStat {
    String getDate();
    Long getRequests();
    Long getErrors();
}
