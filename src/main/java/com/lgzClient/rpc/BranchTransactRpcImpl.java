package com.lgzClient.rpc;

import com.lgzClient.types.Result;
import com.lgzClient.types.sql.service.BranchTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BranchTransactRpcImpl implements BranchTransactRpc {
    @Autowired
    @Qualifier("tractRestTemplate")
    private RestTemplate restTemplate;

    @Override
    public Result<BranchTransaction> getBranchTransaction(String branchId) {
        String url = prefix+"/branchTransaction?branchId=" + branchId;
        ResponseEntity<Result<BranchTransaction>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );
        return response.getBody();
    }

    @Override
    public Result<BranchTransaction> joinBranchTransaction(BranchTransaction localType) {
        String url = prefix+"/branchTransaction";
        HttpEntity<BranchTransaction> request = new HttpEntity<>(localType);
        ResponseEntity<Result<BranchTransaction>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Result<BranchTransaction>>() {}
        );
        return response.getBody();
    }

    @Override
    public void deleteBranchTransaction(String branchId) {
        String url = prefix+"/branchTransaction?branchId=" + branchId;
        restTemplate.delete(url);
    }

    @Override
    public void updateBranchTransactionStatus(BranchTransaction branchTransaction) {
        String url = prefix+"/branchTransaction/status";
        HttpEntity<BranchTransaction> request = new HttpEntity<>(branchTransaction);
        restTemplate.exchange(
                url,
                HttpMethod.PUT,
                request,
                Void.class
        );
    }

    @Override
    public void updateBranchTransactionStatusWithNotice(BranchTransaction branchTransaction) {
        String url = prefix+"/branchTransaction/status/notice";
        HttpEntity<BranchTransaction> request = new HttpEntity<>(branchTransaction);
        restTemplate.exchange(
                url,
                HttpMethod.PUT,
                request,
                Void.class
        );
    }
}
