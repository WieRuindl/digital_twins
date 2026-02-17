package org.example.digitaltwin.dto.request;

public record ConditionerCommand(String uid, Double temperature, Boolean last) {
}
