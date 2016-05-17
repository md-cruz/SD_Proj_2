package restExample.clt;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;

import restExample.srv.Message;

public class PutMessage {

	public static void main(String[] args) throws IOException {
	    ClientConfig config = new ClientConfig();
	    Client client = ClientBuilder.newClient(config);

	    WebTarget target = client.target(getBaseURI()).path("/messages/add/0");

	    Message msg = new Message("nmp", "and a PUT is used to update/replace a resource...");
	    
	    Response response = target.request().put( Entity.entity(msg, MediaType.APPLICATION_JSON));
	    
	    System.out.println(response.getStatus() );
	    
	}
	
	private static URI getBaseURI() {
	    return UriBuilder.fromUri("http://localhost:9090/").build();
	  }
}
