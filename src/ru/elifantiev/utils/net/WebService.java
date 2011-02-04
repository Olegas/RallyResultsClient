package ru.elifantiev.utils.net;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

public abstract class WebService<R, T> {

    protected String login, password, baseUrl;

    public WebService(String _baseUrl, String _login, String _password) {
        baseUrl = _baseUrl;
        login = _login;
        password = _password;
    }

    abstract protected R transformInput(String inupt);
    abstract protected String transformOutput(T output);


    protected R callMethod(String method, Map<String, String> args) throws IOException {
        return transformInput(load(method, args));
    }

    protected R callMethod(String method, Map<String, String> args, T data) throws IOException {
        return transformInput(put(method, args, transformOutput(data)));
    }

    protected String load(String method) throws IOException {
        return load(method, new HashMap<String, String>());
    }

    protected String load(String method, Map<String, String> args) throws IOException {
        StringBuilder response = new StringBuilder();
        String line;

        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.getCredentialsProvider().setCredentials(
                new AuthScope(null, -1),
                new UsernamePasswordCredentials(login, password)
        );

        InputStream is = httpclient.execute(new HttpGet(buildStringUrl(method, args))).getEntity().getContent();
        BufferedReader rdr = new BufferedReader(new InputStreamReader(is));

        while((line = rdr.readLine()) != null)
            response.append(line);

        rdr.close();

        return response.toString();
    }

    protected String put(String method, Map<String, String> args, String data) throws IOException {
        StringBuilder response = new StringBuilder();
        String line;

        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.getCredentialsProvider().setCredentials(
                new AuthScope(null, -1),
                new UsernamePasswordCredentials(login, password)
        );

        HttpPost httpMethod = new HttpPost(buildStringUrl(method, args));
        httpMethod.setEntity(new StringEntity(data));

        InputStream is = httpclient.execute(httpMethod).getEntity().getContent();
        BufferedReader rdr = new BufferedReader(new InputStreamReader(is));

        while((line = rdr.readLine()) != null)
            response.append(line);

        rdr.close();

        return response.toString();
    }

    protected String buildStringUrl(String method, Map<String, String> args) throws MalformedURLException {

        StringBuilder bld = new StringBuilder(baseUrl);
        bld.append(method).append("/");

        for (String k : args.keySet()) {
            bld.append(k).append("/").append(args.get(k)).append("/");
        }

        return bld.toString();
    }

}
