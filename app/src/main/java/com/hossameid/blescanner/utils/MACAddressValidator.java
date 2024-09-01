package com.hossameid.blescanner.utils;

import java.util.regex.Pattern;

public class MACAddressValidator {
    // Regular expression for MAC address
    private static final String MAC_ADDRESS_REGEX =
            "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$|^([0-9A-Fa-f]{4}\\.){2}([0-9A-Fa-f]{4})$";

    // Compile the regex into a pattern
    private static final Pattern MAC_ADDRESS_PATTERN = Pattern.compile(MAC_ADDRESS_REGEX);

    public static boolean isValidMACAddress(String macAddress) {
        // Validate the input string against the pattern
        return MAC_ADDRESS_PATTERN.matcher(macAddress).matches();
    }
}
