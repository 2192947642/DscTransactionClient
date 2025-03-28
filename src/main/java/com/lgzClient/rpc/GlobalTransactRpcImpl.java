package com.lgzClient.rpc;

import com.lgzClient.types.BothTransaction;
import com.lgzClient.types.Result;
import com.lgzClient.types.sql.service.BranchTransaction;
import com.lgzClient.types.sql.service.GlobalTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

@Service
public class GlobalTransactRpcImpl implements GlobalTransactRpc {
    @Autowired
    @Qualifier("tractRestTemplate")
    private RestTemplate restTemplate;

    @Override
    public Result<GlobalTransaction> getGlobalTransaction(String globalId) {
        String url =prefix+"/globalTransaction?globalId=" + globalId;
        ResponseEntity<Result<GlobalTransaction>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );
        return response.getBody();
    }

    @Override
    public Result<GlobalTransaction> createGlobalTransaction(Long timeout) {
        String url = prefix+"/globalTransaction/create?timeout="+timeout;
        ResponseEntity<Result<GlobalTransaction>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<Result<GlobalTransaction>>() {}
        );
        return response.getBody();
    }

    @Override
    public Result<ArrayList<GlobalTransaction>> getGlobalTransactions(ArrayList<String> globalIds) {
        String url = prefix+"/globalTransactions?globalIds=" + String.join(",", globalIds);
        ResponseEntity<Result<ArrayList<GlobalTransaction>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Result<ArrayList<GlobalTransaction>>>() {}
        );
        return response.getBody();
    }



    @Override
    public Result<BothTransaction> createAndJoinGlobalTransaction(Long timeout, BranchTransaction branchTransaction) {
        String url = prefix+"/globalTransaction/createAndJoin?timeout="+timeout;
        HttpEntity<BranchTransaction> request = new HttpEntity<>(branchTransaction);
        ResponseEntity<Result<BothTransaction>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Result<BothTransaction>>() {}
        );
        return response.getBody();
    }

    @Override
    public void deleteGlobalTransaction(String globalId) {
        String url = prefix+"/globalTransaction?globalId=" + globalId;
        restTemplate.delete(url);
    }
}
