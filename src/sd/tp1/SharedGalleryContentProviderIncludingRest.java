package sd.tp1;

import java.io.File;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.SyncInvoker;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;

import sd.clt.ws.FileServerImplWSService;
import sd.clt.ws.IOException_Exception;
import sd.clt.ws.InfoNotFoundException_Exception;
import sd.clt.ws.PictureExistsException_Exception;
import sd.srv.PictureExistsException;
import sd.clt.ws.FileServerImplWS;
import sd.tp1.gui.GalleryContentProvider;
import sd.tp1.gui.Gui;
import sd.tp1.gui.impl.GalleryWindow;

/*
 * This class provides the album/picture content to the gui/main application.
 * 
 * Project 1 implementation should complete this class. 
 */




public class SharedGalleryContentProviderIncludingRest implements GalleryContentProvider {

	Gui gui;
	List<String> servers;
	private static final String MULTICASTIP = "228.0.0.1";
	private static final int PORT = 9000;
	private static final char SOAP = 'S';
	private static final char REST = 'R';
	private Client client;

	SharedGalleryContentProviderIncludingRest() {
		servers = new CopyOnWriteArrayList<String>();
		getServers();
		ClientConfig config = new ClientConfig();
	   	this.client = ClientBuilder.newClient(config);
		// gui = new GalleryWindow(this);
	}

	private URI getBaseURI(String serverUrl) {
	    return UriBuilder.fromUri(serverUrl).port(9090).build();
	  }
	
	// also checks if the servers are alive
	private void getServers() {
		new Thread(() -> {
			try {

				final int port = PORT;
				final String addr = MULTICASTIP;
				final InetAddress address = InetAddress.getByName(addr);

				MulticastSocket socket = new MulticastSocket();

				byte[] input = ("GiveMeYourIps").getBytes();
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

	/**
	 * Downcall from the GUI to register itself, so that it can be updated via
	 * upcalls.
	 */
	@Override
	public void register(Gui gui) {
		if (this.gui == null) {
			this.gui = gui;
		}
	}

	/**
	 * Returns the list of albums in the system. On error this method should
	 * return null.
	 */
	@Override
	public List<Album> getListOfAlbums() {
		List<Album> lst = new ArrayList<Album>();
		List<Album> tmp = new ArrayList<Album>();

		for (String serverUrl : servers) {
			try{
				if(serverUrl.charAt(0)==(SOAP))
					tmp = (soapListOfAlbums(serverUrl.substring(1)));
				else if(serverUrl.charAt(0) == REST)
					tmp = (restListOfAlbums(serverUrl.substring(1)));
				for(Album a : tmp)
					if(!lst.contains(a))
						lst.add(a);
			}catch (Exception e){
				e.printStackTrace();
				
				System.out.println("Server currently unavailable.");
			}
		}
		return lst;
	}
	
	private List<Album> restListOfAlbums(String serverUrl) {
		List<Album> lst = new ArrayList<Album>();
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(serverUrl));
		
			String[] albumNames;
			System.out.println(serverUrl);
			Builder replyB = target.path("RESTServer/getAlbumList/")
					.request().accept(MediaType.APPLICATION_JSON);
			
			Response reply = replyB.get();
			
			if (reply.getStatusInfo().equals(Status.OK)) {
				albumNames = replyB.get(String[].class);

				for (int i = 0; i < albumNames.length; i++) {
					SharedAlbum alb = new SharedAlbum(albumNames[i]);
					if (!lst.contains(alb) && !alb.getName().endsWith(".deleted"))
						lst.add(alb);
				}
				done = true;
			
			} 
			// else try again	
		}
		return lst;
	}
	
	private List<Album> soapListOfAlbums(String serverUrl) throws InfoNotFoundException_Exception, MalformedURLException{
		List<Album> lst = new ArrayList<Album>();
		
			System.out.println(serverUrl + " listAlbum\n");
			URL wsURL = new URL(String.format("%s", serverUrl));
			FileServerImplWSService service = new FileServerImplWSService(wsURL); // wsimport
			FileServerImplWS server = service.getFileServerImplWSPort();
			try {
				List<String> aList = server.getAlbumList();
				for (String album : aList) {
					SharedAlbum alb = new SharedAlbum(album);
					if (!lst.contains(alb)&& !album.endsWith(".deleted"))
						lst.add(alb);
				}
			
				
			} catch (Exception e) {
				// call method again, max 3 times
				boolean executed = false;
				for (int i = 0; !executed && i < 3; i++) { // number of
															// tries
					try {
						List<String> aList = server.getAlbumList();
						for (String album : aList) {
							SharedAlbum alb = new SharedAlbum(album);
							if (!lst.contains(alb))
								lst.add(alb);
						}
						executed = true;
					} catch (RuntimeException e1) {
						if (i < 2) {
							try { // wait some time
								Thread.sleep(5000);
							} catch (InterruptedException e2) {
								// do nothing
							}
						}
					}
				}
			}
		
		return lst;
	}

	/**
	 * Returns the list of pictures for the given album. On error this method
	 * should return null.
	 */
	@Override
	public List<Picture> getListOfPictures(Album album) {

		List<Picture> lst = new ArrayList<Picture>();
		List<Picture> tmp = new ArrayList<Picture>();
		

		for (String serverUrl : servers) {
			try{
				String url = serverUrl.substring(1);
				if(serverUrl.charAt(0)==(SOAP))
					tmp = (soapListOfPictures(url,album));
				else if(serverUrl.charAt(0) == REST)
					tmp = (restListOfPictures(url,album));
				for(Picture p : tmp)
					if(!lst.contains(p))
						lst.add(p);
			}catch (Exception e){
				e.printStackTrace();
				
				System.out.println("Server currently unavailable.");
			}
		}
			
		return lst;
		}
	private List<Picture> restListOfPictures(String serverUrl, Album album) {
		List<Picture> lst = new ArrayList<Picture>();
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(serverUrl));
		
			String[] pictureNames;
			System.out.println(serverUrl);
			Builder replyB = target.path("RESTServer/getPictureList/" + album.getName())
					.request().accept(MediaType.APPLICATION_JSON);
			
			Response reply = replyB.get();
			
			if (reply.getStatusInfo().equals(Status.OK)) {
				pictureNames = replyB.get(String[].class);

				for (int i = 0; i < pictureNames.length; i++) {
					SharedPicture pic = new SharedPicture(pictureNames[i]);
					if (!lst.contains(pic) && !pic.getName().endsWith(".deleted"))
						lst.add(pic);
				}
				done = true;
			
			} 
			// else try again	
		}
		return lst;
	}

	private List<Picture> soapListOfPictures(String serverUrl, Album album) {
		List<Picture> lst = new ArrayList<Picture>();

		try{
			System.out.println(serverUrl + " listPicture\n");
			URL wsURL = new URL(String.format("%s", serverUrl));
			FileServerImplWSService service = new FileServerImplWSService(wsURL);
			FileServerImplWS server = service.getFileServerImplWSPort();
			try {
				List<String> picList = server.getPictureList(album.getName());
				for (String pic : picList) {
					SharedPicture picture = new SharedPicture(pic);
					if (!lst.contains(picture)&& !pic.endsWith(".deleted"))
						lst.add(picture);
				}
			} catch (Exception e) {
				// call method again, max 3 times
				boolean executed = false;
				for (int i = 0; !executed && i < 3; i++) { // number of
															// tries
					try {
						List<String> picList = server.getPictureList(album.getName());
						for (String pic : picList) {
							SharedPicture picture = new SharedPicture(pic);
							if (!lst.contains(picture))
								lst.add(picture);
						}
						executed = true;
					} catch (RuntimeException | InfoNotFoundException_Exception e1) {
						if (i < 2) {
							try { // wait some time
								Thread.sleep(5000);
							} catch (InterruptedException e2) {
								// do nothing
							}
						}
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
			return lst;
		}
	

	/**
	 * Returns the contents of picture in album. On error this method should
	 * return null.
	 */
	@Override
	public byte[] getPictureData(Album album, Picture picture) {
		byte[] pictureData = null;
		
		for (String serverUrl : servers) {
			try{
				if(serverUrl.charAt(0)==(SOAP))
					pictureData = soapGetPicData(serverUrl.substring(1),album,picture);
				else if(serverUrl.charAt(0) == REST) 
					pictureData = restGetPicData(serverUrl.substring(1),album,picture);
				break;
			}catch (Exception e){
				e.printStackTrace();
				
				System.out.println("Server currently unavailable.");
			}
		}
		return pictureData;
	}

	private byte[] restGetPicData(String serverUrl, Album album, Picture picture) {
		byte[] pictureData = null;
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(serverUrl));
			Builder replyB = target.path("RESTServer/downloadPicture/" + album.getName() +"/"+ picture.getName())
					.request().accept(MediaType.APPLICATION_OCTET_STREAM);
			Response reply = replyB.get();
			if (reply.getStatusInfo().equals(Status.OK)) {
				pictureData= replyB.get(byte[].class);
				done = true;
			
			} 
			// else try again	
		}
		return pictureData;
	}

	private byte[] soapGetPicData(String serverUrl, Album album, Picture picture) {
		byte[] pictureData = null;
		try {
			System.out.println(serverUrl + " downloadPicture\n");
			URL wsURL = new URL(String.format("%s", serverUrl));
			FileServerImplWSService service = new FileServerImplWSService(wsURL);
			FileServerImplWS server = service.getFileServerImplWSPort();
			try {
				pictureData = server.downloadPicture(album.getName(), picture.getName());
				return pictureData;
			} catch (Exception e) {
				// call method again, max 3 times
				for (int i = 0; i < 3; i++) { // number of tries
					try {
						pictureData = server.downloadPicture(album.getName(), picture.getName());
						return pictureData;
					} catch (RuntimeException e1) {
						if (i < 2) {
							try { // wait some time
								Thread.sleep(5000);
							} catch (InterruptedException e2) {
								// do nothing
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			// System.out.println("Server " + serverUrl + " may be down, client
			// will remove"
			// + "it withing 6 minutes if it does not show signs of life");
		}
		return pictureData;
	}

	/**
	 * Create a new album. On error this method should return null.
	 */
	@Override
	public Album createAlbum(String name) {
		int i = 0;
		int times = 0;
		Album album = null;
		try{
		final int[] serverIndexes = new Random().ints(0, servers.size()).distinct().limit(servers.size()).toArray();
		
		boolean finished = false;
		
		
		while (!finished && i < serverIndexes.length && serverIndexes[i] < servers.size()) {
			try{
				String serverUrl = servers.get(serverIndexes[i]);
				i++;
				if(serverUrl.charAt(0)==(SOAP))
					album = soapCreateAlbum(serverUrl.substring(1),name);
				else if(serverUrl.charAt(0) == REST)
					album = restCreateAlbum(serverUrl.substring(1),name);
				finished = true;
			}catch (Exception e) {
				
				if (i >= servers.size()) {
					if (times < 2) {
						i = 0;
						times++;
					} else
						break;
				}
			}
		}
		
		}catch (IllegalArgumentException e){
			System.out.println("No servers connected right now");
		}
		System.out.println(album);
		return album;
	}

	private Album restCreateAlbum(String serverUrl, String name) {
		Album alb = new SharedAlbum(name);
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(serverUrl));
			Response replyB = target.path("RESTServer/createNewAlbum/")
					.request().post(Entity.entity(name, MediaType.APPLICATION_OCTET_STREAM));
			
			if (replyB.getStatusInfo().equals(Status.OK)) {
				done = true;
			} 
			// else try again	
		}
		if(!done)
			return null;
		return alb;
	}

	private Album soapCreateAlbum(String serverUrl, String name) throws MalformedURLException {


		boolean finished = false;

		System.out.println(serverUrl + " createAlbum\n");
		URL wsURL = new URL(String.format("%s", serverUrl));
		FileServerImplWSService service = new FileServerImplWSService(wsURL);
		FileServerImplWS server = service.getFileServerImplWSPort();
		try {
			finished = server.createNewAlbum(name);
		} catch (Exception e) {
			// call method again, max 3 times
			boolean executed = false;
			for (int j = 0; !executed && j < 3; j++) { // number of
														// tries
				try {
					finished = server.createNewAlbum(name);
				} catch (RuntimeException e1) {
					if (j < 2) {
						try { // wait some time
							Thread.sleep(5000);
						} catch (InterruptedException e2) {
							// do nothing
						}
					}
				}
			}
		}
		System.out.println(finished);
		if (!finished)
			return null;
		return new SharedAlbum(name);
	}

	/**
	 * Delete an existing album.
	 */
	@Override
	public void deleteAlbum(Album album) {

		
		for (String serverUrl : servers) {
			try{
				if(serverUrl.charAt(0)==(SOAP))
					soapDeleteAlbum(serverUrl.substring(1),album);
				else if(serverUrl.charAt(0) == REST) // TODO: Rest method
					restDeleteAlbum(serverUrl.substring(1),album);
				break;
			}catch (Exception e){
				e.printStackTrace();
				System.out.println("Server currently unavailable.");
			}
		}
	}

	private void restDeleteAlbum(String serverUrl, Album album) {
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(serverUrl));
			Response replyB = target.path("RESTServer/deleteAlbum/" + album.getName())
					.request().delete();
			if (replyB.getStatusInfo().equals(Status.OK)) {
				done = true;
			} 
			// else try again	
		}
	}

	private void  soapDeleteAlbum(String serverUrl, Album album) throws MalformedURLException, InfoNotFoundException_Exception {

		System.out.println(serverUrl + " deleteAlbum\n");
		URL wsURL = new URL(String.format("%s", serverUrl));
		FileServerImplWSService service = new FileServerImplWSService(wsURL);
		FileServerImplWS server = service.getFileServerImplWSPort();
		try {
			server.deleteAlbum(album.getName());
			System.out.println("Deleted");
		} catch (Exception e) {
			// call method again, max 3 times
			System.out.println("rip?");
			boolean executed = false;
			for (int i = 0; !executed && i < 3; i++) { // number of
														// tries
				try {
					server.deleteAlbum(album.getName());
					System.out.println("Deleted");
					executed = true;
				} catch (RuntimeException e1) {
					if (i < 2) {
						try { // wait some time
							Thread.sleep(5000);
						} catch (InterruptedException e2) {
							// do nothing
						}
					}
				}
			}
		}
	}

	/**
	 * Add a new picture to an album. On error this method should return null.
	 */
	@Override
	public Picture uploadPicture(Album album, String name, byte[] data) {

		int i = 0;
		int times = 0;
		Picture pic = null;
		try{
			boolean finished = false;
		final int[] serverIndexes = new Random().ints(0, servers.size()).distinct().limit(servers.size()).toArray();
		while (!finished && i < serverIndexes.length && serverIndexes[i] < servers.size()) {
			try{
				String serverUrl = servers.get(serverIndexes[i]);
				i++;
				if(serverUrl.charAt(0)==(SOAP))
					pic = soapUploadPic(serverUrl.substring(1),album,name,data);
				else if(serverUrl.charAt(0) == REST)
					pic = restUploadPic(serverUrl.substring(1),album,name,data);
				finished = true;
			}catch (Exception e) {
				
				if (i >= servers.size()) {
					if (times < 2) {
						i = 0;
						times++;
					} else
						break;
				}
			}
		}
		
		}catch (IllegalArgumentException e){
			System.out.println("No servers connected right now");
		}
		
		return pic;
	}

	private Picture restUploadPic(String serverUrl, Album album, String name, byte[] data) {
		
		boolean done = false;
		String path = album.getName() +File.separator+ name;
		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(serverUrl));
			System.out.println("hi");
			
			Response replyB = target.path("RESTServer/uploadPicture/"
			+ album.getName() +"/"+ name)
					.request().post( Entity.entity(data, MediaType.APPLICATION_OCTET_STREAM));
		
			if (replyB.getStatusInfo().equals(Status.OK)) {
				done = true;
			
			} 
			// else try again	
		}
		if(!done)
			return null;
		return new SharedPicture(name);
			
	}

	private Picture soapUploadPic(String serverUrl, Album album, String name, byte[] data) throws IOException_Exception, InfoNotFoundException_Exception, MalformedURLException {
		boolean finished = false;
		
		System.out.println(serverUrl + " uploadPicture\n");
		
		URL wsURL = new URL(String.format("%s", serverUrl));

		FileServerImplWSService service = new FileServerImplWSService(wsURL);
		FileServerImplWS server = service.getFileServerImplWSPort();
		try {
			server.uploadPicture(album.getName() +File.separator+ name, data);
			finished = true;
		}catch(PictureExistsException_Exception e){
			return null;
		}catch (Exception e) {
			// call method again, max 3 times
			for (int j = 0; !finished && j < 3; j++) { // number of
														// tries
				try {
					server.uploadPicture(album.getName() +File.separator+ name, data);
					finished = true;
				}catch(PictureExistsException_Exception e2){
					return null;
				}
				catch (RuntimeException e1) {
					if (j < 2) {
						try { // wait some time
							Thread.sleep(5000);
						} catch (InterruptedException e2) {
							// do nothing
						}
					}
				}
			}
		}
		System.out.println("finished uploading picture " + finished);
		if(!finished)
			return null;
		return new SharedPicture(name);
	}

	/**
	 * Delete a picture from an album. On error this method should return false.
	 */
	@Override
	public boolean deletePicture(Album album, Picture picture) {
		
		boolean finished = false;
		

		for (String serverUrl : servers) {
			try{
				if(serverUrl.charAt(0)==(SOAP))
					finished = soapDeletePic(serverUrl.substring(1),album,picture);
				else if(serverUrl.charAt(0) == REST) // TODO: Rest method
					finished = restDeletePic(serverUrl.substring(1),album,picture);
				break;
			}catch (Exception e){
				e.printStackTrace();
				
				System.out.println("Server currently unavailable.");
			}
		}
		return finished;
	}

	private boolean restDeletePic(String serverUrl, Album album, Picture picture) {
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(serverUrl));
			System.out.println(serverUrl);
			Response replyB = target.path("RESTServer/deletePicture/" + album.getName()
			+"/"+ picture.getName())
					.request().delete();
			if (replyB.getStatusInfo().equals(Status.OK)) {
				done = true;
			} 
			// else try again	
		}
		return done;
	}

	private boolean soapDeletePic(String serverUrl, Album album, Picture picture) throws MalformedURLException, InfoNotFoundException_Exception {
		boolean finished = false;
		URL wsURL = new URL(String.format("%s", serverUrl));
		FileServerImplWSService service = new FileServerImplWSService(wsURL);
		FileServerImplWS server = service.getFileServerImplWSPort();
		try {
			server.deletePicture(album.getName(), picture.getName());
			finished = true;
		
		} catch (Exception e) {
			// call method again, max 3 times
			for (int i = 0; !finished && i < 3; i++) {
				try {
					server.deletePicture(album.getName(), picture.getName());
					finished = true;
				} catch (RuntimeException e1) {
					if (i < 2) {
						try { // wait some time
							Thread.sleep(5000);
						} catch (InterruptedException e2) {
							// do nothing
						}
					}
				}
			}
		}
		return finished;
	}
	

	/**
	 * Represents a shared album.
	 */
	static class SharedAlbum implements GalleryContentProvider.Album {
		final String name;

		SharedAlbum(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}
	}

	/**
	 * Represents a shared picture.
	 */
	static class SharedPicture implements GalleryContentProvider.Picture {
		final String name;

		SharedPicture(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}
	}
}
