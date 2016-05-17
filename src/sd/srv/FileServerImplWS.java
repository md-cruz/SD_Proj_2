package sd.srv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;

@WebService
public class FileServerImplWS {
	private static final String WSERVICE = "GiveMeYourIps";
	private File basePath;

	public FileServerImplWS() {
		this(".");
	}

	protected FileServerImplWS(String pathname) {
		super();
		basePath = new File(pathname);
	}

	@WebMethod
	public FileInfo getFileInfo(String path) throws InfoNotFoundException {
		File f = new File(basePath, path);
		if (f.exists())
			return new FileInfo(f.getName(), f.length(), new Date(f.lastModified()), f.isFile());
		else
			throw new InfoNotFoundException("File not found :" + path);
	}
	
	@WebMethod
	public String[] getPictureList (String albumPath) throws InfoNotFoundException {
		File f = new File(basePath, albumPath);
		if(f.exists() && f.isDirectory())
			return f.list();
		else
			throw new InfoNotFoundException("File not found :" + f.getAbsolutePath() + "\n basepath " + basePath.getAbsolutePath());		
	}
	
	
	@WebMethod
	public boolean createNewAlbum(String albumName) {
		File newAlbum = new File(basePath,albumName);
		try{
		return newAlbum.mkdirs();
		
		} catch (SecurityException e) {
			return false;
		}
		

	}
	
	@WebMethod
	public void deleteAlbum(String albumName) throws InfoNotFoundException {
		File deletedAlbum = new File(basePath,albumName);
		
		if(deletedAlbum.exists() && deletedAlbum.isDirectory()){
			File del = new File(deletedAlbum.getAbsolutePath() + ".deleted");
			if(del.exists() && del.isDirectory()){
				copyData(deletedAlbum,del);
				deletedAlbum.delete();
				
			}else
				deletedAlbum.renameTo(del);
		
		}
		else
			throw new InfoNotFoundException("Album not found :" );
	}
	
	@WebMethod
	public void deletePicture(String albumName, String pictureName) throws InfoNotFoundException {
		File deletedPicture = new File(basePath,albumName+File.separator+ pictureName);
		if(deletedPicture.exists() && deletedPicture.isFile()){
			File del = new File(deletedPicture.getAbsolutePath() + ".deleted");
			if(del.exists() && del.isFile())
				deletedPicture.delete();
			deletedPicture.renameTo(del);}
		else
			throw new InfoNotFoundException("Picture not found");
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
			System.out.println("Error copying contents");
		}
	}

	@WebMethod
	public byte[] downloadPicture (String albumName,String pictureName) throws InfoNotFoundException, IOException {
		File pic = new File(basePath,albumName+File.separator + pictureName);
		if(pic.exists() && pic.isFile())
			return Files.readAllBytes(pic.toPath()); 
		else
			throw new InfoNotFoundException("Picture not found");
	}
	
	@WebMethod
	// Possivelmente adicionar um parametro path?
	public String[] getAlbumList () throws InfoNotFoundException {
		File f = basePath;
		if(f.exists() && f.isDirectory()){
			File[] albums = f.listFiles(); 
			List<String> albumsAsStrings = new ArrayList<String>();
			for(int i =0; i<albums.length;i++)
				if(albums[i].isDirectory())
					albumsAsStrings.add(albums[i].getName());
			String[] albumsStringArray = new String[albumsAsStrings.size()];
			albumsStringArray = albumsAsStrings.toArray(albumsStringArray);
			return albumsStringArray;
		}
		else
			throw new InfoNotFoundException("File not found :" );
	}


	@WebMethod
	public byte[] getFile (String path) throws InfoNotFoundException, IOException {
		File f = new File(basePath,path);
		if(f.exists() && f.isFile()){
			FileInputStream sIn = new FileInputStream(f);
			byte[] info = new byte[(int) f.length()];
			sIn.read(info);
			sIn.close();
			return info;
		}
		else
			throw new InfoNotFoundException("File not found :" + path);
	}
	
	
	@WebMethod
	public boolean alive (){
		return true;
	}
	
	@WebMethod
	public void uploadPicture (String path, byte[] data) throws InfoNotFoundException,IOException, PictureExistsException {
		File f = new File(basePath, path);
		if (!f.exists()) {
			FileOutputStream sOut = new FileOutputStream(f);
			sOut.write(data);
			sOut.close();
		} else
			throw new PictureExistsException("No picture");
	}
	
	
	public static void main(String args[]) throws Exception {
		String path = args.length > 0 ? args[0] : ".";
		String url = "http://"+InetAddress.getLocalHost().getHostAddress() +":8080/FileServer";
		Endpoint.publish("http://0.0.0.0:8080/FileServer", new FileServerImplWS(path));
		System.err.println("FileServer started " + url);
		
		final String addr = "228.0.0.1";

		final InetAddress address = InetAddress.getByName(addr);
		MulticastSocket socket = new MulticastSocket(9000);
		socket.joinGroup(address);
		while (true) {
			byte[] buffer = new byte[65536];
			
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			socket.receive(packet);
			String s = new String(packet.getData()).trim();
			
			if (s.equalsIgnoreCase(WSERVICE)){
				
				byte[] data = ("S"+url).getBytes();
				DatagramPacket sendingPacket = new DatagramPacket(data,data.length);
				sendingPacket.setAddress(packet.getAddress());
				sendingPacket.setPort(packet.getPort());
				socket.send(sendingPacket);
			}
		}
	}
}
