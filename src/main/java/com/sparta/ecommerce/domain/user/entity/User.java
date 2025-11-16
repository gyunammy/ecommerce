package com.sparta.ecommerce.domain.user.entity;

import com.sparta.ecommerce.domain.user.exception.UserException;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import static com.sparta.ecommerce.domain.user.exception.UserErrorCode.INSUFFICIENT_POINT;

@Entity
@Table(name = "`user`")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long          userId;    // 사용자_ID
    private String        name;      // 사용자명
    private Integer       point;     // 포인트
    private LocalDateTime createdAt; // 생성일시

    public void validateSufficientPoint(int amount) {
        if (this.point < amount) {
            throw new UserException(INSUFFICIENT_POINT);
        }
    }

    public void deductPoint(int amount) {
        validateSufficientPoint(amount);
        this.point -= amount;
    }

    /**
     * 포인트 복구 (주문 실패 시 롤백용)
     *
     * @param amount 복구할 포인트
     */
    public void restorePoint(int amount) {
        this.point += amount;
    }
}
