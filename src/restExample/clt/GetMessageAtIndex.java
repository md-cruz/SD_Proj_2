package restExample.clt;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;

import restExample.srv.Message;

public class GetMessageAtIndex {

	public static void main(String[] args) throws IOException {
	    ClientConfig config = new ClientConfig();
	    Client client = ClientBuilder.newClient(config);

	    WebTarget target = client.target(getBaseURI());

	    Message msg = target.path("/messages/at/0/")
	    		.request()
	    		.accept(MediaType.APPLICATION_JSON)
	    		.get(Message.class);

	    System.out.println( msg );
	}
	
	private static URI getBaseURI() {
	    return UriBuilder.fromUri("http://localhost:9090/").build();
	  }
}
