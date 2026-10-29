package com.Ayurnexa.Ayurnexa.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Ayurnexa.Ayurnexa.model.User.User;

public interface UserRepository extends MongoRepository<User, String>
{

     java.util.Optional<User> findByEmail(String email);

     boolean existsByEmail(String email);

     boolean existsByUsername(String username);
    
}
