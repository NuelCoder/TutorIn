package com.TutorIn.security;

import com.TutorIn.repository.AdminRepository;
import com.TutorIn.repository.TutorRepository;
import com.TutorIn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        var user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            return user.get();
        }

        var tutor = tutorRepository.findByEmail(email);
        if (tutor.isPresent()) {
            return tutor.get();
        }

         var admin = adminRepository.findByAdminEmail(email);
         if (admin.isPresent()) return admin.get();

        throw new UsernameNotFoundException("User not found with email: " + email);
    }
}
