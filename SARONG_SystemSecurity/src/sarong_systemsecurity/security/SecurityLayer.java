package sarong_systemsecurity.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SecurityLayer {

    // Hashes a plain text password using SHA-256
    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Checks if the input password matches the stored hashed password
    public boolean checkPassword(String input, String hashedPasswordFromDB) {
        String hashedInput = hashPassword(input);
        return hashedInput.equals(hashedPasswordFromDB);
    }

    // Simple 2FA check
    public boolean check2FA(String input, String correct) {
        return input.equals(correct);
    }

    // Intrusion detection: returns true if attempts >= 3
    public boolean intrusionDetected(int attempts) {
        return attempts >= 3; 
    }
}
