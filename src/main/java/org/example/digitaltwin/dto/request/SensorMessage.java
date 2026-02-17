package org.example.digitaltwin.dto.request;

/**
 * MQTT message payload representing a temperature sensor reading.
 *
 * @param id Sensor identifier used to match against {@link ConditionerCommand} commands.
 * @param temp Current temperature value.
 */
public record SensorMessage(String id, Double temp) {}
