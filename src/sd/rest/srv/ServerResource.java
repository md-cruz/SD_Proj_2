package sd.rest.srv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response.Status;

@Path("/RESTServer")
public class ServerResource {

	static File basePath;
	static Client client;
	static List<String> proxyServers;

	@GET
	@Path("getPictureList/{albumName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getPictureList(@PathParam("albumName") String albumName) {
		Set<String> dirPic = new HashSet<String>();
		File f = new File(basePath, albumName);
		if (f.exists() && f.isDirectory())
			dirPic.addAll(Arrays.asList(f.list()));
		for (String picName : dirPic)
			if (picName.endsWith(".deleted"))
				dirPic.remove(picName);

		Set<String> lst = downloadPicListImgur(albumName);
		if (!dirPic.isEmpty() || !lst.isEmpty()) {
			syncServerProxy(dirPic, lst);
			lst.addAll(dirPic);
			String[] pictureStringArr = new String[lst.size()];
			pictureStringArr = lst.toArray(pictureStringArr);
			return Response.ok(pictureStringArr).build();
		}

		return Response.status(Status.NOT_FOUND).build();
	}

	private Set<String> downloadPicListImgur(String albumName) {
		Set<String> lst = new HashSet<String>();
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {
			if (proxyServers == null)
				break;
			String proxyUrl = proxyServers.get(1);
			WebTarget target = client.target(getBaseURI(proxyUrl));

			String[] pictureNames;
			// extrair ID do album antes de fazer o request
			Builder replyB = target.path("RESTProxy/getPictureList/" + albumName).request()
					.accept(MediaType.APPLICATION_JSON);

			Response reply = replyB.get();

			if (reply.getStatusInfo().equals(Status.OK)) {
				pictureNames = replyB.get(String[].class);
				for (String pic : pictureNames)
					lst.add(pic);
				done = true;

			}
			// else try again
		}
		return lst;
	}

	// assumes missing entire album
	private void syncServerProxy(Set<String> dirPic, Set<String> lst) {
		// write to directory
		for (String s : lst) {
			if (!dirPic.contains(s)) {
				// create missing album
				File newAlbum = new File(basePath, s);
				boolean result = false;
				try {
					result = newAlbum.mkdirs();
				} catch (SecurityException e) {

				}
				if (result) {
					// add missing pictures
					Set<String> picList = downloadPicListImgur(s);
					for (String s1 : picList) {
						byte[] picData = downloadFromImgur(s1, s);
						FileOutputStream sOut;
						try {
							sOut = new FileOutputStream(newAlbum);

							sOut.write(picData);
							sOut.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		// upload to imgur
		for(String s : dirPic){
			if(!lst.contains(s)){
				// create missing album
				boolean success = createOnImgur(s);
				if(success){
					// TODO:upload missing pictures
				}
				
			}
		}
		
		
		
	}

	private URI getBaseURI(String serverUrl) {
		return UriBuilder.fromUri(serverUrl).port(9090).build();
	}

	@POST
	@Path("createNewAlbum")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public Response createNewAlbum(String albumName) {
		File newAlbum = new File(basePath, albumName);
		boolean result = false;
		try {
			result = newAlbum.mkdirs();
		} catch (SecurityException e) {

		}
		boolean done = createOnImgur(albumName);
		if (!done)
			Response.status(Status.NOT_FOUND).build();
		if (result && done)
			return Response.ok(true).build();
		return Response.status(Status.NOT_FOUND).build();

	}

	private boolean createOnImgur(String albumName) {
		boolean done = false;

		for (int j = 0; j < 3 && !done; j++) {
			if (proxyServers == null)
				break;
			String proxyUrl = proxyServers.get(1);
			WebTarget target = client.target(getBaseURI(proxyUrl));
			Response replyB = target.path("RESTProxy/createNewAlbum/").request()
					.post(Entity.entity(albumName, MediaType.APPLICATION_OCTET_STREAM));

			if (replyB.getStatusInfo().equals(Status.OK)) {
				done = true;
			}
			// else try again
		}
		return done;
	}

	@DELETE
	@Path("deleteAlbum/{albumName}")
	public Response deleteAlbum(@PathParam("albumName") String albumName) {

		File deletedAlbum = new File(basePath, albumName);
		if (deletedAlbum.exists() && deletedAlbum.isDirectory()) {
			File del = new File(deletedAlbum.getAbsolutePath() + ".deleted");
			if (del.exists() && del.isDirectory()) {
				copyData(deletedAlbum, del);
				deletedAlbum.delete();

			} else
				deletedAlbum.renameTo(del);
			boolean done = false;

			for (int j = 0; j < 3 && !done; j++) {
				if (proxyServers == null)
					break;
				String proxyUrl = proxyServers.get(1);
				WebTarget target = client.target(getBaseURI(proxyUrl));
				Response replyB = target.path("RESTServer/deleteAlbum/" + albumName).request().delete();
				if (replyB.getStatusInfo().equals(Status.OK)) {
					done = true;
				}
				// else try again
			}

			return Response.ok().build();
		}
		return Response.status(Status.NOT_FOUND).build();

	}

	private void copyData(File deletedPicture, File del) {
		try {
			for (File fileName : deletedPicture.listFiles()) {

				byte[] contents = Files.readAllBytes(fileName.toPath());

				FileOutputStream fis = new FileOutputStream(new File(del.getAbsolutePath(), fileName.getName()));
				fis.write(contents);
				fileName.delete();
				fis.close();
			}
		} catch (Exception e) {
			System.err.println("Error copying contents");
		}
	}

	@GET
	@Path("downloadPicture/{albumName}/{pictureName}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadPicture(@PathParam("albumName") String albumName,
			@PathParam("pictureName") String pictureName) {
		File pic = new File(basePath, albumName + File.separator + pictureName);
		if (pic.exists() && pic.isFile())
			try {
				return Response.ok(Files.readAllBytes(pic.toPath())).build();
			} catch (IOException e) {
				System.err.println("Error reading file.");
				return Response.status(Status.EXPECTATION_FAILED).build();
			}
		else {

			return Response.ok(downloadFromImgur(pictureName, albumName)).build();
		}
	}

	private byte[] downloadFromImgur(String pictureName, String albumName) {
		byte[] pictureData = null;
		boolean done = false;
		File pic = new File(basePath, albumName + File.separator + pictureName);

		for (int j = 0; j < 3 && !done; j++) {
			if (proxyServers == null)
				break;
			String proxyUrl = proxyServers.get(1);
			WebTarget target = client.target(getBaseURI(proxyUrl));
			// extrair ID da fotografia antes do request
			Builder replyB = target.path("RESTServer/downloadPicture/" + pictureName).request()
					.accept(MediaType.APPLICATION_OCTET_STREAM);
			Response reply = replyB.get();
			if (reply.getStatusInfo().equals(Status.OK)) {
				pictureData = replyB.get(byte[].class);
				done = true;

			}
			// else try again
		}
		if (done) {
			// create picture in local directory
			FileOutputStream sOut;
			try {

				sOut = new FileOutputStream(pic);
				sOut.write(pictureData);
				sOut.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return pictureData;
	}

	@GET
	@Path("/getAlbumList/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAlbumList() {
		try {
			File f = basePath;
			if (f.exists() && f.isDirectory()) {
				File[] albums = f.listFiles();
				List<String> albumsAsStrings = new ArrayList<String>();
				for (int i = 0; i < albums.length; i++)
					if (albums[i].isDirectory())
						albumsAsStrings.add(albums[i].getName());
				String[] albumsStringArray = new String[albumsAsStrings.size()];
				albumsStringArray = albumsAsStrings.toArray(albumsStringArray);
				return Response.ok(albumsStringArray).build();
			} else
				return Response.status(Status.NOT_FOUND).build();
		} catch (Exception e) {
			System.out.println("exceptiones");
		}
		return null;
	}

	@DELETE
	@Path("deletePicture/{albumName}/{pictureName}")
	public Response deletePicture(@PathParam("albumName") String albumName,
			@PathParam("pictureName") String pictureName) {
		File deletedPicture = new File(basePath, albumName + File.separator + pictureName);
		if (deletedPicture.exists() && deletedPicture.isFile()) {
			File del = new File(deletedPicture.getAbsolutePath() + ".deleted");
			if (del.exists() && del.isFile())
				deletedPicture.delete();
			deletedPicture.renameTo(del);
			return Response.ok().build();
		}
		return Response.status(Status.NOT_FOUND).build();
	}

	@POST
	@Path("uploadPicture/{albumName}/{pictureName}")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public Response uploadPicture(@PathParam("albumName") String albumName,
			@PathParam("pictureName") String pictureName, byte[] data) {
		FileOutputStream sOut;
		try {
			File f = new File(basePath, albumName + File.separator + pictureName);
			if (!f.exists()) {
				sOut = new FileOutputStream(f);
				sOut.write(data);
				sOut.close();

				return Response.ok().build();
			}
			return Response.status(Status.BAD_REQUEST).build();
		} catch (Exception e) {
			System.err.println("Error writing file.");
			e.printStackTrace();
			return Response.status(Status.EXPECTATION_FAILED).build();
		}
	}

}
