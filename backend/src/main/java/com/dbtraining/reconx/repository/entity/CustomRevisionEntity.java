package com.dbtraining.reconx.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 * TICKET-ADV071 — custom Envers revision row: adds the acting username on
 * top of the standard rev/revtstmp columns from 009-envers-revinfo.xml.
 *
 * Defined standalone (not extending DefaultRevisionEntity) with an explicit
 * IDENTITY generator so it maps onto the plain autoIncrement "rev" column
 * Liquibase already created — DefaultRevisionEntity's inherited @GeneratedValue
 * resolves to a sequence ("revinfo_seq") that was never provisioned.
 */
@Entity
@Table(name = "revinfo")
@RevisionEntity(CustomRevisionListener.class)
public class CustomRevisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name = "rev")
    private long id;

    @RevisionTimestamp
    @Column(name = "revtstmp")
    private long timestamp;

    @Column(name = "username")
    private String username;

    public long getId() { return id; }
    public long getTimestamp() { return timestamp; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
