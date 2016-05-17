package sd.rest.srv;

import com.github.scribejava.apis.ImgurApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import java.util.Iterator;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


/**
 * Exemplo de acesso ao servico Imgur.
 * <p>
 * O URL base para programadores esta disponivel em: <br>
 * https://api.imgur.com/
 * <p> 
 * A API REST do sistema esta disponivel em: <br>
 * https://api.imgur.com/endpoints
 * <p>
 * Para poder aceder ao servico Imgur, deve criar uma app em:
 * https://api.imgur.com/oauth2/addclient onde obtera a apiKey e a apiSecret a
 * usar na criacao do objecto OAuthService.
 * Deve use a opcao: OAuth 2 authorization without a callback URL
 * <p>
 * Este exemplo usa a biblioteca OAuth Scribe, disponivel em:
 * https://github.com/scribejava/scribejava
 * Para descarregar a biblioteca deve descarregar o jar do core:
 * http://mvnrepository.com/artifact/com.github.scribejava/scribejava-core
 * e da API
 * http://mvnrepository.com/artifact/com.github.scribejava/scribejava-apis
 * <p>
 * e a biblioteca json-simple, disponivel em:
 * http://code.google.com/p/json-simple/
 * <p>
 * e a biblioteca apache commons codec, disponivel em:
 * http://commons.apache.org/proper/commons-codec/
 */
public abstract class ImgurExample
{

	private static final String SERVICE_NAME = "Imgur";

	public static void main(String... args) {
		try {
			// Substituir pela API key atribuida
			final String apiKey = "87d56e838ce5413"; 
			// Substituir pelo API secret atribuido
			final String apiSecret = "b5ed4dadbc629cfd1058c678d10a795f9dbcb5a9"; 
			
			final OAuth20Service service = new ServiceBuilder().apiKey(apiKey).apiSecret(apiSecret)
					.build(ImgurApi.instance());
			final Scanner in = new Scanner(System.in);

			// Obtain the Authorization URL
			System.out.println("A obter o Authorization URL...");
			final String authorizationUrl = service.getAuthorizationUrl();
			System.out.println("Necessario dar permissao neste URL:");
			System.out.println(authorizationUrl);
			System.out.println("e copiar o codigo obtido para aqui:");
			System.out.print(">>");
			final String code = in.nextLine();

			// Trade the Request Token and Verifier for the Access Token
			System.out.println("A obter o Access Token!");
			final OAuth2AccessToken accessToken = service.getAccessToken(code);

			// Ready to execute operations
			System.out.println("Agora vamos aceder aos albuns dum utilizador...");
			OAuthRequest albumsReq = new OAuthRequest(Verb.GET,
					"https://api.imgur.com/3/account/nunopreguica/albums/ids", service);
			service.signRequest(accessToken, albumsReq);
			final Response albumsRes = albumsReq.send();
			System.out.println(albumsRes.getCode());

			JSONParser parser = new JSONParser();
			JSONObject res = (JSONObject) parser.parse(albumsRes.getBody());

			JSONArray albums = (JSONArray) res.get("data");
			Iterator albumsIt = albums.iterator();
			while (albumsIt.hasNext()) {
				System.out.println( "id : " + albumsIt.next()); 
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}