package com.dbtraining.reconx.repository.entity;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** TICKET-ADV071 — stamps each Envers revision with the authenticated principal's name. */
public class CustomRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
        ((CustomRevisionEntity) revisionEntity).setUsername(username);
    }
}
