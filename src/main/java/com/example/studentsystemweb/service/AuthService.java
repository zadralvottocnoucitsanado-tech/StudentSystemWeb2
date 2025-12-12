package com.example.studentsystemweb.service;

import com.example.studentsystemweb.model.User;
import com.example.studentsystemweb.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User login(String login, String password) {
        User user = userRepository.findByLogin(login);
        if (user == null) return null;
        if (!user.getPassword().equals(password)) return null;
        return user;
    }
}
