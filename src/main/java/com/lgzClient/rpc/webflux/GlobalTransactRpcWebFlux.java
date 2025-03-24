package com.lgzClient.rpc.webflux;

import com.lgzClient.types.Result;
import com.lgzClient.types.sql.service.GlobalTransaction;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.function.Consumer;

public interface GlobalTransactRpcWebFlux {
    @GetMapping("/globalTransactions")
    Mono<Result<ArrayList<GlobalTransaction>>> getGlobalTransactions(@RequestParam("globalIds")ArrayList<String> globalIds);

}
