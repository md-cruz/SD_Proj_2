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
					byte[] received = new byte[65536];
					DatagramPacket receivedPacket = new DatagramPacket(received, received.length);
					boolean foundAllServers = false;
					try {
						while (!foundAllServers) {

							socket.setSoTimeout(60000);

							socket.receive(receivedPacket);

							String serverHost = new String(receivedPacket.getData()).trim();
							consecutiveReplies.put(serverHost, consecutiveReplies.getOrDefault(serverHost, 1) + 1);
							// getOrDefault returns the current value for the
							// key
							// or 1 if the key has no value yet
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
				// sleep for one hour
				Thread.sleep(3600000);
				int numberOfProxies = proxies.size();
				int numberOfServers = servers.size();
				if(numberOfServers > 1){
					Random r = new Random();
					int server1 = r.nextInt(numberOfServers);
					int server2 = r.nextInt(numberOfServers);
					while(server1==server2)
						server2=r.nextInt(numberOfServers);
					
					syncServerWithServer(servers.get(server1),servers.get(server2));
				}
				if(numberOfServers > 0 && numberOfProxies > 0){
					Random r = new Random();
					int server = r.nextInt(numberOfServers);
					int proxy = r.nextInt(numberOfProxies);
					syncServerWithProxy(proxies.get(proxy),servers.get(server));
				}
				// TODO: perguntar como fazer para acordar a thread quando um
				// novo servidor e adicionado e assim que existe um proxy ligado
				// perguntar tambem se e possivel esta thread parar as outras threads
				// momentaneamente
				
				
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
					byte[] received = new byte[65536];
					DatagramPacket receivedPacket = new DatagramPacket(received, received.length);
					boolean foundAllServers = false;
					try {
						while (!foundAllServers) {

							socket.setSoTimeout(60000);

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
		System.out.println("Get List of Albums");

		for (String serverUrl : servers) {
			try {
				if (serverUrl.charAt(0) == (SOAP))
					lst = (soapListOfAlbums(serverUrl.substring(1)));
				else if (serverUrl.charAt(0) == REST)
					lst = (restListOfAlbums(serverUrl.substring(1)));
				
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
			Set<Album> albSet = new HashSet<Album>(imgurListOfAlbums(proxyUrl));
			if (!lst.containsAll(albSet))
				lst.addAll(albSet);
		}

		return lst;
	}


	
	private List<Album> imgurListOfAlbums(String proxyUrl) {
		boolean done = false;
		List<Album> lst = new ArrayList<Album>();
		for (int j = 0; j < 3 && !done; j++) {

			WebTarget target = client.target(getBaseURI(proxyUrl,IMGURPORT));

			String[] albumNames;
			Builder replyB = target.path("RESTProxy/getAlbumList/")
					.request().accept(MediaType.APPLICATION_JSON);

			Response reply = replyB.get();
			System.out.println(reply.getStatusInfo());
			if (reply.getStatusInfo().equals(Status.OK)) {
				albumNames = replyB.get(String[].class);
				for (int i = 0; i < albumNames.length; i++) {
					SharedAlbum alb = new SharedAlbum(albumNames[i]);
					if (!lst.contains(alb) && !alb.getName().endsWith(".deleted"))
						lst.add(alb);
				}
				done = true;

			}
		}
		return lst;
	}

	private List<Album> restListOfAlbums(String serverUrl) {
		List<Album> lst = new ArrayList<Album>();
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(serverUrl,RESTPORT));

			String[] albumNames;
			System.out.println(serverUrl);
			Builder replyB = target.path("RESTServer/getAlbumList/").request().accept(MediaType.APPLICATION_JSON);

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

	private List<Album> soapListOfAlbums(String serverUrl)
			throws InfoNotFoundException_Exception, MalformedURLException {
		List<Album> lst = new ArrayList<Album>();

		System.out.println(serverUrl + " listAlbum\n");
		URL wsURL = new URL(String.format("%s", serverUrl));
		FileServerImplWSService service = new FileServerImplWSService(wsURL);
		FileServerImplWS server = service.getFileServerImplWSPort();
		try {
			List<String> aList = server.getAlbumList();
			for (String album : aList) {
				SharedAlbum alb = new SharedAlbum(album);
				if (!lst.contains(alb) && !album.endsWith(".deleted"))
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
		System.out.println("Get List of Pictures");
		List<Picture> lst = new ArrayList<Picture>();
		List<Picture> tmp = new ArrayList<Picture>();

		for (String serverUrl : servers) {
			try {
				String url = serverUrl.substring(1);
				if (serverUrl.charAt(0) == (SOAP))
					tmp = (soapListOfPictures(url, album.getName()));
				else if (serverUrl.charAt(0) == REST)
					tmp = (restListOfPictures(url, album.getName()));
				for (Picture p : tmp)
					if (!lst.contains(p))
						lst.add(p);
			} catch (Exception e) {
				e.printStackTrace();

				System.out.println("Server currently unavailable.");
			}
		}
		if(proxies.size()>0){
		Random r = new Random();
		String proxyUrl = proxies.get(r.nextInt(proxies.size()));
		Set<Picture> albSet = new HashSet<Picture>(imgurListOfPictures(proxyUrl,extractID(album.getName())));
		if(!lst.containsAll(albSet))
			lst.addAll(albSet);
		}

		return lst;
	}

	private List<Picture> restListOfPictures(String serverUrl, String album) {
		List<Picture> lst = new ArrayList<Picture>();
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

	private List<Picture> imgurListOfPictures(String proxyUrl, String album) {
		List<Picture> lst = new ArrayList<Picture>();
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
				for (int i = 0; i < pictureNames.length; i++) {
					SharedPicture pic = new SharedPicture(pictureNames[i]);
					if (!lst.contains(pic) && !pic.getName().endsWith(".deleted"))
						lst.add(pic);
					done = true;
				}

			}
			// else try again
		}
		return lst;
	}

	private List<Picture> soapListOfPictures(String serverUrl, String album) {
		List<Picture> lst = new ArrayList<Picture>();

		try {
			System.out.println(serverUrl + " listPicture\n");
			URL wsURL = new URL(String.format("%s", serverUrl));
			FileServerImplWSService service = new FileServerImplWSService(wsURL);
			FileServerImplWS server = service.getFileServerImplWSPort();
			try {
				List<String> picList = server.getPictureList(album);
				for (String pic : picList) {
					SharedPicture picture = new SharedPicture(pic);
					if (!lst.contains(picture) && !pic.endsWith(".deleted"))
						lst.add(picture);
				}
			} catch (Exception e) {
				// call method again, max 3 times
				boolean executed = false;
				for (int i = 0; !executed && i < 3; i++) { // number of
															// tries
					try {
						List<String> picList = server.getPictureList(album);
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

	private byte[] soapGetPicData(String serverUrl, String album, String picture) {
		byte[] pictureData = null;
		try {
			System.out.println(serverUrl + " downloadPicture\n");
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
		String id =null;
		if(!proxies.isEmpty()){
		Random r = new Random();
		imgurCreateAlbum(proxies.get(r.nextInt(proxies.size())), name);
		id = imgurGetAlbId(proxies.get(r.nextInt(proxies.size())), name);
		}
		
		try {
			if(servers.size()>0){
			final int[] serverIndexes = new Random().ints(0, servers.size()).distinct().limit(servers.size()).toArray();

			boolean finished = false;

			while (!finished && i < serverIndexes.length && serverIndexes[i] < servers.size()) {
				try {
					String serverUrl = servers.get(serverIndexes[i]);
					i++;
					if (serverUrl.charAt(0) == (SOAP))
						album = soapCreateAlbum(serverUrl.substring(1), id+name);
					else if (serverUrl.charAt(0) == REST)
						album = restCreateAlbum(serverUrl.substring(1), id+name);
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
		if(album == null && id!=null)
			album = new SharedAlbum(id+"." +name);
		return album;
	}

	private Album restCreateAlbum(String serverUrl, String name) {
		Album alb = new SharedAlbum(name);
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {
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

			WebTarget target = client.target(getBaseURI(proxyUrl,IMGURPORT));
			/*Builder reply = target.path("RESTProxy/createNewAlbum/").request()
					.accept(MediaType.APPLICATION_OCTET_STREAM);

			Response replyB = reply.post(Entity.entity(albumName, MediaType.APPLICATION_OCTET_STREAM));
*/				
			Response replyB = target.path("RESTProxy/createNewAlbum/").request()
					.post(Entity.entity(albumName, MediaType.APPLICATION_OCTET_STREAM));
			if (replyB.getStatusInfo().equals(Status.OK)) {
				//albumID = reply.get(String.class);
				System.out.println("created album successfully");
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
			Picture d = imgurUploadPic(proxies.get(r.nextInt(proxies.size())),album.getName(), name,data);
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
		for (int j = 0; j < 3 && !done; j++) {

			WebTarget target = client.target(getBaseURI(proxyUrl,IMGURPORT));

			Response replyB = target.path("RESTProxy/uploadPicture/" + extractID(album) + "/" + picName).request()
					.post(Entity.entity(data, MediaType.APPLICATION_OCTET_STREAM));

			if (replyB.getStatusInfo().equals(Status.OK)) {
				done = true;

			}
			// else try again
		}
		if (done)
			return new SharedPicture(picName);
		return null;
	}

	private Picture restUploadPic(String serverUrl, String album, String name, byte[] data) {

		boolean done = false;
		for (int j = 0; j < 3 && !done; j++) {
			WebTarget target = client.target(getBaseURI(serverUrl,RESTPORT));
			System.out.println("hi");

			Response replyB = target.path("RESTServer/uploadPicture/" + album + "/" + name).request()
					.post(Entity.entity(data, MediaType.APPLICATION_OCTET_STREAM));

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
			boolean d = imgurDeletePic(proxies.get(r.nextInt(proxies.size())),picture.getName());
			if(!d)
				System.out.println("Something went wrong");
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
		String pictureId = null;

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
		List<Album> tmpProxy = imgurListOfAlbums(proxyUrl);
		List<Album> tmpServer = restServer ? restListOfAlbums(serverUrl) : soapListOfAlbums(serverUrl);

		List<String> albProxy = new ArrayList<String>();
		List<String> albServer = new ArrayList<String>();
		for (Album alb : tmpProxy)
			albProxy.add(alb.getName());
		for (Album alb : tmpServer)
			albServer.add(alb.getName());

		for (String album : albProxy) {
			if (!albServer.contains(album)) {
				// check serverlogs
				Date lastModifiedServerAlbum = restServer ? restGetAlbumLogs(serverUrl, album)
						: soapGetAlbumLogs(serverUrl, album);
				if (lastModifiedServerAlbum.after(imgurLastModifiedID(proxyUrl, album))) {
					imgurDeleteAlbum(proxyUrl, album);
				} else {
					String albName = imgurGetAlbId(proxyUrl,album)+"." +album;
					
					if (restServer)
						restCreateAlbum(serverUrl, albName);
					else
						soapCreateAlbum(serverUrl, albName);
					for (Picture pic : imgurListOfPictures(proxyUrl, album)) {
						String picId = imgurGetPicId(proxyUrl,album,pic.getName());
						if (restServer)
							restUploadPic(serverUrl, albName, picId+"." +pic.getName(), imgurGetPicData(proxyUrl, picId+pic.getName()));
						else
							soapUploadPic(serverUrl, albName, picId+"." +pic.getName(), imgurGetPicData(proxyUrl,picId+pic.getName()));
					}
				}
			}
		}

		for (String album : albServer) {
			if (!albProxy.contains(album)) {
				// check serverlogs
				Date lastModifiedServerAlbum = restServer ? restGetAlbumLogs(serverUrl, album)
						: soapGetAlbumLogs(serverUrl, album);
				if (lastModifiedServerAlbum.after(imgurLastModifiedID(proxyUrl, album))) {
					imgurCreateAlbum(proxyUrl, album);
					for (Picture pic : restListOfPictures(serverUrl, album)) {
						imgurUploadPic(proxyUrl, album, pic.getName(), restGetPicData(serverUrl, album, pic.getName()));
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
			List<Picture> tmp1Proxy = imgurListOfPictures(proxyUrl, album);
			List<String> picProxy = new ArrayList<String>();
			for (Picture p : tmp1Proxy)
				picProxy.add(p.getName());

			List<String> picServer = new ArrayList<String>();
			if (restServer) {
				for (Picture pic : restListOfPictures(serverUrl, album))
					picServer.add(pic.getName());
			} else {
				for (Picture pic : soapListOfPictures(serverUrl, album))
					picServer.add(pic.getName());
			}
			for (String pic : picServer) {
				// check pick logs
				if (!picProxy.contains(pic)) {
					Date lastModifiedServerPic = restServer ? restGetAlbumLogs(serverUrl, album)
							: soapGetAlbumLogs(serverUrl, album);
					if (lastModifiedServerPic.after(imgurLastModifiedID(proxyUrl, pic))) {
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
					Date lastModifiedServerPic = restServer ? restGetAlbumLogs(serverUrl, album)
							: soapGetAlbumLogs(serverUrl, album);
					if (lastModifiedServerPic.after(imgurLastModifiedID(proxyUrl, pic))) {
						imgurDeletePic(proxyUrl, pic);
					} else {
						String picId = imgurGetPicId(proxyUrl,album,pic);
						if (restServer)
							restUploadPic(serverUrl, album, picId+"." +pic, imgurGetPicData(proxyUrl, picId+pic));
						else
							soapUploadPic(serverUrl, album, picId+"." +pic, imgurGetPicData(proxyUrl, picId+pic));
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

		List<Album> tmpProxy = serverARest ? restListOfAlbums(serverAUrl) : soapListOfAlbums(serverAUrl);
		List<Album> tmpServer = serverBRest ? restListOfAlbums(serverBUrl) : soapListOfAlbums(serverBUrl);

		List<String> albServerA = new ArrayList<String>();
		List<String> albServerB = new ArrayList<String>();
		for (Album alb : tmpProxy)
			albServerA.add(alb.getName());
		for (Album alb : tmpServer)
			albServerB.add(alb.getName());

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
						for (Picture pic : restListOfPictures(serverAUrl, album)) {
							if (serverBRest)
								restUploadPic(serverBUrl, album, pic.getName(),
										restGetPicData(serverAUrl, album, pic.getName()));
							else
								soapUploadPic(serverBUrl, album, pic.getName(),
										restGetPicData(serverAUrl, album, pic.getName()));
						}
					} else {
						for (Picture pic : soapListOfPictures(serverAUrl, album)) {
							if (serverBRest)
								restUploadPic(serverBUrl, album, pic.getName(),
										soapGetPicData(serverAUrl, album, pic.getName()));
							else
								soapUploadPic(serverBUrl, album, pic.getName(),
										soapGetPicData(serverAUrl, album, pic.getName()));
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
						for (Picture pic : restListOfPictures(serverBUrl, album)) {
							if (serverARest)
								restUploadPic(serverAUrl, album, pic.getName(),
										restGetPicData(serverBUrl, album, pic.getName()));
							else
								soapUploadPic(serverAUrl, album, pic.getName(),
										restGetPicData(serverBUrl, album, pic.getName()));
						}
					} else {
						for (Picture pic : soapListOfPictures(serverBUrl, album)) {
							if (serverARest)
								restUploadPic(serverAUrl, album, pic.getName(),
										soapGetPicData(serverBUrl, album, pic.getName()));
							else
								soapUploadPic(serverAUrl, album, pic.getName(),
										soapGetPicData(serverBUrl, album, pic.getName()));
						}

					}
				}
			}
		}

		// check if any picture is missing from an album
		for (String album : albServerB) {
			List<String> picServerA = new ArrayList<String>();
			if (serverARest) {
				for (Picture pic : restListOfPictures(serverAUrl, album))
					picServerA.add(pic.getName());
			} else {
				for (Picture pic : soapListOfPictures(serverAUrl, album))
					picServerA.add(pic.getName());
			}

			List<String> picServerB = new ArrayList<String>();
			if (serverBRest) {
				for (Picture pic : restListOfPictures(serverBUrl, album))
					picServerB.add(pic.getName());
			} else {
				for (Picture pic : soapListOfPictures(serverBUrl, album))
					picServerB.add(pic.getName());
			}
			for (String pic : picServerB) {
				// check pick logs
				if (!picServerA.contains(pic)) {
					Date lastModifiedServerPicB = serverBRest ? restGetAlbumLogs(serverBUrl, album)
							: soapGetAlbumLogs(serverBUrl, album);
					Date lastModifiedServerPicA = serverARest ? restGetAlbumLogs(serverAUrl, album)
							: soapGetAlbumLogs(serverAUrl, album);
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
					Date lastModifiedServerPicB = serverBRest ? restGetAlbumLogs(serverBUrl, album)
							: soapGetAlbumLogs(serverBUrl, album);
					Date lastModifiedServerPicA = serverARest ? restGetAlbumLogs(serverAUrl, album)
							: soapGetAlbumLogs(serverAUrl, album);
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
				return new Date(Long.parseLong(albTime));
			} catch (Exception e) {
				// call method again, max 3 times
				for (int i = 0; i < 3; i++) { // number of tries
					try {
						albTime = server.getAlbumLastModified(album);
						return new Date(Long.parseLong(albTime));
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
		return null;
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
				return new Date(Long.parseLong(picTime));
			} catch (Exception e) {
				// call method again, max 3 times
				for (int i = 0; i < 3; i++) { // number of tries
					try {
						picTime = server.picLogs(album, picture);
						return new Date(Long.parseLong(picTime));
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
		return null;
	}

	private Date imgurLastModifiedID(String proxyUrl, String album) {
		// TODO Auto-generated method stub
		return null;
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
		return new Date(Long.parseLong(lastMod));
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
		return new Date(Long.parseLong(lastMod));
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
