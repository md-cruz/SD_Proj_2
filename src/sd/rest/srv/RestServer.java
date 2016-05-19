package sd.rest.srv;

import java.io.File;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.sun.net.httpserver.HttpServer;


public class RestServer {
	private static final String WSERVICE = "GiveMeYourIps";
	private static final String WSERVICEP = "ImAProxy";
	private static final String MULTICASTIP = "229.0.0.1";
	private static final int PORT = 9090;
	
	
	public static void main(String[] args) throws Exception {

		
		URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(9090).build();
		final String url = "http://"+InetAddress.getLocalHost().getHostAddress() +":9090/";
		ResourceConfig config = new ResourceConfig();
		try{
		ServerResource.basePath = new File(args[0]);
		if(!ServerResource.basePath.exists())
			ServerResource.basePath.mkdirs();
		}catch(Exception e){
			System.err.println("Please specify the server folder.\nClosing application...");
			return;
		}
		ClientConfig configClient = new ClientConfig();
	   	ServerResource.client = ClientBuilder.newClient(configClient);
		
		getServers(ServerResource.proxyServers );

		config.register(ServerResource.class);
		
		
		HttpServer server = JdkHttpServerFactory.createHttpServer(baseUri, config);

		System.err.println("REST Server ready... ");
		
		
		answerMulticast(url);
		
	}

	private static void getServers(List<String> servers) {
		new Thread(() -> {
			try {
				

				final int port = PORT;
				final String addr = MULTICASTIP;
				final InetAddress address = InetAddress.getByName(addr);

				MulticastSocket socket = new MulticastSocket();

				byte[] input = (WSERVICEP).getBytes();
				DatagramPacket packet = new DatagramPacket(input, input.length);
				packet.setAddress(address);
				packet.setPort(port);
				Map<String,Integer> consecutiveReplies = new HashMap<String,Integer>();
				int numberOfQueries = 0;

			
			    
				while (true) {
					System.out.println("Sent packet");
					socket.send(packet);
					// System.out.println(new String(packet.getData()));
					numberOfQueries++;
					byte[] received = new byte[65536];
					DatagramPacket receivedPacket = new DatagramPacket(received, received.length);
					boolean foundAllServers = false;
					try {
						while (!foundAllServers) {
							
							socket.setSoTimeout(60000);

							socket.receive(receivedPacket);

							String serverHost = new String(receivedPacket.getData()).trim();
							consecutiveReplies.put(serverHost,
									consecutiveReplies.getOrDefault(serverHost, 1)+1); 
									//getOrDefault returns the current value for the key
									// or 1 if the key has no value yet
							System.out.println(serverHost);
							if (!servers.contains(serverHost))
								servers.add(serverHost);
						}
					} catch (SocketTimeoutException e) {
						foundAllServers = true;
					}
					
					// delete servers if they havent replied in the last 3 times
					for(String server : consecutiveReplies.keySet()){
						if (consecutiveReplies.get(server) +3 < numberOfQueries){
							// remove server
							servers.remove(server);
						}
					}
					
					Thread.sleep(60000); // esperar um minuto e executar novo
											// multicast
				}

			} catch (Exception e) {
				e.printStackTrace();

			}
		}).start();
	}
	
	private static void answerMulticast(String localhost) {

		
			try {
				final String addr = "228.0.0.1";
				System.out.println("new thread launched");

				final InetAddress address = InetAddress.getByName(addr);
				MulticastSocket socket = new MulticastSocket(9000);
				socket.joinGroup(address);
				while (true) {
					byte[] buffer = new byte[65536];

					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet);
					String s = new String(packet.getData()).trim();

					if (s.equalsIgnoreCase(WSERVICE)) {

						byte[] data = ("R" + localhost).getBytes();
						DatagramPacket sendingPacket = new DatagramPacket(data, data.length);
						sendingPacket.setAddress(packet.getAddress());
						sendingPacket.setPort(packet.getPort());
						socket.send(sendingPacket);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();

			}
		
	}
}
