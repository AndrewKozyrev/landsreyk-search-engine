package main.model;

import java.io.Serial;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@ToString
@Entity
@Table(name = "_site")
public class Site implements Serializable {
    @Serial
    private static final long serialVersionUID = 444L;

    @Column(nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Column(name = "status_time")
    private Timestamp statusTime;

    @Column(name = "last_error")
    private String lastError;

    private String url;

    private String name;

    public enum Status {
        INDEXING, INDEXED, FAILED
    }
}
