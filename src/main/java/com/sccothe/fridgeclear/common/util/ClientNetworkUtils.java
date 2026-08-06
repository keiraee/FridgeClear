package com.sccothe.fridgeclear.common.util;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ClientNetworkUtils {
    private static final Pattern FORWARDED_FOR_TOKEN = Pattern.compile("for=\"?([^;,\"]+)\"?");
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)(?:\\.(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}$");

    private ClientNetworkUtils() {}

    public static String resolveClientIp(HttpServletRequest request) {
        List<String> candidates = new ArrayList<>();
        candidates.addAll(parseForwardedFor(request.getHeader("X-Forwarded-For")));
        addCandidate(candidates, request.getHeader("X-Real-IP"));
        addCandidate(candidates, parseForwardedHeader(request.getHeader("Forwarded")));
        addCandidate(candidates, request.getHeader("CF-Connecting-IP"));
        addCandidate(candidates, request.getRemoteAddr());

        for (String candidate : candidates) {
            if (isPublicIp(candidate)) {
                return candidate;
            }
        }
        for (String candidate : candidates) {
            if (isValidIp(candidate)) {
                return candidate;
            }
        }
        return "unknown";
    }

    private static List<String> parseForwardedFor(String header) {
        List<String> values = new ArrayList<>();
        if (header == null || header.isBlank()) {
            return values;
        }
        for (String part : header.split(",")) {
            addCandidate(values, part);
        }
        return values;
    }

    private static String parseForwardedHeader(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        Matcher matcher = FORWARDED_FOR_TOKEN.matcher(header.toLowerCase(Locale.ROOT));
        if (!matcher.find()) {
            return null;
        }
        return normalizeIp(matcher.group(1));
    }

    private static void addCandidate(List<String> candidates, String raw) {
        String normalized = normalizeIp(raw);
        if (normalized == null || candidates.contains(normalized)) {
            return;
        }
        candidates.add(normalized);
    }

    static String normalizeIp(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty() || "unknown".equalsIgnoreCase(value)) {
            return null;
        }
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
            value = value.substring(1, value.length() - 1).trim();
        }
        if (value.startsWith("[")) {
            int end = value.indexOf(']');
            if (end > 0) {
                value = value.substring(1, end);
            }
        } else {
            int colon = value.indexOf(':');
            if (colon > 0 && value.indexOf('.') >= 0) {
                value = value.substring(0, colon);
            }
        }
        if ("0:0:0:0:0:0:0:1".equals(value) || "::1".equals(value)) {
            return "127.0.0.1";
        }
        return value.isEmpty() ? null : value;
    }

    static boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        if (IPV4_PATTERN.matcher(ip).matches()) {
            return true;
        }
        try {
            InetAddress.getByName(ip);
            return ip.contains(":");
        } catch (UnknownHostException exception) {
            return false;
        }
    }

    static boolean isPublicIp(String ip) {
        if (!isValidIp(ip)) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            return !(address.isLoopbackAddress()
                    || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress()
                    || address.isAnyLocalAddress());
        } catch (UnknownHostException exception) {
            return false;
        }
    }
}
