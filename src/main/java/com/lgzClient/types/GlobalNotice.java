package com.lgzClient.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GlobalNotice {
    private String localId;
    private String globalId;
    private Boolean  isSuccess;//是否成功
}
