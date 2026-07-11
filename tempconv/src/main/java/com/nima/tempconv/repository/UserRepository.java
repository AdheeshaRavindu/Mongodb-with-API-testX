package com.nima.tempconv.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.nima.tempconv.model.User;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByGoogleId(String googleId);
}
