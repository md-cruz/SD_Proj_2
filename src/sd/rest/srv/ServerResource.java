package sd.rest.srv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Stack;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import sd.clt.ws.FileServerImplWS;

@Path("/RESTServer")
public class ServerResource {

	static File basePath;
	static Map<String, String> albumLogs;
	static Map<String, HashMap<String, String>> picLogs;

	// works
	@GET
	@Path("getPictureList/{albumName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getPictureList(@PathParam("albumName") String albumName) {

		String[] pics = getPicsList(albumName);
		if (pics != null)
			return Response.ok(pics).build();

		return Response.status(Status.NOT_FOUND).build();

	}

	private String[] getPicsList(String albumName) {

		File f = new File(basePath, albumName);
		if (f.exists() && f.isDirectory())
			return f.list();
		return null;
	}

	@POST
	@Path("createNewAlbum")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public Response createNewAlbum(String albumName) {
		boolean result = createNewAlb(albumName);
		if (result) {
			albumLogs.put(albumName, String.valueOf(System.currentTimeMillis()));
			return Response.ok(true).build();
		}
		return Response.status(Status.NOT_FOUND).build();

	}

	private boolean createNewAlb(String albumName) {
		File newAlbum = new File(basePath, albumName);
		boolean result = false;
		try {
			result = newAlbum.mkdirs();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return result;
	}

	@DELETE
	@Path("deleteAlbum/{albumName}")
	public Response deleteAlbum(@PathParam("albumName") String albumName) {
		boolean success = deleteAlb(albumName);
		if (success) {
			albumLogs.put(albumName, String.valueOf(System.currentTimeMillis()));
			return Response.ok().build();
		}
		return Response.status(Status.NOT_FOUND).build();

	}

	@GET
	@Path("albumLastModified/{album}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getAlbumLastModified(@PathParam("album") String album) {
		if (albumLogs.containsKey(album))
			return Response.ok(albumLogs.get(album)).build();
		return Response.status(Status.NOT_FOUND).build();
	}

	@GET
	@Path("picLogs/{album}/{picture}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response picLogs(@PathParam("album") String album, @PathParam("picture") String picture) {
		if (picLogs.containsKey(album)) {
			if (picLogs.get(album).containsKey(picture)) {
				String s = picLogs.get(album).get(picture);

				return Response.ok(s).build();

			}

		}
		return Response.status(Status.NOT_FOUND).build();
	}

	private boolean deleteAlb(String albumName) {

		boolean success = false;
		File deletedAlbum = new File(basePath, albumName);

		if (deletedAlbum.exists() && deletedAlbum.isDirectory()) {
			File del = new File(deletedAlbum.getAbsolutePath() + ".deleted");
			if (del.exists() && del.isDirectory()) {
				copyData(deletedAlbum, del);
				deletedAlbum.delete();

			} else
				deletedAlbum.renameTo(del);

			success = true;
		}
		return success;
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
		byte[] data = downloadPic(albumName, pictureName);
		if (data != null)
			return Response.ok(data).build();
		return Response.status(Status.NOT_FOUND).build();
	}

	private byte[] downloadPic(String albumName, String pictureName) {
		File pic = new File(basePath, albumName + File.separator + pictureName);
		if (pic.exists() && pic.isFile())
			try {
				return Files.readAllBytes(pic.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		return null;
	}

	@GET
	@Path("/getAlbumList/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAlbumList() {

		List<String> albList = getAlbList();
		if (!albList.isEmpty())
			return Response.ok(albList.toArray()).build();

		return Response.status(Status.NOT_FOUND).build();

	}

	private List<String> getAlbList() {
		File f = basePath;
		List<String> albumsAsStrings = new ArrayList<String>();
		if (f.exists() && f.isDirectory()) {
			File[] albums = f.listFiles();
			for (int i = 0; i < albums.length; i++)
				if (albums[i].isDirectory())
					albumsAsStrings.add(albums[i].getName());
		}
		return albumsAsStrings;
	}

	@DELETE
	@Path("deletePicture/{albumName}/{pictureName}")
	public Response deletePicture(@PathParam("albumName") String albumName,
			@PathParam("pictureName") String pictureName) {
		boolean success = deletePic(albumName, pictureName);
		if (success) {
			picLogs.get(albumName).put(pictureName, String.valueOf(System.currentTimeMillis()));

			return Response.ok().build();
		}
		return Response.status(Status.NOT_FOUND).build();
	}

	private boolean deletePic(String albumName, String pictureName) {
		boolean success = false;
		File deletedPicture = new File(basePath, albumName + File.separator + pictureName);
		if (deletedPicture.exists() && deletedPicture.isFile()) {
			File del = new File(deletedPicture.getAbsolutePath() + ".deleted");
			if (del.exists() && del.isFile())
				deletedPicture.delete();
			deletedPicture.renameTo(del);
			success = true;
		}
		return success;
	}

	@POST
	@Path("uploadPicture/{albumName}/{pictureName}")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public Response uploadPicture(@PathParam("albumName") String albumName,
			@PathParam("pictureName") String pictureName, byte[] data) {

		boolean success = uploadPic(albumName, pictureName, data);

		if (success) {
			picLogs.get(albumName).put(pictureName, String.valueOf(System.currentTimeMillis()));

			return Response.ok().build();
		}
		return Response.status(Status.BAD_REQUEST).build();

	}

	private boolean uploadPic(String albumName, String pictureName, byte[] data) {
		boolean success = false;

		FileOutputStream sOut;
		try {

			File f = new File(basePath, albumName + File.separator + pictureName);
			if (!f.exists()) {
				sOut = new FileOutputStream(f);
				sOut.write(data);
				sOut.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return success;
	}	

}
