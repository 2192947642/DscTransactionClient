package com.lgzClient.rpc;

import com.lgzClient.types.LocalType;
import com.lgzClient.types.Result;
import com.lgzClient.types.sql.BranchTransaction;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
@Service
@FeignClient
public interface BranchTransactRpc {
    @GetMapping("/branchTransaction")
    Result<BranchTransaction> getBranchTransaction(@RequestParam("localId") String localId);
    @PostMapping
    Result<BranchTransaction> joinBranchTransaction(@RequestBody LocalType localType);
    @DeleteMapping
    Result<BranchTransaction> deleteBranchTransaction(@RequestParam("localId") String localId);
    @PutMapping("/branchTransaction/status")
    Result<BranchTransaction> updateBranchTransactionStatus(@RequestBody BranchTransaction branchTransaction);
    @PutMapping("/branchTransaction/status/notice")
    Result<BranchTransaction> updateBranchTransactionStatusWithNotice(@RequestBody BranchTransaction branchTransaction);
}
