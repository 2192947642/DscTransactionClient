package com.lgzClient.rpc;

import com.lgzClient.types.Result;
import com.lgzClient.types.sql.service.GlobalTransaction;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
public interface GlobalTransactRpc {
    String prefix="http://TractSqlServiceServlet";
    @GetMapping("/globalTransaction")
    Result<GlobalTransaction> getGlobalTransaction(@RequestParam("globalId") String globalId);
    @PostMapping("/globalTransaction/create")
    Result<GlobalTransaction> createGlobalTransaction();
    @GetMapping("/globalTransactions")
    Result<ArrayList<GlobalTransaction>> getGlobalTransactions(@RequestParam("globalIds")ArrayList<String> globalIds);
}
