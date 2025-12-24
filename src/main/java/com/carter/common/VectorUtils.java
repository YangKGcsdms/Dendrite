package com.carter.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for vector operations.
 * Eliminates repetitive float[]/List&lt;Double&gt; conversion code.
 *
 * @author Carter
 * @since 1.0.0
 */
public final class VectorUtils {

    private VectorUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Converts a float array to a List of Doubles.
     * Required for Hibernate compatibility with pgvector.
     *
     * @param floatArray the source array from embedding model
     * @return List of Double values, or empty list if input is null
     */
    public static List<Double> toDoubleList(float[] floatArray) {
        if (floatArray == null) {
            return List.of();
        }

        List<Double> result = new ArrayList<>(floatArray.length);
        for (float v : floatArray) {
            result.add((double) v);
        }
        return result;
    }

    /**
     * Converts a List of Doubles back to a float array.
     *
     * @param doubleList the source list
     * @return float array, or empty array if input is null/empty
     */
    public static float[] toFloatArray(List<Double> doubleList) {
        if (doubleList == null || doubleList.isEmpty()) {
            return new float[0];
        }

        float[] result = new float[doubleList.size()];
        for (int i = 0; i < doubleList.size(); i++) {
            result[i] = doubleList.get(i).floatValue();
        }
        return result;
    }

    /**
     * Computes cosine similarity between two vectors.
     *
     * @param vec1 first vector as float array
     * @param vec2 second vector as List of Double
     * @return similarity score between -1 and 1, or 0 if vectors are incompatible
     */
    public static double cosineSimilarity(float[] vec1, List<Double> vec2) {
        if (vec1 == null || vec2 == null || vec1.length != vec2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            double b = vec2.get(i);
            dotProduct += vec1[i] * b;
            normA += vec1[i] * vec1[i];
            normB += b * b;
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Computes cosine similarity between two float arrays.
     *
     * @param vec1 first vector
     * @param vec2 second vector
     * @return similarity score between -1 and 1
     */
    public static double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length != vec2.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            normA += vec1[i] * vec1[i];
            normB += vec2[i] * vec2[i];
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Converts a List of Doubles to a PostgreSQL vector string format.
     * Example: [0.123, 0.456, 0.789]
     *
     * @param vector the vector list
     * @return string representation compatible with pgvector
     */
    public static String toVectorString(List<Double> vector) {
        if (vector == null || vector.isEmpty()) {
            return "[]";
        }
        return vector.toString();
    }
}

