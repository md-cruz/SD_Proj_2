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
	private static final int PORT = 9090;
	
	
	public static void main(String[] args) throws Exception {

		
		URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(9090).build();
		final String url = "http://"+InetAddress.getLocalHost().getHostAddress() +":9090/";
		ResourceConfig config = new ResourceConfig();
		try{
		ServerResource.basePath = new File(args[0]);
		ServerResource.albumLogs = new HashMap<String,String>();
		ServerResource.picLogs = new HashMap<String, Map<String,String>>();
		if(!ServerResource.basePath.exists())
			ServerResource.basePath.mkdirs();
		}catch(Exception e){
			System.err.println("Please specify the server folder.\nClosing application...");
			return;
		}
		ClientConfig configClient = new ClientConfig();
	   	System.out.println(url);

		config.register(ServerResource.class);
		
		
		HttpServer server = JdkHttpServerFactory.createHttpServer(baseUri, config);

		System.err.println("REST Server ready... ");
		
		
		answerMulticast(url);
		
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
