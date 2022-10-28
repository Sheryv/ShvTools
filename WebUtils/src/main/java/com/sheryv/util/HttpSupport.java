package com.sheryv.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpSupport {
  private final HttpClient client;
  
  public HttpSupport(HttpClient client) {
    this.client = client;
  }
  
  public HttpSupport() {
    this.client = HttpClient.newHttpClient();
  }
  
  
  public String sendGet(String url) {
    try {
      var request = HttpRequest.newBuilder(new URL(url).toURI()).GET().build();
      return send(request);
    } catch (URISyntaxException | IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public String sendPost(String url, String body) {
    try {
      var request = HttpRequest.newBuilder(new URL(url).toURI()).POST(HttpRequest.BodyPublishers.ofString(body)).build();
      return send(request);
    } catch (URISyntaxException | IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public String send(HttpRequest request) {
    try {
      return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
  
  public InputStream stream(HttpRequest request) {
    try {
      return client.send(request, HttpResponse.BodyHandlers.ofInputStream()).body();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
