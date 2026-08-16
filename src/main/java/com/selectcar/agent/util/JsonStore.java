package com.selectcar.agent.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selectcar.agent.config.PipelineProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads and writes the pipeline's local JSON files, resolving every path relative to the
 * configured data directory.
 */
@Component
public class JsonStore {

    private final ObjectMapper objectMapper;
    private final Path dataDir;

    public JsonStore(ObjectMapper objectMapper, PipelineProperties properties) {
        this.objectMapper = objectMapper;
        this.dataDir = Path.of(properties.getDataDir());
    }

    public Path resolve(String fileName) {
        return dataDir.resolve(fileName);
    }

    public boolean exists(String fileName) {
        return Files.exists(resolve(fileName));
    }

    public <T> T read(String fileName, Class<T> type) {
        try {
            return objectMapper.readValue(resolve(fileName).toFile(), type);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + fileName, e);
        }
    }

    public <T> T read(String fileName, TypeReference<T> type) {
        try {
            return objectMapper.readValue(resolve(fileName).toFile(), type);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + fileName, e);
        }
    }

    /** Reads a file as raw text, e.g. a JSON sample used verbatim inside a prompt. */
    public String readString(String fileName) {
        try {
            return Files.readString(resolve(fileName));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + fileName, e);
        }
    }

    /** Writes already-serialised JSON, re-formatting it when it parses. */
    public void writeString(String fileName, String content) {
        try {
            Path target = resolve(fileName);
            Files.createDirectories(target.getParent() == null ? dataDir : target.getParent());
            Files.writeString(target, prettify(content));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write " + fileName, e);
        }
    }

    private String prettify(String content) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(objectMapper.readTree(content));
        } catch (IOException e) {
            return content;
        }
    }

    public void write(String fileName, Object value) {
        try {
            Path target = resolve(fileName);
            Files.createDirectories(target.getParent() == null ? dataDir : target.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), value);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write " + fileName, e);
        }
    }
}
