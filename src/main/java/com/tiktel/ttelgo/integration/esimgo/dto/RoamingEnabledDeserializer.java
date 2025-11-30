package com.tiktel.ttelgo.integration.esimgo.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Iterator;

public class RoamingEnabledDeserializer extends JsonDeserializer<Boolean> {
    
    @Override
    public Boolean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        // If it's a boolean, return it directly
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        
        // If it's an array, check if it has any true values
        if (node.isArray()) {
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                JsonNode element = elements.next();
                if (element.isBoolean() && element.asBoolean()) {
                    return true;
                }
                // Also handle string "true" values
                if (element.isTextual() && "true".equalsIgnoreCase(element.asText())) {
                    return true;
                }
            }
            return false;
        }
        
        // If it's a string, try to parse it
        if (node.isTextual()) {
            String text = node.asText();
            return "true".equalsIgnoreCase(text) || "1".equals(text);
        }
        
        // Default to false if we can't determine
        return false;
    }
}

