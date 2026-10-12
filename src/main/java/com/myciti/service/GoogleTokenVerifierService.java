package com.myciti.service;

import com.myciti.dto.GoogleUserInfo;

public interface GoogleTokenVerifierService {
    /**
     * Verifies the Google ID token.
     * Validates signature, issuer, audience, expiration, and ensures email_verified == true.
     * Returns GoogleUserInfo or throws AuthenticationException / IllegalArgumentException.
     */
    GoogleUserInfo verifyToken(String idToken);
}
