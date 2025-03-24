package com.lgzClient.rpc.webflux;

import com.lgzClient.types.Result;
import com.lgzClient.types.sql.service.BranchTransaction;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

public interface BranchTransactRpcWebFlux {


    @DeleteMapping("/branchTransaction")
    Mono<Void> deleteBranchTransaction(@RequestParam("branchId") String branchId);

    @PutMapping("/branchTransaction/status")
    Mono<Void> updateBranchTransactionStatus(@RequestBody BranchTransaction branchTransaction);

    @PutMapping("/branchTransaction/status/notice")
    Mono< Void> updateBranchTransactionStatusWithNotice(@RequestBody BranchTransaction branchTransaction);
}