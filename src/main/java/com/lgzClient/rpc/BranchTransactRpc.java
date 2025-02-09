package com.lgzClient.rpc;
import com.lgzClient.types.Result;
import com.lgzClient.types.sql.service.BranchTransaction;
import org.springframework.web.bind.annotation.*;
public interface BranchTransactRpc {
    String prefix="http://TractSqlServiceServlet";
    @GetMapping("/branchTransaction")
    Result<BranchTransaction> getBranchTransaction(@RequestParam("branchId") String branchId);
    @PostMapping("/branchTransaction")
    Result<BranchTransaction> joinBranchTransaction(@RequestBody BranchTransaction localType);
    @DeleteMapping("/branchTransaction")
    Result<BranchTransaction> deleteBranchTransaction(@RequestParam("branchId") String branchId);
    @PutMapping("/branchTransaction/status")
    Result<BranchTransaction> updateBranchTransactionStatus(@RequestBody BranchTransaction branchTransaction);
    @PutMapping("/branchTransaction/status/notice")
    Result<BranchTransaction> updateBranchTransactionStatusWithNotice(@RequestBody BranchTransaction branchTransaction);
}
