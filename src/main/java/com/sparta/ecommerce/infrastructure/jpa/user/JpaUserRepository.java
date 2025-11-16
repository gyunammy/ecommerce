package com.sparta.ecommerce.infrastructure.jpa.user;

import com.sparta.ecommerce.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserRepository extends JpaRepository<User, Long> {
}
