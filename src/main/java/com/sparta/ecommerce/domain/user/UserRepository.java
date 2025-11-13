package com.sparta.ecommerce.domain.user;

import com.sparta.ecommerce.domain.user.entity.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long userId);

    User save(User user);
}
