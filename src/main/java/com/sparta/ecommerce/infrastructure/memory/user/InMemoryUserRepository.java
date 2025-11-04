package com.sparta.ecommerce.infrastructure.memory.user;

import com.sparta.ecommerce.domain.user.UserRepository;
import com.sparta.ecommerce.domain.user.entity.User;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> table = new LinkedHashMap<>();
    private long cursor = 1;

    @PostConstruct
    public void init() {
        LocalDateTime now = LocalDateTime.now();
        table.put(cursor, new User(cursor++, "홍길동", 10000, now));
        table.put(cursor, new User(cursor++, "김철수", 5000, now));
        table.put(cursor, new User(cursor++, "이영희", 20000, now));
    }

    @Override
    public Optional<User> findById(Long userId) {
        return Optional.ofNullable(table.get(userId));
    }

    @Override
    public void update(User user) {
        table.put(user.getUserId(), user);
    }
}
