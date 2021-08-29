package nextstep.jwp.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpResponse.class);

    private static final String DEFAULT_MESSAGE = "Hello world!";
    private static final String DEFAULT_RESOURCE_PATH = "static";

    private final OutputStream outputStream;
    private final HttpHeaders headers = new HttpHeaders(new LinkedHashMap<>());

    public HttpResponse(final OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void transfer(final String url) throws IOException {
        LOGGER.debug("url : {}", url);

        // favicon 오류 보기싫어서 일단 예외처리
        if (url.endsWith(".ico")) {
            return;
        }

        String responseBody;
        setContentType(url);
        try {
            if (url.equals("/")) {
                responseBody = DEFAULT_MESSAGE;
            } else if (url.equals("/login") || url.equals("/register")) {
                responseBody = getBodyByUrl(DEFAULT_RESOURCE_PATH + url + ".html");
            } else {
                responseBody = getBodyByUrl(DEFAULT_RESOURCE_PATH + url);
            }
        } catch (final NullPointerException nullPointerException) {
            redirectWithStatusCode("/404.html", "404");
            LOGGER.debug("잘못된 url로 접근했습니다. wrong url : {}", url);
            return;
        }
        response200(responseBody);
    }

    private void setContentType(final String url) {
        if (url.endsWith(".html")) {
            headers.put("Content-Type: text/html;charset=utf-8");
        } else if (url.endsWith(".css")) {
            headers.put("Content-Type: text/css");
        } else if (url.endsWith(".js")) {
            headers.put("Content-Type: application/javascript");
        } else if (url.endsWith("ico")) {
            headers.put("Content-Type: image/x-icon");
        } else if (url.endsWith("svg")) {
            headers.put("Content-Type: image/svg+xml");
        } else {
            headers.put("Content-Type: text/html;charset=utf-8");
        }
    }

    private String getBodyByUrl(final String url) throws IOException {
        String resourceUrl = Objects.requireNonNull(getClass().getClassLoader().getResource(url)).getPath();
        Path path = Paths.get(resourceUrl);
        return Files.readString(path);
    }

    private void response200(final String responseBody) throws IOException {
        headers.put("Content-Length: " + responseBody.getBytes().length);
        String okResponse = HttpStatusCodes._200.getStatusCodeHeader();
        outputStream.write(okResponse.getBytes(StandardCharsets.UTF_8));
        responseHeaderBody(responseBody);
        send();
    }

    public void redirect302Transfer(final String redirectUrl) throws IOException {
        String foundResponse = HttpStatusCodes._302.getStatusCodeHeader();
        outputStream.write(foundResponse.getBytes(StandardCharsets.UTF_8));
        headers.put("Location: " + redirectUrl);
        responseHeader();
        send();
    }

    public void redirectWithStatusCode(final String redirectUrl, final String statusCode) throws IOException {
        HttpStatusCodes httpStatusCodes = HttpStatusCodes.valueOf("_" + statusCode);
        outputStream.write(httpStatusCodes.getStatusCodeHeader().getBytes(StandardCharsets.UTF_8));
        String responseBody = getBodyByUrl(DEFAULT_RESOURCE_PATH + redirectUrl);
        responseHeaderBody(responseBody);
        send();
    }

    private void responseHeaderBody(final String responseBody) throws IOException {
        responseHeader();
        outputStream.write(responseBody.getBytes(StandardCharsets.UTF_8));
    }

    private void responseHeader() throws IOException {
        outputStream.write(headers.getAllHeaders().getBytes(StandardCharsets.UTF_8));
        outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private void send() throws IOException {
        outputStream.flush();
    }
}
