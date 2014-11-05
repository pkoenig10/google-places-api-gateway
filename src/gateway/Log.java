package gateway;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A simple utility for logging.
 */
public abstract class Log {

	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter
			.ofPattern("yyyy-MM-dd hh:mm:ss");

	/**
	 * Prints an error log message to the console.
	 *
	 * @param message
	 *            the message
	 */
	public static void e(String message) {
		System.err.format("%s:: %s%n", getTimestamp(), message);
		System.err.flush();
	}

	/**
	 * Prints an error log message to the console, logging the {@link Throwable}
	 * that caused it as well.
	 *
	 * @param message
	 *            the message
	 *
	 * @param throwable
	 *            the {@link Throwable}
	 */
	public static void e(String message, Throwable throwable) {
		System.err.format("%s: %s%n", getTimestamp(), message);
		System.err.flush();
		throwable.printStackTrace();
	}

	/**
	 * Prints an informational log message to the console.
	 *
	 * @param message
	 *            the message
	 */
	public static void i(String message) {
		System.out.format("%s: %s%n", getTimestamp(), message);
		System.out.flush();
	}

	/**
	 * Returns a String representing the current date and time.
	 *
	 * @return A String representing the current date and time
	 */
	private static String getTimestamp() {
		return LocalDateTime.now().format(dateTimeFormatter);
	}
}
