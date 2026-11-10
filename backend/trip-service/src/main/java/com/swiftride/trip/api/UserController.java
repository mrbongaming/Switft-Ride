package com.swiftride.trip.api;

import com.swiftride.trip.application.UserApplicationService;
import com.swiftride.trip.domain.UserAccount;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/users")
public class UserController {
    private final UserApplicationService users;
    public UserController(UserApplicationService users) { this.users = users; }
    @GetMapping public List<UserAccount> list(@RequestParam String role) { return users.list(role); }
}
