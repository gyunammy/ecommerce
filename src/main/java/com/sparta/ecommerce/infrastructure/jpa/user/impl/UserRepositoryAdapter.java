package com.sparta.ecommerce.infrastructure.jpa.user.impl;

import com.sparta.ecommerce.domain.user.UserRepository;
import com.sparta.ecommerce.domain.user.entity.User;
import com.sparta.ecommerce.infrastructure.jpa.user.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {
    private final JpaUserRepository jpaUserRepository;

    @Override
    public Optional<User> findById(Long userId) {
        return jpaUserRepository.findById(userId);
    }

    @Override
    public User save(User user) {
        return jpaUserRepository.save(user);
    }
}
