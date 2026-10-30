package ru.ttravel.orchestrator.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ttravel.orchestrator.domain.data.User;

import java.util.Optional;

/**
 * Доступ к таблице {@code users}: пользователи.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Ищет пользователя по электронной почте.
     *
     * @param email адрес электронной почты
     * @return пользователь или пустой результат
     */
    Optional<User> findByEmail(String email);
}