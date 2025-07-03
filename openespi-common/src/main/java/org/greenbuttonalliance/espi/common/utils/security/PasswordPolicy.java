/*
 *
 *    Copyright (c) 2018-2025 Green Button Alliance, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package org.greenbuttonalliance.espi.common.utils.security;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Password policy validation utility for OpenESPI.
 * Provides comprehensive password strength checking and security validation.
 */
@Component
public class PasswordPolicy {

    // Password policy constants
    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 100;
    public static final int MIN_UPPERCASE = 1;
    public static final int MIN_LOWERCASE = 1;
    public static final int MIN_DIGITS = 1;
    public static final int MIN_SPECIAL_CHARS = 1;

    // Common weak passwords to reject
    private static final String[] WEAK_PASSWORDS = {
        "password", "123456", "123456789", "12345678", "12345",
        "1234567", "password123", "admin", "administrator", "root",
        "user", "guest", "test", "demo", "default", "qwerty",
        "letmein", "welcome", "monkey", "dragon", "master",
        "abc123", "password1", "pass123", "admin123"
    };

    // Regex patterns for validation
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[@$!%*?&]");
    private static final Pattern SEQUENTIAL_PATTERN = Pattern.compile("(012|123|234|345|456|567|678|789|890|abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz)");
    private static final Pattern REPEATED_CHAR_PATTERN = Pattern.compile("(.)\\1{2,}");

    /**
     * Validates a password against the complete security policy.
     * 
     * @param password the password to validate
     * @return validation result with details
     */
    public PasswordValidationResult validatePassword(String password) {
        List<String> violations = new ArrayList<>();
        
        if (password == null) {
            violations.add("Password cannot be null");
            return new PasswordValidationResult(false, violations);
        }
        
        // Length validation
        if (password.length() < MIN_LENGTH) {
            violations.add("Password must be at least " + MIN_LENGTH + " characters long");
        }
        if (password.length() > MAX_LENGTH) {
            violations.add("Password cannot exceed " + MAX_LENGTH + " characters");
        }
        
        // Character type requirements
        if (countMatches(LOWERCASE_PATTERN, password) < MIN_LOWERCASE) {
            violations.add("Password must contain at least " + MIN_LOWERCASE + " lowercase letter(s)");
        }
        if (countMatches(UPPERCASE_PATTERN, password) < MIN_UPPERCASE) {
            violations.add("Password must contain at least " + MIN_UPPERCASE + " uppercase letter(s)");
        }
        if (countMatches(DIGIT_PATTERN, password) < MIN_DIGITS) {
            violations.add("Password must contain at least " + MIN_DIGITS + " digit(s)");
        }
        if (countMatches(SPECIAL_CHAR_PATTERN, password) < MIN_SPECIAL_CHARS) {
            violations.add("Password must contain at least " + MIN_SPECIAL_CHARS + " special character(s) (@$!%*?&)");
        }
        
        // Weak password check
        if (isWeakPassword(password)) {
            violations.add("Password is too common or weak, please choose a stronger password");
        }
        
        // Sequential characters check
        if (SEQUENTIAL_PATTERN.matcher(password.toLowerCase()).find()) {
            violations.add("Password cannot contain sequential characters (abc, 123, etc.)");
        }
        
        // Repeated characters check
        if (REPEATED_CHAR_PATTERN.matcher(password).find()) {
            violations.add("Password cannot contain more than 2 consecutive identical characters");
        }
        
        return new PasswordValidationResult(violations.isEmpty(), violations);
    }

    /**
     * Checks if a password is considered weak or common.
     * 
     * @param password the password to check
     * @return true if password is weak, false otherwise
     */
    public boolean isWeakPassword(String password) {
        if (password == null) {
            return true;
        }
        
        String lowerPassword = password.toLowerCase();
        for (String weakPassword : WEAK_PASSWORDS) {
            if (lowerPassword.equals(weakPassword) || lowerPassword.contains(weakPassword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates password strength score (0-100).
     * 
     * @param password the password to score
     * @return strength score from 0 (weakest) to 100 (strongest)
     */
    public int calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }
        
        int score = 0;
        
        // Length scoring (max 25 points)
        if (password.length() >= MIN_LENGTH) {
            score += Math.min(25, password.length() * 2);
        }
        
        // Character diversity (max 40 points)
        if (LOWERCASE_PATTERN.matcher(password).find()) score += 10;
        if (UPPERCASE_PATTERN.matcher(password).find()) score += 10;
        if (DIGIT_PATTERN.matcher(password).find()) score += 10;
        if (SPECIAL_CHAR_PATTERN.matcher(password).find()) score += 10;
        
        // Complexity bonus (max 20 points)
        int uniqueChars = (int) password.chars().distinct().count();
        score += Math.min(20, uniqueChars);
        
        // Penalty for weak patterns (max -15 points)
        if (isWeakPassword(password)) score -= 15;
        if (SEQUENTIAL_PATTERN.matcher(password.toLowerCase()).find()) score -= 10;
        if (REPEATED_CHAR_PATTERN.matcher(password).find()) score -= 5;
        
        return Math.max(0, Math.min(100, score));
    }

    /**
     * Gets a human-readable strength description.
     * 
     * @param score the password strength score
     * @return strength description
     */
    public String getStrengthDescription(int score) {
        if (score < 30) return "Very Weak";
        if (score < 50) return "Weak";
        if (score < 70) return "Fair";
        if (score < 85) return "Good";
        return "Strong";
    }

    /**
     * Counts pattern matches in a string.
     */
    private int countMatches(Pattern pattern, String text) {
        return (int) pattern.matcher(text).results().count();
    }

    /**
     * Password validation result container.
     */
    public static class PasswordValidationResult {
        private final boolean valid;
        private final List<String> violations;

        public PasswordValidationResult(boolean valid, List<String> violations) {
            this.valid = valid;
            this.violations = violations;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getViolations() {
            return violations;
        }

        public String getViolationsAsString() {
            return String.join("; ", violations);
        }
    }
}