package com.lgzClient.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GlobalNotice {
    private LocalType localType;
    private Boolean isSuccess;
}
