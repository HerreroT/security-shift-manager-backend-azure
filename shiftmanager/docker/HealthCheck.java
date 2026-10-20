import java.net.HttpURLConnection;
import java.net.URI;

/**
 * Docker HEALTHCHECK for the distroless image, which has no shell, curl or wget.
 * Exits 0 when the application's health endpoint answers 200, 1 otherwise.
 */
public class HealthCheck {

    public static void main(String[] args) {
        String port = System.getenv().getOrDefault("PORT", "8090");
        try {
            HttpURLConnection connection = (HttpURLConnection) URI
                    .create("http://localhost:" + port + "/actuator/health").toURL().openConnection();
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(3000);
            System.exit(connection.getResponseCode() == 200 ? 0 : 1);
        } catch (Exception e) {
            System.exit(1);
        }
    }
}
