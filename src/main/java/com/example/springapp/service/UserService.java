package com.example.springapp.service;

import com.example.springapp.model.User;
import com.example.springapp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> login(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getPassword() != null && user.getPassword().equals(password));
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> updateUser(Long id, User updatedUser) {
        return userRepository.findById(id).map(existing -> {
            existing.setName(updatedUser.getName());
            existing.setEmail(updatedUser.getEmail());
            return userRepository.save(existing);
        });
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
