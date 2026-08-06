package com.sccothe.fridgeclear.telemetry.service;

import com.sccothe.fridgeclear.auth.repository.UserAccountRepository;
import com.sccothe.fridgeclear.auth.service.CurrentUser;
import com.sccothe.fridgeclear.common.api.AuthenticationFailedException;
import com.sccothe.fridgeclear.telemetry.api.AccessLogDtos;
import com.sccothe.fridgeclear.telemetry.domain.AccessLog;
import com.sccothe.fridgeclear.telemetry.repository.AccessLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccessLogService {
    private final AccessLogRepository repository;
    private final UserAccountRepository userAccountRepository;

    public AccessLogService(AccessLogRepository repository, UserAccountRepository userAccountRepository) {
        this.repository = repository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public void record(AccessLogDtos.RecordRequest request, HttpServletRequest httpRequest, String ipAddress) {
        AccessLog log = new AccessLog();
        log.setUserId(resolveOptionalUserId());
        log.setClientId(request.clientId().trim());
        log.setIpAddress(ipAddress);
        log.setUserAgent(trimToLength(httpRequest.getHeader("User-Agent"), 512));
        log.setDeviceType(trimToLength(request.deviceType(), 32));
        log.setAccessType(trimToLength(request.accessType(), 32) == null ? "WEB" : trimToLength(request.accessType(), 32));
        log.setPagePath(trimToLength(request.pagePath(), 512));
        log.setReferrer(trimToLength(request.referrer(), 512));
        log.setLocale(trimToLength(request.locale(), 32));
        log.setTimezone(trimToLength(request.timezone(), 64));
        log.setScreenWidth(request.screenWidth());
        log.setScreenHeight(request.screenHeight());
        log.setViewportWidth(request.viewportWidth());
        log.setViewportHeight(request.viewportHeight());
        log.setConnectionType(trimToLength(request.connectionType(), 32));
        log.setLatitude(request.latitude());
        log.setLongitude(request.longitude());
        log.setGpsAccuracy(request.gpsAccuracy());
        log.setGpsStatus(trimToLength(request.gpsStatus(), 32));
        log.setExtraJson(trimToLength(request.extraJson(), 2000));
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public AccessLogDtos.ListResponse list(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<AccessLog> result = repository.findAllByOrderByCreatedAtDesc(PageRequest.of(safePage, safeSize));
        Map<Long, String> emailByUserId = resolveUserEmails(result.getContent());
        return new AccessLogDtos.ListResponse(
                result.getContent().stream()
                        .map(log -> AccessLogDtos.AccessLogItem.from(log, emailByUserId.get(log.getUserId())))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public AccessLogDtos.StatsResponse stats() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime trendSince = LocalDate.now().minusDays(6).atStartOfDay();

        Map<LocalDate, Long> trendMap = new HashMap<>();
        for (Object[] row : repository.countDailySince(trendSince)) {
            LocalDate day = row[0] instanceof java.sql.Date sqlDate
                    ? sqlDate.toLocalDate()
                    : LocalDate.parse(row[0].toString());
            trendMap.put(day, ((Number) row[1]).longValue());
        }

        List<AccessLogDtos.DailyTrendItem> dailyTrend = new ArrayList<>();
        for (int offset = 0; offset < 7; offset += 1) {
            LocalDate day = LocalDate.now().minusDays(6 - offset);
            dailyTrend.add(new AccessLogDtos.DailyTrendItem(day.toString(), trendMap.getOrDefault(day, 0L)));
        }

        return new AccessLogDtos.StatsResponse(
                repository.count(),
                repository.countByCreatedAtGreaterThanEqual(startOfToday),
                repository.countDistinctClientIdSince(startOfToday),
                toCountItems(repository.countGroupByDeviceType()),
                toCountItems(repository.countGroupByAccessType()),
                toCountItems(repository.countGroupByGpsStatus()),
                toCountItems(repository.countTopPages()),
                dailyTrend
        );
    }

    private Map<Long, String> resolveUserEmails(List<AccessLog> logs) {
        Set<Long> userIds = logs.stream()
                .map(AccessLog::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userAccountRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        user -> user.getId(),
                        user -> user.getEmail(),
                        (left, right) -> left
                ));
    }

    private List<AccessLogDtos.CountItem> toCountItems(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new AccessLogDtos.CountItem(
                        row[0] == null ? "未知" : row[0].toString(),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    private Long resolveOptionalUserId() {
        try {
            return CurrentUser.id();
        } catch (AuthenticationFailedException ignored) {
            return null;
        }
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
