package com.teleapps.schedulemate.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FileOperation {

	private static final Logger logger = LogManager.getLogger(FileOperation.class);

	public List<String> findFilesWithKeyword(File directory, String type) {
		List<String> result = new ArrayList<>();
		File[] files = directory.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isFile() && file.getName().contains(type)) {
					result.add(file.getName());
				}
			}
		} else {
			logger.warn("Unable to find any {} files in {}", type, directory.getAbsolutePath());
		}
		return result;
	}
	
	public List<JsonObject> readFlatFilesAsJsonArray(String filePath) {
		List<JsonObject> jsonObject = new ArrayList<>();
		logger.info("Reading {}", filePath);
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
			StringBuilder content = new StringBuilder();
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				content.append(line.trim());
			}
			String[] object = content.toString().split("(?<=\\})(?=\\{)");
			for (String item : object) {
				jsonObject.add(JsonParser.parseString(item).getAsJsonObject());
			}
			return jsonObject;
		} catch (IOException ex) {
			logger.error("Exception while reading the file {} : {}", filePath, StackTrace.getMessage(ex));
			return Collections.emptyList();
		}
	}
}