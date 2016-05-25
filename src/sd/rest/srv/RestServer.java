package sd.rest.srv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.sun.net.httpserver.HttpServer;


public class RestServer {
	private static final String WSERVICE = "GiveMeYourIps";
	private static final int PORT = 9090;
	
	
	public static void main(String[] args) throws Exception {

		
		URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(9090).build();
		final String url = "http://"+InetAddress.getLocalHost().getHostAddress() +":9090/";
		ResourceConfig config = new ResourceConfig();
		try{
		String path = args.length > 0 ? args[0] : "./RESTServer";
		ServerResource.basePath = new File(path);
		File albLogs = new File(ServerResource.basePath,"albLogs.dat");
		File picLogs = new File(ServerResource.basePath,"picLogs.dat");
		if(albLogs.exists() && albLogs.isFile()){
			FileInputStream fin = new FileInputStream(albLogs);
			ObjectInputStream ois = new ObjectInputStream(fin);
			ServerResource.albumLogs = (Map<String, String>) ois.readObject();
			System.out.println(ServerResource.albumLogs);
			ois.close();
		}else{
		ServerResource.albumLogs = new HashMap<String,String>();
		}
		if(picLogs.exists() && picLogs.isFile()){
			FileInputStream fin = new FileInputStream(picLogs);
			ObjectInputStream ois = new ObjectInputStream(fin);
			ServerResource.picLogs = (Map<String, Map<String, String>>) ois.readObject();
			System.out.println(ServerResource.picLogs);
			ois.close();
		}else{
			ServerResource.picLogs = new HashMap<String, Map<String,String>>();
		}
		if(!ServerResource.basePath.exists())
			ServerResource.basePath.mkdirs();
		
		createShutDownHook(albLogs,picLogs,ServerResource.albumLogs, ServerResource.picLogs);

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
		Scanner in = new Scanner(System.in);
		System.out.println("Para correr com o eclipse, escrever EXIT na consola para");
		System.out.println("simular uma terminacao do programa com CTRL+C");
		while(!in.nextLine().equals("EXIT")){
			System.out.println("not leaving yet");
		}
		System.exit(0);
		
	}

	
	private static void answerMulticast(String localhost) {

		new Thread(() -> {
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
						System.out.println("sent " +localhost);
						socket.send(sendingPacket);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();

			}
			}).start();
		
	}
	private static void createShutDownHook(File albLogsFile, File picLogsFile, Map<String, String> albLogs, Map<String, Map<String, String>> picLogs){
		  Runtime.getRuntime().addShutdownHook(new Thread() {
		   @Override
		   public void run() {
			   try{
				   System.out.println("writing logs!");
			   	FileOutputStream fout = new FileOutputStream(picLogsFile);
				ObjectOutputStream oos = new ObjectOutputStream(fout);   
				oos.writeObject(picLogs);
				oos.close();
				FileOutputStream fout1 = new FileOutputStream(albLogsFile);
				ObjectOutputStream oos1 = new ObjectOutputStream(fout1);   
				oos1.writeObject(albLogs);
				oos1.close();
			   }catch(Exception e){
				   e.printStackTrace();
			   }
		   }
		  });
		 
		 }
}
