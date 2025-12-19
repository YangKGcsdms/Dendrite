package com.carter.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.List;

/**
 * @author Carter
 * @description Converts List<Double> to PostgreSQL vector string format "[1.1, 2.2, ...]"
 */
@Converter
public class VectorToStringConverter implements AttributeConverter<List<Double>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<Double> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert vector to string", e);
        }
    }

    @Override
    public List<Double> convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<Double>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert string to vector", e);
        }
    }
}

