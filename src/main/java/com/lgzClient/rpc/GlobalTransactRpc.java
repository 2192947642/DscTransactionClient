package com.lgzClient.rpc;

import com.lgzClient.types.Result;
import com.lgzClient.types.sql.GlobalTransaction;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
@Service
@FeignClient
public interface GlobalTransactRpc {
    @GetMapping("/globalTransaction")
    Result<GlobalTransaction> getGlobalTransaction(@RequestParam("globalId") String globalId);
    @PostMapping("/globalTransaction/create")
    Result<GlobalTransaction> createGlobalTransaction();
    @GetMapping("/globalTransactions")
    Result<ArrayList<GlobalTransaction>> getGlobalTransactions(@RequestParam("globalIds")ArrayList<String> globalIds);
}
