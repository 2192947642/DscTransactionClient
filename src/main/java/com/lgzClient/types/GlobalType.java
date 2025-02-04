package com.lgzClient.types;

import com.lgzClient.types.status.GlobalStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GlobalType {

   private GlobalStatus status;
   private Map<String,LocalType> localTypeMap;
}
