package g6.fashionFlex.util;

/**
 * Utility class for country validation and management
 * Currently configured to support Vietnam only
 */
public class CountryValidator {

    private static final String DEFAULT_COUNTRY = "Vietnam";
    private static final String[] SUPPORTED_COUNTRIES = {"Vietnam"};

    /**
     * Validates if the given country is supported
     * @param country the country to validate
     * @return true if supported, false otherwise
     */
    public static boolean isSupportedCountry(String country) {
        if (country == null || country.trim().isEmpty()) {
            return false;
        }

        for (String supportedCountry : SUPPORTED_COUNTRIES) {
            if (supportedCountry.equalsIgnoreCase(country.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the default country (Vietnam)
     * @return "Vietnam"
     */
    public static String getDefaultCountry() {
        return DEFAULT_COUNTRY;
    }

    /**
     * Validates and returns a valid country name
     * If input is invalid, returns default country (Vietnam)
     * @param country the country to validate and normalize
     * @return valid country name (always Vietnam for this configuration)
     */
    public static String getValidatedCountry(String country) {
        if (isSupportedCountry(country)) {
            return DEFAULT_COUNTRY;
        }
        return DEFAULT_COUNTRY;
    }

    /**
     * Checks if shipping is available for the given country
     * @param country the country to check
     * @return true if shipping is available (Vietnam only)
     */
    public static boolean isShippingAvailable(String country) {
        return isSupportedCountry(country);
    }
}