package com.TutorIn.service.LOGIN;

import com.TutorIn.dto.AuthTokens;
import com.TutorIn.dto.LoginRequest;
import com.TutorIn.entity.Tutor;
import com.TutorIn.exceptions.AccountPendingRecoveryException;
import com.TutorIn.exceptions.AccountPermanentlyDeletedException;
import com.TutorIn.exceptions.BadCredentialsException;
import com.TutorIn.repository.TutorRepository;
import com.TutorIn.security.JwtUtil;
import com.TutorIn.service.REFRESHTOKEN.RefreshTokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class TutorLoginService {
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final TutorRepository tutorRepository;
    private final LoginAttemptService loginAttemptService;

    @Transactional
    public AuthTokens loginTutor(LoginRequest request){
        String email = request.getUsername();

        loginAttemptService.checkLoginAllowed(email);

        Tutor tutor = tutorRepository.findByEmail(email).orElse(null);

        if (tutor == null) {
            loginAttemptService.registerFailedLogin(email);
            throw new BadCredentialsException("Invalid email or password.");
        }

        recoverTutorAccount(tutor);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (Exception e) {
            loginAttemptService.registerFailedLogin(email);
            throw new BadCredentialsException("Invalid email or password.");
        }

        loginAttemptService.registerSuccessfulLogin(email);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername());

        return new AuthTokens(accessToken, refreshToken);
    }

    private void recoverTutorAccount(Tutor tutor){
        if (tutor.isSuspended()){
            throw new DisabledException("Your account has been suspended by an Administrator. Please contact support.");
        }
        if (tutor.isDeleted()) {
            OffsetDateTime thirtyDaysAgo = OffsetDateTime.now().minusDays(30);

            if (tutor.getUpdatedAt().isBefore(thirtyDaysAgo)) {
                throw new AccountPermanentlyDeletedException("This account has been permanently deleted and cannot be recovered.");
            } else {
                throw new AccountPendingRecoveryException("Account is marked for deletion. Would you like to recover it?");
            }
        }
    }
}