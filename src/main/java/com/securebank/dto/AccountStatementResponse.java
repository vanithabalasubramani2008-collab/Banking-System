package com.securebank.dto;

import java.util.List;

public class AccountStatementResponse {

    private AccountResponse account;
    private List<TransactionResponse> recentTransactions;
    private int totalTransactionsCount;

    public AccountStatementResponse() {
    }

    public AccountStatementResponse(AccountResponse account, List<TransactionResponse> recentTransactions, int totalTransactionsCount) {
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

    public int getTotalTransactionsCount() {
        return totalTransactionsCount;
    }

    public void setTotalTransactionsCount(int totalTransactionsCount) {
        this.totalTransactionsCount = totalTransactionsCount;
    }
}
