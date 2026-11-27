package com.dhoon.transfertracker.external.football.client;
import tools.jackson.databind.JsonNode;
import java.math.BigDecimal;
public final class StatisticsJson {
    private StatisticsJson() {}
    public static String string(JsonNode n) { return n == null || n.isNull() || n.isMissingNode() ? null : n.asText(); }
    public static Integer integer(JsonNode n) { String s = string(n); return s == null ? null : new BigDecimal(s.replace("%", "")).intValueExact(); }
    public static BigDecimal decimal(JsonNode n) { String s = string(n); return s == null ? null : new BigDecimal(s); }
    public static Boolean bool(JsonNode n) { if (string(n) == null) return null; if (!n.isBoolean()) throw new IllegalArgumentException("Invalid boolean"); return n.asBoolean(); }
    public static Long id(JsonNode n) { if (n == null || !n.isIntegralNumber() || n.asLong() <= 0) throw new IllegalArgumentException("Invalid API ID"); return n.asLong(); }
    public static String name(JsonNode n) { String s = string(n); if (s == null || s.isBlank()) throw new IllegalArgumentException("Missing name"); return s; }
    public static JsonNode response(JsonNode root) { if (root == null || !root.path("errors").isEmpty() || !root.path("response").isArray()) throw new IllegalStateException("Invalid API response"); return root.path("response"); }
}
