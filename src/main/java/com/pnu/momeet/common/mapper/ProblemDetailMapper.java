package com.pnu.momeet.common.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ProblemDetail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProblemDetailMapper {
   private final ObjectMapper objectMapper;

   public ProblemDetailMapper() {
         this.objectMapper = new ObjectMapper();
   }

    public String toJson(ProblemDetail problemDetail) {
         Map<String, Object> map = new HashMap<>();
         map.put("instance", problemDetail.getInstance());
         map.put("detail", problemDetail.getDetail());
         map.put("type", problemDetail.getType());
         map.put("title", problemDetail.getTitle());
         map.put("status", problemDetail.getStatus());

         if (problemDetail.getProperties() != null) {
                problemDetail.getProperties()
                    .entrySet()
                    .stream()
                    .filter(entry -> !map.containsKey(entry.getKey()))
                    .forEach(entry -> map.put(entry.getKey(), entry.getValue()));
         }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (IOException e) {
            String instanceStr = problemDetail.getInstance() == null ?
                    "null" : '"' + problemDetail.getInstance().toString() + '"';

            return "{" +
                    "\"instance\":" + instanceStr + "," +
                    "\"detail\":\"예외를 직렬화하던 중 문제가 발생했습니다.\"," +
                    "\"type\":\"https://example.com/error/internal\"," +
                    "\"title\":\"직렬화 도중 문제 발생\"," +
                    "\"status\":500" +
                "}";
        }
    }
}
