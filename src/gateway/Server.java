package gateway;
import java.io.IOException;
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
			
		}
	}

	public static void main(String[] args) {
		Server server = new Server(12345);
		server.start();
	}
}
