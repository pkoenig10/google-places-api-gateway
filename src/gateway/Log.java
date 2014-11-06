package gateway;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A simple utility for logging.
 */
public class Log {

	private final PrintStream out;
	private final PrintStream err;

	public Log(PrintStream out, PrintStream err) {
		this.out = out;
		this.err = err;
	}

	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter
			.ofPattern("yyyy-MM-dd hh:mm:ss");

	/**
	 * Prints an error log message to the console.
	 *
	 * @param message
	 *            the message
	 */
	public void e(String message) {
		if (err != null) {
			err.format("%s: %s%n", getTimestamp(), message);
			err.flush();
		}
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
	public void e(String message, Throwable throwable) {
		if (err != null) {
			err.format("%s: %s%n", getTimestamp(), message);
			err.flush();
			throwable.printStackTrace(err);
			;
		}
	}

	/**
	 * Prints an informational log message to the console.
	 *
	 * @param message
	 *            the message
	 */
	public void i(String message) {
		if (out != null) {
			out.format("%s: %s%n", getTimestamp(), message);
			out.flush();
		}
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
