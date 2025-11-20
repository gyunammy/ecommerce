package com.sparta.ecommerce.application.user;

import com.sparta.ecommerce.domain.user.UserRepository;
import com.sparta.ecommerce.domain.user.entity.User;
import com.sparta.ecommerce.domain.user.exception.UserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("존재하는_사용자_조회")
    void getUserById_success() {
        // given
        User user = new User(1L, "test", 0, 0L, LocalDateTime.now());
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        User result = userService.getUserById(1L);

        // then
        assertThat(result).isEqualTo(user);
    }

    @Test
    @DisplayName("사용자_없으면_예외발생")
    void getUserById_fail() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThrows(UserException.class, () -> userService.getUserById(1L));
    }

    @Test
    @DisplayName("사용자 정보 업데이트")
    void updateUser() {
        // given
        User user = new User(1L, "test", 0, 0L, LocalDateTime.now());

        // when
        userService.updateUser(user);

        // then
        verify(userRepository).save(user);
    }
}