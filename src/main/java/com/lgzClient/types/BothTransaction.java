package com.lgzClient.types;


import com.lgzClient.types.sql.service.BranchTransaction;
import com.lgzClient.types.sql.service.GlobalTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BothTransaction {
    BranchTransaction branchTransaction;
    GlobalTransaction globalTransaction;
}
