package com.sparta.ecommerce.infrastructure.memory.user;

import com.sparta.ecommerce.domain.user.UserRepository;
import com.sparta.ecommerce.domain.user.entity.User;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

//@Primary
@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> table = new ConcurrentHashMap<>();
    private final AtomicLong cursor = new AtomicLong(1);

    @Override
    public Optional<User> findById(Long userId) {
        return Optional.ofNullable(table.get(userId));
    }

    @Override
    public User save(User user) {
        if (user.getUserId() == null) {
            // ID가 없으면 자동 생성
            user = new User(cursor.getAndIncrement(), user.getName(), user.getPoint(), user.getCreatedAt());
        }
        table.put(user.getUserId(), user);

        return user;
    }
}
