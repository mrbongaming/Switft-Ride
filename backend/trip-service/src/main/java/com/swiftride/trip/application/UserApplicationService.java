package com.swiftride.trip.application;

import com.swiftride.trip.domain.UserAccount;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserApplicationService {
    public interface Accounts { List<UserAccount> byRole(String role); }
    private final Accounts accounts;
    public UserApplicationService(Accounts accounts) { this.accounts = accounts; }
    public List<UserAccount> list(String role) {
        if (!role.equals("rider") && !role.equals("driver")) throw new IllegalArgumentException("Invalid role");
        return accounts.byRole(role);
    }
}
