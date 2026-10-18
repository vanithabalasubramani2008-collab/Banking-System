package com.securebank.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.dto.*;
import com.securebank.entity.AccountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SecureBank Complete REST API Integration Tests")
class BankingSystemIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("End-to-End Banking Workflow: Register -> Open Accounts -> Deposit -> Withdraw -> Transfer -> Audit")
    void fullBankingLifecycleTest() throws Exception {
        // 1. Register User 1 (Bob)
        RegisterRequest bobRegister = new RegisterRequest(
                "bob_banker",
                "bob@example.com",
                "Password@123",
                "1234",
                "Bob Builder",
                "9876543211"
        );

        MvcResult bobRegResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bobRegister)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username", is("bob_banker")))
                .andReturn();

        AuthResponse bobAuth = objectMapper.readValue(bobRegResult.getResponse().getContentAsString(), AuthResponse.class);
        String bobToken = "Bearer " + bobAuth.getToken();

        // 2. Register User 2 (Charlie)
        RegisterRequest charlieRegister = new RegisterRequest(
                "charlie_banker",
                "charlie@example.com",
                "Password@123",
                "5678",
                "Charlie Brown",
                "9876543212"
        );

        MvcResult charlieRegResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(charlieRegister)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse charlieAuth = objectMapper.readValue(charlieRegResult.getResponse().getContentAsString(), AuthResponse.class);
        String charlieToken = "Bearer " + charlieAuth.getToken();

        // 3. Bob creates a Savings Account with ₹3,000 initial deposit
        CreateAccountRequest bobSavingsReq = new CreateAccountRequest(
                AccountType.SAVINGS,
                new BigDecimal("3000.00"),
                "1234"
        );

        MvcResult bobSavingsResult = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bobSavingsReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountType", is("SAVINGS")))
                .andExpect(jsonPath("$.balance", is(3000.00)))
                .andReturn();

        AccountResponse bobSavings = objectMapper.readValue(bobSavingsResult.getResponse().getContentAsString(), AccountResponse.class);

        // 4. Charlie creates a Current Account with ₹1,000 initial deposit
        CreateAccountRequest charlieCurrentReq = new CreateAccountRequest(
                AccountType.CURRENT,
                new BigDecimal("1000.00"),
                "5678"
        );

        MvcResult charlieCurrentResult = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", charlieToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(charlieCurrentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountType", is("CURRENT")))
                .andExpect(jsonPath("$.overdraftLimit", is(5000.00)))
                .andReturn();

        AccountResponse charlieCurrent = objectMapper.readValue(charlieCurrentResult.getResponse().getContentAsString(), AccountResponse.class);

        // 5. Bob deposits ₹2,000 into his Savings Account
        DepositRequest depositReq = new DepositRequest(new BigDecimal("2000.00"), "Bonus deposit");
        mockMvc.perform(post("/api/accounts/" + bobSavings.getId() + "/deposits")
                        .header("Authorization", bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceAfter", is(5000.00)));

        // 6. Bob withdraws ₹1,500 using his PIN
        WithdrawalRequest withdrawReq = new WithdrawalRequest(new BigDecimal("1500.00"), "1234", "Grocery withdrawal");
        mockMvc.perform(post("/api/accounts/" + bobSavings.getId() + "/withdrawals")
                        .header("Authorization", bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceAfter", is(3500.00)));

        // 7. Bob attempts to withdraw ₹3,000 (Violating ₹1,000 min balance -> 3500 - 3000 = 500 < 1000)
        WithdrawalRequest excessiveWithdrawReq = new WithdrawalRequest(new BigDecimal("3000.00"), "1234", "Too large withdrawal");
        mockMvc.perform(post("/api/accounts/" + bobSavings.getId() + "/withdrawals")
                        .header("Authorization", bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessiveWithdrawReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Insufficient Funds")))
                .andExpect(jsonPath("$.details.shortfall", is(500.00)));

        // 8. Bob transfers ₹1,000 to Charlie's Current Account
        TransferRequest transferReq = new TransferRequest(
                bobSavings.getAccountNumber(),
                charlieCurrent.getAccountNumber(),
                new BigDecimal("1000.00"),
                "1234",
                "Dinner split payment"
        );

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceAfter", is(2500.00)))
                .andExpect(jsonPath("$.transferReference").isNotEmpty());

        // 9. Bob checks his account statement and recent 5 transactions
        mockMvc.perform(get("/api/accounts/" + bobSavings.getId() + "/transactions")
                        .header("Authorization", bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(5))));

        // 10. Security Ownership check: Charlie cannot access Bob's account
        mockMvc.perform(get("/api/accounts/" + bobSavings.getId())
                        .header("Authorization", charlieToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Account locks after 3 failed login attempts")
    void accountLocksAfterThreeFailedAttempts() throws Exception {
        String testUser = "lock_test_user";
        RegisterRequest registerReq = new RegisterRequest(
                testUser,
                "locktest@example.com",
                "CorrectPassword@123",
                "4321",
                "Lock Test",
                "9876543299"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest badLogin = new LoginRequest(testUser, "WrongPassword");

        // Attempt 1: 401 Unauthorized
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Remaining attempts before lock: 2")));

        // Attempt 2: 401 Unauthorized
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Remaining attempts before lock: 1")));

        // Attempt 3: 423 Locked
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.error", is("Account Locked")));

        // Attempt 4: Even with correct password, now permanently locked until unlocked
        LoginRequest goodLogin = new LoginRequest(testUser, "CorrectPassword@123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goodLogin)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.message", containsString("locked due to 3 consecutive failed login attempts")));
    }
}
