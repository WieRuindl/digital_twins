package org.example.digitaltwin.dto.request;

/**
 * HTTP request DTO representing a command to the air conditioner.
 *
 * @param uid Unique identifier used for correlation with {@link SensorMessage} data.
 * @param temperature Target temperature.
 * @param last Stop signal flag; if true, initiates system shutdown.
 */
public record ConditionerCommand(String uid, Double temperature, Boolean last) {}
