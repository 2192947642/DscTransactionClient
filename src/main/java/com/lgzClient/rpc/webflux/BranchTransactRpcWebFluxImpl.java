package com.lgzClient.rpc.webflux;

import com.lgzClient.rpc.BranchTransactRpc;
import com.lgzClient.types.Result;
import com.lgzClient.types.sql.service.BranchTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class BranchTransactRpcWebFluxImpl implements BranchTransactRpcWebFlux {

    private final WebClient webClient;

    @Autowired
    public BranchTransactRpcWebFluxImpl(@Qualifier("dcsLoadBalancedWebClientBuilder") WebClient.Builder webClientBuilder) {
        this.webClient=webClientBuilder.baseUrl(BranchTransactRpc.prefix).build();
    }

    @Override
    public Mono<Void> deleteBranchTransaction(String branchId) {
        return webClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/branchTransaction")
                        .queryParam("branchId", branchId)
                        .build())
                .retrieve().bodyToMono(Void.class);
    }

    @Override
    public Mono<Void> updateBranchTransactionStatus(BranchTransaction branchTransaction) {
        return webClient.put()
                .uri("/branchTransaction/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(branchTransaction)
                .retrieve()
                .bodyToMono(Void.class);
    }

    @Override
    public Mono<Void> updateBranchTransactionStatusWithNotice(BranchTransaction branchTransaction) {
        return webClient.put()
                .uri("/branchTransaction/status/notice")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(branchTransaction)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
