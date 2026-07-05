package org.example.datn_sd69.modules.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();

        User user = userRepository.findWithRoleByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy user với email: " + normalizedEmail
                ));

        boolean activeStatus = user.getStatus() != null && user.getStatus() == 1;
        boolean notDeleted = !Boolean.TRUE.equals(user.getIsDeleted());

        boolean enabled = activeStatus && notDeleted;

        String authorityName = user.getRole().getName().trim().toUpperCase();

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                enabled,
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority(authorityName))
        );
    }
}