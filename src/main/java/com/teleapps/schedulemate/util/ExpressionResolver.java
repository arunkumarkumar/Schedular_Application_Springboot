package com.teleapps.schedulemate.util;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExpressionResolver {

	private static final Logger logger = LogManager.getLogger(ExpressionResolver.class);

	public String resolve(String expression) {
		try {
			Random random = SecureRandom.getInstanceStrong();
			String pattern = "\\{\\{(.*?)\\}\\}";
			Pattern regexPattern = Pattern.compile(pattern);
			Matcher matcher = regexPattern.matcher(expression);
			if (matcher.find()) {
				String[] parameter = matcher.group(1).split("\\:");
				if(parameter.length < 1) {
					logger.warn("Invalid expression {}", expression);
					return expression;
				}
				switch (parameter[0]) {
				case "uuid":
					return UUID.randomUUID().toString();
				case "date":
					String[] dateParameters = parameter[1].split("#");
					Calendar calender = Calendar.getInstance();
					if("yesterday".equalsIgnoreCase(dateParameters[0])) {
						calender.add(Calendar.DATE, -1);
						return new SimpleDateFormat(dateParameters[1]).format(calender.getTime());
					} else if("today".equalsIgnoreCase(dateParameters[0])) {
						calender.add(Calendar.DATE, 0);
						return new SimpleDateFormat(dateParameters[1]).format(calender.getTime());
					} else if("tomorrow".equalsIgnoreCase(dateParameters[0])) {
						calender.add(Calendar.DATE, +1);
						return new SimpleDateFormat(dateParameters[1]).format(calender.getTime());
					} else {
						logger.warn("Invalid expression {}", expression);
						return expression;
					}
				case "randomint":
					int size = Integer.parseInt(parameter[1]);
					if(size > 50) {
						logger.warn("Randomint length exceeds the allowed limit {}. Trimming to allowed size", size);
						size = 50;
					}
					return IntStream.range(0, size).mapToObj(iteration -> Integer.toString(random.nextInt(10))).collect(Collectors.joining());
				case "randomstring":
					size = Integer.parseInt(parameter[1]);
					if(size > 50) {
						logger.warn("Random string length exceeds the allowed limit {}. Trimming to allowed size", size);
						size = 50;
					}
					return random.ints(size, 'a', 'z' + 1).mapToObj(c -> String.valueOf((char) c)).collect(Collectors.joining());
				case "alphanumeric":
					size = Integer.parseInt(parameter[1]);
					if(size > 50) {
						logger.warn("Random alphanumeric length exceeds the allowed limit {}. Trimming to allowed size", size);
						size = 50;
					}
					return random.ints(size)
			                .mapToObj(i -> {
			                    if (i % 3 == 0) {
			                        return (char) (random.nextInt(10) + '0');
			                    } else if (i % 3 == 1) {
			                        return (char) (random.nextInt(26) + 'a');
			                    } else {
			                        return (char) (random.nextInt(26) + 'A');
			                    }
			                })
			                .map(Object::toString)
			                .collect(Collectors.joining());
				default :
					return expression;
				} 
			} else {
				return expression;
			}
		} catch (Exception ex) {
			logger.error("Exception while resolving the expression {} : {}", expression, StackTrace.getMessage(ex));
			return expression;
		}
	}
}
