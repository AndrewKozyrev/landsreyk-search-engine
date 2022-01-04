package main.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "_field")
public class Field {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    private String selector;

    private float weight;
}
