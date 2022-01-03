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
@Table(name = "_index")
public class Index implements Serializable {
    @Serial
    private static final long serialVersionUID = 111L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne
    @JoinColumn(name = "lemma_id")
    private Word word;

    @ManyToOne
    @JoinColumn(name = "page_id")
    private Page page;

    @Column(name = "`rank`")
    private float rank;
}
