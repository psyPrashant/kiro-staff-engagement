package com.psybergate.staff_engagement.auth;

import net.jqwik.api.*;
import net.jqwik.api.constraints.CharRange;
import net.jqwik.api.constraints.StringLength;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for backend authentication using jqwik.
 */
class AuthPropertyTests {

	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

	/**
	 * Property 2: BCrypt output format compliance
	 *
	 * For any non-null, non-empty plaintext password, hashing it with the Password_Encoder
	 * SHALL produce a string matching the BCrypt format ($2a$10$... or $2b$10$...) with exactly 60 characters.
	 *
	 * Validates: Requirements 2.1
	 */
	@Property(tries = 20)
	@Tag("Feature:backend-auth")
	@Tag("Property:2")
	void bcryptOutputFormatCompliance(
			@ForAll("printableAsciiStrings") String plaintext) {

		String hash = encoder.encode(plaintext);

		assertThat(hash).hasSize(60);
		assertThat(hash).matches("^\\$2[ab]\\$10\\$.{53}$");
	}

	@Provide
	Arbitrary<String> printableAsciiStrings() {
		return Arbitraries.strings()
				.withCharRange(' ', '~') // printable ASCII (32–126)
				.ofMinLength(1)
				.ofMaxLength(72); // BCrypt has a 72-byte input limit
	}

	/**
	 * Property 3: Password verification round-trip
	 *
	 * For any non-null, non-empty plaintext password, hashing it and then verifying
	 * the original plaintext against the resulting hash SHALL return true, and verifying
	 * any different non-empty plaintext against that hash SHALL return false.
	 *
	 * Validates: Requirements 2.2
	 */
	@Property(tries = 20)
	@Tag("Feature: backend-auth, Property 3: Password verification round-trip")
	void passwordVerificationRoundTrip(
			@ForAll @StringLength(min = 1, max = 71) @CharRange(from = '!', to = '~') String plaintext) {

		String hash = encoder.encode(plaintext);

		// Original plaintext must verify as true
		assertThat(encoder.matches(plaintext, hash)).isTrue();

		// Mutated plaintext (appended char) must verify as false
		String mutated = plaintext + "X";
		assertThat(encoder.matches(mutated, hash)).isFalse();
	}

	/**
	 * Property 4: BCrypt salt uniqueness
	 *
	 * For any non-null, non-empty plaintext password, hashing it twice SHALL produce
	 * two distinct hash strings (due to random salt generation).
	 *
	 * Validates: Requirements 2.3
	 */
	@Property(tries = 20)
	@Tag("Feature: backend-auth, Property 4: BCrypt salt uniqueness")
	void bcryptSaltUniqueness(
			@ForAll @StringLength(min = 1, max = 72) @CharRange(from = '!', to = '~') String plaintext) {

		String hash1 = encoder.encode(plaintext);
		String hash2 = encoder.encode(plaintext);

		assertThat(hash1).isNotEqualTo(hash2);
	}
}
