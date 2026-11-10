package com.Ayurnexa.Ayurnexa.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Ayurnexa.Ayurnexa.model.User.User;

public interface UserRepository extends MongoRepository<User, String>
{

     Optional<User> findByEmail(String email);

     Optional<User> findByUserId(String userId);

     boolean existsByEmail(String email);

     boolean existsByUsername(String username);
    
}
