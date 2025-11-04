package g6.fashionFlex.repository;

import g6.fashionFlex.entity.PasswordResetToken;
import g6.fashionFlex.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUser(User user);

    void deleteByExpiryDateBefore(LocalDateTime now);

    void deleteByUser(User user);
}
