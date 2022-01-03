package main.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.StringJoiner;

@Getter
@Setter
public class MatchedPage implements Serializable {

  @Serial
  private static final long serialVersionUID = 555L;
  private String site;
  private String siteName;
  private String url;
  private String title;
  private String snippet;
  private float relevance;

  @Override
  public String toString() {
    return new StringJoiner(System.lineSeparator())
        .add("=".repeat(100))
        .add("site: " + site)
        .add("siteName: " + siteName)
        .add("uri: " + url)
        .add("title: " + title)
        .add("snippet: " + snippet)
        .add("relevance: " + relevance)
        .add("=".repeat(100))
        .toString();
  }
}
