package com.sccothe.fridgeclear.telemetry.api;

import com.sccothe.fridgeclear.telemetry.domain.AccessLog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class AccessLogDtos {
    private AccessLogDtos() {}

    public record RecordRequest(
            @NotBlank @Size(max = 64) String clientId,
            @Size(max = 32) String deviceType,
            @Size(max = 32) String accessType,
            @Size(max = 512) String pagePath,
            @Size(max = 512) String referrer,
            @Size(max = 32) String locale,
            @Size(max = 64) String timezone,
            Integer screenWidth,
            Integer screenHeight,
            Integer viewportWidth,
            Integer viewportHeight,
            @Size(max = 32) String connectionType,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal gpsAccuracy,
            @Size(max = 32) String gpsStatus,
            @Size(max = 2000) String extraJson
    ) {}

    public record AccessLogItem(
            Long id,
            Long userId,
            String userEmail,
            String clientId,
            String ipAddress,
            String userAgent,
            String deviceType,
            String accessType,
            String pagePath,
            String referrer,
            String locale,
            String timezone,
            Integer screenWidth,
            Integer screenHeight,
            Integer viewportWidth,
            Integer viewportHeight,
            String connectionType,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal gpsAccuracy,
            String gpsStatus,
            String extraJson,
            LocalDateTime createdAt
    ) {
        public static AccessLogItem from(AccessLog log, String userEmail) {
            return new AccessLogItem(
                    log.getId(),
                    log.getUserId(),
                    userEmail,
                    log.getClientId(),
                    log.getIpAddress(),
                    log.getUserAgent(),
                    log.getDeviceType(),
                    log.getAccessType(),
                    log.getPagePath(),
                    log.getReferrer(),
                    log.getLocale(),
                    log.getTimezone(),
                    log.getScreenWidth(),
                    log.getScreenHeight(),
                    log.getViewportWidth(),
                    log.getViewportHeight(),
                    log.getConnectionType(),
                    log.getLatitude(),
                    log.getLongitude(),
                    log.getGpsAccuracy(),
                    log.getGpsStatus(),
                    log.getExtraJson(),
                    log.getCreatedAt()
            );
        }
    }

    public record CountItem(String label, long count) {}

    public record DailyTrendItem(String date, long count) {}

    public record StatsResponse(
            long total,
            long todayCount,
            long uniqueVisitorsToday,
            List<CountItem> deviceBreakdown,
            List<CountItem> accessTypeBreakdown,
            List<CountItem> gpsStatusBreakdown,
            List<CountItem> topPages,
            List<DailyTrendItem> dailyTrend
    ) {}

    public record ListResponse(List<AccessLogItem> items, int page, int size, long total) {}
}
