package sd.rest.srv;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

@Path("/RESTProxy")
public class ProxyResource {

	static File basePath;
	static OAuth20Service service;
	static OAuth2AccessToken at;
	static Map<String, String> albumLogs;
	static Map<String, String> picLogs;

	private com.github.scribejava.core.model.Response buildReq(String url, Verb v, Map<String, String> params,boolean sign) {
		OAuthRequest request = new OAuthRequest(v, url, service);
		if (params != null)
			for (String key : params.keySet())
				request.addParameter(key, params.get(key));
		if(sign)
			service.signRequest(at, request);
		return request.send();
	}

	// funciona, atualiza aquando de um delete, mas nao de um upload
	@GET
	@Path("getPictureList/{albumID}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getPictureList(@PathParam("albumID") String albumID) {
		// garantir que e passado o albumID em vez do albumName
		// ou extrair o ID a partir do name
		String url = "https://api.imgur.com/3/album/" + albumID + "/images";
		com.github.scribejava.core.model.Response albumsRes = buildReq(url, Verb.GET, null,true);
		JSONObject res = null;
		System.out.println("get picture " +albumsRes.getCode() + " a " + albumID);
		List<String> pictureNames = new ArrayList<String>();
		String albumNameId = albumID+getAlbumName(albumID);
		try {
			JSONParser parser = new JSONParser();
			res = (JSONObject) parser.parse(albumsRes.getBody());
			JSONArray pictures = ((JSONArray) res.get("data"));
			
			Iterator picturesIt = pictures.iterator();
			while (picturesIt.hasNext()){
				Object picture = picturesIt.next();
				String nameAndId = ((JSONObject) picture).get("id") + "." + ((JSONObject) picture).get("title");
				if(!picLogs.containsKey(nameAndId)){
					picLogs.put(nameAndId, "create."+String.valueOf(((JSONObject) picture).get("datetime")));
					pictureNames.add(nameAndId);
				}
				else if(!picLogs.get(nameAndId).split("\\.")[0].equalsIgnoreCase("delete")){
					pictureNames.add(nameAndId);
				}
			}
			
			return Response.ok(pictureNames).build();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return Response.status(Status.NOT_FOUND).build();

	}

	private String getAlbumName(String albumID) {
		String url = "https://api.imgur.com/3/account/me/album/" + albumID;
		com.github.scribejava.core.model.Response albumsRes = buildReq(url, Verb.GET, null,true);
		JSONObject res = null;
		
		try {
			JSONParser parser = new JSONParser();

			res = (JSONObject) parser.parse(albumsRes.getBody());
			JSONObject album = (JSONObject) res.get("data");
			return ((String)((JSONObject) album).get("title"));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	// funciona, cria logo no imgur e na aplicacao
	@POST
	@Path("createNewAlbum")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public Response createNewAlbum(String albumName) {
		String url = "https://api.imgur.com/3/album";
		Map<String, String> params = new HashMap<String, String>();
		params.put("title", albumName);
		com.github.scribejava.core.model.Response createRes = buildReq(url, Verb.POST, params,true);
		boolean ok = 200 == createRes.getCode();
		System.out.println("create album " +createRes.getCode());
		if (ok){
			
			JSONParser parser = new JSONParser();

			JSONObject res;
			try {
			res = (JSONObject) parser.parse(createRes.getBody());
			
			JSONObject albums = (JSONObject) res.get("data");
			albumLogs.put(albums.get("id") + "." +albumName, "create."+String.valueOf(System.currentTimeMillis()));

			return Response.ok((String)albums.get("id")).build();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return Response.status(Status.NOT_FOUND).build();

	}

	// funciona, apaga logo no imgur
	@DELETE
	@Path("deleteAlbum/{albumID}")
	public Response deleteAlbum(@PathParam("albumID") String albumID) {
		String url = "https://api.imgur.com/3/account/me/album/" + albumID; 
		com.github.scribejava.core.model.Response delRes = buildReq(url, Verb.DELETE, null,true);
		System.out.println(url);
		boolean ok = 200 == delRes.getCode();
		System.out.println("delete " + delRes.getCode());
		// meter os deletes nos logs
		if (ok)
			return Response.ok().build();
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

	// TODO: fotografias com titulo geram EOF
	@GET
	@Path("downloadPicture/{pictureID}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadPicture(@PathParam("pictureID") String pictureID) {
		String url = "https://api.imgur.com/3/image/" + pictureID;
		com.github.scribejava.core.model.Response picRes = buildReq(url, Verb.GET, null,true);
		System.out.println(" DL pic " + picRes.getCode() + " id " + pictureID);
		try {
			JSONParser parser = new JSONParser();

			JSONObject obj = (JSONObject) parser.parse(picRes.getBody());
			JSONObject picture = (JSONObject) obj.get("data");
			com.github.scribejava.core.model.Response imgRes = buildReq((String) picture.get("link"),
					Verb.GET, null,false);
			//System.out.println((String) picture.get("link"));
		//	System.out.println(imgRes.getBody());
			
			//System.out.println(imgRes.getHeaders());
			int size = Integer.parseInt(imgRes.getHeader("Content-Length"));
			byte[] data = new byte[size];
			//System.out.println(size);
			DataInputStream dataStream = new DataInputStream(imgRes.getStream());
			dataStream.readFully(data);

			return Response.ok(data).build();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.status(Status.NOT_FOUND).build();
	}

	// TODO: descobrir porque não atualiza imediatamente após delete?
	// apenas atualiza neste caso
	// temos dois albums, apagamos um, criamos um album novo e apagamos tambem
	// o primeiro album desaparece da lista
	// mas ambos desaparecem do imgur
	@GET
	@Path("getAlbumList")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAlbumList() {
		String url = "https://api.imgur.com/3/account/me/albums/";
		com.github.scribejava.core.model.Response albumsRes = buildReq(url, Verb.GET, null,true);
		JSONObject res = null;
		System.out.println("list album " +albumsRes.getCode());
		List<String> albumNames = new ArrayList<String>();
		try {
			
			JSONParser parser = new JSONParser();

			res = (JSONObject) parser.parse(albumsRes.getBody());
			JSONArray albums = (JSONArray) res.get("data");
			Iterator albumsIt = albums.iterator();
			while (albumsIt.hasNext()){
				Object album = albumsIt.next();
				String nameAndId = ((JSONObject) album).get("id") + "." + ((JSONObject) album).get("title");
				if(!albumLogs.containsKey(nameAndId)){
					albumLogs.put(nameAndId, "create."+String.valueOf(((JSONObject) album).get("datetime")));
					albumNames.add(nameAndId);
				}
				
				else if(!albumLogs.get(nameAndId).split("\\.")[0].equalsIgnoreCase("delete")){
					albumNames.add(nameAndId);	
					System.out.println("alb " + nameAndId+albumLogs.get(nameAndId));}
				}
			return Response.ok(albumNames).build();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return Response.status(Status.NOT_FOUND).build();
	}
	// funciona
	@GET
	@Path("getAlbumId/{albumName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAlbumId(@PathParam("albumName") String albumName) {
		String albumId = getAlbId(albumName);
		if(albumId!=null)
			return Response.ok(albumId).build();
		return Response.status(Status.NOT_FOUND).build();
	}
	
	private String getAlbId(String albumName) {
		String url = "https://api.imgur.com/3/account/me/albums/";
		com.github.scribejava.core.model.Response albumsRes = buildReq(url, Verb.GET, null,true);
		JSONObject res = null;
		
		try {
			JSONParser parser = new JSONParser();

			res = (JSONObject) parser.parse(albumsRes.getBody());
			JSONArray albums = (JSONArray) res.get("data");
			for (Object album : albums)
				if(((String)((JSONObject) album).get("title")).equals(albumName))
					return ((String)((JSONObject) album).get("id"));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	// funciona?
	@GET
	@Path("getPictureId/{albumID}/{pictureName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getPictureID(@PathParam("albumID") String albumID
			,@PathParam("pictureName") String pictureName) {
		// garantir que e passado o albumID em vez do albumName
		// ou extrair o ID a partir do name
		String url = "https://api.imgur.com/3/album/" + albumID + "/images";
		com.github.scribejava.core.model.Response albumsRes = buildReq(url, Verb.GET, null,true);
		JSONObject res = null;
		
		try {
			JSONParser parser = new JSONParser();

			res = (JSONObject) parser.parse(albumsRes.getBody());
			JSONArray pictures = (JSONArray) ((JSONObject) res.get("data")).get("images");
			for (Object pic : pictures)
				if(((String)((JSONObject) pic).get("title")).equals(pictureName))
					return Response.ok(((String)((JSONObject) pic).get("id"))).build();

		} catch (ParseException e) {
			e.printStackTrace();
		}

		return Response.status(Status.NOT_FOUND).build();

	}

	// apaga corretamente no site
	@DELETE
	@Path("deletePicture/{pictureID}")
	public Response deletePicture(@PathParam("pictureID") String pictureID) {

		String url = "https://api.imgur.com/3/image/" + pictureID;
		com.github.scribejava.core.model.Response delRes = buildReq(url, Verb.DELETE,null,true);
		System.out.println(" del pic " + delRes.getCode());
		System.out.println(pictureID);
		boolean ok = 200 == delRes.getCode();
		if (ok){
			JSONParser parser = new JSONParser();
			JSONObject res;
			try {
				res = (JSONObject) parser.parse(delRes.getBody());
			
			JSONObject picture = ((JSONObject) res.get("data"));
			String nameAndId = pictureID + "." +(String) picture.get("title") ;
			picLogs.put(nameAndId, "delete."+String.valueOf(System.currentTimeMillis()));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			return Response.ok().build();
		}
		return Response.status(Status.NOT_FOUND).build();
	}

	

	// faz upload corretamente
	@POST
	@Path("uploadPicture/{albumName}/{pictureName}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public Response uploadPicture(@PathParam("albumName") String albumName,
			@PathParam("pictureName") String pictureName, byte[] data) {

		String url = "https://api.imgur.com/3/upload"; 
		Map<String,String> params = new HashMap<String,String>();
		params.put("image", Base64.encodeBase64String(data));
		params.put("album", albumName);
		params.put("title", pictureName);
		com.github.scribejava.core.model.Response createRes = buildReq(url, Verb.POST,params,true);
		System.out.println("a " + albumName + " p " + pictureName);
		/*OAuthRequest request = new OAuthRequest(Verb.POST, url, service);
		request.addPayload(data);

		request.addQuerystringParameter("album",albumName);
		request.addQuerystringParameter("title", pictureName);
		
		service.signRequest(at, request);
		com.github.scribejava.core.model.Response r = request.send();*/
		System.out.println(" upload pic " + createRes.getCode());
		System.out.println(createRes.getBody());
		boolean ok = 200 == createRes.getCode();
		if (ok){
			JSONParser parser = new JSONParser();
			JSONObject res;
			try {
				res = (JSONObject) parser.parse(createRes.getBody());
			
			JSONObject picture = ((JSONObject) res.get("data"));
			String nameAndId = (String) picture.get("id") + "."+pictureName;
			picLogs.put(nameAndId,"create."+ String.valueOf(System.currentTimeMillis()));
			return Response.ok(nameAndId).build();
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
		}
		return Response.status(Status.BAD_REQUEST).build();
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
	@Path("picLogs/{picture}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response picLogs( @PathParam("picture") String picture) {
		if (picLogs.containsKey(picture)) 
			return Response.ok(picLogs.get(picture)).build();	
		return Response.status(Status.NOT_FOUND).build();
	}

}
