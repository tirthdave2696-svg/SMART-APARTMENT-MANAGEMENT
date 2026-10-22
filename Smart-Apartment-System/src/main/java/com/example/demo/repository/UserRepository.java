package com.example.demo.repository;

import com.example.demo.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmployeeId(String employeeId);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByEmployeeId(String employeeId);
    boolean existsByRole(Role role);
    List<User> findByRoleAndUnitNumberAndStatus(Role role, String unitNumber, AccountStatus status);
}