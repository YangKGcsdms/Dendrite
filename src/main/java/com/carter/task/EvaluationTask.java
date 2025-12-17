package com.carter.task;


import java.io.Serializable;

/**
 * @author Carter
 * @date 2025/12/16
 * @description
 */
public record EvaluationTask(String employeeName, String rawContent) implements Serializable {

}