package sd.rest.srv;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.URI;
import java.nio.file.Files;
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
		long expirationDate = 0L;
		URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(9090).build();
		final String url = "http://"+InetAddress.getLocalHost().getHostAddress() +":9090/";
		ResourceConfig config = new ResourceConfig();
		ProxyResource.service = new ServiceBuilder()
				.apiKey(apiKey)
				.apiSecret(apiSecret)
				.build(ImgurApi.instance());
		File atFile = new File("AccessToken.dat");
		File rtFile = new File("RefreshToken.dat");
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
			expirationDate = System.currentTimeMillis() + DAYS;
		}else if(System.currentTimeMillis() > expirationDate){
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
			
			saveTokens( ProxyResource.at, atFile, rtFile);
		}else{
			String accessTokenString = new String(Files.readAllBytes(atFile.toPath() ));
			ProxyResource.at = new OAuth2AccessToken(accessTokenString);
		}
		
	
		config.register(ProxyResource.class);
		
		
		HttpServer server = JdkHttpServerFactory.createHttpServer(baseUri, config);

		System.err.println("REST Server ready... ");
		
		answerMulticast(url);
		
	}

	private static void saveTokens( OAuth2AccessToken at, File atFile, File rtFile) {
		String accessToken = at.toString();
		try {
			Files.write(atFile.toPath(), accessToken.getBytes());
			String refreshTokenString = at.getRefreshToken();
			Files.write( rtFile.toPath(), refreshTokenString.getBytes() );
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
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
