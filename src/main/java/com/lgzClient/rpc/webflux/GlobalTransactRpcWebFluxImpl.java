package com.lgzClient.rpc.webflux;

import com.lgzClient.rpc.BranchTransactRpc;
import com.lgzClient.rpc.GlobalTransactRpc;
import com.lgzClient.types.Result;
import com.lgzClient.types.sql.service.GlobalTransaction;
import com.lgzClient.utils.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.function.Consumer;

@Component
public class GlobalTransactRpcWebFluxImpl implements GlobalTransactRpcWebFlux{
    private WebClient webClient;

    @Autowired
    GlobalTransactRpcWebFluxImpl(@Qualifier("DCSLoadBalancedWebClientBuilder") WebClient.Builder builder){
        this.webClient=builder.baseUrl(GlobalTransactRpc.prefix).build();
    }
    @Override
    public Mono<Result<ArrayList<GlobalTransaction>>> getGlobalTransactions(ArrayList<String> globalIds) {
        Mono<Result<ArrayList<GlobalTransaction>>> response= webClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/branchTransaction")
                        .queryParam("globalIds", JsonUtil.objToJson(globalIds))
                        .build())
                .retrieve().bodyToMono(new ParameterizedTypeReference<>() {
                });
        return response;
    }


}
