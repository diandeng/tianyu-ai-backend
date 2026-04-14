package com.hym.tianyuaibackend.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PythonAiResponse {
    private Integer code;
    private String msg;
    private DataDTO data;

    @Data
    public static class DataDTO {
        @JsonProperty("class") // 对应 "class"
        private String className;

        private Double confidence;

        @JsonProperty("is_confident") // 对应 "is_confident"
        private Boolean isConfident;
    }
}
