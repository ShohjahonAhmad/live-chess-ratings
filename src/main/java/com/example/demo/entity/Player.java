package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement(name = "player")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "players")
public class Player {

    @XmlElement(name = "fideid")
    @Id
    private Long fideId;

    @XmlElement(name = "name")
    @Column(length = 100)
    private String name;

    @XmlElement(name = "country")
    @Column(length = 3, columnDefinition = "CHAR(3)")
    private String country;

    @XmlElement(name = "sex")
    @Column(length = 1, columnDefinition = "CHAR(1)")
    private Character sex;

    @XmlElement(name = "title")
    @Column(length = 3, columnDefinition = "CHAR(3)")
    private String title;

    @XmlElement(name = "birthday")
    private Short birthday;

    @XmlElement(name = "flag")
    @Column(length = 2, columnDefinition = "CHAR(2)")
    private String flag;

    @XmlElement(name = "k")
    @Column(name = "std_k")
    private Short stdK;

    @XmlElement(name = "rapid_k")
    @Column(name = "rapid_k")
    private Short rapidK;

    @XmlElement(name = "blitz_k")
    @Column(name = "blitz_k")
    private Short blitzK;

    // temporary fields
    @XmlElement(name = "rating")
    @Transient
    private Short stdRating;

    @XmlElement(name = "rapid_rating")
    @Transient
    private Short rapidRating;

    @XmlElement(name = "blitz_rating")
    @Transient
    private Short blitzRating;
}
