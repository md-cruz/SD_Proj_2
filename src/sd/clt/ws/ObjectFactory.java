
package sd.clt.ws;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the sd.clt.ws package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _DownloadPictureResponse_QNAME = new QName("http://srv.sd/", "downloadPictureResponse");
    private final static QName _GetFile_QNAME = new QName("http://srv.sd/", "getFile");
    private final static QName _IOException_QNAME = new QName("http://srv.sd/", "IOException");
    private final static QName _DownloadPicture_QNAME = new QName("http://srv.sd/", "downloadPicture");
    private final static QName _DeletePicture_QNAME = new QName("http://srv.sd/", "deletePicture");
    private final static QName _InfoNotFoundException_QNAME = new QName("http://srv.sd/", "InfoNotFoundException");
    private final static QName _GetFileResponse_QNAME = new QName("http://srv.sd/", "getFileResponse");
    private final static QName _UploadPicture_QNAME = new QName("http://srv.sd/", "uploadPicture");
    private final static QName _Alive_QNAME = new QName("http://srv.sd/", "alive");
    private final static QName _GetAlbumListResponse_QNAME = new QName("http://srv.sd/", "getAlbumListResponse");
    private final static QName _GetFileInfoResponse_QNAME = new QName("http://srv.sd/", "getFileInfoResponse");
    private final static QName _DeleteAlbumResponse_QNAME = new QName("http://srv.sd/", "deleteAlbumResponse");
    private final static QName _CreateNewAlbum_QNAME = new QName("http://srv.sd/", "createNewAlbum");
    private final static QName _PictureExistsException_QNAME = new QName("http://srv.sd/", "PictureExistsException");
    private final static QName _AliveResponse_QNAME = new QName("http://srv.sd/", "aliveResponse");
    private final static QName _GetPictureList_QNAME = new QName("http://srv.sd/", "getPictureList");
    private final static QName _GetAlbumList_QNAME = new QName("http://srv.sd/", "getAlbumList");
    private final static QName _GetFileInfo_QNAME = new QName("http://srv.sd/", "getFileInfo");
    private final static QName _DeletePictureResponse_QNAME = new QName("http://srv.sd/", "deletePictureResponse");
    private final static QName _GetPictureListResponse_QNAME = new QName("http://srv.sd/", "getPictureListResponse");
    private final static QName _CreateNewAlbumResponse_QNAME = new QName("http://srv.sd/", "createNewAlbumResponse");
    private final static QName _DeleteAlbum_QNAME = new QName("http://srv.sd/", "deleteAlbum");
    private final static QName _UploadPictureResponse_QNAME = new QName("http://srv.sd/", "uploadPictureResponse");
    private final static QName _DownloadPictureResponseReturn_QNAME = new QName("", "return");
    private final static QName _UploadPictureArg1_QNAME = new QName("", "arg1");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: sd.clt.ws
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link CreateNewAlbum }
     * 
     */
    public CreateNewAlbum createCreateNewAlbum() {
        return new CreateNewAlbum();
    }

    /**
     * Create an instance of {@link PictureExistsException }
     * 
     */
    public PictureExistsException createPictureExistsException() {
        return new PictureExistsException();
    }

    /**
     * Create an instance of {@link AliveResponse }
     * 
     */
    public AliveResponse createAliveResponse() {
        return new AliveResponse();
    }

    /**
     * Create an instance of {@link DeleteAlbumResponse }
     * 
     */
    public DeleteAlbumResponse createDeleteAlbumResponse() {
        return new DeleteAlbumResponse();
    }

    /**
     * Create an instance of {@link Alive }
     * 
     */
    public Alive createAlive() {
        return new Alive();
    }

    /**
     * Create an instance of {@link GetAlbumListResponse }
     * 
     */
    public GetAlbumListResponse createGetAlbumListResponse() {
        return new GetAlbumListResponse();
    }

    /**
     * Create an instance of {@link GetFileInfoResponse }
     * 
     */
    public GetFileInfoResponse createGetFileInfoResponse() {
        return new GetFileInfoResponse();
    }

    /**
     * Create an instance of {@link InfoNotFoundException }
     * 
     */
    public InfoNotFoundException createInfoNotFoundException() {
        return new InfoNotFoundException();
    }

    /**
     * Create an instance of {@link GetFileResponse }
     * 
     */
    public GetFileResponse createGetFileResponse() {
        return new GetFileResponse();
    }

    /**
     * Create an instance of {@link UploadPicture }
     * 
     */
    public UploadPicture createUploadPicture() {
        return new UploadPicture();
    }

    /**
     * Create an instance of {@link DeletePicture }
     * 
     */
    public DeletePicture createDeletePicture() {
        return new DeletePicture();
    }

    /**
     * Create an instance of {@link IOException }
     * 
     */
    public IOException createIOException() {
        return new IOException();
    }

    /**
     * Create an instance of {@link DownloadPicture }
     * 
     */
    public DownloadPicture createDownloadPicture() {
        return new DownloadPicture();
    }

    /**
     * Create an instance of {@link GetFile }
     * 
     */
    public GetFile createGetFile() {
        return new GetFile();
    }

    /**
     * Create an instance of {@link DownloadPictureResponse }
     * 
     */
    public DownloadPictureResponse createDownloadPictureResponse() {
        return new DownloadPictureResponse();
    }

    /**
     * Create an instance of {@link UploadPictureResponse }
     * 
     */
    public UploadPictureResponse createUploadPictureResponse() {
        return new UploadPictureResponse();
    }

    /**
     * Create an instance of {@link CreateNewAlbumResponse }
     * 
     */
    public CreateNewAlbumResponse createCreateNewAlbumResponse() {
        return new CreateNewAlbumResponse();
    }

    /**
     * Create an instance of {@link DeleteAlbum }
     * 
     */
    public DeleteAlbum createDeleteAlbum() {
        return new DeleteAlbum();
    }

    /**
     * Create an instance of {@link GetPictureListResponse }
     * 
     */
    public GetPictureListResponse createGetPictureListResponse() {
        return new GetPictureListResponse();
    }

    /**
     * Create an instance of {@link DeletePictureResponse }
     * 
     */
    public DeletePictureResponse createDeletePictureResponse() {
        return new DeletePictureResponse();
    }

    /**
     * Create an instance of {@link GetFileInfo }
     * 
     */
    public GetFileInfo createGetFileInfo() {
        return new GetFileInfo();
    }

    /**
     * Create an instance of {@link GetPictureList }
     * 
     */
    public GetPictureList createGetPictureList() {
        return new GetPictureList();
    }

    /**
     * Create an instance of {@link GetAlbumList }
     * 
     */
    public GetAlbumList createGetAlbumList() {
        return new GetAlbumList();
    }

    /**
     * Create an instance of {@link FileInfo }
     * 
     */
    public FileInfo createFileInfo() {
        return new FileInfo();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DownloadPictureResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "downloadPictureResponse")
    public JAXBElement<DownloadPictureResponse> createDownloadPictureResponse(DownloadPictureResponse value) {
        return new JAXBElement<DownloadPictureResponse>(_DownloadPictureResponse_QNAME, DownloadPictureResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetFile }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "getFile")
    public JAXBElement<GetFile> createGetFile(GetFile value) {
        return new JAXBElement<GetFile>(_GetFile_QNAME, GetFile.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IOException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "IOException")
    public JAXBElement<IOException> createIOException(IOException value) {
        return new JAXBElement<IOException>(_IOException_QNAME, IOException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DownloadPicture }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "downloadPicture")
    public JAXBElement<DownloadPicture> createDownloadPicture(DownloadPicture value) {
        return new JAXBElement<DownloadPicture>(_DownloadPicture_QNAME, DownloadPicture.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DeletePicture }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "deletePicture")
    public JAXBElement<DeletePicture> createDeletePicture(DeletePicture value) {
        return new JAXBElement<DeletePicture>(_DeletePicture_QNAME, DeletePicture.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InfoNotFoundException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "InfoNotFoundException")
    public JAXBElement<InfoNotFoundException> createInfoNotFoundException(InfoNotFoundException value) {
        return new JAXBElement<InfoNotFoundException>(_InfoNotFoundException_QNAME, InfoNotFoundException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetFileResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "getFileResponse")
    public JAXBElement<GetFileResponse> createGetFileResponse(GetFileResponse value) {
        return new JAXBElement<GetFileResponse>(_GetFileResponse_QNAME, GetFileResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UploadPicture }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "uploadPicture")
    public JAXBElement<UploadPicture> createUploadPicture(UploadPicture value) {
        return new JAXBElement<UploadPicture>(_UploadPicture_QNAME, UploadPicture.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Alive }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "alive")
    public JAXBElement<Alive> createAlive(Alive value) {
        return new JAXBElement<Alive>(_Alive_QNAME, Alive.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAlbumListResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "getAlbumListResponse")
    public JAXBElement<GetAlbumListResponse> createGetAlbumListResponse(GetAlbumListResponse value) {
        return new JAXBElement<GetAlbumListResponse>(_GetAlbumListResponse_QNAME, GetAlbumListResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetFileInfoResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "getFileInfoResponse")
    public JAXBElement<GetFileInfoResponse> createGetFileInfoResponse(GetFileInfoResponse value) {
        return new JAXBElement<GetFileInfoResponse>(_GetFileInfoResponse_QNAME, GetFileInfoResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DeleteAlbumResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "deleteAlbumResponse")
    public JAXBElement<DeleteAlbumResponse> createDeleteAlbumResponse(DeleteAlbumResponse value) {
        return new JAXBElement<DeleteAlbumResponse>(_DeleteAlbumResponse_QNAME, DeleteAlbumResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CreateNewAlbum }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "createNewAlbum")
    public JAXBElement<CreateNewAlbum> createCreateNewAlbum(CreateNewAlbum value) {
        return new JAXBElement<CreateNewAlbum>(_CreateNewAlbum_QNAME, CreateNewAlbum.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PictureExistsException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "PictureExistsException")
    public JAXBElement<PictureExistsException> createPictureExistsException(PictureExistsException value) {
        return new JAXBElement<PictureExistsException>(_PictureExistsException_QNAME, PictureExistsException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AliveResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "aliveResponse")
    public JAXBElement<AliveResponse> createAliveResponse(AliveResponse value) {
        return new JAXBElement<AliveResponse>(_AliveResponse_QNAME, AliveResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetPictureList }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "getPictureList")
    public JAXBElement<GetPictureList> createGetPictureList(GetPictureList value) {
        return new JAXBElement<GetPictureList>(_GetPictureList_QNAME, GetPictureList.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAlbumList }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "getAlbumList")
    public JAXBElement<GetAlbumList> createGetAlbumList(GetAlbumList value) {
        return new JAXBElement<GetAlbumList>(_GetAlbumList_QNAME, GetAlbumList.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetFileInfo }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "getFileInfo")
    public JAXBElement<GetFileInfo> createGetFileInfo(GetFileInfo value) {
        return new JAXBElement<GetFileInfo>(_GetFileInfo_QNAME, GetFileInfo.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DeletePictureResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "deletePictureResponse")
    public JAXBElement<DeletePictureResponse> createDeletePictureResponse(DeletePictureResponse value) {
        return new JAXBElement<DeletePictureResponse>(_DeletePictureResponse_QNAME, DeletePictureResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetPictureListResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "getPictureListResponse")
    public JAXBElement<GetPictureListResponse> createGetPictureListResponse(GetPictureListResponse value) {
        return new JAXBElement<GetPictureListResponse>(_GetPictureListResponse_QNAME, GetPictureListResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CreateNewAlbumResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "createNewAlbumResponse")
    public JAXBElement<CreateNewAlbumResponse> createCreateNewAlbumResponse(CreateNewAlbumResponse value) {
        return new JAXBElement<CreateNewAlbumResponse>(_CreateNewAlbumResponse_QNAME, CreateNewAlbumResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DeleteAlbum }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "deleteAlbum")
    public JAXBElement<DeleteAlbum> createDeleteAlbum(DeleteAlbum value) {
        return new JAXBElement<DeleteAlbum>(_DeleteAlbum_QNAME, DeleteAlbum.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UploadPictureResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://srv.sd/", name = "uploadPictureResponse")
    public JAXBElement<UploadPictureResponse> createUploadPictureResponse(UploadPictureResponse value) {
        return new JAXBElement<UploadPictureResponse>(_UploadPictureResponse_QNAME, UploadPictureResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link byte[]}{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "return", scope = DownloadPictureResponse.class)
    public JAXBElement<byte[]> createDownloadPictureResponseReturn(byte[] value) {
        return new JAXBElement<byte[]>(_DownloadPictureResponseReturn_QNAME, byte[].class, DownloadPictureResponse.class, ((byte[]) value));
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link byte[]}{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "arg1", scope = UploadPicture.class)
    public JAXBElement<byte[]> createUploadPictureArg1(byte[] value) {
        return new JAXBElement<byte[]>(_UploadPictureArg1_QNAME, byte[].class, UploadPicture.class, ((byte[]) value));
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link byte[]}{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "return", scope = GetFileResponse.class)
    public JAXBElement<byte[]> createGetFileResponseReturn(byte[] value) {
        return new JAXBElement<byte[]>(_DownloadPictureResponseReturn_QNAME, byte[].class, GetFileResponse.class, ((byte[]) value));
    }

}
