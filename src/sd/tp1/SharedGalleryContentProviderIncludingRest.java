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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
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
import sd.clt.ws.FileServerImplWS;
import sd.tp1.gui.GalleryContentProvider;
import sd.tp1.gui.Gui;

/*
 * This class provides the album/picture content to the gui/main application.
 * 
 * Project 1 implementation should complete this class. 
 */

public class SharedGalleryContentProviderIncludingRest implements GalleryContentProvider {

	private Gui gui;
	private List<String> servers;
	private List<String> proxies;
	private static final String MULTICASTIP = "228.0.0.1";
	private static final String MULTICASTIPPROXY = "229.0.0.1";
	private static final String WSERVICEP = "ImAProxy";
	private static final int PORT = 9000;
	private static final char SOAP = 'S';
	private static final char REST = 'R';
	private static final int RESTPORT = 9090;
	private static final int IMGURPORT = 9060;
	private Client client;

	SharedGalleryContentProviderIncludingRest() {
		servers = new CopyOnWriteArrayList<String>();
		proxies = new CopyOnWriteArrayList<String>();
		getServers();
		getProxies();
		synchronizeStuff();
		try {
			// dar algum buffer
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ClientConfig config = new ClientConfig();
		this.client = ClientBuilder.newClient(config);
	}

	private URI getBaseURI(String serverUrl,int port) {
		return UriBuilder.fromUri(serverUrl).build();
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
				Map<String, Integer> consecutiveReplies = new HashMap<String, Integer>();
				int numberOfQueries = 0;

				while (true) {
					System.out.println("Sent packet");
					socket.send(packet);
					// System.out.println(new String(packet.getData()));
					numberOfQueries++;
					
					boolean foundAllServers = false;
					try {
						while (!foundAllServers) {

							socket.setSoTimeout(60000);
							byte[] received = new byte[65536];
							DatagramPacket receivedPacket = new DatagramPacket(received, received.length);

							socket.receive(receivedPacket);

							String serverHost = new String(receivedPacket.getData()).trim();
							consecutiveReplies.put(serverHost, consecutiveReplies.getOrDefault(serverHost, 1) + 1);
							// getOrDefault returns the current value for the
							// key
							// or 1 if the key has no value yet
							System.out.println("got " + serverHost);
							if (!servers.contains(serverHost))
								servers.add(serverHost);
						}
					} catch (SocketTimeoutException e) {
						foundAllServers = true;
					}

					// delete servers if they havent replied in the last 3 times
					for (String server : consecutiveReplies.keySet()) {
						if (consecutiveReplies.get(server) + 3 < numberOfQueries) {
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


	private void synchronizeStuff() {
		new Thread(() -> {
			try {
				for(;;){
				// sleep for one hour
				//Thread.sleep(3600000);
					Thread.sleep(30000);
				int numberOfProxies = proxies.size();
				int numberOfServers = servers.size();
				if(numberOfServers > 1){
					
					Random r = new Random();
					int server1 = r.nextInt(numberOfServers);
					int server2 = r.nextInt(numberOfServers);
					while(server1==server2)
						server2=r.nextInt(numberOfServers);
					System.out.println("sycning " + servers.get(server1) + " with "+ servers.get(server2));
					syncServerWithServer(servers.get(server1),servers.get(server2));
					System.out.println("SYNC DONE!");
				}
				if(numberOfServers > 0 && numberOfProxies > 0){
					Random r = new Random();
					int server = r.nextInt(numberOfServers);
					int proxy = r.nextInt(numberOfProxies);
					syncServerWithProxy(proxies.get(proxy),servers.get(server));
				}
				// debug - deixar descomentado!
				//Thread.sleep(5000);
				
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();		}
	
	private void getProxies() {
		new Thread(() -> {
			try {

				final int port = PORT;
				final String addr = MULTICASTIPPROXY;
				final InetAddress address = InetAddress.getByName(addr);

				MulticastSocket socket = new MulticastSocket();

				byte[] input = (WSERVICEP).getBytes();
				DatagramPacket packet = new DatagramPacket(input, input.length);
				packet.setAddress(address);
				packet.setPort(port);
				Map<String, Integer> consecutiveReplies = new HashMap<String, Integer>();
				int numberOfQueries = 0;

				while (true) {
					System.out.println("Sent packet");
					socket.send(packet);
					// System.out.println(new String(packet.getData()));
					numberOfQueries++;
					
					boolean foundAllServers = false;
					try {
						while (!foundAllServers) {

							socket.setSoTimeout(60000);
							byte[] received = new byte[65536];
							DatagramPacket receivedPacket = new DatagramPacket(received, received.length);
							socket.receive(receivedPacket);

							String serverHost = new String(receivedPacket.getData()).trim();
							System.out.println("proxy address " + serverHost);
							consecutiveReplies.put(serverHost, consecutiveReplies.getOrDefault(serverHost, 1) + 1);
							// getOrDefault returns the current value for the
							// key
							// or 1 if the key has no value yet
							if (!proxies.contains(serverHost))
								proxies.add(serverHost);
						}
					} catch (SocketTimeoutException e) {
						foundAllServers = true;
					}

					// delete servers if they havent replied in the last 3 times
					for (String server : consecutiveReplies.keySet()) {
						if (consecutiveReplies.get(server) + 3 < numberOfQueries) {
							// remove server
							proxies.remove(server);
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

	private String extractID(String name) {
		String [] a = name.split("\\.");
		if(a.length>1)
			return a[0];
		return name;
	}

	/**
	 * Returns the list of albums in the system. On error this method should
	 * return null.
	 */
	@Override
	public List<Album> getListOfAlbums() {
		List<Album> lst = new ArrayList<Album>();
		List<String> tmp = new ArrayList<String>();
		List<String> tmp1 = new ArrayList<String>();
		System.out.println("Get List of Albums");

		for (String serverUrl : servers) {
			try {
				if (serverUrl.charAt(0) == (SOAP))
					tmp = (soapListOfAlbums(serverUrl.substring(1)));
				else if (serverUrl.charAt(0) == REST)
					tmp = (restListOfAlbums(serverUrl.substring(1)));
				for(String s : tmp)
					if(!tmp1.contains(s))
						tmp1.add(s);
				
			} catch (Exception e) {
				e.printStackTrace();

				System.out.println("Server currently unavailable.");
			}
		}
		// escolher proxy aleatoriamente
		
		  //nao usar os lists da proxy, esses sao usados so para sincronizacao
	
		Random r = new Random();
		if (proxies.size() > 0) {
			String proxyUrl = proxies.get(r.nextInt(proxies.size()));
			List<String> albSet = imgurListOfAlbums(proxyUrl);
			for(String s : albSet)
				if(!tmp1.contains(s))
					tmp1.add(s);
		}
		for(String s : tmp1)
			lst.add(new SharedAlbum(s));

		return lst;
	}


	
	private List<String> imgurListOfAlbums(String proxyUrl) {
		boolean done = false;
		List<String> lst = new ArrayList<String>();
		for (int j = 0; j < 3 && !done; j++) {

			WebTarget target = client.target(getBaseURI(proxyUrl,IMGURPORT));

			String[] albumNames;
			Builder replyB = target.path("RESTProxy/getAlbumList/")
					.request().accept(MediaType.APPLICATION_JSON);

			Response reply = replyB.get();
			if (reply.getStatusInfo().equals(Status.OK)) {
				albumNames = replyB.get(String[].class);
				for(String s : albumNames)
					lst.add(s);
				done = true;

			}
		}
		return lst;
	}

	private List<String> restListOfAlbums(String serverUrl) {
		List<String> lst = new ArrayList<String>();
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(serverUrl,RESTPORT));

			String[] albumNames;
			System.out.println(serverUrl);
			Builder replyB = target.path("RESTServer/getAlbumList/").request().accept(MediaType.APPLICATION_JSON);

			Response reply = replyB.get();

			if (reply.getStatusInfo().equals(Status.OK)) {
				albumNames = replyB.get(String[].class);
				for(String s : albumNames)
					lst.add(s);
				done = true;

			}
			// else try again
		}
		return lst;
	}

	private List<String> soapListOfAlbums(String serverUrl)
			throws InfoNotFoundException_Exception, MalformedURLException {
		List<String> lst = new ArrayList<String>();

		System.out.println(serverUrl + " listAlbum\n");
		URL wsURL = new URL(String.format("%s", serverUrl));
		FileServerImplWSService service = new FileServerImplWSService(wsURL);
		FileServerImplWS server = service.getFileServerImplWSPort();
		try {
			lst = server.getAlbumList();
			

		} catch (Exception e) {
			// call method again, max 3 times
			boolean executed = false;
			for (int i = 0; !executed && i < 3; i++) { // number of
														// tries
				try {
					lst = server.getAlbumList();
					
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
		System.out.println("Get List of Pictures");
		List<Picture> lst = new ArrayList<Picture>();
		
		List<String> tmp = new ArrayList<String>();
		List<String> tmp1 = new ArrayList<String>();


		for (String serverUrl : servers) {
			try {
				String url = serverUrl.substring(1);
				if (serverUrl.charAt(0) == (SOAP))
					tmp = (soapListOfPictures(url, album.getName()));
				else if (serverUrl.charAt(0) == REST)
					tmp = (restListOfPictures(url, album.getName()));
				for(String s : tmp)
					if(!tmp1.contains(s))
						tmp1.add(s);
			} catch (Exception e) {
				e.printStackTrace();

				System.out.println("Server currently unavailable.");
			}
		}
		
		if(proxies.size()>0){
		Random r = new Random();
		String proxyUrl = proxies.get(r.nextInt(proxies.size()));
		List<String> albSet = imgurListOfPictures(proxyUrl,extractID(album.getName()));
		for(String s : albSet)
			if(!tmp1.contains(s))
				tmp1.add(s);
		}
		for(String s : tmp1)
			lst.add(new SharedPicture(s));
		return lst;
	}

	private List<String> restListOfPictures(String serverUrl, String album) {
		List<String> lst = new ArrayList<String>();
		List<String> albList = restListOfAlbums(serverUrl);
		if(!albList.contains(album))
			return lst;
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(serverUrl,RESTPORT));

			String[] pictureNames;
			System.out.println(serverUrl);
			Builder replyB = target.path("RESTServer/getPictureList/" + album).request()
					.accept(MediaType.APPLICATION_JSON);

			Response reply = replyB.get();

			if (reply.getStatusInfo().equals(Status.OK)) {
				pictureNames = replyB.get(String[].class);
				for(String s : pictureNames)
					lst.add(s);
				done = true;

			}
			// else try again
		}
		return lst;
	}

	private List<String> imgurListOfPictures(String proxyUrl, String album) {
		List<String> lst = new ArrayList<String>();
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {

			WebTarget target = client.target(getBaseURI(proxyUrl,IMGURPORT));

			String[] pictureNames;
			// extrair ID do album antes de fazer o request
			String albumID = extractID(album);
			Builder replyB = target.path("RESTProxy/getPictureList/" + albumID).request()
					.accept(MediaType.APPLICATION_JSON);

			Response reply = replyB.get();
			if (reply.getStatusInfo().equals(Status.OK)) {
				pictureNames = replyB.get(String[].class);
				for(String s : pictureNames)
					lst.add(s);
				done = true;


			}
			// else try again
		}
		return lst;
	}

	private List<String> soapListOfPictures(String serverUrl, String album) throws MalformedURLException, InfoNotFoundException_Exception {
		List<String> lst = new ArrayList<String>();
		List<String> albList = soapListOfAlbums(serverUrl);
		if(!albList.contains(album))
			return lst;

		try {
			System.out.println(serverUrl + " listPicture\n");
			URL wsURL = new URL(String.format("%s", serverUrl));
			FileServerImplWSService service = new FileServerImplWSService(wsURL);
			FileServerImplWS server = service.getFileServerImplWSPort();
			try {
				lst = server.getPictureList(album);
				
			} catch (Exception e) {
				// call method again, max 3 times
				boolean executed = false;
				for (int i = 0; !executed && i < 3; i++) { // number of
															// tries
					try {
						lst = server.getPictureList(album);
						
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
		} catch (Exception e) {
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
		System.out.println("Get Picture Data");
		byte[] pictureData = null;

		for (String serverUrl : servers) {
			try {
				if (serverUrl.charAt(0) == (SOAP))
					pictureData = soapGetPicData(serverUrl.substring(1), album.getName(), picture.getName());
				else if (serverUrl.charAt(0) == REST)
					pictureData = restGetPicData(serverUrl.substring(1), album.getName(), picture.getName());
				break;
			} catch (Exception e) {
				e.printStackTrace();

				System.out.println("Server currently unavailable.");
			}
		}
		// picture nao encontrada nos servers, recorrer a proxy
		if(pictureData==null && !proxies.isEmpty()){
			Random r = new Random();
			pictureData= imgurGetPicData(proxies.get(r.nextInt(proxies.size())), picture.getName());
		}
		return pictureData;
	}

	private byte[] imgurGetPicData(String proxyUrl, String picture) {
		byte[] pictureData = null;
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {

			WebTarget target = client.target(getBaseURI(proxyUrl,IMGURPORT));
			// extrair ID da fotografia antes do request
			String pictureID = extractID(picture);
			Builder replyB = target.path("RESTProxy/downloadPicture/" + pictureID).request()
					.accept(MediaType.APPLICATION_OCTET_STREAM);
			Response reply = replyB.get();
			if (reply.getStatusInfo().equals(Status.OK)) {
				pictureData = replyB.get(byte[].class);
				done = true;

			}
			// else try again
		}
		return pictureData;
	}

	private byte[] restGetPicData(String serverUrl, String album, String picture) {
		byte[] pictureData = null;
		List<String> albList = restListOfAlbums(serverUrl);
		if(!albList.contains(album)){
			System.out.println("no alb");
			return pictureData;
		}
		List<String> picList = restListOfPictures(serverUrl,album);
		if(!picList.contains(picture)){
			System.out.println("no pic");
			return pictureData;
		}
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(serverUrl,RESTPORT));
			Builder replyB = target.path("RESTServer/downloadPicture/" + album + "/" + picture).request()
					.accept(MediaType.APPLICATION_OCTET_STREAM);
			Response reply = replyB.get();
			if (reply.getStatusInfo().equals(Status.OK)) {
				pictureData = replyB.get(byte[].class);
				done = true;

			}
			// else try again
		}
		return pictureData;
	}

	private byte[] soapGetPicData(String serverUrl, String album, String picture) throws MalformedURLException, InfoNotFoundException_Exception {
		byte[] pictureData = null;
		List<String> albList = soapListOfAlbums(serverUrl);
		if(!albList.contains(album))
			return pictureData;
		List<String> picList = soapListOfPictures(serverUrl,album);
		if(!picList.contains(picture))
			return pictureData;
		try {
			URL wsURL = new URL(String.format("%s", serverUrl));
			FileServerImplWSService service = new FileServerImplWSService(wsURL);
			FileServerImplWS server = service.getFileServerImplWSPort();
			try {
				pictureData = server.downloadPicture(album, picture);
				return pictureData;
			} catch (Exception e) {
				// call method again, max 3 times
				for (int i = 0; i < 3; i++) { // number of tries
					try {
						pictureData = server.downloadPicture(album, picture);
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
		} catch (Exception e) {
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
		System.out.println("Create Album");
		int i = 0;
		int times = 0;
		Album album = null;
		// criar na proxy primeiro, e utilizar o id tambem como nome
		String id ="";
		if(!proxies.isEmpty()){
		Random r = new Random();
		id = imgurCreateAlbum(proxies.get(r.nextInt(proxies.size())), name);
		//id = imgurGetAlbId(proxies.get(r.nextInt(proxies.size())), name);
		}
		
		try {
			if(servers.size()>0){
			final int[] serverIndexes = new Random().ints(0, servers.size()).distinct().limit(servers.size()).toArray();

			boolean finished = false;

			while (!finished && i < serverIndexes.length && serverIndexes[i] < servers.size()) {
				try {
					String serverUrl = servers.get(serverIndexes[i]);
					String usingName = id=="" ? name: id+"."+name;
					i++;
					if (serverUrl.charAt(0) == (SOAP))
						album = soapCreateAlbum(serverUrl.substring(1), usingName);
					else if (serverUrl.charAt(0) == REST)
						album = restCreateAlbum(serverUrl.substring(1), usingName);
					finished = true;
				} catch (Exception e) {

					if (i >= servers.size()) {
						if (times < 2) {
							i = 0;
							times++;
						} else
							break;
					}
				}
			}
			}

		} catch (IllegalArgumentException e) {
			System.out.println("No servers connected right now");
			e.printStackTrace();
		}
		if(album == null && id!="")
			album = new SharedAlbum(id+"." +name);
		return album;
	}

	private Album restCreateAlbum(String serverUrl, String name) {
		Album alb = new SharedAlbum(name);
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {
			System.out.println(getBaseURI(serverUrl,RESTPORT));
			WebTarget target = client.target(getBaseURI(serverUrl,RESTPORT));
			Response replyB = target.path("RESTServer/createNewAlbum/").request()
					.post(Entity.entity(name, MediaType.APPLICATION_OCTET_STREAM));

			if (replyB.getStatusInfo().equals(Status.OK)) {
				done = true;
			}
			// else try again
		}
		if (!done)
			return null;
		return alb;
	}

	private String imgurCreateAlbum(String proxyUrl, String albumName) {
		boolean done = false;
		String albumID = "";
		for (int j = 0; j < 3 && !done; j++) {
			System.out.println(getBaseURI(proxyUrl,RESTPORT));

			WebTarget target = client.target(getBaseURI(proxyUrl,IMGURPORT));
			Builder reply = target.path("RESTProxy/createNewAlbum/").request()					
					.accept(MediaType.APPLICATION_OCTET_STREAM);

			Response replyB = reply.post(Entity.entity(albumName, MediaType.APPLICATION_OCTET_STREAM));
				
			/*Response replyB = target.path("RESTProxy/createNewAlbum/").request()
					.post(Entity.entity(albumName, MediaType.APPLICATION_OCTET_STREAM));*/
			
			if (replyB.getStatusInfo().equals(Status.OK)) {
				albumID = replyB.readEntity(String.class);
				System.out.println("created album successfully " + albumID);
				done = true;
			}
			// else try again
		}
		return albumID;
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
		System.out.println("Delete album");
		
		if(!proxies.isEmpty()){
			Random r = new Random();
			boolean d = imgurDeleteAlbum(proxies.get(r.nextInt(proxies.size())), album.getName());
			if(!d)
				System.out.println("something went wrong");
			}
		
		for (String serverUrl : servers) {
			try {
				if (serverUrl.charAt(0) == (SOAP))
					soapDeleteAlbum(serverUrl.substring(1), album.getName());
				else if (serverUrl.charAt(0) == REST)
					restDeleteAlbum(serverUrl.substring(1), album.getName());
				break;
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Server currently unavailable.");
			}
		}
	}

	private boolean imgurDeleteAlbum(String proxyUrl, String album) {
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {

			WebTarget target = client.target(getBaseURI(proxyUrl,IMGURPORT));
			// extrair id do albumName
			String albumID = extractID(album);
			Response replyB = target.path("RESTProxy/deleteAlbum/" + albumID).request()
					.delete();
			System.out.println(replyB.getStatusInfo());
			if (replyB.getStatusInfo().equals(Status.OK)) {
				done = true;
			}
			// else try again
		}
		return done;
	}

	private void restDeleteAlbum(String serverUrl, String album) {
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(serverUrl,RESTPORT));
			Response replyB = target.path("RESTServer/deleteAlbum/" + album).request().delete();
			if (replyB.getStatusInfo().equals(Status.OK)) {
				done = true;
			}
			// else try again
		}
	}

	private void soapDeleteAlbum(String serverUrl, String album)
			throws MalformedURLException, InfoNotFoundException_Exception {

		System.out.println(serverUrl + " deleteAlbum\n");
		URL wsURL = new URL(String.format("%s", serverUrl));
		FileServerImplWSService service = new FileServerImplWSService(wsURL);
		FileServerImplWS server = service.getFileServerImplWSPort();
		try {
			server.deleteAlbum(album);
			System.out.println("Deleted");
		} catch (Exception e) {
			// call method again, max 3 times
			System.out.println("rip?");
			boolean executed = false;
			for (int i = 0; !executed && i < 3; i++) { // number of
														// tries
				try {
					server.deleteAlbum(album);
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
		System.out.println("upload Picture");

		int i = 0;
		int times = 0;
		Picture pic = null;
		if(!proxies.isEmpty()){
			Random r = new Random();
			pic = imgurUploadPic(proxies.get(r.nextInt(proxies.size())),album.getName(), name,data);
			}
		try {
			if(servers.size()>0){
			boolean finished = false;
			final int[] serverIndexes = new Random().ints(0, servers.size()).distinct().limit(servers.size()).toArray();
			while (!finished && i < serverIndexes.length && serverIndexes[i] < servers.size()) {
				try {
					String serverUrl = servers.get(serverIndexes[i]);
					i++;
					if (serverUrl.charAt(0) == (SOAP))
						pic = soapUploadPic(serverUrl.substring(1), album.getName(), name, data);
					else if (serverUrl.charAt(0) == REST)
						pic = restUploadPic(serverUrl.substring(1), album.getName(), name, data);
					finished = true;
				} catch (Exception e) {

					if (i >= servers.size()) {
						if (times < 2) {
							i = 0;
							times++;
						} else
							break;
					}
				}
			}

			}
		} catch (IllegalArgumentException e) {
			System.out.println("No servers connected right now");
		}

		return pic;
	}

	private Picture imgurUploadPic(String proxyUrl, String album, String picName, byte[] data) {
		boolean done = false;
		String name = picName;
		for (int j = 0; j < 3 && !done; j++) {

			WebTarget target = client.target(getBaseURI(proxyUrl,IMGURPORT));

			/*Response replyB = target.path("RESTProxy/uploadPicture/" + extractID(album) + "/" + picName).request()
					.post(Entity.entity(data, MediaType.APPLICATION_OCTET_STREAM));
*/
			Builder reply = target.path("RESTProxy/uploadPicture/" + extractID(album) + "/" + picName).request()					
					.accept(MediaType.APPLICATION_OCTET_STREAM);

			Response replyB = reply.post(Entity.entity(data, MediaType.APPLICATION_OCTET_STREAM));
			System.out.println(replyB.getStatusInfo());
			if (replyB.getStatusInfo().equals(Status.OK)) {
				name = replyB.readEntity(String.class);
				done = true;

			}
			// else try again
			System.out.println("done " + done);
		}
		if (done)
			return new SharedPicture(name);
		return null;
	}

	private Picture restUploadPic(String serverUrl, String album, String name, byte[] data) {

		boolean done = false;
		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(serverUrl,RESTPORT));

			Response replyB = target.path("RESTServer/uploadPicture/" + album + "/" + name).request()
					.post(Entity.entity(data, MediaType.APPLICATION_OCTET_STREAM));
			System.out.println(replyB.getStatusInfo());
			if (replyB.getStatusInfo().equals(Status.OK)) {
				done = true;

			}
			// else try again
		}
		if (!done)
			return null;
		return new SharedPicture(name);

	}

	private Picture soapUploadPic(String serverUrl, String album, String name, byte[] data)
			throws IOException_Exception, InfoNotFoundException_Exception, MalformedURLException {
		boolean finished = false;

		System.out.println(serverUrl + " uploadPicture\n");

		URL wsURL = new URL(String.format("%s", serverUrl));

		FileServerImplWSService service = new FileServerImplWSService(wsURL);
		FileServerImplWS server = service.getFileServerImplWSPort();
		try {
			server.uploadPicture(album + File.separator + name, data);
			finished = true;
		} catch (PictureExistsException_Exception e) {
			return null;
		} catch (Exception e) {
			// call method again, max 3 times
			for (int j = 0; !finished && j < 3; j++) { // number of
														// tries
				try {
					server.uploadPicture(album + File.separator + name, data);
					finished = true;
				} catch (PictureExistsException_Exception e2) {
					return null;
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
		System.out.println("finished uploading picture " + finished);
		if (!finished)
			return null;
		return new SharedPicture(name);
	}

	/**
	 * Delete a picture from an album. On error this method should return false.
	 */
	@Override
	public boolean deletePicture(Album album, Picture picture) {
		System.out.println("Delete picture");
		boolean finished = false;

		if(!proxies.isEmpty()){
			Random r = new Random();
			finished = imgurDeletePic(proxies.get(r.nextInt(proxies.size())),picture.getName());
			
			}
		for (String serverUrl : servers) {
			try {
				if (serverUrl.charAt(0) == (SOAP))
					finished = soapDeletePic(serverUrl.substring(1), album.getName(), picture.getName());
				else if (serverUrl.charAt(0) == REST)
					finished = restDeletePic(serverUrl.substring(1), album.getName(), picture.getName());
				break;
			} catch (Exception e) {
				e.printStackTrace();

				System.out.println("Server currently unavailable.");
			}
		}
		return finished;
	}

	private String imgurGetAlbId(String proxyUrl, String albumName){
		boolean done = false;
		String albumId = null;

		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(proxyUrl,IMGURPORT));

			Builder replyB = target.path("RESTProxy/getAlbumId/" + albumName).request().accept(MediaType.APPLICATION_JSON);

			Response reply = replyB.get();

			if (reply.getStatusInfo().equals(Status.OK)) {
				albumId = replyB.get(String.class);

				done = true;

			}
			// else try again
		}
		return albumId;
	}
	
	private String imgurGetPicId(String proxyUrl, String albumName, String pictureName){
		boolean done = false;
		String pictureId = "";

		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(proxyUrl,IMGURPORT));

			Builder replyB = target.path("RESTProxy/getPictureId/" + albumName + "/" + pictureName)
					.request().accept(MediaType.APPLICATION_JSON);

			Response reply = replyB.get();

			if (reply.getStatusInfo().equals(Status.OK)) {
				pictureId = replyB.get(String.class);

				done = true;

			}
			// else try again
		}
		return pictureId;
	}
	// working correctly, can delete and check the album has been deleted on imgur
	private boolean imgurDeletePic(String proxyUrl, String picture) {
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {

			WebTarget target = client.target(getBaseURI(proxyUrl,IMGURPORT));
			// extrair ID antes de enviar o pictureName
			String pictureID = extractID(picture);
			Response replyB = target.path("RESTProxy/deletePicture/" + pictureID).request().delete();
			if (replyB.getStatusInfo().equals(Status.OK)) {
				done = true;
			}
			// else try again
		}
		return done;
	}

	private boolean restDeletePic(String serverUrl, String album, String picture) {
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(serverUrl,RESTPORT));
			System.out.println(serverUrl);
			Response replyB = target.path("RESTServer/deletePicture/" + album + "/" + picture).request().delete();
			if (replyB.getStatusInfo().equals(Status.OK)) {
				done = true;
			}
			// else try again
			
		}
		return done;
	}

	private boolean soapDeletePic(String serverUrl, String album, String picture)
			throws MalformedURLException, InfoNotFoundException_Exception {
		boolean finished = false;
		URL wsURL = new URL(String.format("%s", serverUrl));
		FileServerImplWSService service = new FileServerImplWSService(wsURL);
		FileServerImplWS server = service.getFileServerImplWSPort();
		try {
			server.deletePicture(album, picture);
			finished = true;

		} catch (Exception e) {
			// call method again, max 3 times
			for (int i = 0; !finished && i < 3; i++) {
				try {
					server.deletePicture(album, picture);
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
	
	

	// syncing methods
	private void syncServerWithProxy(String proxyUrl, String serverUrl)
			throws MalformedURLException, InfoNotFoundException_Exception, IOException_Exception {
		boolean restServer = serverUrl.charAt(0) == REST ? true : false;
		serverUrl = serverUrl.substring(1);
		System.out.println("Syncing with proxy");
		List<String> albProxy = imgurListOfAlbums(proxyUrl);
		List<String> albServer = restServer ? restListOfAlbums(serverUrl) : soapListOfAlbums(serverUrl);

		
		

		for (String album : albProxy) {
			if (!albServer.contains(album)) {
				// check serverlogs
				Date lastModifiedServerAlbum = restServer ? restGetAlbumLogs(serverUrl, album)
						: soapGetAlbumLogs(serverUrl, album);
				if (lastModifiedServerAlbum.after(imgurLastModifiedAlbum(proxyUrl, album))) {
					imgurDeleteAlbum(proxyUrl, album);
				} else {
					String albName = album;
					if(!album.contains("\\."))
							albName = imgurGetAlbId(proxyUrl,album)+"." +album;
					
					if (restServer)
						restCreateAlbum(serverUrl, albName);
					else
						soapCreateAlbum(serverUrl, albName);
					for (String pic : imgurListOfPictures(proxyUrl, album)) {
						String picId = pic;
						if(!picId.contains("\\."))
							picId = imgurGetPicId(proxyUrl,album,pic);
						if (restServer)
							restUploadPic(serverUrl, albName, picId+"." +pic, imgurGetPicData(proxyUrl, picId+"."+pic));
						else
							soapUploadPic(serverUrl, albName, picId+"." +pic, imgurGetPicData(proxyUrl,picId+"."+pic));
					}
				}
			}
		}

		for (String album : albServer) {
			if (!albProxy.contains(album)) {
				// check serverlogs
				Date lastModifiedServerAlbum = restServer ? restGetAlbumLogs(serverUrl, album)
						: soapGetAlbumLogs(serverUrl, album);
				if (lastModifiedServerAlbum.after(imgurLastModifiedAlbum(proxyUrl, album))) {
					imgurCreateAlbum(proxyUrl, album);
					for (String pic : restListOfPictures(serverUrl, album)) {
						imgurUploadPic(proxyUrl, album, pic, restGetPicData(serverUrl, album, pic));
					}
				} else {
					if (restServer)
						restDeleteAlbum(serverUrl, album);
					else
						soapDeleteAlbum(serverUrl, album);
				}
			}
		}

		// both lists contain the same
		for (String album : albServer) {
			 
			List<String> picProxy = imgurListOfPictures(proxyUrl, album);
			

			List<String> picServer = new ArrayList<String>();
			if (restServer) {
				for (String pic : restListOfPictures(serverUrl, album))
					picServer.add(pic);
			} else {
				for (String pic : soapListOfPictures(serverUrl, album))
					picServer.add(pic);
			}
			for (String pic : picServer) {
				// check pick logs
				if (!picProxy.contains(pic)) {
					Date lastModifiedServerPic = restServer ? restGetPicLogs(serverUrl, album,pic)
							: soapGetPicLogs(serverUrl, album,pic);
					if (lastModifiedServerPic.after(imgurLastModifiedPicture(proxyUrl, pic))) {
						imgurUploadPic(proxyUrl, album, pic, restGetPicData(serverUrl, album, pic));
					} else {
						if (restServer)
							restDeletePic(serverUrl, album, pic);
						else
							soapDeletePic(serverUrl, album, pic);
					}
				}
			}
			for (String pic : picProxy) {
				if (!picServer.contains(pic)) {
					Date lastModifiedServerPic = restServer ? restGetPicLogs(serverUrl, album,pic)
							: soapGetPicLogs(serverUrl, album,pic);
					if (lastModifiedServerPic.after(imgurLastModifiedPicture(proxyUrl, pic))) {
						imgurDeletePic(proxyUrl, pic);
					} else {
						String picId = pic;
						if(!pic.contains("\\."))
							picId = imgurGetPicId(proxyUrl,album,pic)+"."+picId;
						if (restServer)
							restUploadPic(serverUrl, album, picId, imgurGetPicData(proxyUrl, picId));
						else
							soapUploadPic(serverUrl, album, picId, imgurGetPicData(proxyUrl, picId));
					}
				}
			}
		}
	}

	

	private void syncServerWithServer(String serverAUrl, String serverBUrl)
			throws MalformedURLException, InfoNotFoundException_Exception, IOException_Exception {
		boolean serverBRest = serverBUrl.charAt(0) == REST ? true : false;
		boolean serverARest = serverAUrl.charAt(0) == REST ? true : false;

		serverBUrl = serverBUrl.substring(1);
		serverAUrl = serverAUrl.substring(1);



		List<String> albServerA = serverARest ? restListOfAlbums(serverAUrl) : soapListOfAlbums(serverAUrl);
		List<String> albServerB = serverBRest ? restListOfAlbums(serverBUrl) : soapListOfAlbums(serverBUrl);
		

		for (String album : albServerA) {
			if (!albServerB.contains(album)) {
				// check serverlogs
				Date lastModifiedServerAlbumA = serverARest ? restGetAlbumLogs(serverAUrl, album)
						: soapGetAlbumLogs(serverAUrl, album);
				Date lastModifiedServerAlbumB = serverBRest ? restGetAlbumLogs(serverBUrl, album)
						: soapGetAlbumLogs(serverBUrl, album);
				if (lastModifiedServerAlbumB.after(lastModifiedServerAlbumA)) {
					if (serverARest)
						restDeleteAlbum(serverAUrl, album);
					else
						soapDeleteAlbum(serverAUrl, album);

				} else {
					if (serverBRest)
						restCreateAlbum(serverBUrl, album);
					else
						soapCreateAlbum(serverBUrl, album);
					if (serverARest) {
						for (String pic : restListOfPictures(serverAUrl, album)) {
							if (serverBRest)
								restUploadPic(serverBUrl, album, pic,
										restGetPicData(serverAUrl, album, pic));
							else
								soapUploadPic(serverBUrl, album, pic,
										restGetPicData(serverAUrl, album, pic));
						}
					} else {
						for (String pic : soapListOfPictures(serverAUrl, album)) {
							if (serverBRest)
								restUploadPic(serverBUrl, album, pic,
										soapGetPicData(serverAUrl, album, pic));
							else
								soapUploadPic(serverBUrl, album, pic,
										soapGetPicData(serverAUrl, album, pic));
						}

					}
				}
			}
		}

		for (String album : albServerB) {
			if (!albServerA.contains(album)) {
				// check serverlogs
				Date lastModifiedServerAlbumA = serverARest ? restGetAlbumLogs(serverAUrl, album)
						: soapGetAlbumLogs(serverAUrl, album);
				Date lastModifiedServerAlbumB = serverBRest ? restGetAlbumLogs(serverBUrl, album)
						: soapGetAlbumLogs(serverBUrl, album);
				if (lastModifiedServerAlbumA.after(lastModifiedServerAlbumB)) {
					if (serverBRest)
						restDeleteAlbum(serverBUrl, album);
					else
						soapDeleteAlbum(serverBUrl, album);

				} else {
					if (serverARest)
						restCreateAlbum(serverAUrl, album);
					else
						soapCreateAlbum(serverAUrl, album);
					if (serverBRest) {
						for (String pic : restListOfPictures(serverBUrl, album)) {
							if (serverARest)
								restUploadPic(serverAUrl, album, pic,
										restGetPicData(serverBUrl, album, pic));
							else
								soapUploadPic(serverAUrl, album, pic,
										restGetPicData(serverBUrl, album, pic));
						}
					} else {
						for (String pic : soapListOfPictures(serverBUrl, album)) {
							if (serverARest)
								restUploadPic(serverAUrl, album, pic,
										soapGetPicData(serverBUrl, album, pic));
							else
								soapUploadPic(serverAUrl, album, pic,
										soapGetPicData(serverBUrl, album, pic));
						}

					}
				}
			}
		}

		// check if any picture is missing from an album
		for (String album : albServerB) {
			List<String> picServerA = new ArrayList<String>();
			if (serverARest) {
				for (String pic : restListOfPictures(serverAUrl, album))
					picServerA.add(pic);
			} else {
				for (String pic : soapListOfPictures(serverAUrl, album))
					picServerA.add(pic);
			}

			List<String> picServerB = new ArrayList<String>();
			if (serverBRest) {
				for (String pic : restListOfPictures(serverBUrl, album))
					picServerB.add(pic);
			} else {
				for (String pic : soapListOfPictures(serverBUrl, album))
					picServerB.add(pic);
			}
			for (String pic : picServerB) {
				// check pick logs
				if (!picServerA.contains(pic)) {
					Date lastModifiedServerPicB = serverBRest ? restGetPicLogs(serverBUrl, album,pic)
							: soapGetPicLogs(serverBUrl, album,pic);
					Date lastModifiedServerPicA = serverARest ? restGetPicLogs(serverAUrl, album,pic)
							: soapGetPicLogs(serverAUrl, album,pic);
					if (lastModifiedServerPicB.after(lastModifiedServerPicA)) {
						if(serverBRest){
						if (serverARest)
							restUploadPic(serverAUrl, album, pic, restGetPicData(serverBUrl, album, pic));
						else
							soapUploadPic(serverAUrl, album, pic, restGetPicData(serverBUrl, album, pic));
						}else{
							if (serverARest)
								restUploadPic(serverAUrl, album, pic, soapGetPicData(serverBUrl, album, pic));
							else
								soapUploadPic(serverAUrl, album, pic, soapGetPicData(serverBUrl, album, pic));
						}
					} else {
						if (serverBRest)
							restDeletePic(serverBUrl, album, pic);
						else
							soapDeletePic(serverBUrl, album, pic);
					}
				}
			}
			for (String pic : picServerA) {
				if (!picServerB.contains(pic)) {
					Date lastModifiedServerPicB = serverBRest ? restGetPicLogs(serverBUrl, album,pic)
							: soapGetPicLogs(serverBUrl, album,pic);
					Date lastModifiedServerPicA = serverARest ? restGetPicLogs(serverAUrl, album,pic)
							: soapGetPicLogs(serverAUrl, album,pic);
					if (lastModifiedServerPicB.after(lastModifiedServerPicA)) {
						if (serverARest)
							restDeletePic(serverAUrl, album, pic);
						else
							soapDeletePic(serverAUrl, album, pic);
					} else {
						if (serverARest) {
							if (serverBRest)
								restUploadPic(serverBUrl, album, pic, restGetPicData(serverAUrl, album, pic));
							else
								soapUploadPic(serverBUrl, album, pic, restGetPicData(serverAUrl, album, pic));
						} else {
							if (serverBRest)
								restUploadPic(serverBUrl, album, pic, soapGetPicData(serverAUrl, album, pic));
							else
								soapUploadPic(serverBUrl, album, pic, soapGetPicData(serverAUrl, album, pic));
						}

					}
				}
			}
		}
	}

	private Date soapGetAlbumLogs(String serverUrl, String album) {
		String albTime = "";
		try {
			System.out.println(serverUrl + " downloadPicture\n");
			URL wsURL = new URL(String.format("%s", serverUrl));
			FileServerImplWSService service = new FileServerImplWSService(wsURL);
			FileServerImplWS server = service.getFileServerImplWSPort();
			try {
				albTime = server.getAlbumLastModified(album);
			} catch (Exception e) {
				// call method again, max 3 times
				for (int i = 0; i < 3; i++) { // number of tries
					try {
						albTime = server.getAlbumLastModified(album);
						
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
		} catch (Exception e) {
			e.printStackTrace();
	
		}
		if(albTime.equals(""))
			return new Date(100L);
		return new Date(Long.parseLong(albTime));
	}

	private Date soapGetPicLogs(String serverUrl, String album, String picture) {
		String picTime = "";
		try {
			System.out.println(serverUrl + " downloadPicture\n");
			URL wsURL = new URL(String.format("%s", serverUrl));
			FileServerImplWSService service = new FileServerImplWSService(wsURL);
			FileServerImplWS server = service.getFileServerImplWSPort();
			try {
				picTime = server.picLogs(album, picture);
			} catch (Exception e) {
				// call method again, max 3 times
				for (int i = 0; i < 3; i++) { // number of tries
					try {
						picTime = server.picLogs(album, picture);
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
		} catch (Exception e) {
			e.printStackTrace();
			// System.out.println("Server " + serverUrl + " may be down, client
			// will remove"
			// + "it withing 6 minutes if it does not show signs of life");
		}
		if(picTime.equals(""))
			return new Date(100L);
		return new Date(Long.parseLong(picTime));
	}

	private Date imgurLastModifiedAlbum(String proxyUrl, String album) {
		boolean done = false;
		String lastMod = "";

		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(proxyUrl,RESTPORT));

			System.out.println(proxyUrl);
			Builder replyB = target.path("RESTProxy/albumLastModified/"  + extractID(album)).request()
					.accept(MediaType.APPLICATION_JSON);

			Response reply = replyB.get();

			if (reply.getStatusInfo().equals(Status.OK)) {
				lastMod = replyB.get(String.class);
				done = true;
			}
			// else try again
		}
		if(lastMod.equals(""))
			return new Date(100L);
		return new Date(Long.parseLong(lastMod.split("\\.")[1]));
	}
	private Date imgurLastModifiedPicture(String proxyUrl, String pic) {
		boolean done = false;
		String lastMod = "";

		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(proxyUrl,RESTPORT));

			System.out.println(proxyUrl);
			Builder replyB = target.path("RESTProxy/picLogs/"  + pic).request()
					.accept(MediaType.APPLICATION_JSON);

			Response reply = replyB.get();

			if (reply.getStatusInfo().equals(Status.OK)) {
				lastMod = replyB.get(String.class);
				done = true;
			}
			// else try again
		}
		if(lastMod.equals(""))
			return new Date(100L);
		return new Date(Long.parseLong(lastMod.split("\\.")[1]));
	}

	private Date restGetPicLogs(String serverUrl, String album, String picture) {
		boolean done = false;
		String lastMod = "";

		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(serverUrl,RESTPORT));

			System.out.println(serverUrl);
			Builder replyB = target.path("RESTServer/picLogs/" + album + "/" + picture).request()
					.accept(MediaType.APPLICATION_JSON);

			Response reply = replyB.get();

			if (reply.getStatusInfo().equals(Status.OK)) {
				lastMod = replyB.get(String.class);
				done = true;
			}
			// else try again
		}
		if(lastMod.equals(""))
			return new Date(100L);
		return new Date(Long.parseLong(lastMod.split("\\.")[1]));
	}

	private Date restGetAlbumLogs(String serverUrl, String album) {
		boolean done = false;
		String lastMod = "";

		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(serverUrl,RESTPORT));

			System.out.println(serverUrl);
			Builder replyB = target.path("RESTServer/albumLastModified/" + album).request()
					.accept(MediaType.APPLICATION_JSON);

			Response reply = replyB.get();

			if (reply.getStatusInfo().equals(Status.OK)) {
				lastMod = replyB.get(String.class);
				done = true;
			}
			// else try again
		}
		if(lastMod.equals(""))
			return new Date(100L);
		return new Date(Long.parseLong(lastMod.split("\\.")[1]));
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
