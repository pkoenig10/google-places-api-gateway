package gateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server extends Thread {

	private static final int POOL_SIZE = 10;

	private final int port;
	private final ExecutorService executor;

	public Server(int port) {
		this.port = port;
		executor = Executors.newFixedThreadPool(POOL_SIZE);
	}

	@Override
	public void run() {
		// Create the server socket
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		// Accept incoming connections, handle them on a background thread,
		// and immediately begin listening for other incoming client
		// connections.
		while (true) {
			try {
				Socket clientSocket = serverSocket.accept();
				executor.execute(new ClientHandler(clientSocket, System
						.currentTimeMillis()));
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}

		try {
			serverSocket.close();
		} catch (IOException e) {
			// Ignore because we're about to exit anyway.
		} finally {
			executor.shutdown();
		}
	}

	private static class ClientHandler implements Runnable {

		private final Socket socket;
		private final long timestamp;

		public ClientHandler(Socket socket, long timestamp) {
			this.socket = socket;
			this.timestamp = timestamp;
		}

		@Override
		public void run() {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				String request = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					// Ignore because we're about to exit anyway.
				}
			}
		}
	}

	public static void main(String[] args) {
		Server server = new Server(12345);
		server.start();
	}
}
