package service;

import domsin.Account;
import domsin.Transaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface BankService {
    String openAccount(String name, String email, String accountType);
    List<Account> listAccounts();
    void deposit(String accountNumber, Double amount, String note);
    void withdraw(String accountNumber, Double amount, String withdraw);
    void transfer(String from, String to, Double amount, String transfer);

    List<Transaction> getStatement(String account);

    List<Account> searchAccountsByCustomerName(String q);
}
