package com.lgzClient.exceptions;

public class DcsTransactionError extends RuntimeException{
    public DcsTransactionError(String message) {
        super(message);
    }
}
