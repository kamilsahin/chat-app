package com.chatapp.domain.repository;

import com.chatapp.domain.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);
}
