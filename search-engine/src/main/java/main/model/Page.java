package main.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@ToString(exclude = {"content", "site"})
@Entity
@Table(name = "_page")
public class Page implements Serializable {
    @Serial
    private static final long serialVersionUID = 222L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "path")
    private String url;

    private int code;

    private String content;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private Site site;
}
