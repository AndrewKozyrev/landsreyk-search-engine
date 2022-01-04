package main.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "_lemma")
public class Word implements Serializable {
    @Serial
    private static final long serialVersionUID = 333L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "lemma")
    private String name;

    private int frequency;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private Site site;

    @Transient
    private float rank;
}
