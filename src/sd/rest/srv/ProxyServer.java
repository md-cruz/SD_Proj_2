package sd.rest.srv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.URI;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.github.scribejava.apis.ImgurApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.sun.net.httpserver.HttpServer;


public class ProxyServer {
	private static final String WSERVICE = "ImAProxy";
	private static final long DAYS = 2629746000L;
	
	public static void main(String[] args) throws Exception {

		final String apiKey = "ae169aff6383f6f"; 
		final String apiSecret = "a009a369aeee37a15e549fc6f851cc5ac01ef09e"; 
		URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(9060).build();
		final String url = "http://"+InetAddress.getLocalHost().getHostAddress() +":9060/";
		ResourceConfig config = new ResourceConfig();
		ProxyResource.service = new ServiceBuilder()
				.apiKey(apiKey)
				.apiSecret(apiSecret)
				.build(ImgurApi.instance());
		ProxyResource.albumLogs = new HashMap<String,String>();
		ProxyResource.picLogs = new HashMap<String, String>();
		String path = args.length > 0 ? args[0] : "./PROXYServer";
		File serverFile = new File(path);
		if(!serverFile.exists())
			serverFile.mkdirs();
		File atFile = new File(serverFile,"AccessToken.dat");
		File rtFile = new File(serverFile,"RefreshToken.dat");
		if(!atFile.exists()){
		
			Scanner in = new Scanner(System.in);
			final String authorizationUrl = ProxyResource.service.getAuthorizationUrl();
			System.out.println("Necessario dar permissao neste URL:");
			System.out.println(authorizationUrl);
			System.out.println("e copiar o codigo obtido para aqui:");
			System.out.print(">>");
			String code = in.nextLine();
			in.close();
			ProxyResource.at = ProxyResource.service.getAccessToken(code);
			
			saveTokens(ProxyResource.at, atFile,rtFile);
		}else if(System.currentTimeMillis() > atFile.lastModified()+DAYS){
			String refreshTokenString =
					new String(Files.readAllBytes(rtFile.toPath() ));

			OAuthRequest request = new OAuthRequest(Verb.POST,
					"https://api.imgur.com/oauth2/token", ProxyResource.service);
			request.addBodyParameter("refresh_token", refreshTokenString); 
			request.addBodyParameter("client_id", apiKey);
			request.addBodyParameter("client_secret", apiSecret);
			request.addBodyParameter("grant_type", "refresh_token");
			Response response = request.send();
			JSONParser parser = new JSONParser();
			JSONObject getTokens = (JSONObject) parser.parse(response.getBody());
			ProxyResource.at = ProxyResource.
					service.
					getAccessToken((String) getTokens.get("access_token"));
			System.out.println(ProxyResource.at.getAccessToken());
			saveTokens( ProxyResource.at, atFile, rtFile);
		}else{
			//String accessTokenString = new String(Files.readAllBytes(atFile.toPath() ));

			FileInputStream fin = new FileInputStream(atFile);
			ObjectInputStream ois = new ObjectInputStream(fin);
			ProxyResource.at = (OAuth2AccessToken) ois.readObject();
			ois.close();
			 
			System.out.println(ProxyResource.at.getAccessToken());
		}
		
		File albLogs = new File(serverFile,"albLogs.dat");
		File picLogs = new File(serverFile,"picLogs.dat");
		if(albLogs.exists() && albLogs.isFile()){
			FileInputStream fin = new FileInputStream(albLogs);
			ObjectInputStream ois = new ObjectInputStream(fin);
			ProxyResource.albumLogs = (Map<String, String>) ois.readObject();
			System.out.println(ProxyResource.albumLogs);
			ois.close();
		}else{
			ProxyResource.albumLogs = new HashMap<String,String>();
		}
		if(picLogs.exists() && picLogs.isFile()){
			FileInputStream fin = new FileInputStream(picLogs);
			ObjectInputStream ois = new ObjectInputStream(fin);
			ProxyResource.picLogs = (Map<String, String>) ois.readObject();
			System.out.println(ProxyResource.picLogs);
			ois.close();
		}else{
			ProxyResource.picLogs = new HashMap<String, String>();
		}
		createShutDownHook(albLogs,picLogs,ServerResource.albumLogs, ServerResource.picLogs);

		
	
		config.register(ProxyResource.class);
		
		
		HttpServer server = JdkHttpServerFactory.createHttpServer(baseUri, config);

		System.err.println("REST Proxy ready... ");
		
		answerMulticast(url);
		
		Scanner in = new Scanner(System.in);
		System.out.println("Para correr com o eclipse, escrever EXIT na consola para");
		System.out.println("simular uma terminacao do programa com CTRL+C");
		while(!in.nextLine().equals("EXIT")){
			System.out.println("not leaving yet");
		}
		System.exit(0);
		
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

	private static void saveTokens( OAuth2AccessToken at, File atFile, File rtFile) {
		String accessToken = at.toString();
		try {

			FileOutputStream fout = new FileOutputStream(atFile);
			ObjectOutputStream oos = new ObjectOutputStream(fout);   
			oos.writeObject(at);
			oos.close();
			
			FileOutputStream fout1 = new FileOutputStream(rtFile);
			ObjectOutputStream oos1 = new ObjectOutputStream(fout1);   
			oos1.writeObject(at.getRefreshToken());
			oos1.close();
			System.out.println(at.getAccessToken());
			/*Files.write(atFile.toPath(), accessToken.getBytes());
			String refreshTokenString = at.getRefreshToken();
			Files.write( rtFile.toPath(), refreshTokenString.getBytes() );
			*/
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void answerMulticast(String localhost) {	
		new Thread(() -> {
			try {
				final String addr = "229.0.0.1";
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

						byte[] data = (localhost).getBytes();
						DatagramPacket sendingPacket = new DatagramPacket(data, data.length);
						sendingPacket.setAddress(packet.getAddress());
						sendingPacket.setPort(packet.getPort());
						socket.send(sendingPacket);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();

			}
			}).start();
	}
}
