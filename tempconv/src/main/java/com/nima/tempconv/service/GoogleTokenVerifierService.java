package com.nima.tempconv.service;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.nima.tempconv.exception.InvalidGoogleTokenException;

@Service
public class GoogleTokenVerifierService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierService(@Value("${app.google.client-id}") String googleClientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    public GoogleUserProfile verify(String idTokenString) {
        if (!StringUtils.hasText(idTokenString)) {
            throw new InvalidGoogleTokenException("Google ID token is required");
        }

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new InvalidGoogleTokenException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            return new GoogleUserProfile(
                    payload.getSubject(),
                    payload.getEmail(),
                    (String) payload.get("name"),
                    (String) payload.get("picture"));
        } catch (InvalidGoogleTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidGoogleTokenException("Failed to verify Google ID token: " + ex.getMessage());
        }
    }

    public record GoogleUserProfile(String googleId, String email, String name, String picture) {
    }
}
