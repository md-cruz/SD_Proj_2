package sd.rest.srv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;


@Path("/RESTServer")
public class ProxyResource {
	
	static File basePath;
	
	
	private com.github.scribejava.core.model.Response buildReq(String url, OAuth20Service service, OAuth2AccessToken at, Verb v){
		OAuthRequest request = new OAuthRequest(v, url, service);
		service.signRequest(at, request);
		return request.send();
	}
	
	
	
	
	//works
	@GET
	@Path("getPictureList/{albumName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getPictureList(@PathParam("albumName") String albumName,OAuth20Service service, OAuth2AccessToken at){
		// fazer pedido a proxy para listar os albums do user
		String url = ""; // TODO: preencher url
		com.github.scribejava.core.model.Response albumsRes = buildReq(url,service,at, Verb.GET);
		JSONObject res = null;
		List<String> albumNames = new ArrayList<String>();
		try {
			JSONParser parser = new JSONParser();
	
			res = (JSONObject) parser.parse(albumsRes.getBody());
			JSONArray albums = (JSONArray) res.get("data");
			for(Object album: albums)
				albumNames.add(((JSONObject) album).get("id") +""+((JSONObject) album).get("title") );
			
			return Response.ok(albumNames).build();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return Response.status(Status.NOT_FOUND).build();
			
		
	}
	
	@POST
	@Path("createNewAlbum")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public Response createNewAlbum(String albumName){
		// criar um novo album vazio atraves da proxy
		File newAlbum = new File(basePath,albumName);
		boolean result = false;
		try{
			result = newAlbum.mkdirs();
		}catch(SecurityException e){
			
		}
		if(result)
			return Response.ok(true).build();
		return Response.status(Status.NOT_FOUND).build();
 
	}
	
	@DELETE
	@Path("deleteAlbum/{albumName}")
	public Response deleteAlbum(@PathParam("albumName")String albumName)  {
		
		File deletedAlbum = new File(basePath,albumName);
		if(deletedAlbum.exists() && deletedAlbum.isDirectory()){
			File del = new File(deletedAlbum.getAbsolutePath() + ".deleted");
			if(del.exists() && del.isDirectory()){
				copyData(deletedAlbum,del);
				deletedAlbum.delete();
				
			}else
				deletedAlbum.renameTo(del);
			
			return Response.ok().build();
		}
		return Response.status(Status.NOT_FOUND).build();
		
	}
	
	private void copyData(File deletedPicture, File del) {
		try {
		for(File fileName : deletedPicture.listFiles()){
			
			byte[] contents = Files.readAllBytes(fileName.toPath());
			
			
			FileOutputStream fis = new FileOutputStream(new File(del.getAbsolutePath(),fileName.getName()));
			fis.write(contents);
			fileName.delete();
			fis.close();
		}
		}catch (Exception e){
			System.err.println("Error copying contents");
		}
	}
	
	@GET
	@Path("downloadPicture/{albumName}/{pictureName}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadPicture (@PathParam("albumName")String albumName,@PathParam("pictureName")String pictureName){
		File pic = new File(basePath,albumName+File.separator + pictureName);
		if(pic.exists() && pic.isFile())
			try {
				return Response.ok(Files.readAllBytes(pic.toPath())).build();
			} catch (IOException e) {
				System.err.println("Error reading file.");
				return Response.status(Status.EXPECTATION_FAILED).build();
			} 
		return Response.status(Status.NOT_FOUND).build();
	}
	
	
	@GET
	@Path("/getAlbumList/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAlbumList ()  {
		try{
		File f = basePath;
		if(f.exists() && f.isDirectory()){
			File[] albums = f.listFiles(); 
			List<String> albumsAsStrings = new ArrayList<String>();
			for(int i =0; i<albums.length;i++)
				if(albums[i].isDirectory())
					albumsAsStrings.add(albums[i].getName());
			String[] albumsStringArray = new String[albumsAsStrings.size()];
			albumsStringArray = albumsAsStrings.toArray(albumsStringArray);
			return Response.ok(albumsStringArray).build();
		}
		else
			return Response.status(Status.NOT_FOUND).build();
		}catch(Exception e){
			System.out.println("exceptiones");
		}
		return null;
	}
	@DELETE
	@Path("deletePicture/{albumName}/{pictureName}")
	public Response deletePicture(@PathParam("albumName")String albumName,@PathParam("pictureName")String pictureName) {
		File deletedPicture = new File(basePath,albumName+File.separator + pictureName);
		if(deletedPicture.exists() && deletedPicture.isFile()){
			File del = new File(deletedPicture.getAbsolutePath() + ".deleted");
			if(del.exists() && del.isFile())
				deletedPicture.delete();
			deletedPicture.renameTo(del);
			return Response.ok().build();
		}
		return Response.status(Status.NOT_FOUND).build();
	}
	
	@POST
	@Path("uploadPicture/{albumName}/{pictureName}")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public Response uploadPicture (@PathParam("albumName")String albumName,
			@PathParam("pictureName")String pictureName,byte[] data)  {
		FileOutputStream sOut;
		try {
		File f = new File(basePath,albumName+File.separator+pictureName);
		if(!f.exists()){
		sOut = new FileOutputStream(f);
		sOut.write(data);
		sOut.close();
		
		return Response.ok().build();}
		return Response.status(Status.BAD_REQUEST).build();
		} catch (Exception e) {
			System.err.println("Error writing file.");
			e.printStackTrace();
			return Response.status(Status.EXPECTATION_FAILED).build();
		}
	}
	

}
