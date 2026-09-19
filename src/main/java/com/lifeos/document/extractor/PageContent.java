package com.lifeos.document.extractor;

/**
 * Encapsulates the extracted text for a specific physical or logical page.
 *
 * @param pageNumber 1-based page number
 * @param text extracted text for the page
 */
public record PageContent(int pageNumber, String text) {}
