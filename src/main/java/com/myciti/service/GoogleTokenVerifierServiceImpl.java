package com.myciti.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.myciti.dto.GoogleUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class GoogleTokenVerifierServiceImpl implements GoogleTokenVerifierService {

    private final GoogleIdTokenVerifier verifier;
    private final String clientId;

    public GoogleTokenVerifierServiceImpl(@Value("${app.auth.google-client-id}") String clientId) {
        this.clientId = clientId;
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
            .setAudience(Collections.singletonList(clientId))
            .setIssuers(List.of("accounts.google.com", "https://accounts.google.com"))
            .build();
    }

    @Override
    public GoogleUserInfo verifyToken(String idTokenString) {
        if (idTokenString == null || idTokenString.trim().isEmpty()) {
            throw new BadCredentialsException("ID token is required");
        }

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString.trim());
            if (idToken == null) {
                throw new BadCredentialsException("Invalid, expired, or wrong-audience Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            // Verify email_verified == true
            Boolean emailVerified = payload.getEmailVerified();
            if (emailVerified == null || !emailVerified) {
                throw new BadCredentialsException("Google email is not verified");
            }

            String sub = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            if (sub == null || email == null) {
                throw new BadCredentialsException("Google token missing sub or email");
            }

            return new GoogleUserInfo(sub, email, true, name, picture);
        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new BadCredentialsException("Failed to verify Google ID token: " + e.getMessage(), e);
        }
    }
}
