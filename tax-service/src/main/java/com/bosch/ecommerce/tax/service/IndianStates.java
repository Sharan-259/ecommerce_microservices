package com.bosch.ecommerce.tax.service;

import java.util.Set;

/**
 * Reference list of 2-letter Indian state / union territory codes accepted as seller/buyer state.
 * This is validation reference data, not a tax rate.
 */
public final class IndianStates {

    private static final Set<String> CODES = Set.of(
            "AN", "AP", "AR", "AS", "BR", "CH", "CG", "DH", "DL", "GA", "GJ", "HR", "HP", "JK", "JH",
            "KA", "KL", "LA", "LD", "MP", "MH", "MN", "ML", "MZ", "NL", "OD", "PB", "PY", "RJ", "SK",
            "TN", "TS", "TR", "UP", "UK", "WB");

    private IndianStates() {
    }

    public static boolean isValid(String code) {
        return code != null && CODES.contains(code);
    }
}
