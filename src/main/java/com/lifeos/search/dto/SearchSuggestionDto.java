package com.lifeos.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Autocomplete / typeahead suggestion DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestionDto {

    private String text;

    private SearchEntityType entityType;

    private UUID entityId;
}
