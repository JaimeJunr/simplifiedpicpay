package com.simplifiedpicpay.services;

import com.simplifiedpicpay.domain.transaction.Transaction;
import com.simplifiedpicpay.domain.user.User;
import com.simplifiedpicpay.dtos.TransactionDTO;
import com.simplifiedpicpay.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Service
public class TransactionService {

    @Autowired
    private UserService userService;
    @Autowired
    private TransactionRepository repository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RestTemplate restTemplate;

    public Transaction createTransaction(TransactionDTO transaction) throws Exception {
        User sender = this.userService.findUserById(transaction.senderID());
        User receiver = this.userService.findUserById(transaction.receiverID());

        userService.validateTransaction(sender, transaction.value());
        boolean isAuthorized = this.authorizeTransaction(sender, transaction.value());
        if (!isAuthorized) {
            throw new Exception("Transaction not authorized");
        }

        Transaction newTransaction = new Transaction();
        newTransaction.setAmount(transaction.value());
        newTransaction.setSender(sender);
        newTransaction.setReceiver(receiver);
        newTransaction.setTimestamp(LocalDateTime.now());

        sender.setBalance(sender.getBalance().subtract(transaction.value()));
        receiver.setBalance(receiver.getBalance().add(transaction.value()));

        this.repository.save(newTransaction);
        this.userService.saveUser(sender);
        this.userService.saveUser(receiver);

        this.notificationService.sendNotification(sender, "Transaction completed successfully");

        this.notificationService.sendNotification(sender, "Transaction received successfully");

        return newTransaction;
    }

    public boolean authorizeTransaction(User sender, BigDecimal value) {
        ResponseEntity<Map> authorizationResponse = restTemplate.getForEntity("https://util.devi.tools/api/v2/authorize", Map.class);
        if (authorizationResponse.getStatusCode() == HttpStatus.OK) {
            String message = (String) Objects.requireNonNull(authorizationResponse.getBody()).get("status");
            return "success".equalsIgnoreCase(message);

        } else return false;
    }
}
