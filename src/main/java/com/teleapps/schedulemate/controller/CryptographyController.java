package com.teleapps.schedulemate.controller;

import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.teleapps.schedulemate.util.Cryptography;

@Controller
public class CryptographyController {
	
	private static final Logger logger = LogManager.getLogger(CryptographyController.class);
	

	@GetMapping(value = "/crypto", produces = " text/plain; charset=UTF-8")
	public ResponseEntity<String> getLastTenStatus(@RequestParam String plainText) {
		new Cryptography().getCipher(plainText);
		if (plainText != null && plainText.length() > 0 && plainText.length() <= 100) {
			String maskedText = plainText.chars()
	                .mapToObj(i -> {
	                    if (i % 2 == 0) {
	                        return '*';
	                    } else {
	                        return (char) i;
	                    }
	                })
	                .map(Object::toString)
	                .collect(Collectors.joining());
			logger.info("Masked plain text: {}", maskedText);
			return ResponseEntity.status(HttpStatus.OK).body(new Cryptography().getCipher(plainText));
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		}
	}
}