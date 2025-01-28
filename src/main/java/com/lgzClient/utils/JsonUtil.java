package com.lgzClient.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T jsonToObject(String json, Class<T> clazz)   {
        try {
            return objectMapper.readValue(json, clazz);
        }catch (JsonProcessingException e){
            throw new RuntimeException(e);
        }

    }

    public static String objToJson(Object obj){
        try{
            return objectMapper.writeValueAsString(obj);
        }catch (Exception e){
            throw  new RuntimeException(e);
        }
    }

}
