package restExample.clt;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;

public class GetMessages_asBytes {

	public static void main(String[] args) throws IOException {
	    ClientConfig config = new ClientConfig();
	    Client client = ClientBuilder.newClient(config);

	    WebTarget target = client.target(getBaseURI());

	    byte[] bytes = target.path("/messages").request()
	        .accept(MediaType.APPLICATION_OCTET_STREAM).get(byte[].class);

	    System.out.write( bytes );
	    System.out.println();
	}
	
	private static URI getBaseURI() {
	    return UriBuilder.fromUri("http://localhost:9090/").build();
	  }
}
