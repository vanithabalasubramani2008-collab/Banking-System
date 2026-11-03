package com.securebank.dto;

import java.util.List;

public class AccountStatementResponse {

    private AccountResponse account;
    private List<TransactionResponse> recentTransactions;
    private long totalTransactionsCount;

    public AccountStatementResponse() {
    }

    public AccountStatementResponse(AccountResponse account, List<TransactionResponse> recentTransactions, long totalTransactionsCount) {
        this.account = account;
        this.recentTransactions = recentTransactions;
        this.totalTransactionsCount = totalTransactionsCount;
    }

    public AccountResponse getAccount() {
        return account;
    }

    public void setAccount(AccountResponse account) {
        this.account = account;
    }

    public List<TransactionResponse> getRecentTransactions() {
        return recentTransactions;
    }

    public void setRecentTransactions(List<TransactionResponse> recentTransactions) {
        this.recentTransactions = recentTransactions;
    }

    public long getTotalTransactionsCount() {
        return totalTransactionsCount;
    }

    public void setTotalTransactionsCount(long totalTransactionsCount) {
        this.totalTransactionsCount = totalTransactionsCount;
    }
}
