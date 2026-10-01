package com.example.backend.scim.controller;

/** Values the SCIM controller unit tests share. */
final class ScimTestUris {

    /**
     * An absolute SCIM base the rendered documents are located under. A host no request would
     * default to, so a document that ignored the base it was given could not pass by accident.
     */
    static final String BASE_URI = "https://scim.example.test/scim/v2";

    private ScimTestUris() {
    }
}
