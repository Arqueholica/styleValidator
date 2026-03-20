package com.docstyler.profile.model;

public record ApprovedStyle(
    String styleName,
    String element,
    String resultedHTML,
    boolean fresh
) {}
