package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "players")
public class Player {
    @Id
    private Long fideId;
    @Column(length = 100)
    private String name;
    @Column(length = 3, columnDefinition = "CHAR(3)")
    private String country;
    @Column(length = 1, columnDefinition = "CHAR(1)")
    private Character sex;
    @Column(length = 3, columnDefinition = "CHAR(3)")
    private String title;
    private Short birthday;
    @Column(name = "std_k")
    private Short stdK;
    @Column(name = "rapid_k")
    private Short rapidK;
    @Column(name = "blitz_k")
    private Short blitzK;
}
