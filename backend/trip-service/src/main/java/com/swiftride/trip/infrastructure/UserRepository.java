package com.swiftride.trip.infrastructure;

import com.swiftride.trip.application.UserApplicationService;
import com.swiftride.trip.domain.UserAccount;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Entity @Table(name = "demo_users")
class UserRow {
    @Id public String id;
    public String displayName, role, phone, vehicle, licensePlate;
    protected UserRow() { }
    UserRow(UserAccount a) {
        id = a.id(); displayName = a.displayName(); role = a.role(); phone = a.phone(); vehicle = a.vehicle(); licensePlate = a.licensePlate();
    }
    UserAccount toDomain() { return new UserAccount(id, displayName, role, phone, vehicle, licensePlate); }
}
interface UserJpaRepository extends JpaRepository<UserRow, String> { List<UserRow> findByRole(String role); }
@Repository
public class UserRepository implements UserApplicationService.Accounts {
    private final UserJpaRepository rows;
    public UserRepository(UserJpaRepository rows) { this.rows = rows; }
    public List<UserAccount> byRole(String role) { return rows.findByRole(role).stream().map(UserRow::toDomain).toList(); }
    @Bean CommandLineRunner demoAccounts(UserJpaRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new UserRow(new UserAccount("rider-1", "Nguyễn Minh Anh", "rider", "0900000001", null, null)));
                repository.save(new UserRow(new UserAccount("driver-1", "Trần Quốc Bảo", "driver", "0900000002", "Toyota Vios", "51A-12345")));
                repository.save(new UserRow(new UserAccount("driver-2", "Lê Thanh Tùng", "driver", "0900000003", "Honda City", "51A-67890")));
            }
        };
    }
}
