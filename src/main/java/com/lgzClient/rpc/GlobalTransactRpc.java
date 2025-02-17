package com.lgzClient.rpc;

import com.lgzClient.types.BothTransaction;
import com.lgzClient.types.Result;
import com.lgzClient.types.sql.service.BranchTransaction;
import com.lgzClient.types.sql.service.GlobalTransaction;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
public interface GlobalTransactRpc {
    String prefix="http://TractSqlServiceServlet";
    @GetMapping("/globalTransaction")
    Result<GlobalTransaction> getGlobalTransaction(@RequestParam("globalId") String globalId);
    @PostMapping("/globalTransaction/create")
    Result<GlobalTransaction> createGlobalTransaction(@RequestParam("timeout") Long timeout);
    @GetMapping("/globalTransactions")
    Result<ArrayList<GlobalTransaction>> getGlobalTransactions(@RequestParam("globalIds")ArrayList<String> globalIds);
    @PostMapping("/globalTransaction/createAndJoin")
    Result<BothTransaction> createAndJoinGlobalTransaction(@RequestParam("timeout") Long timeout, @RequestBody BranchTransaction branchTransaction);
}
