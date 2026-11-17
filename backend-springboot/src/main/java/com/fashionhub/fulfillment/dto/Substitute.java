package com.fashionhub.fulfillment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Substitute(String damagedSku, String substituteSku, String substituteName) {
}
