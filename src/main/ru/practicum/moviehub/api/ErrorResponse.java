package ru.practicum.moviehub.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ErrorResponse {
    private final String error;
    private final List<String> details;

    public ErrorResponse(String error) {
        this.error = error;
        this.details = new ArrayList<>();
    }

    public ErrorResponse(String error, String... details) {
        this.error = error;
        this.details = Arrays.asList(details);
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"error\":\"").append(escape(error)).append("\"");

        if (!details.isEmpty()) {
            sb.append(",\"details\":[");
            for (int i = 0; i < details.size(); i++) {
                sb.append("\"").append(escape(details.get(i))).append("\"");
                if (i < details.size() - 1) sb.append(",");
            }
            sb.append("]");
        }

        sb.append("}");
        return sb.toString();
    }

    private String escape(String s) {
        return s.replace("\"", "\\\"");
    }
}
