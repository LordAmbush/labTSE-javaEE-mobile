package uy.com.orih.iduruguay;

public class Constants {
    //AUTH
    public static final String AUTHORIZATION_URL = "https://auth-testing.iduruguay.gub.uy/oidc/v1/authorize";
    public static final String ACCESSTOKEN_URL = "https://auth-testing.iduruguay.gub.uy/oidc/v1/token";
    public static final String USERINFO_URL = "https://auth-testing.iduruguay.gub.uy/oidc/v1/userinfo";
    public static final String LOGOUT_URL = "https://auth-testing.iduruguay.gub.uy/oidc/v1/logout";
    public static final String REDIRECT_URI = "https://openidconnect.net/callback";
    public static final String CLIENT_ID = "890192";
    public static final String CLIENT_SECRET = "457d52f181bf11804a3365b49ae4d29a2e03bbabe74997a2f510b179";


    //COMP CENTRARL
    //cambiar por la url que corresponda despues del deploy en elastic
    public static final String baseUrl = "http://10.0.2.2:8080/javaEE.lab2023-web/rest/api";
}
